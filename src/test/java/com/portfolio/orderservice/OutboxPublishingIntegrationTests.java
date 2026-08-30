package com.portfolio.orderservice;

import com.portfolio.orderservice.dto.CreateOrderRequest;
import com.portfolio.orderservice.model.Order;
import com.portfolio.orderservice.persistence.repository.OutboxEventRepository;
import com.portfolio.orderservice.service.OrderService;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.kafka.KafkaContainer;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.List;
import java.util.Properties;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import org.springframework.test.annotation.DirtiesContext;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

@SpringBootTest(properties = {
        "outbox.publisher.enabled=true",
        "outbox.publisher.fixed-delay-ms=100",
        "spring.kafka.listener.auto-startup=false"
})
@Testcontainers
@DirtiesContext(
        classMode = DirtiesContext.ClassMode.AFTER_CLASS
)
class OutboxPublishingIntegrationTests {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres =
            new PostgreSQLContainer<>("postgres:17-alpine");

    @Container
    @ServiceConnection
    static KafkaContainer kafka =
            new KafkaContainer("apache/kafka:4.3.1");

    @Autowired
    private OrderService orderService;

    @Autowired
    private OutboxEventRepository outboxEventRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void shouldPublishOutboxEventAndMarkItPublished() throws Exception {
        Order created = orderService.createOrder(
                new CreateOrderRequest(
                        "customer-kafka-test",
                        List.of(
                                new CreateOrderRequest.OrderItemRequest(
                                        "product-001",
                                        2,
                                        new BigDecimal("49.99")
                                )
                        )
                )
        );

        assertThat(outboxEventRepository.countByPublishedAtIsNull())
                .isEqualTo(1);

        await()
                .atMost(Duration.ofSeconds(15))
                .pollInterval(Duration.ofMillis(200))
                .untilAsserted(() ->
                        assertThat(
                                outboxEventRepository
                                        .countByPublishedAtIsNull()
                        ).isZero()
                );

        ConsumerRecord<String, String> record =
                consumeOrderCreatedEvent(created.id());

        assertThat(record).isNotNull();
        assertThat(record.key())
                .isEqualTo(created.id().toString());
        JsonNode payload = objectMapper.readTree(record.value());

        assertThat(payload.get("orderId").asText())
                .isEqualTo(created.id().toString());

        assertThat(payload.get("customerId").asText())
                .isEqualTo("customer-kafka-test");

        assertThat(payload.get("eventVersion").asInt())
                .isEqualTo(1);

        assertThat(payload.get("items").size())
                .isEqualTo(1);

        assertThat(
                payload.get("items")
                        .get(0)
                        .get("productId")
                        .asText()
        ).isEqualTo("product-001");
    }

    private ConsumerRecord<String, String> consumeOrderCreatedEvent(
            UUID orderId
    ) {
        Properties properties = new Properties();

        properties.put(
                ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG,
                kafka.getBootstrapServers()
        );
        properties.put(
                ConsumerConfig.GROUP_ID_CONFIG,
                "outbox-integration-test-" + UUID.randomUUID()
        );
        properties.put(
                ConsumerConfig.AUTO_OFFSET_RESET_CONFIG,
                "earliest"
        );
        properties.put(
                ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG,
                StringDeserializer.class
        );
        properties.put(
                ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG,
                StringDeserializer.class
        );

        AtomicReference<ConsumerRecord<String, String>> result =
                new AtomicReference<>();

        try (KafkaConsumer<String, String> consumer =
                     new KafkaConsumer<>(properties)) {

            consumer.subscribe(List.of("order-events"));

            await()
                    .atMost(Duration.ofSeconds(15))
                    .pollInterval(Duration.ofMillis(200))
                    .until(() -> {
                        for (ConsumerRecord<String, String> record :
                                consumer.poll(Duration.ofMillis(500))) {

                            if (orderId.toString()
                                    .equals(record.key())) {
                                result.set(record);
                                return true;
                            }
                        }

                        return false;
                    });
        }

        return result.get();
    }
}