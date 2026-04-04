package edu.pict.apigateway.events;

import edu.pict.apigateway.kafkaEvent.LogEvent;
import edu.pict.apigateway.kafkaEvent.SecurityAlertEvent;

public interface GatewayEventFactory {
    LogEvent buildLogEvent(RequestContext context, int statusCode, long latencyMs);

    SecurityAlertEvent buildSecurityAlert(RequestContext context, int statusCode, String reasonPhrase);
}

