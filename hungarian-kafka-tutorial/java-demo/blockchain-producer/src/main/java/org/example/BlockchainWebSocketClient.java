package org.example;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.WebSocket;
import okhttp3.WebSocketListener;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;

import java.io.IOException;

public class BlockchainWebSocketClient {
    private final OkHttpClient client;
    private WebSocket webSocket;

    KafkaProducer<String, String> kafkaProducer;
    String topic;

    public BlockchainWebSocketClient(KafkaProducer<String, String> kafkaProducer, String topic) {
        this.client = new OkHttpClient();
        this.kafkaProducer = kafkaProducer;
        this.topic = topic;
    }

    public void connect(String url) throws IOException {
        Request request = new Request.Builder().url(url).build();
        webSocket = client.newWebSocket(request, new WebSocketListener() {
            @Override
            public void onOpen(WebSocket webSocket, Response response) {
                System.out.println("WebSocket connection established");
            }

            @Override
            public void onMessage(WebSocket webSocket, String message) {
                System.out.println("Received message: " + message);
                kafkaProducer.send(new ProducerRecord<>(topic, message));
            }

            @Override
            public void onClosed(WebSocket webSocket, int code, String reason) {
                System.out.println("WebSocket connection closed: " + code + " " + reason);
            }

            @Override
            public void onFailure(WebSocket webSocket, Throwable t, Response response) {
                t.printStackTrace();
            }
        });
    }

    public void sendMessage(String message) {
        webSocket.send(message);
    }

    public void close() {
        webSocket.close(1000, "Closing connection");
    }
}
