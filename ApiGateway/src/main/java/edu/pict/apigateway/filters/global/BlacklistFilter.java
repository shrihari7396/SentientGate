package edu.pict.apigateway.filters.global;

import lombok.RequiredArgsConstructor;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import javax.swing.text.StyledEditorKit;

@Component
@RequiredArgsConstructor
@Order(Ordered.HIGHEST_PRECEDENCE + 1)
public class BlacklistFilter implements GlobalFilter {

    private final ReactiveStringRedisTemplate reactiveStringRedisTemplate;

    public static final String DECISION_ATTR = "decision";

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String clientIp = exchange.getAttribute("clientIp");

        if(clientIp == null  || "UNKNOWN".equals(clientIp)){
            return chain.filter(exchange);
        }

        String key = "blocked: " + clientIp;
        return reactiveStringRedisTemplate.hasKey(key)
                .flatMap(
                        isBlocked -> {
                            if(Boolean.TRUE.equals(isBlocked)){
                                exchange
                                        .getAttributes()
                                        .put(DECISION_ATTR, "BLOCKED_BLACKLIST");

                                exchange
                                        .getResponse()
                                        .setStatusCode(HttpStatus.FORBIDDEN);

                                return exchange
                                        .getResponse()
                                        .setComplete();
                            }
                            return chain.filter(exchange);
                        }
                )
                .onErrorResume(ex -> chain.filter(exchange));
    }
}
