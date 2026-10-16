# Using ksqlDB

# 0. Prerequisite: 

- having a Kubernetes cluster, since we deploy ksqlDB as a K8s resource

- having a Kafka cluster that ksqlDB can connect to


## 1. Install ksqlDB

For this guide let's start `ksqlDB` in single mode:
```
cat ksqldb.yaml
kubectl apply -f ksqldb.yaml -n kafka
kubectl get pods -n kafka
```

## 2. Start the ksqlDB terminal

The easiest way to start a `ksqldb` terminal is in the running pod:
```
kubectl exec -it -n kafka ksqldb-0 -- ksql
```

You can run similar queries to a regular SQL terminal, e.g. if you want to see the stream and tables in ksqlDB run the following:
```
show streams; 
show tables; 
```

## 3. Create a stream from a Kafka topic

- prerequisite: 
  ```
  # create topic `movement` (we use strimzi operator for this)
  cat <<EOF | kubectl apply -f -
  apiVersion: kafka.strimzi.io/v1beta2
  kind: KafkaTopic
  metadata:
    name: movements
    labels:
      strimzi.io/cluster: my-cluster
  spec:
    topicName: movements
    partitions: 3
    replicas: 3
  EOF
  ```

We can create a ksqlDB stream from a Kafka topic as follows:
```
CREATE STREAM MOVEMENTS (PERSON VARCHAR KEY, LOCATION VARCHAR)
  WITH (VALUE_FORMAT='JSON', KAFKA_TOPIC='movements');
```

If the topic was not existing before the command, ksqlDB will create it (if it is configured so).

You can see that the stream is currently empty:
```
SELECT * FROM MOVEMENTS;
```

Now let's add some entries to the stream:
```
INSERT INTO MOVEMENTS VALUES ('David', 'Budapest');
INSERT INTO MOVEMENTS VALUES ('Peter', 'Budaors');
INSERT INTO MOVEMENTS VALUES ('David', 'Tapolca');
INSERT INTO MOVEMENTS VALUES ('Tibor', 'Debrecen');
```

Rerun the `SELECT` command to see that the entries are in the stream:
```
SELECT * FROM MOVEMENTS;
```

Note: 
  - If you run it multiple times, you can see that the ordering doesn't strict. 
  - Actually there is ordering but only partition-wise. 
  - You can check which partition the messages ended up, but using `pusyh` query (see later).

Moreover, this relationship is two-way, meaning that if you produce messages to Kafka, you will also see them in the ksqlDB stream:
```
kubectl exec -it -n kafka my-cluster-broker-0  -- bin/kafka-console-producer.sh --bootstrap-server 127.0.0.1:9092 --topic movements --property "parse.key=true" --property "key.separator=:"

# enter the following messages:
Peter:{"LOCATION": "Szeged"}
David:{"LOCATION": "Budapest"}
Janos:{"LOCATION": "Szentendre"}
```

Now, rerun the `SELECT` query in the ksqlDB terminal to see the new entries:
```
SELECT * FROM MOVEMENTS;
```

This was actually a so-called `pull query` where we just queried all data.

We can run a `push query` as well, which is more similar to a consumer, so it will hold the terminal and print all new messages:
```
SELECT * FROM MOVEMENTS EMIT CHANGES;
```

You can see that this command does not print anything at first because the default setting is `auto.offset.reset = earliest`.

We can change this behaviour with the following command (this is similar to the `console consumer` setting `--from-beginning`):
```
SET 'auto.offset.reset' = 'earliest';

# rerun the pull query
SELECT * FROM MOVEMENTS EMIT CHANGES;

# you can also check which partition each messages belongs to:
SELECT ROWPARTITION, * FROM MOVEMENTS EMIT CHANGES;
```

Now add new entries to the Kafka topic and see that they all written out to the terminal:
```
Gabor:{"LOCATION": "Pecs"}
Balazs:{"LOCATION": "Vienna"}
Janos:{"LOCATION": "Budapest"}
```

Now let's create a table from this stream data that counts for each person how many unique location that person has been and how movements he/she did:
```
CREATE TABLE PERSON_STATS WITH (VALUE_FORMAT='JSON') AS
  SELECT PERSON,
    LATEST_BY_OFFSET(LOCATION) AS LATEST_LOCATION,
    COUNT(*) AS LOCATION_CHANGES,
    COUNT_DISTINCT(LOCATION) AS UNIQUE_LOCATIONS
  FROM MOVEMENTS
GROUP BY PERSON
EMIT CHANGES;
```

You should see on the UI that the `PERSON_STATS` topic was created.

This is a persistent query — one that continuously runs in the background and updates the PERSON_STATS table as new data arrives in the MOVEMENTS stream.

Let's query the content:
```
SELECT * FROM PERSON_STATS;
```

The other thing you can do is to run a push query and see how this table gets new entries (similar to a real-time changelog):
```
SELECT * FROM PERSON_STATS EMIT CHANGES;
```

So add some new entries to the topic:
```
Peter:{"LOCATION": "Kazincbarcika"}
David:{"LOCATION": "Tapolca"}
Janos:{"LOCATION": "Szentendre"}
```

You can also do filtering when you select:
```
SELECT * FROM PERSON_STATS WHERE person ='Janos';
```
Notice that these pull queries on a table (not a stream) are much more faster, since they are also stored locally in a RockDB.

Now let's create a new table that stores for every location how many person are currently there:
```
CREATE TABLE LOCATION_STATS WITH (VALUE_FORMAT='JSON') AS
  SELECT LATEST_LOCATION as LOCATION,
  count(*) as PERSON_COUNT
 FROM PERSON_STATS
 GROUP BY LATEST_LOCATION
EMIT CHANGES;
```

See the result:
```
select * from LOCATION_STATS;
select * from LOCATION_STATS EMIT CHANGES;
```

Send some more data (one by one):
```
Peter:{"LOCATION": "Szeged"}
David:{"LOCATION": "Budapest"}
Janos:{"LOCATION": "Budapest"}
```
