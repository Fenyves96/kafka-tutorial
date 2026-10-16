# Kafka Operations

## 1. Install a Kafka cluster

Step 1: Install a Java Runtime Environmet. We'll use OpenJDK for these demos.

```
sudo apt update
sudo apt install openjdk-11-jdk -y

#check the Install
java -version
```

Step 2: Configure static DNS entries, by adding the following lines to `/etc/hosts` in all 3 VMs.

```
sudo nano /etc/hosts

# add these lines to the file
<VM1 IP> kafka-node-1
<VM2 IP> kafka-node-2
<VM3 IP> kafka-node-3
```

Step 3: Download and extract the Kafka archive.
```
export KAFKA_VERSION=3.8.1
wget https://downloads.apache.org/kafka/$KAFKA_VERSION/kafka_2.13-$KAFKA_VERSION.tgz
tar -xzf kafka_2.13-$KAFKA_VERSION.tgz
sudo ln -s /home/$USER/kafka_2.13-$KAFKA_VERSION /usr/local/kafka
cd /usr/local/kafka
ls -lha
```

Step 4: Configure and start Zookeeper.

First, crete a config file for Zookeeper at `config/zookeeper.properties`
```
dataDir=/tmp/zookeeper
clientPort=2181
initLimit=5
syncLimit=2
4lw.commands.whitelist=stat, ruok, conf, isro
server.1=kafka-node-1:2888:3888
server.2=kafka-node-2:2888:3888
server.3=kafka-node-3:2888:3888
```

Second, we need to give each Zookeeper node a unique ID, so create a file in the Zookeeper `dataDir` called `myid`, like this on the first node:
```
mkdir -p /tmp/zookeeper
echo 1 > /tmp/zookeeper/myid
# on node 2 it should be echo 2 > /tmp/zookeeper/myid, etc...
```

If you set up the hostnames correctly (e.g. `kafka-node-1`, `kafka-node-2`, etc.), you can use the following snippet as well:
```
hostname | awk -F'-' '{print $NF}' >/tmp/zookeeper/myid
```

Finally, start Zookeeper:
```
bin/zookeeper-server-start.sh config/zookeeper.properties
```

Step 5: Create a `systemd` service for Zookeeper

It is recommended to run these services with `systemd`.
To that we need to create a file in `/etc/systemd/system/zookeeper.service` with the following content:
```
[Unit]
Description=Apache Zookeeper Server
Documentation=https://zookeeper.apache.org
Wants=network.target
After=network.target

[Service]
Type=simple
User=root
Group=root
ExecStart=/usr/local/kafka/bin/zookeeper-server-start.sh /usr/local/kafka/config/zookeeper.properties
ExecStop=/usr/local/kafka/bin/zookeeper-server-stop.sh
Restart=on-failure
RestartSec=5
LimitNOFILE=65536

[Install]
WantedBy=multi-user.target
```

To start it run the following:
```
sudo systemctl daemon-reload
sudo systemctl enable zookeeper
sudo systemctl start zookeeper
sudo systemctl status zookeeper
```

If you want to see the logs of Zookeeper run the following command:
```
journalctl -xef | grep zookeeper
```

Run this on every other node to get a working Zookeeper cluster.

You can also check the health of the Zookeeper cluster with the following command:
```
echo ruok | nc localhost 2181
```

Step 6: Configure and start Kafka

Fisrt, edit the Kafka server properties file at `config/server.properties` as follows:

Set a unique broker ID for each node:
```
broker.id=1 # Change this to 2 and 3 on the other nodes
```

Specify the log directory:
```
log.dirs=/tmp/kafka-logs
```

Point to the Zookeeper cluster:
```
zookeeper.connect=kafka-node-1:2181,kafka-node-2:2181,kafka-node-3:2181
```

Advertise the broker's hostname:
```
listeners=PLAINTEXT://:9092
advertised.listeners=PLAINTEXT://<node-hostname>:9092
```

You can run Kafka with the following command:
```
bin/kafka-server-start.sh config/server.properties
```

Step 7: Create a `systemd` service for Kafka

