package com.lapzone.api.order;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record AdminOrderResponse(
        UUID orderId,
        UUID userId,
        String customerName,
        String customerEmail,
        String status,
        BigDecimal total,
        String paymentMethod,
        String paymentStatus,
        LocalDateTime createdAt,
        List<OrderItemResponse> items
) {

    public static AdminOrderResponse of(
            Order order,
            Payment payment,
            List<OrderItem> items
    ) {
        List<OrderItemResponse> itemResponses = items.stream()
                .map(OrderItemResponse::fromEntity)
                .toList();

        String customerName = order.getUser().getFirstName() + " " + order.getUser().getLastName();

        return new AdminOrderResponse(
                order.getId(),
                order.getUser().getId(),
                customerName,
                order.getUser().getEmail(),
                order.getStatus(),
                order.getTotal(),
                payment.getPaymentMethod(),
                payment.getPaymentStatus(),
                order.getCreatedAt(),
                itemResponses
        );
    }
}