package edu.pict.apigateway.filters.global;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import edu.pict.apigateway.service.JwtBlacklistService;
import edu.pict.apigateway.service.JwtService;
import edu.pict.apigateway.util.Constants;
import io.jsonwebtoken.Claims;
import java.util.Date;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

@ExtendWith(MockitoExtension.class)
class JwtExtractionFilterTest {

    @Mock private JwtService jwtService;

    @Mock private JwtBlacklistService jwtBlacklistService;

    @Mock private GatewayFilterChain chain;

    @InjectMocks private JwtExtractionFilter jwtExtractionFilter;

    @Test
    void testFilter_NoAuthHeader_Proceeds() {
        MockServerHttpRequest request = MockServerHttpRequest.get("/").build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);

        when(chain.filter(exchange)).thenReturn(Mono.empty());

        StepVerifier.create(jwtExtractionFilter.filter(exchange, chain)).verifyComplete();

        verify(chain).filter(exchange);
        verifyNoInteractions(jwtService);
    }

    @Test
    void testFilter_InvalidAuthHeader_Proceeds() {
        MockServerHttpRequest request =
                MockServerHttpRequest.get("/")
                        .header(HttpHeaders.AUTHORIZATION, "Basic someauth")
                        .build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);

        when(chain.filter(exchange)).thenReturn(Mono.empty());

        StepVerifier.create(jwtExtractionFilter.filter(exchange, chain)).verifyComplete();

        verify(chain).filter(exchange);
    }

    @Test
    void testFilter_InvalidJwt_ReturnsUnauthorized() {
        String token = "invalid-token";
        MockServerHttpRequest request =
                MockServerHttpRequest.get("/")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);

        when(jwtService.validateAndExtractClaims(token)).thenThrow(new RuntimeException("invalid"));

        StepVerifier.create(jwtExtractionFilter.filter(exchange, chain)).verifyComplete();

        assertEquals(401, exchange.getResponse().getStatusCode().value());
        assertEquals("INVALID_OR_EXPIRED_JWT", exchange.getAttribute(Constants.DECISION_ATTR));
        verify(chain, never()).filter(any());
    }

    @Test
    void testFilter_ExpiredJwt_ReturnsUnauthorized() {
        String token = "expired-token";
        MockServerHttpRequest request =
                MockServerHttpRequest.get("/")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);

        Claims claims = mock(Claims.class);
        when(claims.getExpiration()).thenReturn(new Date(System.currentTimeMillis() - 1000));
        when(jwtService.validateAndExtractClaims(token)).thenReturn(claims);

        StepVerifier.create(jwtExtractionFilter.filter(exchange, chain)).verifyComplete();

        assertEquals(401, exchange.getResponse().getStatusCode().value());
        assertEquals("JWT_EXPIRED", exchange.getAttribute(Constants.DECISION_ATTR));
    }

    @Test
    void testFilter_BlacklistedJwt_ReturnsUnauthorized() {
        String token = "blacklisted-token";
        String jti = "test-jti";
        MockServerHttpRequest request =
                MockServerHttpRequest.get("/")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);

        Claims claims = mock(Claims.class);
        when(claims.getExpiration()).thenReturn(new Date(System.currentTimeMillis() + 10000));
        when(claims.getId()).thenReturn(jti);
        when(jwtService.validateAndExtractClaims(token)).thenReturn(claims);
        when(jwtBlacklistService.isBlocked(jti)).thenReturn(Mono.just(true));

        StepVerifier.create(jwtExtractionFilter.filter(exchange, chain)).verifyComplete();

        assertEquals(401, exchange.getResponse().getStatusCode().value());
        assertEquals("JWT_BLOCKED", exchange.getAttribute(Constants.DECISION_ATTR));
    }

    @Test
    void testFilter_ValidJwt_Proceeds() {
        String token = "valid-token";
        String jti = "test-jti";
        String sub = "user123";
        MockServerHttpRequest request =
                MockServerHttpRequest.get("/")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);

        Claims claims = mock(Claims.class);
        when(claims.getExpiration()).thenReturn(new Date(System.currentTimeMillis() + 10000));
        when(claims.getId()).thenReturn(jti);
        when(claims.getSubject()).thenReturn(sub);
        when(jwtService.validateAndExtractClaims(token)).thenReturn(claims);
        when(jwtBlacklistService.isBlocked(jti)).thenReturn(Mono.just(false));
        when(chain.filter(exchange)).thenReturn(Mono.empty());

        StepVerifier.create(jwtExtractionFilter.filter(exchange, chain)).verifyComplete();

        assertEquals(jti, exchange.getAttribute(Constants.JTI_ATTR));
        assertEquals(sub, exchange.getAttribute(Constants.JWT_SUB_ATTR));
        verify(chain).filter(exchange);
    }
}
