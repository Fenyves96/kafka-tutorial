from kafka import KafkaConsumer
import logging

# Set up the logger
logging.basicConfig(level=logging.INFO)
log = logging.getLogger("ConsumerBasic")

def log_client_config(client):
    config = client.config
    for key, value in config.items():
        log.info(f"{key}: {value}")

def main():
    log.info("I am a Kafka Consumer!")
    
    group_id = "my-python-application"
    topic = "demo_topic"
    
    # Create Kafka consumer instance with string deserialization
    consumer = KafkaConsumer(
        topic,
        bootstrap_servers='127.0.0.1:9092',
        group_id=group_id,
        key_deserializer=lambda x: x.decode('utf-8') if x is not None else None, 
        value_deserializer=lambda x: x.decode('utf-8') if x is not None else None,
        auto_offset_reset='earliest'
    )

    # Log the client's configuration 
    log_client_config(consumer)

    for msg in consumer:
        log.info(f"Key: {msg.key}, Value: {msg.value}")
        log.info(f"Partition: {msg.partition}, Offset: {msg.offset}")

if __name__ == "__main__":
    try:
        main()
    except KeyboardInterrupt:
        log.info("Consumer interrupted")