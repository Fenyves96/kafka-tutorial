package io.conduktor.demos.kafka;

import org.apache.kafka.clients.producer.*;
import org.apache.kafka.common.serialization.StringSerializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Properties;

public class ProducerDemoCallback {
    private static final Logger logger = LoggerFactory.getLogger(ProducerDemoCallback.class);
    public static void main(String[] args) throws InterruptedException {
        logger.info("I am a kafka producer");
        //create producer properties
        Properties props = new Properties();
        props.setProperty("bootstrap.servers", "http://127.0.0.1:19092");

        //set produces properties
        props.setProperty("key.serializer", StringSerializer.class.getName());
        props.setProperty("value.serializer", StringSerializer.class.getName());
        props.setProperty("partitioner.class", RoundRobinPartitioner.class.getName());
        props.setProperty("batch.size", String.valueOf(400));
        //create the producer
        KafkaProducer<String, String> producer = new KafkaProducer<>(props);



        for (int j = 0; j < 30; j++) {
            for (int i = 0; i < 30; i++) {
                //create produce record
                ProducerRecord<String, String> producerRecord = new ProducerRecord<>("demo_java_topic", "Hello World Value!" + i);
                //send data
                producer.send(producerRecord, new Callback() {
                    @Override
                    public void onCompletion(RecordMetadata metadata, Exception e) {
                        //executes everytime a record sent or an exception is thrown
                        if (e == null) {
                            //the record was successfully sent
                            logger.info(" Received metadata:" + metadata.topic() +"\n"+
                                    "partition: " + metadata.partition()+"\n"+
                                    "offset: " + metadata.offset() +"\n" +
                                    "timestamp: " + metadata.timestamp() + "\n");
                        }
                        else {
                            logger.error("Error occurred while sending record: " + e );
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
