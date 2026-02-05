package edu.pict.apigateway.filters.global;

import edu.pict.apigateway.config.KafkaTopics;
import edu.pict.apigateway.kafkaEvent.GatewayDecisionEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.cloud.gateway.route.Route;
import org.springframework.cloud.gateway.support.ServerWebExchangeUtils;
import org.springframework.core.annotation.Order;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@Component
@Order(-1) // run late but before response commit
@RequiredArgsConstructor
public class DecisionLoggingFilter implements GlobalFilter {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    private static final String DECISION_ATTR = "decision";

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        long startTime = System.currentTimeMillis();

        return chain.filter(exchange)
                .doFinally(signal -> {

                    String clientIp =
                            exchange.getAttributeOrDefault("clientIp", "UNKNOWN");

                    Route route =
                            exchange.getAttribute(ServerWebExchangeUtils.GATEWAY_ROUTE_ATTR);

                    String routeId = route != null ? route.getId() : "UNKNOWN";

                    String decision =
                            exchange.getAttributeOrDefault(DECISION_ATTR, "ALLOWED");

                    int status =
                            exchange.getResponse().getStatusCode() != null
                                    ? exchange.getResponse().getStatusCode().value()
                                    : 200;

                    long latency = System.currentTimeMillis() - startTime;

                    GatewayDecisionEvent event = new GatewayDecisionEvent(
                            clientIp,
                            routeId,
                            decision,
                            status,
                            latency,
                            System.currentTimeMillis()
                    );

                    // Fire-and-forget (NON-BLOCKING)
                    kafkaTemplate.send(
                            KafkaTopics.SECURITY_EVENTS.topic(),
                            routeId,
                            event
                    );

                    kafkaTemplate.send(
                            KafkaTopics.USER_LOGS.topic(),
                            routeId,
                            event
                    );
                });
    }
}
