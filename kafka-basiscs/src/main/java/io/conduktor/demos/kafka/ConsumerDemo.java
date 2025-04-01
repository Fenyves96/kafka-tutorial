package io.conduktor.demos.kafka;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.Arrays;
import java.util.Properties;

public class ConsumerDemo {
    private static final Logger logger = LoggerFactory.getLogger(ConsumerDemo.class);
    public static void main(String[] args) {
        logger.info("I am a kafka consumer");
        String groupId = "my-java-application";
        String topic = "demo_java";
        //create producer properties
        Properties props = new Properties();
        props.setProperty("bootstrap.servers", "http://127.0.0.1:19092");

        //set produces properties
        props.setProperty("key.serializer", StringSerializer.class.getName());
        props.setProperty("value.serializer", StringSerializer.class.getName());

        //create consumer configs
        props.setProperty("key.deserializer", StringDeserializer.class.getName());
        props.setProperty("value.deserializer", StringDeserializer.class.getName());

        props.setProperty("group.id", groupId);

        props.setProperty("auto.offset.reset", "earliest"); //none, earliest, latest
        //none: if there is no consumer group -> faild
        //earliest: read from the beginning
        //lates: read from just now

        //create a consumer
        KafkaConsumer<String, String> consumer = new KafkaConsumer<>(props);

        //subscribe to a topic
        consumer.subscribe(Arrays.asList(topic));

        //Poll for data
        while (true) {
            logger.info("polling...");
            //wait for messages for 1s
            ConsumerRecords<String, String > records = consumer.poll(Duration.ofMillis(1000));

            for (ConsumerRecord<String, String> record : records) {
                logger.info("key: {}, value: {}", record.key(), record.value());
                logger.info("partition: {}, offset: {}", record.partition(), record.offset());
            }

        }
    }
}
