package edu.pict.apigateway.filters.global;

import edu.pict.apigateway.service.JwtBlacklistService;
import edu.pict.apigateway.service.JwtService;
import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import static edu.pict.apigateway.util.Constants.*;

@Component
@RequiredArgsConstructor
public class JwtExtractionFilter implements GlobalFilter, Ordered {

    private final JwtService jwtService;
    private final JwtBlacklistService jwtBlacklistService;

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {

        String authHeader =
                exchange.getRequest().getHeaders().getFirst(HttpHeaders.AUTHORIZATION);

        // ✅ 1️⃣ If no JWT → directly continue (public endpoint)
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return chain.filter(exchange);
        }

        String token = authHeader.substring(7);

        Claims claims;

        try {
            // ✅ 2️⃣ Validate signature + expiration automatically
            claims = jwtService.validateAndExtractClaims(token);
        } catch (Exception e) {
            return reject(exchange, "INVALID_OR_EXPIRED_JWT");
        }

        String jti = claims.getId();
        String subject = claims.getSubject();

        long expirationMillis = claims.getExpiration().getTime();
        long nowMillis = System.currentTimeMillis();
        long ttlSeconds = (expirationMillis - nowMillis) / 1000;

        if (ttlSeconds <= 0) {
            return reject(exchange, "JWT_EXPIRED");
        }

        // ✅ 3️⃣ Check Redis blacklist
        return jwtBlacklistService.isBlocked(jti)
                .flatMap(isBlocked -> {
                    if (Boolean.TRUE.equals(isBlocked)) {
                        return reject(exchange, "JWT_BLOCKED");
                    }

                    // ✅ Store verified values in exchange
                    exchange.getAttributes().put(JTI_ATTR, jti);
                    exchange.getAttributes().put(JWT_SUB_ATTR, subject);
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
        return Ordered.HIGHEST_PRECEDENCE + 2;
    }
}