This is a very similar step that what we did for Zookeeper.
To that we need to create a file in `/etc/systemd/system/kafka.service` with the following content:
```
[Unit]
Description=Apache kafka Server
Documentation=https://kafka.apache.org
Wants=network.target
After=network.target

[Service]
Type=simple
User=root
Group=root
ExecStart=/usr/local/kafka/bin/kafka-server-start.sh /usr/local/kafka/config/server.properties
ExecStop=/usr/local/kafka/bin/kafka-server-stop.sh
Restart=on-failure
RestartSec=5
LimitNOFILE=65536

[Install]
WantedBy=multi-user.target
```

To start it run the following:
```
sudo systemctl daemon-reload
sudo systemctl enable kafka
sudo systemctl start kafka
sudo systemctl status kafka
```

If you want to see the logs of Kafka run the following command:
```
journalctl -xef | grep kafka
```

Repeat these on node2 and node3.

Now you should have a working Kafka cluster. To test it, try the `console-producer`:
```
bin/kafka-console-producer.sh --topic test-topic --bootstrap-server kafka-node-1:9092

# produce some messages
# the first message should generate a warning since the topic was not previously existing
# hit ctrl+c to stop
# try to read it with the console-consumer

bin/kafka-console-consumer.sh --topic test-topic --from-beginning --bootstrap-server kafka-node-1:9092
```

Step 8: Run Kafka UI for check cluster state

