package edu.pict.apigateway.config;

import edu.pict.apigateway.util.Constants;
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
            String visitorId =
                    exchange.getRequest().getHeaders().getFirst(Constants.VISITOR_ID);
            if (visitorId != null && !visitorId.isBlank()) {
                return Mono.just(visitorId);
            }

            HttpCookie visitorCookie =
                    exchange.getRequest().getCookies().getFirst(Constants.VISITOR_ID);
            if (visitorCookie != null && visitorCookie.getValue() != null) {
                String cookieValue = visitorCookie.getValue().trim();
                if (!cookieValue.isBlank()) {
                    return Mono.just(cookieValue);
                }
            }

            if (exchange.getRequest().getRemoteAddress() != null
                    && exchange.getRequest().getRemoteAddress().getAddress() != null) {
                return Mono.just(
                        exchange.getRequest().getRemoteAddress().getAddress().getHostAddress());
            }

            // Last-resort key to ensure limiter still functions instead of silently skipping.
            return Mono.just("anonymous");
        };
    }
}
