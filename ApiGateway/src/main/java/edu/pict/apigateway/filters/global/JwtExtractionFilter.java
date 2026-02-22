package edu.pict.apigateway.filters.global;

import edu.pict.apigateway.service.JwtBlacklistService;
import lombok.RequiredArgsConstructor;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@Component
@RequiredArgsConstructor
public class JwtExtractionFilter implements GlobalFilter, Ordered {

    private final JwtBlacklistService jwtBlacklistService;

    public static final String JTI_ATTR = "jti";
    public static final String JWT_TTL_ATTR = "jwt_ttl";
    public static final String JWT_SUB_ATTR = "jwtSubject";
    public static final String DECISION_ATTR = "decision";

    private final JwtParsingService jwtParsingService;

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {

        String authHeader =
                exchange.getRequest().getHeaders().getFirst(HttpHeaders.AUTHORIZATION);

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return reject(exchange, "MISSING_JWT");
        }

        String token = authHeader.substring(7);

        JwtParsingService.ParsedJwt parsed;
        try {
            parsed = jwtParsingService.parse(token);
        } catch (Exception e) {
            return reject(exchange, "INVALID_JWT");
        }

        long now = System.currentTimeMillis() / 1000;
        long ttlSeconds = parsed.expEpochSeconds() - now;

        if (ttlSeconds <= 0) {
            return reject(exchange, "JWT_EXPIRED");
        }

        String jti = parsed.jti();

        return jwtBlacklistService.isBlocked(jti)
                .flatMap(isBlocked -> {
                    if (isBlocked) {
                        return reject(exchange, "JWT_BLOCKED");
                    }

                    exchange.getAttributes().put(JTI_ATTR, jti);
                    exchange.getAttributes().put(JWT_SUB_ATTR, parsed.subject());
                    exchange.getAttributes().put(JWT_TTL_ATTR, ttlSeconds);

                    return chain.filter(exchange);
                });
    }


    private Mono<Void> reject(ServerWebExchange exchange, String reason) {
        exchange.getAttributes().put(DECISION_ATTR, reason);
        exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
        return exchange.getResponse().setComplete();
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE + 1;
    }
}

