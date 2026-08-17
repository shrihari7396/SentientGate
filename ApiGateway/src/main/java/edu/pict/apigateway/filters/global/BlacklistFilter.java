package edu.pict.apigateway.filters.global;

import edu.pict.apigateway.service.IpService;
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
    private final IpService ipService;
    private static final String LEGACY_BLACKLIST_PREFIX = "blacklist:";
    private static final String UUID_BLACKLIST_PREFIX = "blacklist:uuid:";
    private static final String IP_BLACKLIST_PREFIX = "blacklist:ip:";

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String uuid = exchange.getRequest().getHeaders().getFirst(Constants.VISITOR_ID);

        if (uuid == null) {
            exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
            return exchange.getResponse().setComplete();
        }

        String remoteAddress =
                exchange.getRequest().getRemoteAddress() != null
                        ? exchange.getRequest().getRemoteAddress().getAddress().getHostAddress()
                        : null;
        String clientIp =
                ipService.resolveClientIp(exchange.getRequest().getHeaders(), remoteAddress);

        return reactiveStringRedisTemplate
                .hasKey(BLACKLIST_PREFIX + uuid)
                .onErrorResume(
                        e -> {
                            log.error(
                                    "Redis connection failed in BlacklistFilter for UUID {}. Allowing request to proceed.",
                                    uuid,
                                    e);
                            return Mono.just(false);
                        })
                .flatMap(
                        isBlocked -> {
                            if (Boolean.TRUE.equals(isBlocked)) {
                                //                                For Performance reason log is
                                // commented
                                //                                log.warn("Request blocked: UUID {}
                                // is on the blacklist.", uuid);
                                exchange.getResponse().setStatusCode(HttpStatus.FORBIDDEN);
                                return exchange.getResponse().setComplete();
                            }
                            if (clientIp == null || clientIp.isBlank()) {
                                return chain.filter(exchange);
                            }
                            return reactiveStringRedisTemplate
                                    .hasKey(IP_BLACKLIST_PREFIX + clientIp)
                                    .flatMap(
                                            isBlockedByIp -> {
                                                if (Boolean.TRUE.equals(isBlockedByIp)) {
                                                    log.warn(
                                                            "Request blocked: IP {} is on blacklist.",
                                                            clientIp);
                                                    exchange.getResponse()
                                                            .setStatusCode(HttpStatus.FORBIDDEN);
                                                    return exchange.getResponse().setComplete();
                                                }
                                                return chain.filter(exchange);
                                            });
                        });
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE + 1;
    }
}
