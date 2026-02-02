package edu.pict.apigateway.filters.global;

import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.List;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class ClientIdentityFilter implements GlobalFilter {

    public static final String CLIENT_IP_ATTR = "clientIp";
    private static final String X_FORWARDED_FOR = "X-Forwarded-For";

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {

        HttpHeaders headers = exchange.getRequest().getHeaders();

        String remoteAddress = exchange.getRequest().getRemoteAddress() != null
                ? exchange.getRequest().getRemoteAddress().getAddress().getHostAddress()
                : null;

        String clientIp = resolveClientIp(headers, remoteAddress);

        // IMPORTANT: Never block traffic here
        exchange.getAttributes().put(
                CLIENT_IP_ATTR,
                clientIp != null ? clientIp : "UNKNOWN"
        );

        return chain.filter(exchange);
    }

    private String resolveClientIp(HttpHeaders headers, String remoteAddress) {

        // 1️⃣ X-Forwarded-For (first IP only)
        List<String> xffHeaders = headers.get(X_FORWARDED_FOR);
        if (xffHeaders != null && !xffHeaders.isEmpty()) {
            String firstIp = xffHeaders.get(0).split(",")[0].trim();
            if (isValidIp(firstIp)) {
                return firstIp;
            }
        }

        // 2️⃣X-Real-IP
        String realIp = headers.getFirst("X-Real-IP");
        if (isValidIp(realIp)) {
            return realIp;
        }

        // 3️⃣Fallback to remote address
        if (isValidIp(remoteAddress)) {
            return remoteAddress;
        }

        return null;
    }

    private boolean isValidIp(String ip) {
        if (ip == null || ip.isBlank()) {
            return false;
        }
        try {
            InetAddress.getByName(ip);
            return true;
        } catch (UnknownHostException ex) {
            return false;
        }
    }
}
