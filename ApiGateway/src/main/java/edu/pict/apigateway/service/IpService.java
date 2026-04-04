package edu.pict.apigateway.service;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class IpService {

    private static final String X_FORWARDED_FOR = "X-Forwarded-For";

    public String resolveClientIp(HttpHeaders headers, String remoteAddress) {
        boolean trustForwardedHeaders = isTrustedProxy(remoteAddress);

        if (trustForwardedHeaders) {
            List<String> xffHeaders = headers.get(X_FORWARDED_FOR);
            if (xffHeaders != null && !xffHeaders.isEmpty()) {
                String firstIp = xffHeaders.get(0).split(",")[0].trim();
                if (isValidIp(firstIp)) {
                    return firstIp;
                }
            }

            String realIp = headers.getFirst("X-Real-IP");
            if (isValidIp(realIp)) {
                return realIp;
            }
        }

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

    private boolean isTrustedProxy(String remoteAddress) {
        if (!isValidIp(remoteAddress)) {
            return false;
        }
        try {
            InetAddress address = InetAddress.getByName(remoteAddress);
            return address.isLoopbackAddress()
                    || address.isSiteLocalAddress()
                    || address.isAnyLocalAddress();
        } catch (UnknownHostException ex) {
            return false;
        }
    }
}
