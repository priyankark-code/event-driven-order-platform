package com.portfolio.orderservice;

import com.portfolio.orderservice.dto.CreateOrderRequest;
import com.portfolio.orderservice.event.InventoryResultEvent;
import com.portfolio.orderservice.model.Order;
import com.portfolio.orderservice.model.OrderStatus;
import com.portfolio.orderservice.persistence.repository.ProcessedEventRepository;
import com.portfolio.orderservice.service.OrderInventoryResultService;
import com.portfolio.orderservice.service.OrderService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = {
        "outbox.publisher.enabled=false",
        "spring.kafka.listener.auto-startup=false"
})
@Testcontainers
@DirtiesContext(
        classMode = DirtiesContext.ClassMode.AFTER_CLASS
)
@Transactional
class OrderInventoryResultIntegrationTests {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres =
            new PostgreSQLContainer<>("postgres:17-alpine");

    @Autowired
    private OrderService orderService;

    @Autowired
    private OrderInventoryResultService resultService;

    @Autowired
    private ProcessedEventRepository processedEventRepository;

    @Test
    void reservedResultShouldUpdateCreatedOrder() {
        Order created = createOrder();

        InventoryResultEvent event = inventoryResult(
                created.id(),
                InventoryResultEvent.Result.RESERVED,
                null
        );

        resultService.process(event);

        Order updated = orderService.getOrder(created.id());

        assertThat(updated.status())
                .isEqualTo(OrderStatus.INVENTORY_RESERVED);

        assertThat(updated.rejectionReason()).isNull();

        assertThat(
                processedEventRepository.existsById(event.eventId())
        ).isTrue();
    }

    @Test
    void rejectedResultShouldStoreBusinessReason() {
        Order created = createOrder();

        InventoryResultEvent event = inventoryResult(
                created.id(),
                InventoryResultEvent.Result.REJECTED,
                "Insufficient stock for product product-001"
        );

        resultService.process(event);

        Order updated = orderService.getOrder(created.id());

        assertThat(updated.status())
                .isEqualTo(OrderStatus.REJECTED);

        assertThat(updated.rejectionReason())
                .isEqualTo(
                        "Insufficient stock for product product-001"
                );

        assertThat(
                processedEventRepository.existsById(event.eventId())
        ).isTrue();
    }

    @Test
    void duplicateInventoryResultShouldBeProcessedOnce() {
        Order created = createOrder();

        InventoryResultEvent event = inventoryResult(
                created.id(),
                InventoryResultEvent.Result.RESERVED,
                null
        );

        resultService.process(event);
        resultService.process(event);

        Order updated = orderService.getOrder(created.id());

        assertThat(updated.status())
                .isEqualTo(OrderStatus.INVENTORY_RESERVED);

        assertThat(processedEventRepository.count())
                .isEqualTo(1);
    }

    @Test
    void lateInventoryResultShouldNotOverwriteCancellation() {
        Order created = createOrder();

        orderService.cancelOrder(created.id());

        InventoryResultEvent lateResult = inventoryResult(
                created.id(),
                InventoryResultEvent.Result.RESERVED,
                null
        );

        resultService.process(lateResult);

        Order updated = orderService.getOrder(created.id());

        assertThat(updated.status())
                .isEqualTo(OrderStatus.CANCELLED);

        assertThat(
                processedEventRepository.existsById(
                        lateResult.eventId()
                )
        ).isTrue();
    }

    private Order createOrder() {
        return orderService.createOrder(
                new CreateOrderRequest(
                        "order-result-test-customer",
                        List.of(
                                new CreateOrderRequest.OrderItemRequest(
                                        "product-001",
                                        2,
                                        new BigDecimal("49.99")
                                )
                        )
                )
        );
    }

    private InventoryResultEvent inventoryResult(
            UUID orderId,
            InventoryResultEvent.Result result,
            String reason
    ) {
        return new InventoryResultEvent(
                UUID.randomUUID(),
                orderId,
                result,
                reason,
                Instant.now(),
                1
        );
    }
}