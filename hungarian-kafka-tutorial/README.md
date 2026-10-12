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

- Check file `java-demo/blockchain-producer/src/main/java/org/example/BlockchainProducerCompression.java`

- Run java code 

- Check the log messages for the configured values `compression.type`, `linger.ms`, `batch.size`

- Note that the messages can get by the consumer without any setup for that:

  ```
  kafka-console-consumer.sh --bootstrap-server localhost:9092 --topic blockchain.transaction-2.json --from-beginning
  ```
