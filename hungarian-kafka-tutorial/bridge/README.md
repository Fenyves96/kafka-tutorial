# Using Kafka Bridge

## 1. Install Kafka Bridge with Strimzi

You can easily create a Kafka Bridge for your Kafka Cluster with the following example `yaml`˙:
```
apiVersion: kafka.strimzi.io/v1beta2
kind: KafkaBridge
metadata:
  name: my-bridge
  namespace: kafka
spec:
  replicas: 1
  bootstrapServers: my-cluster-kafka-bootstrap:9092
  http:
    port: 8080
```

```
kubectl -n kafka apply -f bridge.yaml
```

Start a port-forward, so you can access the API on your localhost:
```
kubectl port-forward -n kafka svc/my-bridge-bridge-service 8080:8080
```


## 2. Producing messages to topics and partitions

We can use the Kafka Bridge to produce messages to a Kafka topic in JSON format by using the topics endpoint.

We can specify destination partitions for messages in the request body. 

The `partitions` endpoint provides an alternative method for specifying a single destination partition for all messages as a path parameter.
In this procedure, messages are produced to a topic called `bridge-topic`.

First, let's use the `kafka-topics.sh` utility to create this topic.

Example topic creation with three partitions:
```
# we use here kafka-topics.sh from a Kafka deployment on Kubernetes, but of course, you can use it from elsewhere too.
kubectl exec -it -n kafka my-cluster-broker-0 -- \
  bin/kafka-topics.sh --bootstrap-server localhost:9092 \
  --create --topic bridge-topic \
  --partitions 3 --replication-factor 1 \
  --config min.insync.replicas=1

# verifying the topic was created
kubectl exec -it -n kafka my-cluster-broker-0  -- bin/kafka-topics.sh --bootstrap-server localhost:9092 --describe --topic bridge-topic
```

Now use the Kafka Bridge, produce three messages to the topic you created:
```
curl -X POST \
  http://localhost:8080/topics/bridge-topic \
  -H 'content-type: application/vnd.kafka.json.v2+json' \
  -d '{
    "records": [
        {
            "key": "1my-key",
            "value": "sales-lead-0001"
        },
        {
            "value": "1sales-lead-0002",
            "partition": 2
        },
        {
            "value": "1sales-lead-0003"
        }
    ]
}'
```

In this scenario:
 - `sales-lead-0001` is sent to a partition based on the hash of the key.
 - `sales-lead-0002` is sent directly to partition 2.
 - `sales-lead-0003` is sent to a partition in the bridge-topic topic using a round-robin method.

If the request is successful, the Kafka Bridge returns an offsets array, along with a 200 code and a content-type header of `application/vnd.kafka.v2+json`. 
For each message, the offsets array describes:
 - The partition that the message was sent to
 - The current message offset of the partition

An example response looks like this
```
{
  "offsets":[
    {
      "partition":0,
      "offset":0
    },
    {
      "partition":2,
      "offset":0
    },
    {
      "partition":0,
      "offset":1
    }
  ]
}
```

