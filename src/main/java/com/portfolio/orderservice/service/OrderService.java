package com.portfolio.orderservice.service;

import com.portfolio.orderservice.dto.CreateOrderRequest;
import com.portfolio.orderservice.exception.OrderNotFoundException;
import com.portfolio.orderservice.model.Order;
import com.portfolio.orderservice.model.OrderItem;
import com.portfolio.orderservice.model.OrderStatus;
import com.portfolio.orderservice.persistence.entity.OrderEntity;
import com.portfolio.orderservice.persistence.entity.OrderItemEntity;
import com.portfolio.orderservice.persistence.repository.OrderRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.portfolio.orderservice.dto.OrderSummaryResponse;
import com.portfolio.orderservice.dto.PagedResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
public class OrderService {

    private final OrderRepository orderRepository;

    public OrderService(OrderRepository orderRepository) {
        this.orderRepository = orderRepository;
    }

    @Transactional
    public Order createOrder(CreateOrderRequest request) {
        BigDecimal totalAmount = request.items().stream()
                .map(item -> item.unitPrice()
                        .multiply(BigDecimal.valueOf(item.quantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        OrderEntity entity = new OrderEntity(
                UUID.randomUUID(),
                request.customerId(),
                OrderStatus.CREATED,
                totalAmount,
                Instant.now()
        );

        request.items().forEach(item ->
                entity.addItem(new OrderItemEntity(
                        item.productId(),
                        item.quantity(),
                        item.unitPrice()
                ))
        );

        OrderEntity savedEntity = orderRepository.save(entity);
        return toDomain(savedEntity);
    }

    @Transactional(readOnly = true)
    public Order getOrder(UUID id) {
        OrderEntity entity = orderRepository.findById(id)
                .orElseThrow(() -> new OrderNotFoundException(id));

        return toDomain(entity);
    }

    @Transactional(readOnly = true)
    public PagedResponse<OrderSummaryResponse> getOrders(
            int page,
            int size
    ) {
        PageRequest pageRequest = PageRequest.of(
                page,
                size,
                Sort.by(Sort.Direction.DESC, "createdAt")
        );

        Page<OrderSummaryResponse> result = orderRepository
                .findAll(pageRequest)
                .map(entity -> new OrderSummaryResponse(
                        entity.getId(),
                        entity.getCustomerId(),
                        entity.getStatus(),
                        entity.getTotalAmount(),
                        entity.getCreatedAt()
                ));

        return new PagedResponse<>(
                result.getContent(),
                result.getNumber(),
                result.getSize(),
                result.getTotalElements(),
                result.getTotalPages()
        );
    }

    private Order toDomain(OrderEntity entity) {
        List<OrderItem> items = entity.getItems().stream()
                .map(item -> new OrderItem(
                        item.getProductId(),
                        item.getQuantity(),
                        item.getUnitPrice()
                ))
                .toList();

        return new Order(
                entity.getId(),
                entity.getCustomerId(),
                entity.getStatus(),
                items,
                entity.getTotalAmount(),
                entity.getCreatedAt()
        );
    }

    @Transactional
    public Order cancelOrder(UUID id) {
        OrderEntity entity = orderRepository.findById(id)
                .orElseThrow(() -> new OrderNotFoundException(id));

        entity.cancel();

        return toDomain(entity);
    }
}