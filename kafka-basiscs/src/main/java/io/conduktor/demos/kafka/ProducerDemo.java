package io.conduktor.demos.kafka;

import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.StringSerializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Properties;

public class ProducerDemo {
    private static final Logger logger = LoggerFactory.getLogger(ProducerDemo.class);
    public static void main(String[] args) {
        logger.info("I am a kafka producer");
        //create producer properties
        Properties props = new Properties();
        props.setProperty("bootstrap.servers", "http://127.0.0.1:19092");

        //set produces properties
        props.setProperty("key.serializer", StringSerializer.class.getName());
        props.setProperty("value.serializer", StringSerializer.class.getName());
        //create the producer
        KafkaProducer<String, String> producer = new KafkaProducer<>(props);
        //create produce record
        ProducerRecord<String, String> producerRecord = new ProducerRecord<>("demo_java_topic", "Hello World Value!");
        //send data
        producer.send(producerRecord);
        //flush and close the producer
        //tell the producer to send all data and block until done --synchronous
        producer.flush();
        producer.close();
    }
}
