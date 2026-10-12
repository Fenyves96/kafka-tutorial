from kafka import KafkaConsumer
import logging
import signal
import sys

# Set up the logger
logging.basicConfig(level=logging.INFO)
log = logging.getLogger("ConsumerBasic")

def log_client_config(client):
    config = client.config
    for key, value in config.items():
        log.info(f"{key}: {value}")

def shutdown_consumer(consumer):
    log.info("Shutdown signal received, closing the consumer...")
    consumer.close()
    log.info("The consumer is now gracefully shut down")
    sys.exit(0)

def main():
    log.info("I am a Kafka Consumer!")
    
    group_id = "my-python-application"
    topic = "second_topic"
    
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
    log_client_config(producer)

    def handle_signal(signum, frame):
        shutdown_consumer(consumer)

    signal.signal(signal.SIGINT, handle_signal)
    signal.signal(signal.SIGTERM, handle_signal)

    try:
        while True:
            log.info("Polling")
            for msg in consumer:
                log.info(f"Key: {msg.key}, Value: {msg.value}")
                log.info(f"Partition: {msg.partition}, Offset: {msg.offset}")
    except KeyboardInterrupt:
        log.info("Consumer interrupted")
    except Exception as e:
        log.error("Unexpected exception in the consumer: %s", e)
    finally:
        shutdown_consumer(consumer)

if __name__ == "__main__":
    main()
