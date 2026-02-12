package edu.pict.apigateway.filters.global;

import lombok.RequiredArgsConstructor;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpCookie;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.UUID;

@RequiredArgsConstructor
@Component
public class VisitorIdFilter implements GlobalFilter, Ordered {

    public static final String VISITOR_ID = "VISITOR_ID";

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        HttpCookie cookie = exchange.getRequest().getCookies().getFirst(VISITOR_ID);

        if (cookie != null) {
            String uuid = cookie.getValue();
            exchange.getRequest().mutate().header(VISITOR_ID, uuid).build();
            return chain.filter(exchange);
        }

        String newUuid = UUID.randomUUID().toString();

        exchange.getResponse()
                .addCookie(ResponseCookie.from(VISITOR_ID, newUuid)
                .path("/")
                .httpOnly(true)
                .maxAge(Duration.ofDays(365))
                .build());


        return exchange.getResponse().setComplete();

    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE;
    }
}
