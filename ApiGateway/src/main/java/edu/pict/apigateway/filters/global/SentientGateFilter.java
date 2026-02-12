package edu.pict.apigateway.filters.global;

import edu.pict.apigateway.config.kafka.KafkaTopics;
import edu.pict.apigateway.kafkaEvent.SecurityAlertEvent;
import edu.pict.apigateway.kafkaEvent.UserLogEvent;
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

import java.util.Objects;

@Component
@RequiredArgsConstructor
@Slf4j
public class SentientGateFilter implements GlobalFilter, Ordered {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        long startTime = System.currentTimeMillis();
        return chain.filter(exchange).then(
                Mono.fromRunnable(
                        () -> {
                            long duration = System.currentTimeMillis() - startTime;
                            HttpStatusCode status = exchange.getResponse().getStatusCode();

                            int statusCode = (status != null) ? status.value() : 500;

                            String reasonPhrase = (status instanceof HttpStatus) ?
                                    ((HttpStatus) status).getReasonPhrase() : "Unknown";

                            String uuid = exchange.getRequest().getHeaders().getFirst(VisitorIdFilter.VISITOR_ID);
                            String path = exchange.getRequest().getURI().getPath();
                            String method = exchange.getRequest().getMethod().toString();
                            String clientIp = Objects.requireNonNull(exchange.getRequest().getRemoteAddress())
                                    .getAddress().getHostAddress();

                            // 1. General Pipeline: Always log the history
                            UserLogEvent userLogEvent = UserLogEvent.builder()
                                    .uuid(uuid)
                                    .path(path)
                                    .method(method)
                                    .latencyMs(duration)
                                    .statusCode(statusCode)
                                    .clientIp(clientIp)
                                    .timestamp(System.currentTimeMillis())
                                    .build();

                            kafkaTemplate.send(KafkaTopics.USER_LOGS.topic(), uuid, userLogEvent);

                            // 2. Security Pipeline: Trigger for non-200s (Redirection, Client/Server Errors)
                            if (statusCode < 200 || statusCode >= 300) {
                                SecurityAlertEvent alertEvent = SecurityAlertEvent.builder()
                                        .uuid(uuid)
                                        .errorCode(statusCode)
                                        .reason(reasonPhrase)
                                        .attemptedPath(path)
                                        .alertSeverity(determineSeverity(statusCode))
                                        .timestamp(System.currentTimeMillis())
                                        .build();

                                kafkaTemplate.send(KafkaTopics.SECURITY_EVENTS.topic(), uuid, alertEvent);
                                log.warn("Security Event: Sent alert for UUID {} due to status {}", uuid, statusCode);
                            }
                        }
                )
        );
    }

    private String determineSeverity(int code) {
        if (code >= 500) return "HIGH";
        if (code == 429 || code == 403 || code == 401) return "MEDIUM";
        return "LOW";
    }

    @Override
    public int getOrder() {
        // Run last to ensure we have the final status code from the microservice
        return Ordered.LOWEST_PRECEDENCE;
    }
}