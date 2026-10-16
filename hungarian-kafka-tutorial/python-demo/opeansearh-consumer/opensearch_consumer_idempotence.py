import logging
import json
import signal
import sys
import time
from kafka import KafkaConsumer
from opensearchpy import OpenSearch, RequestsHttpConnection

# Set up the logger
logging.basicConfig(level=logging.INFO)
log = logging.getLogger("OpenSearchConsumer")

def log_client_config(client):
    config = client.config
    for key, value in config.items():
        log.info(f"{key}: {value}")

def create_open_search_client():
    conn_string = "http://localhost:9200"
    
    # OpenSearch client configuration
    client = OpenSearch(
        hosts=[{'host': 'localhost', 'port': 9200}],
        http_auth=('username', 'password'),  # if needed
        use_ssl=False,
        verify_certs=False,
        connection_class=RequestsHttpConnection
    )
    return client

def create_kafka_consumer():
    group_id = "consumer-opensearch-demo"
    # Kafka consumer configuration
    consumer = KafkaConsumer(
        'blockchain.transaction.json',
        bootstrap_servers='127.0.0.1:9092',
        group_id=group_id,
        key_deserializer=lambda x: x.decode('utf-8') if x is not None else None,
        value_deserializer=lambda x: x.decode('utf-8') if x is not None else None,
        auto_offset_reset='latest'
    )

    # Log the client's configuration 
    log_client_config(consumer)
    return consumer

def extract_id(json_str):
    return json.loads(json_str)["x"]["hash"]

def shutdown_consumer(consumer, open_search_client):
    log.info("Shutdown signal received, closing the consumer and OpenSearch client...")
    consumer.close()
    open_search_client.transport.close()
    log.info("The consumer and OpenSearch client are now gracefully shut down")
    sys.exit(0)

def main():
    log.info("I am a Kafka Consumer!")

    # Create OpenSearch client
    open_search_client = create_open_search_client()

    # Check if index exists, create if not
    index_name = "blockchain"
    if not open_search_client.indices.exists(index=index_name):
        open_search_client.indices.create(index=index_name)
        log.info("The Blockchain Index has been created!")
    else:
        log.info("The Blockchain Index already exists")

    # Create Kafka consumer
    consumer = create_kafka_consumer()

    def handle_signal(signum, frame):
        shutdown_consumer(consumer, open_search_client)

    signal.signal(signal.SIGINT, handle_signal)
    signal.signal(signal.SIGTERM, handle_signal)

    try:
        while True:
            log.info("Polling")
            records = consumer.poll(timeout_ms=3000)
            record_count = sum(len(record) for record in records.values())
            log.info(f"Received {record_count} record(s)")

            for record_batch in records.values():
                for record in record_batch:
                    try:
                        id_str = extract_id(record.value)
                        response = open_search_client.index(
                            index=index_name,
                            id=id_str,
                            body=json.loads(record.value)
                        )
                        log.info(f"Inserted document ID: {response['_id']}")
                    except Exception as e:
                        log.error(f"Failed to insert document ID: {id_str}, Error: {e}")

    except Exception as e:
        log.error("Unexpected exception in the consumer: %s", e)
    finally:
        shutdown_consumer(consumer, open_search_client)

if __name__ == "__main__":
    main()
