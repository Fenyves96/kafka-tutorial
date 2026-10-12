import json
import logging
import time
from kafka import KafkaProducer
from websocket import WebSocketApp

# Set up the logger
logging.basicConfig(level=logging.INFO)
log = logging.getLogger("BlockchainProducer")

def log_client_config(client):
    config = client.config
    for key, value in config.items():
        log.info(f"{key}: {value}")

class BlockchainWebSocketClient:
    def __init__(self, kafka_producer, topic):
        self.kafka_producer = kafka_producer
        self.topic = topic
        self.ws = None

    def on_open(self, ws):
        log.info("WebSocket connection established")
        ws.send(json.dumps({"op": "unconfirmed_sub"}))

    def on_message(self, ws, message):
        if message is not None:
            log.info(f"Received message: {message}")
            self.kafka_producer.send(self.topic, value=message.encode('utf-8'))
        else:
            log.warning("Received NoneType message")

    def on_close(self, ws, close_status_code, close_msg):
        log.info(f"WebSocket connection closed: {close_status_code} {close_msg}")
##        self.reconnect()

    def on_error(self, ws, error):
        log.error(f"WebSocket error: {error}")
##        self.reconnect()

    def connect(self, url):
        self.ws_url = url
        self.ws = WebSocketApp(
            url,
            on_open=self.on_open,
            on_message=self.on_message,
            on_close=self.on_close,
            on_error=self.on_error
        )
        self.ws.run_forever()

##    def reconnect(self):
##        log.info("Attempting to reconnect...")
##        time.sleep(5)
##        self.connect(self.ws_url)

    def close(self):
        if self.ws:
            self.ws.close()

def main():
    # Kafka Producer configuration
    producer_config = {
        'bootstrap_servers': '127.0.0.1:9092',
        'compression_type': 'gzip',
        'linger_ms': 10.0,
        'batch_size': 32768
    }

    producer = KafkaProducer(**producer_config)

    # Log the client's configuration 
    log_client_config(producer)

    topic = "blockchain.transaction.json"
    blockchain_ws_client = BlockchainWebSocketClient(producer, topic)

    try:
        blockchain_ws_client.connect("wss://ws.blockchain.info/inv")
        while True:
            time.sleep(1)
    except Exception as e:
        log.error(f"Exception occurred: {e}")
    finally:
        blockchain_ws_client.close()

if __name__ == "__main__":
    main()
