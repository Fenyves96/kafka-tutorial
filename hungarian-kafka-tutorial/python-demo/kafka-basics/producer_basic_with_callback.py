from kafka import KafkaProducer
import logging

# Set up the logger
logging.basicConfig(level=logging.INFO)
log = logging.getLogger("ProducerBasic")

def log_client_config(client):
    config = client.config
    for key, value in config.items():
        log.info(f"{key}: {value}")

def on_send_success(record_metadata):
    log.info(f"Message delivered to {record_metadata.topic} | Partition: {record_metadata.partition}")

def on_send_error(excp):
    log.error('Error while producing', exc_info=excp)

def main():
    log.info("I am a Kafka Producer!")
    
    # Set up the Kafka producer configuration
    producer = KafkaProducer(
        bootstrap_servers='127.0.0.1:9092',
        key_serializer=lambda k: k.encode('utf-8') if k is not None else None,
        value_serializer=lambda v: v.encode('utf-8') if v is not None else None
    )

    # Log the client's configuration 
    log_client_config(producer)
    
    # Create a producer record
    topic = "demo_topic"
    #key = "key1"  # You can set this to None if you don't have a key
    value = "hello world"
    
    # Send data
    #future = producer.send(topic, key=key, value=value)
    future = producer.send(topic, value=value)
    
    # Add callback for delivery report
    future.add_callback(on_send_success)
    future.add_errback(on_send_error)

    # Block until the messages are sent
    producer.flush()

if __name__ == "__main__":
    main()
