package edu.pict.apigateway.filters.global;

import lombok.RequiredArgsConstructor;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.route.Route;
import org.springframework.cloud.gateway.support.ServerWebExchangeUtils;
import org.springframework.core.Ordered;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;

import java.util.List;

@Component
@RequiredArgsConstructor
public class RateLimitFilter implements GlobalFilter, Ordered {

    private final ReactiveStringRedisTemplate redisTemplate;
    private final DefaultRedisScript<Long> rateLimitScript;

    private static final long LIMIT = 100;
    private static final long WINDOW_SECONDS = 60;

    public static final String DECISION_ATTR = "decision";

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE + 2;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {

        String clientIp = exchange.getAttribute("clientIp");

        if (clientIp == null || "UNKNOWN".equals(clientIp)) {
            return chain.filter(exchange);
        }

        Route route = exchange.getAttribute(ServerWebExchangeUtils.GATEWAY_ROUTE_ATTR);
        String routeId = (route != null) ? route.getId() : "UNKNOWN";

        String key = "rate:" + clientIp + ":" + routeId;

        return redisTemplate.execute(
                        rateLimitScript,
                        List.of(key),
                        String.valueOf(WINDOW_SECONDS)
                )
                .single()
                .flatMap(count -> {
                    if (count > LIMIT) {

                        exchange.getAttributes()
                                .put(DECISION_ATTR, "RATE_LIMITED");

                        exchange.getResponse()
                                .setStatusCode(HttpStatus.TOO_MANY_REQUESTS);

                        return exchange.getResponse().setComplete();
                    }
                    return chain.filter(exchange);
                })
                // Fail-open: Redis failure must not block traffic
                .onErrorResume(ex -> chain.filter(exchange));
    }
}
