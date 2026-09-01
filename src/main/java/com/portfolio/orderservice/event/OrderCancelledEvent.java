package com.portfolio.orderservice.event;

import java.time.Instant;
import java.util.UUID;

public record OrderCancelledEvent(
        UUID eventId,
        UUID orderId,
        Instant occurredAt,
        int eventVersion
) {
}