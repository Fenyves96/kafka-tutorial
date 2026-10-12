package org.example;

import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringSerializer;

import java.io.IOException;
import java.util.Properties;
import java.util.concurrent.TimeUnit;

public class BlockchainProducer {

    public static void main(String[] args) throws InterruptedException {

        // create Producer Properties
        Properties properties = new Properties();
        properties.setProperty(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, "127.0.0.1:9092");
        properties.setProperty(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        properties.setProperty(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());

        // create the Producer
        KafkaProducer<String, String> producer = new KafkaProducer<>(properties);

        String topic = "blockchain.transaction.json";

        BlockchainWebSocketClient BlockchainWebSocketClient = new BlockchainWebSocketClient(producer, topic);
        try {
            // https://www.blockchain.com/explorer/api/api_websocket
            BlockchainWebSocketClient.connect("wss://ws.blockchain.info/inv");
            BlockchainWebSocketClient.sendMessage("{op: unconfirmed_sub}");

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
            BlockchainWebSocketClient.close();
        }
    }
}
