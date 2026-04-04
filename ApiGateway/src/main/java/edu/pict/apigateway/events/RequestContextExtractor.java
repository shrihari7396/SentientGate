package edu.pict.apigateway.events;

import org.springframework.web.server.ServerWebExchange;

public interface RequestContextExtractor {
    RequestContext extract(ServerWebExchange exchange);
}

