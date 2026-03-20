package edu.pict.apigateway.filters.global;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import edu.pict.apigateway.util.Constants;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

@ExtendWith(MockitoExtension.class)
class BlacklistFilterTest {

    @Mock private ReactiveStringRedisTemplate reactiveStringRedisTemplate;

    @Mock private GatewayFilterChain chain;

    @InjectMocks private BlacklistFilter blacklistFilter;

    private static final String BLACKLIST_PREFIX = "blacklist:";

    @Test
    void testFilter_MissingVisitorId_ReturnsUnauthorized() {
        MockServerHttpRequest request = MockServerHttpRequest.get("/").build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);

        StepVerifier.create(blacklistFilter.filter(exchange, chain)).verifyComplete();

        assertEquals(401, exchange.getResponse().getStatusCode().value());
        verify(chain, never()).filter(any());
    }

    @Test
    void testFilter_VisitorIdBlacklisted_ReturnsForbidden() {
        String uuid = "blocked-uuid";
        MockServerHttpRequest request =
                MockServerHttpRequest.get("/").header(Constants.VISITOR_ID, uuid).build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);

        when(reactiveStringRedisTemplate.hasKey(BLACKLIST_PREFIX + uuid))
                .thenReturn(Mono.just(true));

        StepVerifier.create(blacklistFilter.filter(exchange, chain)).verifyComplete();

        assertEquals(403, exchange.getResponse().getStatusCode().value());
        verify(chain, never()).filter(any());
    }

    @Test
    void testFilter_VisitorIdNotBlacklisted_Proceeds() {
        String uuid = "allowed-uuid";
        MockServerHttpRequest request =
                MockServerHttpRequest.get("/").header(Constants.VISITOR_ID, uuid).build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);

        when(reactiveStringRedisTemplate.hasKey(BLACKLIST_PREFIX + uuid))
                .thenReturn(Mono.just(false));
        when(chain.filter(exchange)).thenReturn(Mono.empty());

        StepVerifier.create(blacklistFilter.filter(exchange, chain)).verifyComplete();

        verify(chain).filter(exchange);
    }
}
