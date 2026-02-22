package edu.pict.apigateway.filters.global;

import edu.pict.apigateway.config.kafka.KafkaTopics;
import edu.pict.apigateway.kafkaEvent.LogEvent;
import edu.pict.apigateway.kafkaEvent.SecurityAlertEvent;
import edu.pict.apigateway.util.Constants;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.net.InetSocketAddress;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SentientGateFilterTest {

    @Mock
    private KafkaTemplate<String, Object> kafkaTemplate;

    @Mock
    private GatewayFilterChain chain;

    @InjectMocks
    private SentientGateFilter sentientGateFilter;

    @Test
    void testFilter_SuccessResponse_SendsLogEvent() {
        String uuid = "test-uuid";
        MockServerHttpRequest request = MockServerHttpRequest.get("/test")
                .header(Constants.VISITOR_ID, uuid)
                .remoteAddress(new InetSocketAddress("127.0.0.1", 8080))
                .build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);
        exchange.getResponse().setStatusCode(org.springframework.http.HttpStatus.OK);

        when(chain.filter(exchange)).thenReturn(Mono.empty());

        StepVerifier.create(sentientGateFilter.filter(exchange, chain))
                .verifyComplete();

        verify(kafkaTemplate).send(eq(KafkaTopics.USER_LOGS.topic()), eq(uuid), any(LogEvent.class));
        verify(kafkaTemplate, never()).send(eq(KafkaTopics.SECURITY_EVENTS.topic()), anyString(), any());
    }

    @Test
    void testFilter_ErrorResponse_SendsLogAndSecurityEvents() {
        String uuid = "test-uuid";
        MockServerHttpRequest request = MockServerHttpRequest.get("/test")
                .header(Constants.VISITOR_ID, uuid)
                .remoteAddress(new InetSocketAddress("127.0.0.1", 8080))
                .build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);
        exchange.getResponse().setStatusCode(org.springframework.http.HttpStatus.BAD_REQUEST);

        when(chain.filter(exchange)).thenReturn(Mono.empty());

        StepVerifier.create(sentientGateFilter.filter(exchange, chain))
                .verifyComplete();

        verify(kafkaTemplate).send(eq(KafkaTopics.USER_LOGS.topic()), eq(uuid), any(LogEvent.class));
        verify(kafkaTemplate).send(eq(KafkaTopics.SECURITY_EVENTS.topic()), eq(uuid), any(SecurityAlertEvent.class));
    }

    @Test
    void testFilter_5xxError_SendsHighSeverityAlert() {
        String uuid = "test-uuid";
        MockServerHttpRequest request = MockServerHttpRequest.get("/test")
                .header(Constants.VISITOR_ID, uuid)
                .remoteAddress(new InetSocketAddress("127.0.0.1", 8080))
                .build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);
        exchange.getResponse().setStatusCode(org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR);

        when(chain.filter(exchange)).thenReturn(Mono.empty());

        StepVerifier.create(sentientGateFilter.filter(exchange, chain))
                .verifyComplete();

        verify(kafkaTemplate).send(eq(KafkaTopics.SECURITY_EVENTS.topic()), eq(uuid), argThat(event -> {
            if (event instanceof SecurityAlertEvent alert) {
                return "HIGH".equals(alert.getAlertSeverity());
            }
            return false;
        }));
    }
}
