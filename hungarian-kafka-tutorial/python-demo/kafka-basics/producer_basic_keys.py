from kafka import KafkaProducer
from kafka.errors import KafkaError
import logging
import time

# Set up the logger
logging.basicConfig(level=logging.INFO)
log = logging.getLogger("ProducerBasicKeys")

def log_client_config(client):
    config = client.config
    for key, value in config.items():
        log.info(f"{key}: {value}")

def on_send_success(record_metadata, key): 
    log.info(f"Key: {key} | Partition: {record_metadata.partition}")  

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
    
    for j in range(2):
        for i in range(10):
            topic = "second_topic"
            key = f"id_{i}"
            value = f"hello world {i}"
            
            # Send data
            future = producer.send(topic, key=key, value=value)
            
            # Add callbacks
            future.add_callback(on_send_success, key=key)
            future.add_errback(on_send_error)
        
        time.sleep(0.5)
    
    # Block until all messages are sent
    producer.flush()

if __name__ == "__main__":
    main()
