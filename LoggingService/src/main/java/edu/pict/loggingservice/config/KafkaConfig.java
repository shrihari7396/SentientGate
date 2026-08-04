package edu.pict.loggingservice.config;

import java.util.HashMap;
import java.util.Map;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;

@Configuration
public class KafkaConfig {

    private final ConsumerFactory<String, Object> baseConsumerFactory;

    public KafkaConfig(ConsumerFactory<String, Object> consumerFactory) {
        this.baseConsumerFactory = consumerFactory;
    }

    /**
     * Container factory for the Redis consumer. Uses a separate group ID
     * (logging-redis-writer) so it consumes independently from the DB consumer.
     */
    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, Object>
            redisKafkaListenerContainerFactory() {
        ConcurrentKafkaListenerContainerFactory<String, Object> factory =
                new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(
                createConsumerFactoryWithGroupId("logging-redis-writer"));
        factory.setBatchListener(true);
        return factory;
    }

    /**
     * Container factory for the Database consumer. Uses a separate group ID
     * (logging-db-writer) so it consumes independently from the Redis consumer.
     */
    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, Object>
            dbKafkaListenerContainerFactory() {
        ConcurrentKafkaListenerContainerFactory<String, Object> factory =
                new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(
                createConsumerFactoryWithGroupId("logging-db-writer"));
        factory.setBatchListener(true);
        return factory;
    }

    /**
     * Creates a new ConsumerFactory that inherits all properties from the base factory
     * but overrides the group.id so each listener gets its own consumer group.
     */
    private ConsumerFactory<String, Object> createConsumerFactoryWithGroupId(String groupId) {
        Map<String, Object> props = new HashMap<>(baseConsumerFactory.getConfigurationProperties());
        props.put(ConsumerConfig.GROUP_ID_CONFIG, groupId);
        return new DefaultKafkaConsumerFactory<>(props);
    }
}
