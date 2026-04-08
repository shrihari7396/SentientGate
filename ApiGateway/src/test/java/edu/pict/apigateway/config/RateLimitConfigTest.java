package edu.pict.apigateway.config;

import static org.junit.jupiter.api.Assertions.assertEquals;

import edu.pict.apigateway.util.Constants;
import java.net.InetSocketAddress;
import org.junit.jupiter.api.Test;
import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
import org.springframework.http.HttpCookie;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import reactor.test.StepVerifier;

class RateLimitConfigTest {

    private final KeyResolver keyResolver = new RateLimitConfig().visitorKeyResolver();

    @Test
    void shouldUseVisitorIdHeaderWhenPresent() {
        MockServerHttpRequest request =
                MockServerHttpRequest.get("/api/dummy/test")
                        .header(Constants.VISITOR_ID, "visitor-header-123")
                        .build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);

        StepVerifier.create(keyResolver.resolve(exchange))
                .assertNext(key -> assertEquals("visitor-header-123", key))
                .verifyComplete();
    }

    @Test
    void shouldFallbackToVisitorCookieWhenHeaderMissing() {
        MockServerHttpRequest request =
                MockServerHttpRequest.get("/api/dummy/test")
                        .cookie(new HttpCookie(Constants.VISITOR_ID, "visitor-cookie-456"))
                        .build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);

        StepVerifier.create(keyResolver.resolve(exchange))
                .assertNext(key -> assertEquals("visitor-cookie-456", key))
                .verifyComplete();
    }

    @Test
    void shouldFallbackToRemoteIpWhenHeaderAndCookieMissing() {
        MockServerHttpRequest request =
                MockServerHttpRequest.get("/api/dummy/test")
                        .remoteAddress(new InetSocketAddress("127.0.0.1", 8080))
                        .build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);

        StepVerifier.create(keyResolver.resolve(exchange))
                .assertNext(key -> assertEquals("127.0.0.1", key))
                .verifyComplete();
    }
}
