package edu.pict.apigateway.filters.global;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import edu.pict.apigateway.service.SentinelSecurityService;
import edu.pict.apigateway.util.Constants;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.http.HttpCookie;
import org.springframework.http.ResponseCookie;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

@ExtendWith(MockitoExtension.class)
class VisitorIdFilterTest {

    @Mock private SentinelSecurityService sentinelSecurityService;

    @Mock private GatewayFilterChain chain;

    @InjectMocks private VisitorIdFilter visitorIdFilter;

    @Test
    void testFilter_ExistingValidCookie() {
        String fullToken = "uuid.signature";
        String extractedUuid = "uuid-123";
        HttpCookie cookie = new HttpCookie(Constants.VISITOR_ID, fullToken);

        MockServerHttpRequest request = MockServerHttpRequest.get("/").cookie(cookie).build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);

        when(sentinelSecurityService.verifyAndExtractId(fullToken)).thenReturn(extractedUuid);
        when(chain.filter(any(ServerWebExchange.class))).thenReturn(Mono.empty());

        StepVerifier.create(visitorIdFilter.filter(exchange, chain)).verifyComplete();

        // Verify that the chain was called with a mutated exchange that has the header
        verify(chain)
                .filter(
                        argThat(
                                ex ->
                                        extractedUuid.equals(
                                                ex.getRequest()
                                                        .getHeaders()
                                                        .getFirst(Constants.VISITOR_ID))));
    }

    @Test
    void testFilter_ExistingInvalidCookie() {
        String fullToken = "invalid.token";
        HttpCookie cookie = new HttpCookie(Constants.VISITOR_ID, fullToken);

        MockServerHttpRequest request = MockServerHttpRequest.get("/").cookie(cookie).build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);

        when(sentinelSecurityService.verifyAndExtractId(fullToken)).thenReturn(null);

        StepVerifier.create(visitorIdFilter.filter(exchange, chain)).verifyComplete();

        assertEquals(401, exchange.getResponse().getStatusCode().value());
        verify(chain, never()).filter(any());
    }

    @Test
    void testFilter_NoCookie_GeneratesNewOne() {
        String signedId = "new-uuid.new-signature";
        MockServerHttpRequest request = MockServerHttpRequest.get("/").build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);

        when(sentinelSecurityService.generateSignedId()).thenReturn(signedId);
        when(chain.filter(any(ServerWebExchange.class))).thenReturn(Mono.empty());

        StepVerifier.create(visitorIdFilter.filter(exchange, chain)).verifyComplete();

        ResponseCookie responseCookie =
                exchange.getResponse().getCookies().getFirst(Constants.VISITOR_ID);
        assertNotNull(responseCookie);
        assertEquals(signedId, responseCookie.getValue());
        assertTrue(responseCookie.isHttpOnly());

        verify(chain).filter(any(ServerWebExchange.class));
    }
}
