## Prerequisite: Kafka setup

##### Option 1: 

- Installing Kafka on your machine, that includes:
  - installing a Java SDK
  - downloading Kafka binaries (including Kafka CLI tools)

[Here](./linux-start-kafka-kraft.md) you can find a tutorial for this.

##### Option 2: 

- Starting Kafka Docker container, that requires only Docker:

```
docker run -d -p 9092:9092 --name kafka apache/kafka:4.0.0
```


## Demo: Kafka CLI - kafka-topics.sh

- Create a new Kafka topic named `test_topic` with a single partition and one replica:

  ```
  kafka-topics.sh --create --topic test_topic --bootstrap-server localhost:9092 --partitions 1 --replication-factor 1
  ```

- List all available Kafka topics:

  ```
  kafka-topics.sh --list --bootstrap-server localhost:9092
  ```

- Get details about the `test_topic`:

  ```
  kafka-topics.sh --describe --topic test_topic --bootstrap-server localhost:9092
  ```

- Delete the `test_topic`:

  ```
  kafka-topics.sh --delete --topic test_topic --bootstrap-server localhost:9092
  ```

## Demo: Kafka CLI - kafka-console-producer.sh

- **Note:** If you haven’t created a topic yet, do so now (described in the previous Demo). 

- Start the Kafka console producer. This allows you to send messages to your Kafka topic directly from the command line:
  ```
  kafka-console-producer.sh --topic test_topic --bootstrap-server localhost:9092
  ```

- With the console producer running, you can type messages into your terminal. Each line you type will be sent as a message to the test_topic topic. For example:
  ```
  > Hello Kafka!
  > This is a test message.
  ```

## Demo: Kafka CLI - kafka-console-consumer.sh

- **Note:** If you haven’t created a topic a wrote message yet, do so now (described in the previous Demo). 

- Let’s read messages from the `test_topic` using the Kafka console consumer.
  ```
  kafka-console-consumer.sh --topic test_topic --from-beginning --bootstrap-server localhost:9092
  ```

- You can start consuming messages from a specific offset in a partition.
  ```
  kafka-console-consumer.sh --topic test_topic --partition 0 --offset 1 --bootstrap-server localhost:9092
  ```

## Demo: Kafka log segments

- check the official Kafka container file system (with its default settings for the log storage):
  ```
  docker exec -it kafka bash
  ```
  ```
  ls -ls /tmp/kraft-combined-logs
  ```
- "__consumer_offsets-*" folders are for the internal offset tracking system of Kafka, there are 50 paritions.

- check `test_topic-0`:
  ```
  ls -la /tmp/kraft-combined-logs/test_topic-0/
  ```

  ```
  # check messages
  cat /tmp/kraft-combined-logs/test_topic-0/00000000000000000000.log 
  ```


## Demo: Kafka CLI - kafka-consumer-groups.sh

- **Note:** If you haven’t created a topic a wrote message yet, do so now (described in the previous Demo). 

- Start a consumer that reads from `test_topic` and assign it to a consumer group named `test_group`. Open a new terminal to run the consumer.
  ```
  kafka-console-consumer.sh --topic test_topic --group test_group --bootstrap-server localhost:9092
  ```

- Describe the `test_group` to see the details of the consumer group
  ```
  kafka-consumer-groups.sh --describe --group test_group --bootstrap-server localhost:9092
  ```
  This will show details such as partition assignment, offsets, and lag.

- List all consumer groups to see the groups available:
  ```
  kafka-consumer-groups.sh --list --bootstrap-server localhost:9092
  ```

- You can reset offsets for the consumer group `test_group` to start from the beginning (if the group is inactive, so no client connects to Kafka):
  ```
  kafka-consumer-groups.sh --reset-offsets --group test_group --topic test_topic --to-earliest --execute --bootstrap-server localhost:9092
  ```

- You use `kafka-console-consumer.sh` to read with a specified group: 
  ```
  kafka-console-consumer.sh --topic test_topic --group test_group --bootstrap-server localhost:9092
  ```

- If you want to delete a consumer group, you can do it like:
  ```
  kafka-consumer-groups.sh --delete --group test_group --bootstrap-server localhost:9092
  ```

