package edu.pict.apigateway.config.kafka;

public enum KafkaTopics {
    USER_LOGS("user-logs"),
    SECURITY_EVENTS("security-events");


    private final String topic;

    KafkaTopics(String topic) {
        this.topic = topic;
    }

    public String topic() {
        return topic;
    }
}
