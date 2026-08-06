package edu.pict.apigateway.config;

import edu.pict.apigateway.service.SentinelSecurityService;
import edu.pict.apigateway.util.Constants;
import lombok.RequiredArgsConstructor;
import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpCookie;
import reactor.core.publisher.Mono;

@Configuration
public class RateLimitConfig {

    @Bean
    public KeyResolver visitorKeyResolver() {
        return exchange -> {
            String visitorId = exchange.getRequest().getHeaders().getFirst(Constants.VISITOR_ID);
            if (visitorId != null && !visitorId.isBlank()) {
                return Mono.just(visitorId);
            }
            return Mono.just("anonymous");
        };
    }
}
