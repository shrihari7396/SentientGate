package edu.pict.apigateway.filters.global;

import edu.pict.apigateway.util.Constants;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@Component
@RequiredArgsConstructor
@Slf4j
public class BlacklistFilter implements GlobalFilter, Ordered {

    private final ReactiveStringRedisTemplate reactiveStringRedisTemplate;
    private static final String BLACKLIST_PREFIX = "blacklist:";

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String uuid = exchange.getRequest().getHeaders().getFirst(Constants.VISITOR_ID);

        if (uuid == null) {
            exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
            return exchange.getResponse().setComplete();
        }

        return reactiveStringRedisTemplate
                .hasKey(BLACKLIST_PREFIX + uuid)
                .flatMap(
                        isBlocked -> {
                            if (Boolean.TRUE.equals(isBlocked)) {
                                log.warn("Request blocked: UUID {} is on the blacklist.", uuid);
                                exchange.getResponse().setStatusCode(HttpStatus.FORBIDDEN);
                                return exchange.getResponse().setComplete();
                            }
                            return chain.filter(exchange);
                        });
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE + 1;
    }
}
