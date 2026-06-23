package io.conduktor.demos.wikimedia;

import com.launchdarkly.eventsource.MessageEvent;
import com.launchdarkly.eventsource.background.BackgroundEventHandler;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class WikimediaChangeHandler implements BackgroundEventHandler {
    KafkaProducer<String, String> producer;
    String topic;

    private static final Logger logger = LoggerFactory.getLogger(WikimediaChangeHandler.class.getSimpleName());

    public WikimediaChangeHandler(KafkaProducer<String, String> producer, String topic) {
        this.producer = producer;
        this.topic = topic;
    }

    @Override
    public void onOpen() throws Exception {
        //nothing here
    }

    @Override
    public void onClosed() throws Exception {
        producer.close();
    }

    @Override
    public void onMessage(String s, MessageEvent messageEvent) throws Exception {
        logger.info(messageEvent.getData());
        //asyncronus
        producer.send(new ProducerRecord<>(topic, s, messageEvent.getData()));
    }

    @Override
    public void onComment(String s) throws Exception {
        //nothing here
    }

    @Override
    public void onError(Throwable throwable) {
        logger.error(" Error on streaming: " + throwable.getMessage());
    }
}
