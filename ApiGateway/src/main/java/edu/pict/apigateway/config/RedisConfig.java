package edu.pict.apigateway.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.ReactiveRedisConnectionFactory;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

@Configuration
public class RedisConfig {

    @Bean
    public ReactiveStringRedisTemplate reactiveStringRedisTemplate(
            ReactiveRedisConnectionFactory connectionFactory
    ) {
        RedisSerializationContext<String, String> context =
                RedisSerializationContext
                        .<String, String>newSerializationContext(new StringRedisSerializer())
                        .value(new StringRedisSerializer())
                        .build();

        return new ReactiveStringRedisTemplate(connectionFactory, context);
    }
}


