# Using Kafka Connect with Strimzi

## 0. Prerequisits

Have Strimzi installed with a Kafka cluster called `first-cluster`.
Quick-start:
```
kubectl create namespace kafka
kubectl create -f 'https://strimzi.io/install/latest?namespace=kafka' -n kafka
kubectl apply -f kafka-cluster.yaml -n kafka 
```


## 1. Start a Connect Cluster

Strimzi has a dedicated CRD for Connect clusters. An example configuration looks like this:
```
apiVersion: kafka.strimzi.io/v1beta2
kind: KafkaConnect
metadata:
  annotations:
  # use-connector-resources configures this KafkaConnect
  # to use KafkaConnector resources to avoid
  # needing to call the Connect REST API directly
    strimzi.io/use-connector-resources: "true"
  name: first-connect-cluster
spec:
  version: 4.0.0
  replicas: 3
  bootstrapServers: my-cluster-kafka-bootstrap:9092
  config:
    group.id: connect-cluster
    offset.storage.topic: connect-cluster-offsets
    config.storage.topic: connect-cluster-configs
    status.storage.topic: connect-cluster-status
    # -1 means it will use the default replication factor configured in the broker
    config.storage.replication.factor: -1
    offset.storage.replication.factor: -1
    status.storage.replication.factor: -1
```

```
kubectl -n kafka apply -f kafka-connect.yaml
watch kubectl -n kafka get kafkaconnect,pods
```


Start the cluster and wait until the pods are ready.
Now describe the Connect pod to see the configuration:
```
kubectl describe -n kafka pod/first-connect-cluster-connect-0
```

The problem with this cluster is that there are no connectors built into it (you can see that the `/opt/kafka/plugins` folder is empty).
So we have to add them to the Docker image of the Connect pods.
Strimzi can build such container for us, by adding the following lines to the `yaml` definition:
```
spec:
...
  build:
    output:
      type: docker
      # use your own repo!!
      image: docker.io/szabvid/my-connect:4.0.0
      pushSecret: dockerhub-registry-secret
    plugins:
      - name: kafka-connect-file
        artifacts:
          - type: maven
            group: org.apache.kafka
            artifact: connect-file
            version: 4.0.0
```

Update configuration:
```
# since we will push the image to Docker Hub we need to include an imagepullsecret and also a secret to be created:
kubectl -n kafka create secret docker-registry dockerhub-registry-secret --docker-server=https://index.docker.io/v1/ --docker-username=<username> --docker-password=<access-token> --docker-email=<email>

# update the KafkaConnect configuration
kubectl -n kafka apply -f kafka-connect-with-file-reader-plugin.yaml
watch kubectl -n kafka get kafkaconnect,pods
```

This will start a new build process. You can see the logs here:
```
kubectl logs -n kafka first-connect-cluster-connect-build -f
```

If the build is successful, you should see that the Connect pods are redeployed with the new image version:
```
kubectl get pods -n kafka 
kubectl describe -n kafka pod/first-connect-cluster-connect-0
kubectl exec -n kafka first-connect-cluster-connect-0 -- ls -lhR /opt/kafka/plugins
```

You can start a port-forward and examine the API of the Connect cluster:
```
kubectl port-forward -n kafka svc/first-connect-cluster-connect-api 8083:8083
```