Make other curl requests to find information on topics and partitions.
```
# List topics:
curl -X GET \
  http://localhost:8080/topics | jq

# Example response
[
  "__strimzi_store_topic",
  "__strimzi-topic-operator-kstreams-topic-store-changelog",
  "bridge-topic",
  "my-topic"
]

# Get topic configuration and partition details
curl -X GET \
  http://localhost:8080/topics/bridge-topic | jq

# Example response
{
  "name": "bridge-topic",
  "configs": {
    "compression.type": "producer",
    "leader.replication.throttled.replicas": "",
    "min.insync.replicas": "1",
    "message.downconversion.enable": "true",
    "segment.jitter.ms": "0",
    "cleanup.policy": "delete",
    "flush.ms": "9223372036854775807",
    "follower.replication.throttled.replicas": "",
    "segment.bytes": "1073741824",
    "retention.ms": "604800000",
    "flush.messages": "9223372036854775807",
    "message.format.version": "2.8-IV1",
    "max.compaction.lag.ms": "9223372036854775807",
    "file.delete.delay.ms": "60000",
    "max.message.bytes": "1048588",
    "min.compaction.lag.ms": "0",
    "message.timestamp.type": "CreateTime",
    "preallocate": "false",
    "index.interval.bytes": "4096",
    "min.cleanable.dirty.ratio": "0.5",
    "unclean.leader.election.enable": "false",
    "retention.bytes": "-1",
    "delete.retention.ms": "86400000",
    "segment.ms": "604800000",
    "message.timestamp.difference.max.ms": "9223372036854775807",
    "segment.index.bytes": "10485760"
  },
  "partitions": [
    {
      "partition": 0,
      "leader": 0,
      "replicas": [
        {
          "broker": 0,
          "leader": true,
          "in_sync": true
        },
        {
          "broker": 1,
          "leader": false,
          "in_sync": true
        },
        {
          "broker": 2,
          "leader": false,
          "in_sync": true
        }
      ]
    },
    {
      "partition": 1,
      "leader": 2,
      "replicas": [
        {
          "broker": 2,
          "leader": true,
          "in_sync": true
        },
        {
          "broker": 0,
          "leader": false,
          "in_sync": true
        },
        {
          "broker": 1,
          "leader": false,
          "in_sync": true
        }
      ]
    },
    {
      "partition": 2,
      "leader": 1,
      "replicas": [
        {
          "broker": 1,
          "leader": true,
          "in_sync": true
        },
        {
          "broker": 2,
          "leader": false,
          "in_sync": true
        },
        {
          "broker": 0,
          "leader": false,
          "in_sync": true
        }
      ]
    }
  ]
}

# List the partitions of a specific topic
curl -X GET \
  http://localhost:8080/topics/bridge-topic/partitions | jq

# Example response
[
  {
    "partition": 0,
    "leader": 0,
    "replicas": [
      {
        "broker": 0,
        "leader": true,
        "in_sync": true
      },
      {
        "broker": 1,
        "leader": false,
        "in_sync": true
      },
      {
        "broker": 2,
        "leader": false,
        "in_sync": true
      }
    ]
  },
  {
    "partition": 1,
    "leader": 2,
    "replicas": [
      {
        "broker": 2,
        "leader": true,
        "in_sync": true
      },
      {
        "broker": 0,
        "leader": false,
        "in_sync": true
      },
      {
        "broker": 1,
        "leader": false,
        "in_sync": true
      }
    ]
  },
  {
    "partition": 2,
    "leader": 1,
    "replicas": [
      {
        "broker": 1,
        "leader": true,
        "in_sync": true
      },
      {
        "broker": 2,
        "leader": false,
        "in_sync": true
      },
      {
        "broker": 0,
        "leader": false,
        "in_sync": true
      }
    ]
  }
]

# List the details of a specific topic partition
curl -X GET \
  http://localhost:8080/topics/bridge-topic/partitions/0 | jq

# Example response
{
  "partition": 0,
  "leader": 0,
  "replicas": [
    {
      "broker": 0,
      "leader": true,
      "in_sync": true
    },
    {
      "broker": 1,
      "leader": false,
      "in_sync": true
    },
    {
      "broker": 2,
      "leader": false,
      "in_sync": true
    }
  ]
}

# List the offsets of a specific topic partition
curl -X GET \
  http://localhost:8080/topics/bridge-topic/partitions/0/offsets | jq

# Example response
{
  "beginning_offset": 0,
  "end_offset": 1
}
```

## 3. Creating a Kafka Bridge consumer
Before you can perform any consumer operations in the Kafka cluster, you must first create a consumer by using the consumers endpoint. 
The consumer is referred to as a Kafka Bridge consumer.

Create a Kafka Bridge consumer in a new consumer group named `bridge-consumer-group`:
```
curl -X POST http://localhost:8080/consumers/bridge-consumer-group \
  -H 'content-type: application/vnd.kafka.v2+json' \
  -d '{
    "name": "bridge-consumer",
    "auto.offset.reset": "earliest",
    "format": "json",
    "enable.auto.commit": false,
    "fetch.min.bytes": 512,
    "consumer.request.timeout.ms": 30000
  }'
```

The consumer is named `bridge-consumer` and the embedded data format (deserialization) is set as json.
Some basic configuration settings are defined.
The consumer will not commit offsets to the log automatically because the `enable.auto.commit` setting is `false`. 
We will commit the offsets manually later in this guide.

If the request is successful, the Kafka Bridge returns the consumer ID (`instance_id`) and base URL (`base_uri`) in the response body, along with a 200 code.

Example response:
```
{
  "instance_id": "bridge-consumer",
  "base_uri":"http://<bridge_id>-bridge-service:8080/consumers/bridge-consumer-group/instances/bridge-consumer"
}
```

Copy the base URL (`base_uri`) to use in the other consumer operations in this guide.


## 4. Subscribing a Kafka Bridge consumer to topics

After you have created a Kafka Bridge consumer, subscribe it to one or more topics by using the subscription endpoint. 
When subscribed, the consumer starts receiving all messages that are produced to the topic.

Subscribe the consumer to the `bridge-topic` topic that you created earlier, in `Producing messages to topics and partitions`:
```
curl -X POST http://localhost:8080/consumers/bridge-consumer-group/instances/bridge-consumer/subscription \
  -H 'content-type: application/vnd.kafka.v2+json' \
  -d '{
    "topics": [
        "bridge-topic"
    ]
}'
```

The topics array can contain a single topic (as shown here) or multiple topics. 
If you want to subscribe the consumer to multiple topics that match a regular expression, 
you can use the `topic_pattern` string instead of the topics array.

If the request is successful, the Kafka Bridge returns a 204 (No Content) code only.

When using an Apache Kafka client, the HTTP subscribe operation adds topics to the local consumer’s subscriptions. 
Joining a consumer group and obtaining partition assignments occur after running multiple HTTP poll operations, 
starting the partition rebalance and join-group process. 
It’s important to note that the initial HTTP poll operations may not return any records.


