package com.portfolio.orderservice.service;

import com.portfolio.orderservice.dto.CreateOrderRequest;
import com.portfolio.orderservice.exception.OrderNotFoundException;
import com.portfolio.orderservice.model.Order;
import com.portfolio.orderservice.model.OrderItem;
import com.portfolio.orderservice.model.OrderStatus;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class OrderService {

    private final Map<UUID, Order> orders = new ConcurrentHashMap<>();

    public Order createOrder(CreateOrderRequest request) {

        List<OrderItem> items = request.items()
                .stream()
                .map(item -> new OrderItem(
                        item.productId(),
                        item.quantity(),
                        item.unitPrice()
                ))
                .toList();

        BigDecimal totalAmount = items.stream()
                .map(item -> item.unitPrice()
                        .multiply(BigDecimal.valueOf(item.quantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        Order order = new Order(
                UUID.randomUUID(),
                request.customerId(),
                OrderStatus.CREATED,
                items,
                totalAmount,
                Instant.now()
        );

        orders.put(order.id(), order);

        return order;
    }

    public Order getOrder(UUID id) {
        Order order = orders.get(id);

        if (order == null) {
            throw new OrderNotFoundException(id);
        }

        return order;
    }
}