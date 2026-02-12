package edu.pict.apigateway.config;

import edu.pict.apigateway.filters.global.VisitorIdFilter;
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
            // 1. Try to get it from the request header (added by our filter for the first request)
            String visitorId = exchange.getRequest().getHeaders().getFirst(VisitorIdFilter.VISITOR_ID);
            // 2. Or get it from the cookie (for subsequent requests)
            if (visitorId == null) {
                HttpCookie cookie = exchange.getRequest().getCookies().getFirst(VisitorIdFilter.VISITOR_ID);
                visitorId = (cookie != null) ? cookie.getValue() : "anonymous";
            }
            return Mono.just(visitorId);
        };
    }

}