- If we would define a new consumer group it would not start from the beginning, since it checks `auto.offset.reset` setting which is `auto.offset.reset=latest` by default, i.e. consumer starts reading only new messages, skipping older ones.
  ```
  kafka-consumer-groups.sh --delete --group test_group_2 --bootstrap-server localhost:9092
  ```

## Demo: Basic Java producer sending just one "Hello world" message.

**Note**: The equivalent Python code can be found here: `python-demo/kafka-basics/producer_basic.py`

- Minimal producer 
- Sending only one message to one topic then exits

---

- Check file `java-demo/kafka-basics/src/main/java/org.example/ProducerBasic.java`

- Run java code

- Check whether the message is sent to the topic (for now we use kafka CLI for this and introduce the java consumer later):

   ```
   kafka-console-consumer.sh --bootstrap-server localhost:9092 --topic demo_topic --from-beginning
   ```

- Check consumer groups (a random group was created for the client): 

  ```
  kafka-consumer-groups.sh --list --bootstrap-server localhost:9092
  ```


## Demo: Introducing callback function

**Note**: The equivalent Python code can be found here: `python-demo/kafka-basics/producer_basic_with_callback.py`

- Using callback after sending message to handle other tasks
- Implementing confirmation of the sent message by logging the message metadata
---

- Create a `demo_topic` with 3 partitions:
  ```
  # deleteing previous setting
  kafka-topics.sh --bootstrap-server localhost:9092 --delete --topic demo_topic
  ```
  ```
  # re-create the new topic
  kafka-topics.sh --bootstrap-server localhost:9092 --create --topic demo_topic --partitions 3
  ```

- Check file: `src/main/java/org.example/ProducerBasicWithCallback.java`

- Run java code multiple times and notice that it chooses the partitions to send the message randomly


## Demo: Sticky partitioner vs. Round Robin partitioner setup

**Note**: There is no equivalent Python code for this case, since `kafka-python` and `confluent-kafka` libs do not have a direct partitioner setting (you could code such logic manually if there is a real need for it.)

- Sending multiple messages in rapid succession to showcase that by default the partition choice is not random

- Create a `demo_topic` with 3 partitions:

```
kafka-topics.sh --bootstrap-server localhost:9092 --create --topic demo_topic --partitions 3
```

- Check file: `src/main/java/org.example/ProducerBasicStickyPartitioner.java`

- Run java code and notice that partition does not changes randomly

- Check the Kafka Producer Config at the beginning of the logs, where `partitioner.class = null` means the StickyPartitioner usage by default

- Note: the batch size 400 in the code is only to be able to notice this behaviour. With the default 16KB batch size every message would be sent to one partition

- Let's uncomment line 30 with `RoundRobinPartitioner` partitioner and run the code again

- Note: the Producer Config now shows `partitioner.class = class org.apache.kafka.clients.producer.RoundRobinPartitioner`

- This is a much less effecient setup.

```
kafka-console-consumer.sh --bootstrap-server localhost:9092 --topic demo_topic --from-beginning
```


## Demo: Using keys when sending messages

**Note**: The equivalent Python code can be found here: `python-demo/kafka-basics/producer_basic_keys.py`

- Attach keys to the messages to ensure that related data goes to the same partition

- Create a `demo_topic` with 3 partitions:

```
kafka-topics.sh --bootstrap-server localhost:9092 --create --topic demo_topic --partitions 3
```

- Check file: `src/main/java/org.example/ProducerBasicKeys.java`

- Run java code and notice that messages with the same keys are sent to the same partition

- Note: by setting RoundRobinPartitioner we would ruin this behaviour.


## Demo: Basic Java consumer

**Note**: The equivalent Python code can be found here: `python-demo/kafka-basics/consumer_basic.py`

- Minimal consumer
  - set string deserializers
  - set group id
  - set read from the earliest
---

- Create a `demo_topic` with 3 partitions:

```
kafka-topics.sh --bootstrap-server localhost:9092 --create --topic demo_topic --partitions 3
```

- Check file `src/main/java/org.example/ConsumerBasic.java`

