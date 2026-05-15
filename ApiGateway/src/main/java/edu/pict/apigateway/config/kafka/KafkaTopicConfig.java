package edu.pict.apigateway.config.kafka;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class KafkaTopicConfig {

    @Bean
    public NewTopic userEventsTopic() {
        return TopicBuilder.name(KafkaTopics.USER_LOGS.topic()).partitions(3).replicas(1).build();
    }

    @Bean
    public NewTopic logEventsTopic() {
        return TopicBuilder.name(KafkaTopics.SECURITY_EVENTS.topic())
                .partitions(6)
                .replicas(1)
                .build();
    }
}
