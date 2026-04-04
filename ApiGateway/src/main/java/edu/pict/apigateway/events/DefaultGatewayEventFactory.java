package edu.pict.apigateway.events;

import edu.pict.apigateway.kafkaEvent.LogEvent;
import edu.pict.apigateway.kafkaEvent.SecurityAlertEvent;
import org.springframework.stereotype.Component;

@Component
public class DefaultGatewayEventFactory implements GatewayEventFactory {

    @Override
    public LogEvent buildLogEvent(RequestContext context, int statusCode, long latencyMs) {
        return LogEvent.builder()
                .uuid(context.uuid())
                .path(context.path())
                .method(context.method())
                .routeId(context.routeId())
                .decision("ALLOWED")
                .latencyMs(latencyMs)
                .queryParams(context.queryParams())
                .clientIp(context.clientIp())
                .statusCode(statusCode)
                .requestSize(context.requestSize())
                .timestamp(context.timestamp())
                .userAgent(context.userAgent())
                .build();
    }

    @Override
    public SecurityAlertEvent buildSecurityAlert(
            RequestContext context, int statusCode, String reasonPhrase) {
        return SecurityAlertEvent.builder()
                .uuid(context.uuid())
                .errorCode(statusCode)
                .reason(reasonPhrase)
                .attemptedPath(context.path())
                .method(context.method())
                .userAgent(context.userAgent())
                .clientIp(context.clientIp())
                .alertSeverity(determineSeverity(statusCode))
                .timestamp(context.timestamp())
                .build();
    }

    private String determineSeverity(int code) {
        if (code >= 500) return "HIGH";
        if (code == 429 || code == 403 || code == 401) return "MEDIUM";
        return "LOW";
    }
}

