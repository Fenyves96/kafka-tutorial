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
