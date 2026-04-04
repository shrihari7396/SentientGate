package edu.pict.apigateway.filters.global;

import edu.pict.apigateway.config.kafka.KafkaTopics;
import edu.pict.apigateway.events.GatewayEventFactory;
import edu.pict.apigateway.events.RequestContext;
import edu.pict.apigateway.events.RequestContextExtractor;
import edu.pict.apigateway.kafkaEvent.LogEvent;
import edu.pict.apigateway.kafkaEvent.SecurityAlertEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@Component
@RequiredArgsConstructor
@Slf4j
public class SentientGateFilter implements GlobalFilter, Ordered {

    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final RequestContextExtractor requestContextExtractor;
    private final GatewayEventFactory gatewayEventFactory;

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        long startTime = System.currentTimeMillis();
        return chain.filter(exchange)
                .then(
                        Mono.fromRunnable(
                                () -> {
                                    long duration = System.currentTimeMillis() - startTime;
                                    HttpStatusCode status = exchange.getResponse().getStatusCode();

                                    int statusCode = (status != null) ? status.value() : 500;

                                    String reasonPhrase =
                                            (status instanceof HttpStatus)
                                                    ? ((HttpStatus) status).getReasonPhrase()
                                                    : "Unknown";

                                    RequestContext context = requestContextExtractor.extract(exchange);

                                    if (context.uuid() == null || context.uuid().isBlank()) {
                                        log.warn("Skipping event publishing because visitor UUID is missing");
                                        return;
                                    }

                                    LogEvent logEvent =
                                            gatewayEventFactory.buildLogEvent(context, statusCode, duration);
                                    kafkaTemplate.send(KafkaTopics.USER_LOGS.topic(), context.uuid(), logEvent);

                                    if (statusCode < 200 || statusCode >= 300) {
                                        SecurityAlertEvent alertEvent =
                                                gatewayEventFactory.buildSecurityAlert(
                                                        context, statusCode, reasonPhrase);

                                        kafkaTemplate.send(
                                                KafkaTopics.SECURITY_EVENTS.topic(),
                                                context.uuid(),
                                                alertEvent);
                                        log.warn(
                                                "Security Event: Sent alert for UUID {} due to status {}",
                                                context.uuid(),
                                                statusCode);
                                    }
                                }));
    }

    @Override
    public int getOrder() {
        return Ordered.LOWEST_PRECEDENCE;
    }
}
