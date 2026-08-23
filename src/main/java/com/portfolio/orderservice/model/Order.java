package com.portfolio.orderservice.model;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record Order(
        UUID id,
        String customerId,
        OrderStatus status,
        List<OrderItem> items,
        BigDecimal totalAmount,
        Instant createdAt
) {
}