Couple of example calls (you can find more [here](https://developer.confluent.io/courses/kafka-connect/rest-api/)):
```
curl http://localhost:8083/
curl -s localhost:8083/connector-plugins | jq '.'
curl -s localhost:8083/connectors/
```

## 2. Start a source connector

Now let's start a `FileConnector` using Strimzi (you need the `strimzi.io/use-connector-resources: "true"` annotation on your cluster for this to work):
```
apiVersion: kafka.strimzi.io/v1beta2
kind: KafkaConnector
metadata:
  name: file-source-connector
  labels:
    strimzi.io/cluster: first-connect-cluster
spec:
  class: org.apache.kafka.connect.file.FileStreamSourceConnector
  tasksMax: 1
  config:
    value.converter: org.apache.kafka.connect.json.JsonConverter
    value.converter.schemas.enable: true
    file: "/tmp/test-file"
    topic: file-topic
```

```
kubectl -n kafka apply -f kafka-connector-file-source-connector.yaml
```

You should see that the connector was created (it isnot a pod), and also see which worker runs it:
```
curl -s localhost:8083/connectors/file-source-connector | jq
curl -s localhost:8083/connectors/file-source-connector/tasks/0/status | jq
```

Now let's start a consumer to see the topic:
```
kubectl -n kafka run kafka-scripts --image=quay.io/strimzi/kafka:0.44.0-kafka-3.8.0 -- sh -c "sleep infinity"

# consuming messages
kubectl exec -it -n kafka kafka-scripts -- bin/kafka-console-consumer.sh --bootstrap-server my-cluster-kafka-bootstrap:9092 --topic file-topic --from-beginning
```

We can also `exec` into the corresponding pod and start add lines to the file `/tmp/test-file` and see that the consumer reads it:
```
kubectl exec -it -n kafka first-connect-cluster-connect-0 -- bash
echo '{"id": 1, "name": "David"}' >> /tmp/test-file
```
```
# or generate random lines every sec create and run this script:
FILE="/tmp/test-file"
NAMES=("Dávid" "Anna" "Bence" "Eszter" "Gábor" "Lili" "Zoltán" "Réka" "Tamás" "Judit")

for i in {1..50}; do
  ID=$i
  NAME=${NAMES[$RANDOM % ${#NAMES[@]}]}
  EMAIL=$(echo "${NAME,,}${ID}@example.com" | iconv -f utf8 -t ascii//TRANSLIT)
  AGE=$((RANDOM % 50 + 18))

  echo "{\"id\": $ID, \"name\": \"$NAME\", \"email\": \"$EMAIL\", \"age\": $AGE}" >> "$FILE"
done
```

## 3. Using a JDBC sink connector to load topic into PostgreSQL

We'll use the [JDBC connector](https://github.com/confluentinc/kafka-connect-jdbc) to push messages into a PostgreSQL server.
First, we'll need a PSQL server. Just run the provided `yaml` file:
```
kubectl apply -f psql.yaml
kubectl get pods
# exec into the container using a psql terminal
kubectl exec -it postgresql-0 -- psql -U postgresuser -d mydb
# query the tables (should be empty)
\dt
exit
```

Next, we need to rebuild the Connect container and add the `JDBC connector`.
Add these lines to the Connect cluster definition `yaml`:
```
spec:
...
  build:
...
    plugins:
...
      - name: kafka-connect-jdbc
        artifacts:
          - type: maven
            repository: https://packages.confluent.io/maven
            group: io.confluent
            artifact: kafka-connect-jdbc
            version: 10.4.0
```

Apply the new config, and verify if everything went all right:
```
# update configuration:
kubectl -n kafka apply -f kafka-connect-with-psql-plugin.yaml

# check build logs
kubectl logs -n kafka pod/first-connect-cluster-connect-build -f

# if build is ok, see that the cluster is redeployed
watch kubectl get pods -n kafka

# check the plugin jar files:
kubectl exec -n kafka first-connect-cluster-connect-0 -- ls -lhR /opt/kafka/plugins

# check API for connetctor plugins
# restart the port-forward first
kubectl port-forward -n kafka svc/first-connect-cluster-connect-api 8083:8083
curl -s localhost:8083/connector-plugins | jq '.'
```

Let's create a config for the `JDBC connector`:
```
apiVersion: kafka.strimzi.io/v1beta2
kind: KafkaConnector
metadata:
  name: sink-postgres
  labels:
    strimzi.io/cluster: first-connect-cluster
spec:
  class: io.confluent.connect.jdbc.JdbcSinkConnector
  tasksMax: 1
  config:
    topics: structured-topic
    value.converter: org.apache.kafka.connect.json.JsonConverter
    value.converter.schemas.enable: true
    connection.url: jdbc:postgresql://postgresql.default.svc:5432/mydb
    connection.user: postgresuser
    connection.password: changeme123
    insert.mode: insert
    auto.create: true
    table.name.format: structured_topic
    pk.mode: record_value
    pk.fields: id
```

```
kubectl -n kafka apply -f kafka-connector-psql-sink-connector.yaml
```

You should see that the connector was created, and also see which worker runs it:
```
curl -s localhost:8083/connectors/sink-postgres | jq
curl -s localhost:8083/connectors/sink-postgres/tasks/0/status | jq
```

Let's create a topic with messages, for which we will use the REST API via Kafka Bridge:
- crate Kafka Bridge instance:
  ```
  kubectl -n kafka apply -f ../bridge/bridge.yaml
  ```

- Start a port-forward, so you can access the API on your localhost:
  ```
  kubectl port-forward -n kafka svc/my-bridge-bridge-service 8080:8080
  ```

Send message in topic `structured-topic`:
```
curl -X POST http://localhost:8080/topics/structured-topic \
  -H "Content-Type: application/vnd.kafka.json.v2+json" \
  -d '{
    "records": [
      {
        "value": {
          "schema": {
            "type": "struct",
            "fields": [
              { "field": "id", "type": "int32", "optional": false },
              { "field": "name", "type": "string", "optional": false },
              { "field": "email", "type": "string", "optional": false },
              { "field": "age", "type": "int32", "optional": false }
            ],
            "optional": false,
            "name": "User"
          },
          "payload": {
            "id": 101,
            "name": "Structured David",
            "email": "david101@example.com",
            "age": 33
          }
        }
      }
    ]
  }'
```

Check the PLSQL table - we should see the record:
```
kubectl exec -it postgresql-0 -- psql -U postgresuser -d mydb

\dt;

SELECT * FROM structured_topic;
```