package edu.pict.apigateway.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.KafkaTemplate;

@RequiredArgsConstructor
@Configuration
public class KafkaProducerConfig {

    @Bean
    public KafkaTemplate<String, Object> kafkaTemplate(
            org.springframework.kafka.core.ProducerFactory<String, Object> factory
    ) {
        return new KafkaTemplate<>(factory);
    }
}