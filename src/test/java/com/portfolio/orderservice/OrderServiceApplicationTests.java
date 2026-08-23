package com.portfolio.orderservice;

import com.portfolio.orderservice.dto.CreateOrderRequest;
import com.portfolio.orderservice.model.Order;
import com.portfolio.orderservice.service.OrderService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Testcontainers
class OrderServiceApplicationTests {

	@Container
	@ServiceConnection
	static PostgreSQLContainer<?> postgres =
			new PostgreSQLContainer<>("postgres:17-alpine");

	@Autowired
	private OrderService orderService;

	@Test
	void shouldPersistAndRetrieveOrder() {
		CreateOrderRequest request = new CreateOrderRequest(
				"customer-123",
				List.of(
						new CreateOrderRequest.OrderItemRequest(
								"product-001",
								2,
								new BigDecimal("49.99")
						),
						new CreateOrderRequest.OrderItemRequest(
								"product-002",
								1,
								new BigDecimal("19.99")
						)
				)
		);

		Order created = orderService.createOrder(request);
		Order retrieved = orderService.getOrder(created.id());

		assertThat(retrieved.id()).isEqualTo(created.id());
		assertThat(retrieved.customerId()).isEqualTo("customer-123");
		assertThat(retrieved.totalAmount())
				.isEqualByComparingTo("119.97");
		assertThat(retrieved.items()).hasSize(2);
	}
}