package edu.pict.loggingservice.kafka.consumer;

import static org.mockito.Mockito.verify;

import edu.pict.loggingservice.kafka.model.GatewayDecisionEvent;
import edu.pict.loggingservice.service.KafkaBatchService;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DatabaseLogConsumerTest {

    @Mock private KafkaBatchService kafkaBatchService;

    @InjectMocks private DatabaseLogConsumer databaseLogConsumer;

    @Test
    void testConsume() {
        // Arrange
        GatewayDecisionEvent event =
                new GatewayDecisionEvent(
                        UUID.randomUUID().toString(),
                        "/api/test",
                        "GET",
                        "127.0.0.1",
                        "route-1",
                        "ALLOW",
                        200,
                        100L,
                        50L,
                        "",
                        "test-agent",
                        System.currentTimeMillis());
        List<GatewayDecisionEvent> events = List.of(event);

        // Act
        databaseLogConsumer.consume(events);

        // Assert
        verify(kafkaBatchService).consumeBatch(events);
    }
}