- Run java code

- Note: `auto.offset.reset = earliest`, `group.id`, `key.deserializer` and `value.deserializer` is configured as we set from code.

- Note: the consumer receives the data in batches by partition because of efficiency

- Let's start `ProducerBasicKeys.java` to check it receives the new data

- Let's restart the consumer and check that this time it does not receives the data from the beginning, instead it rejoins to the offset where it left for each partition

- Note: since we haven't implemented a proper shutdown it might take some time to find out the proper offsets


## Demo: Adding proper shutdown implementation to the consumer

**Note**: The equivalent Python code can be found here: `python-demo/kafka-basics/consumer_basic_proper_shutdown.py`

- Implementing proper shutdown 
---

- Create a `demo_topic` with 3 partitions:

```
kafka-topics.sh --bootstrap-server localhost:9092 --create --topic demo_topic --partitions 3
```

- Check file `src/main/java/org.example/ConsumerBasicProperShutdown.java`

- Run java code and after a while stop it

- Check the log messages starting from `Detected a shutdown, let's exit by calling consumer.wakeup()...`. The consumer properly revoke partitions and send leaving group request.

- Let's restart the application and check in the logs that all the partitions are assigned to it: `Adding newly assigned partitions: demo_topic-0, demo_topic-1, demo_topic-2`

- Let's start a second instance of the consumer and check how the partitions are rebalanced:

```
Configure to run multiple instances in Intellij:
1. Edit configurations...
2. Modify options
3. Allow multiple instances
4. Start the new instance
```

- Let's start a third instance and check the logs again

- Run `ProducerBasicKeys.java` to generate messages and check how the consumers receives them

- Let's stop the first instance and check the logs again and note how the partitions are rebalanced due to the proper shutdown mechanism

- Let's stop the second instance 

- Let's stop the third instance


## Demo: Partition Rebalance - Setting cooperative consumers

**Note**: There is no equivalent Python code for this case, since `kafka-python` and `confluent-kafka` do not support the cooperative-sticky assignment strategy natively. This is a feature supported by the underlying Kafka client in Java but not exposed directly in these libraries.
However, you can still implement custom partition assignment strategies in Python by manually managing partitions:
https://github.com/confluentinc/confluent-kafka-python/issues/1071

- Configure `CooperativeStickyAssignor` partition assignment strategy to not revoke all partitions before rebalancing 

- Create a `demo_topic` with 3 partitions:

  ```
  kafka-topics.sh --bootstrap-server localhost:9092 --create --topic demo_topic --partitions 3
  ```

- Check file `src/main/java/org.example/ConsumerBasicCooperative.java`

- Run three instance of the application and check the partition rebalancing without revoking all the partitions first.

```
Configure to run multiple instances in Intellij:
1. Edit configurations...
2. Modify options
3. Allow multiple instances
4. Start the new instance
```

- In case the consumer - partition mapping is important, e.g. always the same consumer reads the same partition you can configure a `group.instance.id` that is used to wait for some time for a disappeared consumer to rejoin before assigning the partition to another consumer. 


## Demo: Real world scenario - Blockchain Producer

**Note**: The equivalent Python code can be found here: `python-demo/blockchain_producer/blockchain_producer.py`

- Implementing a Producer that connects to a provider to get real-time notifications about new blockchain transactions and blocks.
- Connecting to the provider via WebSocket 
- Write the transaction data to a Kafka topic
---

- Check file `java-demo/blockchain-producer/src/main/java/org/example/BlockchainProducer.java`

- For now we let the application creating the topic with name `blockchain.transaction.json`

- Run java code and check the messages with the CLI consumer:

  ```
  kafka-console-consumer.sh --bootstrap-server localhost:9092 --topic blockchain.transaction.json --from-beginning
  ```


## Demo: Real world scenario - Blockchain Producer - Compression (High throughput)

**Note**: The equivalent Python code can be found here: `python-demo/blockchain_producer/blockchain_producer_compression.py`

- Implementing compression at Producer side
---

- Create a `demo_topic` with 3 partitions:

