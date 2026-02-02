package edu.pict.apigateway.filters.route;

import lombok.RequiredArgsConstructor;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@Component
@RequiredArgsConstructor
public class JWTValidationFilter implements GatewayFilter {

    private final JwtDecoder jwtDecoder;

    public static final String DECISION_ATTR = "decision";

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String authHeader = exchange.getRequest().getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        if(authHeader == null || !authHeader.startsWith("Bearer ")) {
            return reject(exchange, "MISSING_JWT");
        }
        String token = authHeader.substring(7);

        try {
            Jwt jwt = jwtDecoder.decode(token);
            exchange.getAttributes().put("jwtSubject", jwt.getSubject());

            return chain.filter(exchange);
        } catch (JwtException e) {
            return reject(exchange, "INVALID_JWT");
        }
    }

    private Mono<Void> reject(ServerWebExchange exchange, String reason) {
        exchange.getAttributes().put(DECISION_ATTR, reason);
        exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
        return exchange.getResponse().setComplete();
    }
}