## 5 Retrieving the latest messages from a Kafka Bridge consumer

Retrieve the latest messages from the Kafka Bridge consumer by requesting data from the records endpoint. 
In production, HTTP clients can call this endpoint repeatedly (in a loop).
Produce additional messages to the Kafka Bridge consumer, as described in Producing messages to topics and partitions:

```
# sending six more messages
curl -X POST \
  http://localhost:8080/topics/bridge-topic \
  -H 'content-type: application/vnd.kafka.json.v2+json' \
  -d '{
    "records": [
        { "key": "lead-001", "value": "sales-lead-0001" },
        { "key": "lead-002", "value": "sales-lead-0002", "partition": 2 },
        { "key": "lead-003", "value": "sales-lead-0003" },
        { "key": "lead-004", "value": "sales-lead-0004", "partition": 1 },
        { "key": "lead-005", "value": "sales-lead-0005" },
        { "key": "lead-006", "value": "sales-lead-0006", "partition": 0 }
    ]
}'
```

Submit a GET request to the records endpoint:
```
curl -X GET http://localhost:8080/consumers/bridge-consumer-group/instances/bridge-consumer/records \
  -H 'accept: application/vnd.kafka.json.v2+json'
```

After creating and subscribing to a Kafka Bridge consumer, a first GET request will return an empty response because the 
poll operation starts a rebalancing process to assign partitions.

Repeat this step to retrieve messages from the Kafka Bridge consumer.

The Kafka Bridge returns an array of messages — describing the topic name, key, value, partition, and offset — in the response body, along with a 200 code. 

```
  {
    "topic":"bridge-topic",
    "key":"my-key",
    "value":"sales-lead-0001",
    "partition":0,
    "offset":0
  },
  {
    "topic":"bridge-topic",
    "key":null,
    "value":"sales-lead-0003",
    "partition":0,
    "offset":1
  },
```

Note: if an empty response is returned, produce more records to the consumer as described in Producing messages to topics and partitions, and then try retrieving messages again. (Even with `auto.offset.reset=earliest` and `enable.auto.commit=false`, Kafka Bridge maintains offset state in memory for each consumer instance — so repeated fetches only return new messages unless the instance is recreated.).

After retrieving messages from a Kafka Bridge consumer, try committing offsets to the log.


## 6 . Commiting offsets to the log

Use the offsets endpoint to manually commit offsets to the log for all messages received by the Kafka Bridge consumer.
This is required because the Kafka Bridge consumer that you created earlier, in Creating a Kafka Bridge consumer, 
was configured with the `enable.auto.commit` setting as `false`.

Commit offsets to the log for the bridge-consumer:
```
curl -X POST http://localhost:8080/consumers/bridge-consumer-group/instances/bridge-consumer/offsets
```

Because no request body is submitted, offsets are committed for all the records that have been received by the consumer. 
Alternatively, the request body can contain an array (OffsetCommitSeekList) that specifies the topics and partitions that you want to commit offsets for.

If the request is successful, the Kafka Bridge returns a 204 code only.

After committing offsets to the log, try out the endpoints for seeking to offsets.

## 7. Seeking to offsets for a partition
Use the positions endpoints to configure the Kafka Bridge consumer to retrieve messages for a partition from a specific offset, and then from the latest offset. This is referred to in Apache Kafka as a seek operation.

Seek to a specific offset for partition 0 of the `bridge-topic` topic:
```
curl -X POST http://localhost:8080/consumers/bridge-consumer-group/instances/bridge-consumer/positions \
  -H 'content-type: application/vnd.kafka.v2+json' \
  -d '{
    "offsets": [
        {
            "topic": "bridge-topic",
            "partition": 0,
            "offset": 2
        }
    ]
}'
```
If the request is successful, the Kafka Bridge returns a 204 code only.

Submit a GET request to the records endpoint:
```
curl -X GET http://localhost:8080/consumers/bridge-consumer-group/instances/bridge-consumer/records \
  -H 'accept: application/vnd.kafka.json.v2+json'
```

The Kafka Bridge returns messages from the offset that you seeked to.

Restore the default message retrieval behavior by seeking to the last offset for the same partition. This time, use the positions/end endpoint.
```
curl -X POST http://localhost:8080/consumers/bridge-consumer-group/instances/bridge-consumer/positions/end \
  -H 'content-type: application/vnd.kafka.v2+json' \
  -d '{
    "partitions": [
        {
            "topic": "bridge-topic",
            "partition": 0
        }
    ]
}'
```

If the request is successful, the Kafka Bridge returns another 204 code.

Note: you can also use the positions/beginning endpoint to seek to the first offset for one or more partitions.

## 8. Deleting a Kafka Bridge consumer

Delete the Kafka Bridge consumer by sending a DELETE request to the instances endpoint.
```
curl -X DELETE http://localhost:8080/consumers/bridge-consumer-group/instances/bridge-consumer
```
If the request is successful, the Kafka Bridge returns a 204 code.
