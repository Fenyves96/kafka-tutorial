# Kafka MirrorMaker 2

## 1. Creating two Kafka test clusters (with just one node for each), since we want to carry out replication from `other-cluster` to `my-cluster`:

Create the cluster 
```
kubectl apply -f my-cluster.yaml -n kafka
watch kubectl get pods,pvc -n kafka

kubectl apply -f other-cluster.yaml -n kafka
watch kubectl get pods,pvc -n kafka
```

Check connectivity:
- These clusters currently are only accessible from inside the Kubernetes cluster.
We can check whether everything is all right by running a Kafka pod, exec into it, and run a simple producer / consumer:
```
kubectl -n kafka run kafka-scripts --image=quay.io/strimzi/kafka:0.44.0-kafka-3.8.0 -- sh -c "sleep infinity"

# simple topic listing - should return empty
kubectl exec -it -n kafka kafka-scripts -- bin/kafka-topics.sh --bootstrap-server my-cluster-kafka-bootstrap:9092 --list

# creating a topic
kubectl exec -it -n kafka kafka-scripts -- bin/kafka-console-producer.sh --bootstrap-server other-cluster-kafka-bootstrap:9092 --topic first-topic

# consuming messages
kubectl exec -it -n kafka kafka-scripts -- bin/kafka-console-consumer.sh --bootstrap-server other-cluster-kafka-bootstrap:9092 --topic first-topic --from-beginning
```

# Deploy MirrorMaker 2

This will create the KafkaMirrorMaker2 resource and start the replication process:
```
kubectl apply -f mirrormaker.yaml
```

Let's check logs of the MirrorMaker2 instance:
```
kubectl -n kafka logs my-mirror-maker-2-mirrormaker2-0
```

We should see successful connections to both clusters and synchronization processes

# Check topics in `my-cluster` which was empty before:

We sould see multiple mirrormaker2 topics and also a topic named `other-cluster.first-topic`:

```
kubectl exec -it -n kafka kafka-scripts -- bin/kafka-topics.sh --bootstrap-server my-cluster-kafka-bootstrap:9092 --list
```

Consume messages from the topic:
```
kubectl exec -it -n kafka kafka-scripts -- bin/kafka-console-consumer.sh --bootstrap-server my-cluster-kafka-bootstrap:9092 --topic other-cluster.first-topic --from-beginning
```

# Let's create a topic in `other-cluster` and verify replication:

Creating a `second-topic`:
```
kubectl exec -it -n kafka kafka-scripts -- bin/kafka-console-producer.sh --bootstrap-server other-cluster-kafka-bootstrap:9092 --topic second-topic
```

Checking replication:
```
kubectl exec -it -n kafka kafka-scripts -- bin/kafka-topics.sh --bootstrap-server my-cluster-kafka-bootstrap:9092 --list
```

Consume messages from the topic:
```
kubectl exec -it -n kafka kafka-scripts -- bin/kafka-console-consumer.sh --bootstrap-server my-cluster-kafka-bootstrap:9092 --topic other-cluster.second-topic --from-beginning
```