```
kafka-topics.sh --bootstrap-server localhost:9092 --create --topic demo_topic --partitions 3
```

- Check file `java-demo/blockchain-producer/src/main/java/org/example/BlockchainProducerCompression.java`

- Run java code 

- Check the log messages for the configured values `compression.type`, `linger.ms`, `batch.size`

- Note that the messages can get by the consumer without any setup for that:

  ```
  kafka-console-consumer.sh --bootstrap-server localhost:9092 --topic demo_topic --from-beginning
  ```


## Demo: Real world scenario - Blockchain Consumer to OpenSearch

**Note**: The equivalent Python code can be found here: `python-demo/opensearch_consumer/opensearch_consumer.py`

- Implementing a Consumer that reads from the blockchain Kafka topic and send the messages to a local OpeanSearch instance via REST API calls
---

- Let's setup first a local OpenSearch instance with `docker-compose`

Check the compose file in `./java-demo/opensearch-consumer/` folder: 
```
cat docker-compose.yaml 
```

Create the instances and access OpenSearch at http://localhost:5601:
```
docker-compose up
```

- Familiarize yourself with OpenSearch briefly: 

Check `Dev Tools` under `Management` 

Get some general information about the OpenSearch instance: 

```
GET /
```

Create an index: 

```
PUT my_index
```

Get the index:

```
GET my_index
```

Create a record: 

```
PUT my_index/_doc/1
{
  "body": "here"
}
```

Get the record: 

```
GET my_index/_doc/1
```

- Check file `java-demo/opensearch-consumer/src/main/java/org/example/OpenSearchConsumer.java`

- Run java code

- Since it reads `latest` offset you have to start `BlockchainProducer.java` as well 

- It creates the `blockchain` index and writes the messages from the kafka topic `blockchain.transaction.json` for every insert it logs a message id

- On the OpenSearch `Dev Tools` menu query an inserted message: 

```
GET blockchain/_doc/<RECORD-ID>
```


## Demo: Real World scenarion - Delivery Semantics - Least-once auto commit strategy (quite common one)

**Note**: The equivalent Python code can be found here: `python-demo/kafka-basics/opensearch_consumer.py`

- The consumer commit offsets when in calls `.poll()` and `auto.commit.interval.ms` has elapsed 
- Our codes are already written this way, since:
  - `auto.commit.interval.ms=5000` and `enable.auto.commit=true` are the default setting
  - the code makes sure that all messages are successfully processed before calling poll again
- So here we do not have to do any modification just enough to note the relevant parts

- Check file `java-demo/opensearch-consumer/src/main/java/org/example/OpenSearchConsumer.java`

- Run the code and check the relevant settings in the logs.
- Also check the code from line 141 where we process messages before polling again and do proper error handling.


## Demo: Real world scenario - Blockchain Consumer to OpenSearch - Idempotence

**Note**: The equivalent Python code can be found here: `python-demo/opensearch_consumer/opensearch_consumer_idempotence.py`

- Supporting idempotence in a way to align to the receiving system, here OpenSearch
- When OpenSearch receives record with the same ID it does not create a new record, instead it updates the existing one
- In our consumer we can build on this and calculate the ID to support and idempotence behavior
- Note: if OpenSearch would work otherwise we should consider implementing idempotence elsewhere, e.g. keep tracking on consumer side what we sent already which would be more complicated.
---

- Let's setup first a local OpenSearch instance with `docker-compose` (`./java-demo/opensearch-consumer/docker-compose.yml`) if it is not running

- Check file `java-demo/opensearch-consumer/src/main/java/org/example/OpenSearchConsumerIdempotence.java`

- With method `extractId` we use the `hash` field which we can consider unique for this demo and use it as ID for OpenSearch.

- Run java code

- On the OpenSearch `Dev Tools` menu query an inserted message: 

```
GET blockchain/_doc/<RECORD-ID>
```

- Just to showcase OpenSearch update behaviour try to update a record and query it (so with incorrect id-s we could ruin idempotency for the process):

```
PUT blockchain/_doc/<RECORD-ID>
{
  "body": "here"
}
```

```
GET blockchain/_doc/<RECORD-ID>
```


