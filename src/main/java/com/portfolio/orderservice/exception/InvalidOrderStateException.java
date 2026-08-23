package com.portfolio.orderservice.exception;

import com.portfolio.orderservice.model.OrderStatus;

import java.util.UUID;

public class InvalidOrderStateException extends RuntimeException {

    public InvalidOrderStateException(UUID id, OrderStatus status) {
        super("Order " + id
                + " cannot be cancelled while in status " + status);
    }
}