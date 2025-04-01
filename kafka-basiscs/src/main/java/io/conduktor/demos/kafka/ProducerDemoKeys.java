package io.conduktor.demos.kafka;

import org.apache.kafka.clients.producer.*;
import org.apache.kafka.common.serialization.StringSerializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Properties;

public class ProducerDemoKeys {
    private static final Logger logger = LoggerFactory.getLogger(ProducerDemoKeys.class);

    public static void main(String[] args) throws InterruptedException {
        logger.info("I am a kafka producer");
        //create producer properties
        Properties props = new Properties();
        props.setProperty("bootstrap.servers", "http://127.0.0.1:19092");

        //set produces properties
        props.setProperty("key.serializer", StringSerializer.class.getName());
        props.setProperty("value.serializer", StringSerializer.class.getName());
        //create the producer
        KafkaProducer<String, String> producer = new KafkaProducer<>(props);

        for(int j = 0; j < 10; j++) {
            for (int i = 0; i < 10; i++) {
                String topic = "demo_java";
                String key = "id_" + i;
                String value = "hello world" + i;
                //create produce record
                ProducerRecord<String, String> producerRecord = new ProducerRecord<>(topic, key, value);
                //send data
                producer.send(producerRecord, new Callback() {
                    @Override
                    public void onCompletion(RecordMetadata metadata, Exception e) {
                        //executes everytime a record sent or an exception is thrown
                        if (e == null) {
                            //the record was successfully sent
                            logger.info("Key:" + key + "|" + "partition: " + metadata.partition() + "|");
                        } else {
                            logger.error("Error occurred while sending record: " + e);
                        }
                    }
                });
            }
            Thread.sleep(500);
        }


        //flush and close the producer
        //tell the producer to send all data and block until done --synchronous
        producer.flush();
        producer.close();
    }
}
