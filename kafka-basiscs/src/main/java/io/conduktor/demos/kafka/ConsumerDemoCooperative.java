package io.conduktor.demos.kafka;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.CooperativeStickyAssignor;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.errors.WakeupException;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.Arrays;
import java.util.Properties;

public class ConsumerDemoCooperative {
    private static final Logger logger = LoggerFactory.getLogger(ConsumerDemoCooperative.class);
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
        props.setProperty("partition.assignment.strategy", CooperativeStickyAssignor.class.getName());
        //none: if there is no consumer group -> faild
        //earliest: read from the beginning
        //lates: read from just now

        //create a consumer
        KafkaConsumer<String, String> consumer = new KafkaConsumer<>(props);

        //subscribe to a topic
        consumer.subscribe(Arrays.asList(topic));

        //get a reference to the main thread
        final Thread mainThread = Thread.currentThread();

        //adding the shutdown hook
        Runtime.getRuntime().addShutdownHook(new Thread() {
            public void run(){
                logger.info("Detected a shutdown, let's exit by calling consumer.wakeup()");
                consumer.wakeup();

                //join the main thread to allow the execution of the code in the main thread (waith for main code to finish)
                try {
                    mainThread.join();
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }


            }
        });
        try {
            //Poll for data
            while (true) {
                //wait for messages for 1s
                ConsumerRecords<String, String> records = consumer.poll(Duration.ofMillis(1000));

                for (ConsumerRecord<String, String> record : records) {
                    logger.info("key: {}, value: {}", record.key(), record.value());
                    logger.info("partition: {}, offset: {}", record.partition(), record.offset());
                }

            }
        }catch (WakeupException e) {
            logger.error("consumer is starting to shut down");
        }catch (Exception e) {
            logger.error("unexpected exception: "+e.getMessage());
        }
        finally {
            consumer.close(); //close the consumer, this will also commit offsets
            logger.info("gracefully shut down");
        }
    }
}
