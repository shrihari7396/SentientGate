package edu.pict.apigateway.filters.global;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

import edu.pict.apigateway.util.Constants;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

@ExtendWith(MockitoExtension.class)
public class BlacklistFilterTest {

    @Mock private ReactiveStringRedisTemplate redisTemplate;

    @Mock private GatewayFilterChain filterChain;

    @InjectMocks private BlacklistFilter blacklistFilter;

    private static final String BLACKLIST_PREFIX = "blacklist:";

    @Test
    void testFilter_MissingVisitorId() {
        MockServerHttpRequest request = MockServerHttpRequest.get("/api/test").build();
        ServerWebExchange exchange = MockServerWebExchange.from(request);

        StepVerifier.create(blacklistFilter.filter(exchange, filterChain)).verifyComplete();

        assert exchange.getResponse().getStatusCode() == HttpStatus.UNAUTHORIZED;
        verify(redisTemplate, never()).hasKey(anyString());
        verify(filterChain, never()).filter(any());
    }

    @Test
    void testFilter_VisitorIsBlacklisted() {
        String uuid = "blocked-uuid";
        MockServerHttpRequest request =
                MockServerHttpRequest.get("/api/test").header(Constants.VISITOR_ID, uuid).build();
        ServerWebExchange exchange = MockServerWebExchange.from(request);

        when(redisTemplate.hasKey(BLACKLIST_PREFIX + uuid)).thenReturn(Mono.just(true));

        StepVerifier.create(blacklistFilter.filter(exchange, filterChain)).verifyComplete();

        assert exchange.getResponse().getStatusCode() == HttpStatus.FORBIDDEN;
        verify(filterChain, never()).filter(any());
    }

    @Test
    void testFilter_VisitorIsNotBlacklisted() {
        String uuid = "allowed-uuid";
        MockServerHttpRequest request =
                MockServerHttpRequest.get("/api/test").header(Constants.VISITOR_ID, uuid).build();
        ServerWebExchange exchange = MockServerWebExchange.from(request);

        when(redisTemplate.hasKey(BLACKLIST_PREFIX + uuid)).thenReturn(Mono.just(false));
        when(filterChain.filter(exchange)).thenReturn(Mono.empty());

        StepVerifier.create(blacklistFilter.filter(exchange, filterChain)).verifyComplete();

        assert exchange.getResponse().getStatusCode() == null
                || exchange.getResponse().getStatusCode() == HttpStatus.OK;
        verify(filterChain, times(1)).filter(exchange);
    }

    @Test
    void testFilter_RedisHasKeyFailure() {
        String uuid = "error-uuid";
        MockServerHttpRequest request =
                MockServerHttpRequest.get("/api/test").header(Constants.VISITOR_ID, uuid).build();
        ServerWebExchange exchange = MockServerWebExchange.from(request);

        when(redisTemplate.hasKey(BLACKLIST_PREFIX + uuid))
                .thenReturn(Mono.error(new RuntimeException("Redis error")));
        when(filterChain.filter(exchange)).thenReturn(Mono.empty());

        StepVerifier.create(blacklistFilter.filter(exchange, filterChain)).verifyComplete();

        // Expect it to fail open and proceed with the filter chain
        verify(filterChain, times(1)).filter(exchange);
    }
}
