package edu.pict.apigateway.events;

import edu.pict.apigateway.service.IpService;
import edu.pict.apigateway.util.Constants;
import lombok.RequiredArgsConstructor;
import org.springframework.cloud.gateway.route.Route;
import org.springframework.cloud.gateway.support.ServerWebExchangeUtils;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;

@Component
@RequiredArgsConstructor
public class DefaultRequestContextExtractor implements RequestContextExtractor {

    private final IpService ipService;

    @Override
    public RequestContext extract(ServerWebExchange exchange) {
        String uuid = exchange.getRequest().getHeaders().getFirst(Constants.VISITOR_ID);
        String path = exchange.getRequest().getURI().getPath();
        String method = exchange.getRequest().getMethod().toString();
        String queryParams = exchange.getRequest().getQueryParams().toString();
        long requestSize = exchange.getRequest().getHeaders().getContentLength();
        String userAgent = exchange.getRequest().getHeaders().getFirst("User-Agent");
        String remoteAddress =
                exchange.getRequest().getRemoteAddress() != null
                        ? exchange.getRequest().getRemoteAddress().getAddress().getHostAddress()
                        : null;
        String clientIp = ipService.resolveClientIp(exchange.getRequest().getHeaders(), remoteAddress);

        Route route = exchange.getAttribute(ServerWebExchangeUtils.GATEWAY_ROUTE_ATTR);
        String routeId = (route != null) ? route.getId() : "unknown";

        return new RequestContext(
                uuid,
                path,
                method,
                routeId,
                queryParams,
                Math.max(requestSize, 0),
                clientIp,
                userAgent,
                System.currentTimeMillis());
    }
}

