package com.portfolio.orderservice.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.List;

public record CreateOrderRequest(

        @NotBlank
        String customerId,

        @NotEmpty
        List<@Valid OrderItemRequest> items

) {

    public record OrderItemRequest(

            @NotBlank
            String productId,

            @Min(1)
            int quantity,

            @NotNull
            @DecimalMin(value = "0.01")
            BigDecimal unitPrice
    ) {
    }
}