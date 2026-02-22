package edu.pict.apigateway.config;

import edu.pict.apigateway.util.Constants;
import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import reactor.core.publisher.Mono;

@Configuration
public class RateLimitConfig {

    @Bean
    public KeyResolver visitorKeyResolver() {
        return exchange -> Mono.justOrEmpty(
                exchange.getRequest()
                        .getHeaders()
                        .getFirst(Constants.VISITOR_ID)
        );
    }

}