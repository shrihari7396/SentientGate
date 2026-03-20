package edu.pict.apigateway.config.kafka;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;

@RequiredArgsConstructor
@Configuration
public class KafkaProducerConfig {

    @Bean
    public KafkaTemplate<String, Object> kafkaTemplate(ProducerFactory<String, Object> factory) {
        return new KafkaTemplate<>(factory);
    }
}
