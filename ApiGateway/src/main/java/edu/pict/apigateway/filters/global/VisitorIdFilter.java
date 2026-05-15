package edu.pict.apigateway.filters.global;

import edu.pict.apigateway.service.SentinelSecurityService;
import edu.pict.apigateway.util.Constants;
import java.time.Duration;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpCookie;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@RequiredArgsConstructor
@Component
@Slf4j
public class VisitorIdFilter implements GlobalFilter, Ordered {

    private final SentinelSecurityService sentinelSecurityService;

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
//        log.info("exchange path: {}", exchange.getRequest().getPath().toString());
        HttpCookie cookie = exchange.getRequest().getCookies().getFirst(Constants.VISITOR_ID);

        if (cookie != null) {
            String fullToken = cookie.getValue();
            String uuid = sentinelSecurityService.verifyAndExtractId(fullToken);

            if (uuid == null) {
                exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
                return exchange.getResponse().setComplete();
            }

            ServerWebExchange mutatedExchange =
                    exchange.mutate()
                            .request(
                                    exchange.getRequest()
                                            .mutate()
                                            .header(Constants.VISITOR_ID, uuid)
                                            .build())
                            .build();

            return chain.filter(mutatedExchange);
        }

        String signedId = sentinelSecurityService.generateSignedId();
        String uuid = sentinelSecurityService.verifyAndExtractId(signedId);

        exchange.getResponse()
                .addCookie(
                        ResponseCookie.from(Constants.VISITOR_ID, signedId)
                                .path("/")
                                .httpOnly(true)
                                .secure(true)
                                .sameSite("Strict")
                                .maxAge(Duration.ofDays(365))
                                .build());

        ServerWebExchange mutatedExchange =
                exchange.mutate()
                        .request(
                                exchange.getRequest()
                                        .mutate()
                                        .header(Constants.VISITOR_ID, uuid)
                                        .build())
                        .build();

        return chain.filter(mutatedExchange);
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE;
    }
}