[Kafka UI](https://github.com/provectus/kafka-ui) is a great UI to check basic cluster state.
You can install it via Docker as follow (e.g. on node-1):
```
sudo apt install -y docker.io
sudo docker run -d --net=host -e DYNAMIC_CONFIG_ENABLED=true -e SERVER_PORT=8088 provectuslabs/kafka-ui
```

Make sure your firewall allows port `8088`. If you then go to the web UI you can add a cluster using `localhost:9092` as a bootstrap server.

## 2. Update configuration

In this example, we'll update a configuration parameter in Kafka, and perform a manual rolling update.

First, edit the `config/server.properties` file (on all three brokers), and modify the following lines:
```
default.replication.factor=3
min.insync.replicas=2
```

Then, restart Kafka (all nodes one-by-one):
```
sudo systemctl restart kafka
sudo systemctl status kafka
```

Now, create a topic and examin that the new config is in place:
```
bin/kafka-topics.sh --bootstrap-server localhost:9092 --topic first-topic --create --partitions 30
```

## 3. Test broker outage

Now, we'll simulate a broker outage.

First, let's start producing messages continously with the following script:
```
#!/bin/bash

# Configuration
BROKER="localhost:9092"  # Kafka broker address
TOPIC="first_topic"      # Kafka topic name
INTERVAL=1               # Interval between messages in seconds

echo "Starting message producer to Kafka topic '$TOPIC' on broker '$BROKER'..."

while true; do
  TIMESTAMP=$(date +"%Y-%m-%d %H:%M:%S")
  MESSAGES="1
2
3
4
5
6
7
8
9
10 - $TIMESTAMP"
  
  echo "$MESSAGES" | bin/kafka-console-producer.sh --bootstrap-server $BROKER --topic $TOPIC 2>/dev/null
  
  if [ $? -ne 0 ]; then
    echo "Failed to send message. Check Kafka broker and topic settings."
    exit 1
  fi

  echo "Produced: $MESSAGES"
  sleep $INTERVAL
done
```

Run the script, and also start a `console-consumer`:
```
bin/kafka-console-consumer.sh --topic first_topic --bootstrap-server kafka-node-1:9092
```

Now go to one of the VMs, and stop Kafka:˛
```
sudo systemctl stop kafka
```

Notice that everything is still ok (both producer, and consumer) even if one broker is down.
This is because a leader election executed in the background, so all topics have a new leader.
Now bring back the broker:
```
sudo systemctl start kafka
```

## 4. Broker upgrade 

<b>Note</b>: The upgrade works only if the new version is < 4.0.0 that is a breaking change and requires very careful, planned migration:
- Backing up your data from `log.dirs`
- Export Metadata from ZooKeeper (e.g. Topic definitions,  Configs, ACLs, Quotas)
  - if you use Kafka CLI, probably a Java version upgrade is also necessary, since Kafka 4.0.0 was compiled using Java 17 (class file version 61.0).
- Prepare KRaft Configs (`config/server.properties` file) and start the cluster
- Recreate Topics using the exported configs
- Reapply ACLs and Configs
- There are tools that can ease the migration and partially cover the former tasks tools for the former  (e.g. kafka-metadata.migration.sh, MirrorMaker 2) but there is currently no single high-level tool that performs a full, seamless migration of both metadata and data from a ZooKeeper-based Kafka cluster to a KRaft-based one.
<hr>

We'll perform the following steps:
1: Set inter broker and log version to current Kafka version. If you are using `3.8` now, just put the following broker properties: 
- `inter.broker.protocol.version=3.8`:
  - This controls the protocol version used for communication between brokers.
  - Setting it allows mixed-version clusters during rolling upgrades.
  - It ensures that older brokers can still talk to newer ones until all brokers are upgraded.

- `log.message.format.version=3.8`:
  - It defines the format version used to write messages to disk.
  - This affects how producers encode messages and how consumers read them. 
  - It ensures backward compatibility with older consumers that may not support newer formats.

2: One broker at a time: stop the broker, upgrade the binaries to `3.9`, and start the broker. Wait for the cluster to stabilize (all the partitions are in-sync replicas (ISR).

  ```
  cd
  export KAFKA_VERSION=3.9.1
  wget https://downloads.apache.org/kafka/$KAFKA_VERSION/kafka_2.13-$KAFKA_VERSION.tgz
  tar -xzf kafka_2.13-$KAFKA_VERSION.tgz
  cp /usr/local/kafka/config/server.properties ~/kafka_2.13-$KAFKA_VERSION/config/
  sudo rm /usr/local/kafka
  sudo ln -s /home/$USER/kafka_2.13-$KAFKA_VERSION /usr/local/kafka
  cd /usr/local/kafka
  ls -lha
  sudo systemctl restart kafka
  ```


3: At this stage, all your brokers are running the newer binaries of `3.9`, but running with `inter.broker.protocol.version=3.8` and `log.message.format.version=3.8`

4: Ensure the cluster is stable in this form. You still have the option to downgrade here if you want to because the log message format has not been upgraded yet:
```
bin/zookeeper-shell.sh kafka-node-1:2181 ls /brokers/ids
```

5: One broker at a time: stop the broker, change the inter broker protocol version for that broker with `inter.broker.protocol.version=3.9` and start that broker. Wait for the cluster to stabilize (all the partitions are in-sync replicas (ISR).

6: At the end of this stage, all the brokers are running with the new inter broker protocol version, but they are not using the new log message format yet.

7: Upgrade Kafka clients if documentation specifies it (all or most of them to avoid up and down conversion - which can affect performance)

8: One broker at a time: stop the broker, upgrade message protocol version `log.message.format.version=3.9` and start the broker. Wait for the cluster to stabilize (all the partitions are in-sync replicas (ISR).

9: At this stage, all brokers are upgraded to the latest binary, are running the latest inter-broker protocol and are using the latest log message format version.

If you check this on Kafka UI an you still see an older version it is worth to restart the docker container and check again.

## 5. Monitoring

First, download the Prometheus JMX exporter (you should do the following setup on each node):
```
cd /usr/local/kafka
wget https://repo.maven.apache.org/maven2/io/prometheus/jmx/jmx_prometheus_javaagent/1.0.1/jmx_prometheus_javaagent-1.0.1.jar
# create a config file for the JMX exporter
curl https://raw.githubusercontent.com/prometheus/jmx_exporter/refs/heads/main/examples/kafka-kraft-3_0_0.yml -o jmx-config.yaml

# other examples are here:
https://github.com/prometheus/jmx_exporter/tree/main/examples
```	

Add the following config to the Kafka `systemd` file at `/etc/systemd/system/kafka.service` under the `[Service]` section:
```
[Service]
...
# lines to add
Environment="JMX_PORT=9999"
Environment="JMX_HOSTNAME=0.0.0.0"
Environment="KAFKA_OPTS=-javaagent:/usr/local/kafka/jmx_prometheus_javaagent-1.0.1.jar=7071:/usr/local/kafka/jmx-config.yaml"
```

Then restart Kafka:
```
sudo systemctl daemon-reload
sudo systemctl restart kafka
```

You can check if Kafka exposes metrics correctly in the Prometheus format with this command:
```
curl localhost:7071/metrics
```

Now, let's download and start Prometheus (it's enough to do it on one server):
```
cd
export PROMETHEUS_VERSION=3.5.0
wget https://github.com/prometheus/prometheus/releases/download/v$PROMETHEUS_VERSION/prometheus-$PROMETHEUS_VERSION.linux-amd64.tar.gz
tar -xvzf prometheus-$PROMETHEUS_VERSION.linux-amd64.tar.gz
sudo ln -s /home/$USER/prometheus-$PROMETHEUS_VERSION.linux-amd64 /usr/local/prometheus
cd /usr/local/prometheus
ls -lh
```

You can start Prometheus by the following command:
```
./prometheus --config.file="prometheus.yml"
```

Now, let's configure Prometheus to start monitor the Kafka brokers.
We need to modify the configuration file of Prometheus at `prometheus.yml` with the following under section `scrape_configs:`:
```
scrape_configs:
...

  - job_name: "kafka"
    static_configs:
      - targets:
        - kafka-node-1:7071
        - kafka-node-2:7071
        - kafka-node-3:7071
```

Finally, let's create a `systemd` file for Prometheus and run it as a service:
```
nano /etc/systemd/system/prometheus.service
```

Copy this content to the file:
```
[Unit]
Description=Prometheus Monitoring System
After=network.target

[Service]
Type=simple
User=root
Group=root
ExecStart=/usr/local/prometheus/prometheus \
  --config.file=/usr/local/prometheus/prometheus.yml \
  --storage.tsdb.path=/var/lib/prometheus/data \
  --web.listen-address=:9090
Restart=always
RestartSec=5
LimitNOFILE=65536

[Install]
WantedBy=multi-user.target
```

Finally, set up the service:
```
sudo systemctl daemon-reload
sudo systemctl enable prometheus
sudo systemctl start prometheus
sudo systemctl status prometheus
```

You can examin the following metrics:
```
# Number of active controllers in the cluster (should be 1)
- kafka_controller_kafkacontroller_activecontrollercount

# Number of partitions that don't have all replicas in sync
- kafka_server_replicamanager_underreplicatedpartitions

# Number of partitions with no available leader (offline)
- kafka_controller_kafkacontroller_offlinepartitionscount

# Total number of messages received by the broker
- kafka_server_brokertopicmetrics_messagesin_total

# Total number of bytes received by the broker
- kafka_server_brokertopicmetrics_bytesin_total
```

Now let's add a couple of alert rule to Prometheus.
First, create an alert rule file at `/usr/local/prometheus/alerts.yaml` with the following content:
```
groups:
  - name: kafka_alerts
    rules:
      - alert: KafkaActiveControllers
        expr: count(kafka_controller_kafkacontroller_iscontroller == 1) != 1
        for: 1m
        labels:
          severity: critical
        annotations:
          summary: "Kafka has wrong number of active controllers"
          description: "Kafka cluster has {{ $value }} active controllers."
      - alert: KafkaUnderReplicatedPartitions
        expr: kafka_server_replicamanager_underreplicatedpartitions > 0
        for: 1m
        labels:
          severity: warning
        annotations:
          summary: "Kafka has under-replicated partitions"
          description: "Broker {{ $labels.instance }} has {{ $value }} under-replicated partitions."
      - alert: KafkaOfflinePartitions
        expr: kafka_controller_kafkacontroller_offlinepartitionscount > 0
        for: 1m
        labels:
          severity: warning
        annotations:
          summary: "Kafka has offline partitions"
          description: "Broker {{ $labels.instance }} has {{ $value }} offline partitions."
```

Now add this rule to the Prometheus config at `/usr/local/prometheus/prometheus.yml`:
```
...
rule_files:
  - "/usr/local/prometheus/alerts.yaml"
```

Finally, restart Prometheus and examin the `Alerts` tab on the UI:
```
sudo systemctl restart prometheus
```

## 6. Use Grafana for visualize metrics

To install Grafana run the following:
```
# add Grafana apt repo
sudo apt-get install -y apt-transport-https software-properties-common wget
sudo mkdir -p /etc/apt/keyrings/
wget -q -O - https://apt.grafana.com/gpg.key | gpg --dearmor | sudo tee /etc/apt/keyrings/grafana.gpg > /dev/null
echo "deb [signed-by=/etc/apt/keyrings/grafana.gpg] https://apt.grafana.com stable main" | sudo tee -a /etc/apt/sources.list.d/grafana.list

# Updates the list of available packages
sudo apt-get update

# Installs the latest OSS release:
sudo apt-get install grafana

# start Grafana
sudo systemctl enable grafana-server
sudo systemctl start grafana-server
sudo systemctl status grafana-server
```

Grafana listens at port `:3000`, so don't forget to add it to your firewall rules.
Go to the UI and login with `admin:admin`.
First, add Prometheus as a datasource at the menu `Connections / Data Sources`.
Then import the following dashboard: https://grafana.com/grafana/dashboards/11962-kafka-metrics/.

## 7. Installing Strimzi operator:

Prerequisite: have a running Kubernetes cluster with `kubectl` and `helm` installed.
  - you can create one with [`kind`](https://kind.sigs.k8s.io/docs/user/quick-start/) for example :
    ```
    kind create cluster --name test-cluster
    ```

Install the Strimzi operator:
```
helm repo add strimzi https://strimzi.io/charts/
helm repo update

helm install strimzi-kafka strimzi/strimzi-kafka-operator --version 0.47.0 --namespace kafka --create-namespace

watch kubectl get pods -n kafka
```

You can check what kind of `Custom Resources` was installed into your Kubernetes cluster:
```
kubectl get crds | grep strimzi
```

Now we can install a Kafka cluster.
Before that you can check Strimzi version vs. Kafka version compatiblity [here](https://strimzi.io/downloads/).
Also, there's a lot of other examples in the [Strimzi GitHub repo](https://github.com/strimzi/strimzi-kafka-operator/tree/main/examples/kafka).


## 7.a) (Obsolete: relevant only for older setups) Running Kafka-Zookeeper with Strimzi operator

<b>Note</b>: if you would like to create a zookeeper included kafka cluster with `Strimzi` it is not supported from version `0.47.0`, but here is how such config looked like if it happens you need one and it's okay to use an erlier Strimzi version (<= 0.46.0):

`kafka-zookeeper.yaml`:
```
apiVersion: kafka.strimzi.io/v1beta2
kind: Kafka
metadata:
  name: first-cluster
spec:
  kafka:
    version: 3.7.0
    replicas: 3
    listeners:
      - name: plain
        port: 9092
        type: internal
        tls: false
    config:
      offsets.topic.replication.factor: 3
      transaction.state.log.replication.factor: 3
      transaction.state.log.min.isr: 2
      default.replication.factor: 3
      min.insync.replicas: 2
    storage:
      type: jbod
      volumes:
      - id: 0
        type: persistent-claim
        size: 5Gi
        deleteClaim: false
  zookeeper:
    replicas: 3
    storage:
      type: persistent-claim
      size: 1Gi
      deleteClaim: false
  entityOperator:
    topicOperator: {}
    userOperator: {}
```

Create the cluster 
```
kubectl apply -f kafka-zookeeper.yaml -n kafka
watch kubectl get pods,pvc -n kafka
```

# 7.b) Install a Kafka cluster in KRaft mode with Strimzi

Strimzi supports the new `Kraft` mode in Kafka.
In order to install a cluster with `KRaft` enabled, you have to do the following two things.
First, you have to use the newly created CRD `KafkaNodePool` to define the Kafka pods. 
Here, there are two important settings:
 1: you have to define the role of the nodes, which can be `controller`, `broker` or the two at the same time (which is referred as `dual-role` nodes),
 2: have to add the following label to the node pool so that Strimzi knows which Kafka cluster it belongs to: `strimzi.io/cluster: my-cluster`.
The following is an example for 3 controller and 3 broker Kafka pods:
- save it to a file `kafka-node-pools.yaml`:
  ```
  apiVersion: kafka.strimzi.io/v1beta2
  kind: KafkaNodePool
  metadata:
    name: controller
    labels:
      strimzi.io/cluster: my-cluster
  spec:
    replicas: 3
    roles:
      - controller
    storage:
      type: jbod
      volumes:
        - id: 0
          type: persistent-claim
          size: 10Gi
          kraftMetadata: shared
          deleteClaim: false
  ---

  apiVersion: kafka.strimzi.io/v1beta2
  kind: KafkaNodePool
  metadata:
    name: broker
    labels:
      strimzi.io/cluster: my-cluster
  spec:
    replicas: 3
    roles:
      - broker
    storage:
      type: jbod
      volumes:
        - id: 0
          type: persistent-claim
          size: 10Gi
          kraftMetadata: shared
          deleteClaim: false
  ```
- let's create the node pools:
  ```
  kubectl -n kafka apply -f kafka-node-pools.yaml 
  watch kubectl -n kafka get kafkanodepools.kafka.strimzi.io
  ```

Secondly, when you create the Kafka cluster you have to define that you use `NodePools` and you want `Kraft` mode enabled, with the following two annotations:
- save it to a file `kafka-cluster.yaml`: 
  ```
  apiVersion: kafka.strimzi.io/v1beta2
  kind: Kafka
  metadata:
    name: my-cluster
    annotations:
      strimzi.io/node-pools: enabled
      strimzi.io/kraft: enabled
  spec:
    kafka:
      version: 3.9.0
      metadataVersion: 3.9-IV0
      listeners:
        - name: plain
          port: 9092
          type: internal
          tls: false
        - name: tls
          port: 9093
          type: internal
          tls: true
      config:
        offsets.topic.replication.factor: 3
        transaction.state.log.replication.factor: 3
        transaction.state.log.min.isr: 2
        default.replication.factor: 3
        min.insync.replicas: 2
    entityOperator:
      topicOperator: {}
      userOperator: {}
  ```

- let's create the node pools:
  ```
  kubectl -n kafka apply -f kafka-cluster.yaml 
  watch kubectl get pods,pvc -n kafka
  ```

# 8. Accessing the Strimzi managed Kafka cluster:

This cluster currently is only accessible from inside the Kubernetes cluster.
We can check whether everything is all right by running a Kafka pod, exec into it, and run a simple producer / consumer:
```
kubectl -n kafka run kafka-scripts --image=quay.io/strimzi/kafka:0.47.0-kafka-3.9.0 -- sh -c "sleep infinity"

kubectl exec -it -n kafka kafka-scripts -- bin/kafka-console-producer.sh --bootstrap-server my-cluster-kafka-bootstrap:9092 --topic first-topic

kubectl exec -it -n kafka kafka-scripts -- bin/kafka-console-consumer.sh --bootstrap-server my-cluster-kafka-bootstrap:9092 --topic first-topic --from-beginning
```

Not, try to upgrade from `3.9.0` to `4.0.0`:
```
sed -i 's/3.9.0/4.0.0/' kafka-cluster.yaml
kubectl -n kafka apply -f kafka-cluster.yaml 
watch kubectl get pods,pvc -n kafka

# check (we dhould see first the controller nodes upgrading)
kubectl -n kafka get kafka my-cluster -o yaml
```

Now you can see that the metadata is still `kafkaMetadataVersion: 3.9-IV0`, add this line under `version: 4.0.0` line to the `kafka-cluster.yaml`:
```
sed -i 's/3.9-IV0/4.0-IV0/' kafka-cluster.yaml
kubectl -n kafka apply -f kafka-cluster.yaml 
watch kubectl get pods,pvc -n kafka
```

Similarly, you can easily add a new broker by increasing the replica number for the broker node pool in the yaml file:
```
nano kafka-node-pools.yaml
# increase Kafka replicas to 4
kubectl apply -f kafka-node-pools.yaml -n kafka
watch kubectl get pods,pvc -n kafka
```

# 9. Managing topics with Strimzi:

Strimzi includes a Topic Operator to manage Kafka topics
The `KafkaTopic` resource configures topics, including partition and replication factor settings. 
When you create, modify, or delete a topic using `KafkaTopic`, the Topic Operator ensures that these changes are reflected in the Kafka cluster.
When a KafkaTopic is created, deleted, or changed, the Topic Operator performs the corresponding operation on the Kafka topic.
If a topic is created, deleted, or modified directly within the Kafka cluster, without the presence of a corresponding KafkaTopic resource, the Topic Operator does not manage that topic. The Topic Operator will only manage Kafka topics associated with KafkaTopic resources and does not interfere with topics managed independently within the Kafka cluster.

Let's query the topics:
```
kubectl get kafkatopics -n kafka
# should be empty
```

Now let's crate a topic with the Topic Operator. Create a file called `topic.yaml` with the following content:
```
apiVersion: kafka.strimzi.io/v1beta2
kind: KafkaTopic
metadata:
  name: managed-topic
  namespace: kafka
  labels:
    strimzi.io/cluster: my-cluster
spec:
  topicName: managed-topic
```

Apply the config:
```
kubectl apply -f topic.yaml
kubectl get kafkatopics -n kafka
```

Let's check whether the topic was indeed created on the Kafka cluster:
```
kubectl exec -it -n kafka kafka-scripts -- bin/kafka-topics.sh --bootstrap-server my-cluster-kafka-bootstrap:9092 --list
```

We can easily modify the configuration of topic by just adding it to the `yaml` file and apply it. Modify `topic.yaml` with this:
```
apiVersion: kafka.strimzi.io/v1beta2
kind: KafkaTopic
metadata:
  name: managed-topic
  namespace: kafka
  labels:
    strimzi.io/cluster: my-cluster
spec:
  topicName: managed-topic
  partitions: 10
  config:
    min.insync.replicas: 2
    retention.ms: 86400000
```

Check the config in Kafka as well:
```
kubectl exec -it -n kafka kafka-scripts -- bin/kafka-topics.sh --bootstrap-server my-cluster-kafka-bootstrap:9092 --topic managed-topic --describe
``` 

## 10. Enable Prometheus monitoring with Strimzi

Using Strimzi, we can easily add Prometheus / Grafana monitoring to our cluster.
As a first step, create a `configMap` for the JMX exporter (it's basically the same file we created before, just added as a `ConfigMap`):
```
kubectl apply -f metrics-config.yaml -n kafka
```

Now add the `metricsConfig` to your Kafka cluster file:
```
apiVersion: kafka.strimzi.io/v1beta2
kind: Kafka
metadata:
  name: my-cluster
  annotations:
    strimzi.io/node-pools: enabled
    strimzi.io/kraft: enabled
spec:
  kafka:
    version: 4.0.0
    metadataVersion: 4.0-IV0
    listeners:
      - name: plain
        port: 9092
        type: internal
        tls: false
      - name: tls
        port: 9093
        type: internal
        tls: true
    config:
      offsets.topic.replication.factor: 3
      transaction.state.log.replication.factor: 3
      transaction.state.log.min.isr: 2
      default.replication.factor: 3
      min.insync.replicas: 2
    metricsConfig:
      type: jmxPrometheusExporter
      valueFrom:
        configMapKeyRef:
          name: kafka-metrics
          key: kafka-metrics-config.yml
  entityOperator:
    topicOperator: {}
    userOperator: {}
```

Apply the new config (should restart the controller nodes then the broker nodes):
```
kubectl apply -f kafka-cluster.yaml -n kafka
watch kubectl get pods,pvc -n kafka
```

Secondly, we have to install Prometheus and configure it to monitor Kafka (and Zookeeper).
```
git clone https://github.com/prometheus-operator/kube-prometheus
cd kube-prometheus
kubectl apply --server-side -f manifests/setup
kubectl wait --for condition=Established --all CustomResourceDefinition --namespace=monitoring
kubectl apply -f manifests/
watch kubectl get pods -n monitoring 
```

You can access the Prometheus dashboard using one of the following methods:
 1: change the `prometheus-k8s` to `type: LoadBalancer`
 2: create and `Ingress` for the `prometheus-k8s` sevice
 3: use simple port-forward: `kubectl port-forward -n monitoring svc/prometheus-k8s 9090:9090 --address 0.0.0.0`
 
Now we want to add the Kafka (and Zookeeper) pods to Prometheus to start the monitoring process.
We have to create a `PodMonitor` custom resource for this in `pod-monitor.yaml`:
```
apiVersion: monitoring.coreos.com/v1
kind: PodMonitor
metadata:
  name: kafka-resources-metrics
  namespace: monitoring
  labels:
    app: strimzi
spec:
  selector:
    matchExpressions:
      - key: "strimzi.io/kind"
        operator: In
        values: ["Kafka", "KafkaConnect", "KafkaMirrorMaker", "KafkaMirrorMaker2"]
  namespaceSelector:
    matchNames:
      - kafka
  podMetricsEndpoints:
  - path: /metrics
    port: tcp-prometheus
    relabelings:
    - separator: ;
      regex: __meta_kubernetes_pod_label_(strimzi_io_.+)
      replacement: $1
      action: labelmap
    - sourceLabels: [__meta_kubernetes_namespace]
      separator: ;
      regex: (.*)
      targetLabel: namespace
      replacement: $1
      action: replace
    - sourceLabels: [__meta_kubernetes_pod_name]
      separator: ;
      regex: (.*)
      targetLabel: kubernetes_pod_name
      replacement: $1
      action: replace
    - sourceLabels: [__meta_kubernetes_pod_node_name]
      separator: ;
      regex: (.*)
      targetLabel: node_name
      replacement: $1
      action: replace
    - sourceLabels: [__meta_kubernetes_pod_host_ip]
      separator: ;
      regex: (.*)
      targetLabel: node_ip
      replacement: $1
      action: replace
```

```
kubectl -n monitoring apply -f pod-monitor.yaml
```

This `PodMonitor` will only work, if Prometheus can list the pods in the `kafka` namespace.
If this is not the case, you might want to add this config with `kubectl edit clusterrole prometheus-k8s`:
```
- apiGroups:
  - ""
  resources:
  - pods
  - services
  - endpoints
  verbs:
  - get
  - list
```

If everything works correctly you should see metrics starting with `kafka_` on the Prometheus UI.

Now we can add a rules file that will include custom alerting rules for Kafka:
```
kubectl apply -f https://raw.githubusercontent.com/strimzi/strimzi-kafka-operator/refs/heads/main/examples/metrics/prometheus-install/prometheus-rules/prometheus-kafka-rules.yaml -n monitoring
```

You can also check these newly added alert rules on the UI.


You can also add a [Kafka Exporter](https://github.com/danielqsj/kafka_exporter) that will help monitoring further topic and consumer offset metrics.
You can simply add the following lines to your cluster defintion at `kafka.yaml`:
```
apiVersion: kafka.strimzi.io/v1beta2
kind: Kafka
metadata:
  name: my-cluster
  annotations:
    strimzi.io/node-pools: enabled
    strimzi.io/kraft: enabled
spec:
  kafka:
    version: 4.0.0
    metadataVersion: 4.0-IV0
    listeners:
      - name: plain
        port: 9092
        type: internal
        tls: false
      - name: tls
        port: 9093
        type: internal
        tls: true
    config:
      offsets.topic.replication.factor: 3
      transaction.state.log.replication.factor: 3
      transaction.state.log.min.isr: 2
      default.replication.factor: 3
      min.insync.replicas: 2
    metricsConfig:
      type: jmxPrometheusExporter
      valueFrom:
        configMapKeyRef:
          name: kafka-metrics
          key: kafka-metrics-config.yml
  entityOperator:
    topicOperator: {}
    userOperator: {}
  kafkaExporter:
    topicRegex: ".*"
    groupRegex: ".*"
```
Then apply the new config and see that a new pod was started for exposing these metrics:
```
kubectl apply -f kafka-cluster.yaml -n kafka
kubectl get pods -n kafka
```
You should see that the new metrics are added in the Prometheus UI.

We can also add a nice Grafana dashboard for Strimzi.
First, access Grafana similar way you used for Prometheus, e.g.
```
kubectl port-forward -n monitoring svc/grafana 3000:3000 --address 0.0.0.0
```

Then navigate to `Dashboards --> New Dashboard --> Import Dashboard`, then you can add the `json` content of the following file:
https://raw.githubusercontent.com/strimzi/strimzi-kafka-operator/refs/heads/main/examples/metrics/grafana-dashboards/strimzi-kafka.json

You should see a nice dashboard to monitor every Kafka cluster you deployed with Strimzi.