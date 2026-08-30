package com.portfolio.orderservice.service;

import com.portfolio.orderservice.event.InventoryResultEvent;
import com.portfolio.orderservice.exception.OrderNotFoundException;
import com.portfolio.orderservice.persistence.entity.OrderEntity;
import com.portfolio.orderservice.persistence.entity.ProcessedEventEntity;
import com.portfolio.orderservice.persistence.repository.OrderRepository;
import com.portfolio.orderservice.persistence.repository.ProcessedEventRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
public class OrderInventoryResultService {

    private static final Logger log =
            LoggerFactory.getLogger(
                    OrderInventoryResultService.class
            );

    private final OrderRepository orderRepository;
    private final ProcessedEventRepository processedEventRepository;

    public OrderInventoryResultService(
            OrderRepository orderRepository,
            ProcessedEventRepository processedEventRepository
    ) {
        this.orderRepository = orderRepository;
        this.processedEventRepository = processedEventRepository;
    }

    @Transactional
    public void process(InventoryResultEvent event) {
        if (processedEventRepository.existsById(event.eventId())) {
            log.info(
                    "Skipping duplicate inventory result {}",
                    event.eventId()
            );
            return;
        }

        OrderEntity order = orderRepository
                .findById(event.orderId())
                .orElseThrow(() ->
                        new OrderNotFoundException(event.orderId())
                );

        switch (event.result()) {
            case RESERVED ->
                    order.markInventoryReserved();

            case REJECTED ->
                    order.rejectInventory(event.reason());
        }

        processedEventRepository.save(
                new ProcessedEventEntity(
                        event.eventId(),
                        "Inventory" + event.result(),
                        Instant.now()
                )
        );

        log.info(
                "Applied inventory result {} to order {}",
                event.result(),
                event.orderId()
        );
    }
}