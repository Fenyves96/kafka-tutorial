package org.example;

import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringSerializer;

import java.io.IOException;
import java.util.Properties;

public class BlockchainProducerCompression {

    public static void main(String[] args) throws InterruptedException {

        // create Producer Properties
        Properties properties = new Properties();
        properties.setProperty(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, "127.0.0.1:9092");
        properties.setProperty(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        properties.setProperty(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        // high throughput settings
        properties.setProperty(ProducerConfig.LINGER_MS_CONFIG, "30");
        properties.setProperty(ProducerConfig.BATCH_SIZE_CONFIG, Integer.toString(32*1024));
        properties.setProperty(ProducerConfig.COMPRESSION_TYPE_CONFIG, "gzip");

        // create the Producer
        KafkaProducer<String, String> producer = new KafkaProducer<>(properties);

        String topic = "blockchain.transaction-2.json";

        BlockchainWebSocketClient blockchainWebSocketClient = new BlockchainWebSocketClient(producer, topic);
        try {
            // https://www.blockchain.com/explorer/api/api_websocket
            blockchainWebSocketClient.connect("wss://ws.blockchain.info/inv");
            blockchainWebSocketClient.sendMessage("{op: unconfirmed_sub}");

            // Keep the main thread alive to receive messages
            while (true) {
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            blockchainWebSocketClient.close();
        }
    }
}