## Demo: Real world scenario - Blockchain Consumer to OpenSearch - Commit Strategy

**Note**: The equivalent Python code can be found here: `python-demo/opensearch_consumer/opensearch_consumer_manual_commit.py`

- Implementing manual commit by disabling auto commit and call it directly after message processing
---

- Check file `java-demo/opensearch-consumer/src/main/java/org/example/OpenSearchConsumerManualCommit.java`

- At line 79 we disable the auto commit behavior by setting `ENABLE_AUTO_COMMIT_CONFIG` to `false`

- Before running the code check the offset with Kafka CLI: 

```
kafka-consumer-groups.sh --bootstrap-server localhost:9092 --group consumer-opensearch-demo  --describe
```

- Start the code and `BlockchainProducer.java` as well if there is no message to process

- The Consumer inserts records to OpenSearch, however, not commits them to Kafka, so the offset is not increased

- Rerun Consumer multiple times and check that it reads the same messages again and that the offset not changes: 

```
kafka-consumer-groups.sh --bootstrap-server localhost:9092 --group consumer-opensearch-demo  --describe
```

- Uncomment line 168 to implement the commit mechanism after message processing, rerun the code and check the offsets again
- Now we should see the consumer working properly


## Demo: Real world scenario - Blockchain Consumer to OpenSearch - Offset Behaviour

**Note**: The equivalent Python code can be found here: `python-demo/opensearch_consumer/opensearch_consumer_manual_commit.py`

- Showcasing how we can reset the offset for a specific group and for a specific partition in it. 
- For this no code modification is necessary, we use Kafka CLI tools mostly.
---

- Describe the `consumer-opensearch-demo` consumer group:

```
kafka-consumer-groups.sh --describe --group consumer-opensearch-demo --bootstrap-server localhost:9092
```

- Run `OpenSearchConsumerManualCommit.java` (if no processable messages then run a Blockchain Producer as well)

- Describe again the group: 

```
kafka-consumer-groups.sh --describe --group consumer-opensearch-demo --bootstrap-server localhost:9092
```

- Re-set the offset for the topic, for the group: 
```
kafka-consumer-groups.sh --bootstrap-server localhost:9092 --group consumer-opensearch-demo --topic blockchain.transaction.json --reset-offsets --to-offset 44 --execute
```

- Describe again the group: 

```
kafka-consumer-groups.sh --describe --group consumer-opensearch-demo --bootstrap-server localhost:9092
```

- Re-set the offset for the topic, for the group, for partition 0: 
```
kafka-consumer-groups.sh --bootstrap-server localhost:9092 --group consumer-opensearch-demo --topic blockchain.transaction.json:0 --reset-offsets --to-offset 44 --execute
```

- Describe again the group: 

```
kafka-consumer-groups.sh --describe --group consumer-opensearch-demo --bootstrap-server localhost:9092
```

Note: you can check this with the earlier demo in which we create `demo_topic` with 3 partitions and use a basic Producer and Consumer to write and read.


## Demo: Real world scenario - Blockchain Consumer to OpenSearch - Bulk Request

**Note**: The equivalent Python code can be found here: `python-demo/opensearch_consumer/opensearch_consumer_bulk_request.py`

- Implementing a more efficient workflow where the Consumer waits several messages together and sends them in a batch request to OpenSearch
---

- Check file `java-demo/opensearch-consumer/src/main/java/org/example/OpenSearchConsumerBulkRequest.java`

- Before running the code reset the offset to see how efficient the processing will be:

Check current offset:
```
kafka-consumer-groups.sh --bootstrap-server localhost:9092 --group consumer-opensearch-demo  --describe
```
Reset the offset:
```
kafka-consumer-groups.sh --bootstrap-server localhost:9092 --group consumer-opensearch-demo --reset-offsets --to-earliest --topic blockchain.transaction.json --execute
```

- Run the code and check the logs how the records are grouped together.


## Demo: Kafka UI

- To get familiarized with Kafka UI a local setup with `docker-compose` is the easiest and quickest way. 
- With `docker-compose` we create a one node local Kafka cluster and a Kafka UI instance


