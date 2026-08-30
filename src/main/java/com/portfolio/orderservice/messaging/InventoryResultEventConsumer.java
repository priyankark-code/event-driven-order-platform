package com.portfolio.orderservice.messaging;

import com.portfolio.orderservice.event.InventoryResultEvent;
import com.portfolio.orderservice.service.OrderInventoryResultService;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

@Component
public class InventoryResultEventConsumer {

    private final ObjectMapper objectMapper;
    private final OrderInventoryResultService resultService;

    public InventoryResultEventConsumer(
            ObjectMapper objectMapper,
            OrderInventoryResultService resultService
    ) {
        this.objectMapper = objectMapper;
        this.resultService = resultService;
    }

    @KafkaListener(
            topics = "${order.kafka.inventory-events-topic:inventory-events}"
    )
    public void consume(String payload) {
        try {
            InventoryResultEvent event = objectMapper.readValue(
                    payload,
                    InventoryResultEvent.class
            );

            if (event.eventVersion() != 1) {
                throw new IllegalArgumentException(
                        "Unsupported InventoryResult event version: "
                                + event.eventVersion()
                );
            }

            resultService.process(event);
        } catch (RuntimeException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new IllegalArgumentException(
                    "Invalid InventoryResult event payload",
                    exception
            );
        }
    }
}