Check the compose file in `./kafka-ui` folder: 
```
cat docker-compose.yaml 
```

Kafka advertises two address, `kafka:29092` is to be available for Kafka UI and `localhost:9092` to be available for Kafka CLI from local terminal to be able to create/write/read topics.

Create the instances:
```
docker-compose up
```

Access Kafka UI at http://localhost:8080

Connect to our local test Kafka cluster:
- set `Cluster name` to "local-kafka"
- set `Bootstrap Servers` to "kafka:29092" 
- click on Validate

Note: there are other Kafka APIs (e.g. Kafka Connect, KSQL DB ...) that we won't demo now but are available with Kafka UI.

- After some seconds we should see our cluster (without page refresh)

- Create some topics and messages with Kafka CLI:

```
kafka-topics.sh --bootstrap-server localhost:9092 --create --topic first_topic --partitions 5
kafka-topics.sh --bootstrap-server localhost:9092 --create --topic second_topic --partitions 2

kafka-console-producer.sh --bootstrap-server localhost:9092 --topic first_topic

kafka-console-consumer.sh --bootstrap-server localhost:9092 --topic first_topic --from-beginning
```

- Go to `Topics` menu 

- Check the submenus `Messages`, `Consumers`, `Settings` and `Statistics`

- Create a `third_topic` via Kafka-UI on `Topics` with the `+ Add a Topic` button

- Go to the `third_topic` and create message with the `Produce Message` button

- On `Topics` delete the `third_topic` (you can choose to delete only the messages)


## Demo: Kafdrop

- To get familiarized with Kafdrop a local setup with `docker-compose` is the easiest and quickest way. 
- With `docker-compose` we create a one node local Kafka cluster and a Kafdrop instance


Check the compose file in `./kafdrop` folder: 
```
cat docker-compose.yaml 
```

Kafka advertises two address, `kafka:29092` is to be available for Kafka UI and `localhost:9092` to be available for Kafka CLI from local terminal to be able to create/write/read topics.

Create the instances:
```
docker-compose up
```

Access Kafka UI at http://localhost:9000

- Create some topics and messages with Kafka CLI:

```
kafka-topics.sh --bootstrap-server localhost:9092 --create --topic first_topic --partitions 5
kafka-topics.sh --bootstrap-server localhost:9092 --create --topic second_topic --partitions 2

kafka-console-producer.sh --bootstrap-server localhost:9092 --topic first_topic

kafka-console-consumer.sh --bootstrap-server localhost:9092 --topic first_topic --from-beginning
```

- Under `Topics` section you can find the available topics 

- Click on any topic and you can search for messages

- Under `Topics` you also have the option to add a new one and for any topic the delete is also available


## Demo: Kafka Streams Application - WordCount

- Implementing a Kafka Streams application that reads from a topic with messages containing space separated words, count the occurence of each word and writes it back to another topic.
---

- Create two topics `word_count_input` and `word_count_output`:
  ```
  kafka-topics.sh --create --topic word_count_input --bootstrap-server localhost:9092 --partitions 1 --replication-factor 1
  kafka-topics.sh --create --topic word_count_output --bootstrap-server localhost:9092 --partitions 1 --replication-factor 1
  ```

- Start the java application at `java-demo/kafka-streams/src/main/java/org.example/WordCount.java`

- Run java code

- Check the extisting topics and note that internal topics are created for the WordCount streams app:
  ```
  kafka-topics.sh --list --bootstrap-server localhost:9092
  ```

- You can start consuming messages from the topic
  ```
  kafka-console-consumer.sh --bootstrap-server localhost:9092 --topic word_count_output --from-beginning --formatter org.apache.kafka.tools.consumer.DefaultMessageFormatter --property print.key=true --property print.value=true --property key.deserializer=org.apache.kafka.common.serialization.StringDeserializer --property value.deserializer=org.apache.kafka.common.serialization.LongDeserializer
  ```

- Start the Kafka console producer. This allows you to send messages to your Kafka topic directly from the command line:
  ```
  kafka-console-producer.sh --topic word_count_input --bootstrap-server localhost:9092
  ```
  
