package com.lapzone.api.order;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record OrderResponse(
        UUID orderId,
        String status,
        BigDecimal total,
        String paymentMethod,
        String paymentStatus,
        String stripeCheckoutUrl,
        LocalDateTime createdAt,
        List<OrderItemResponse> items
) {

    public static OrderResponse of(
            Order order,
            Payment payment,
            List<OrderItem> items
    ) {
        List<OrderItemResponse> itemResponses = items.stream()
                .map(OrderItemResponse::fromEntity)
                .toList();

        return new OrderResponse(
                order.getId(),
                order.getStatus(),
                order.getTotal(),
                payment.getPaymentMethod(),
                payment.getPaymentStatus(),
                null,
                order.getCreatedAt(),
                itemResponses
        );
    }

    public OrderResponse withStripeCheckoutUrl(String stripeCheckoutUrl) {
        return new OrderResponse(
                orderId,
                status,
                total,
                paymentMethod,
                paymentStatus,
                stripeCheckoutUrl,
                createdAt,
                items
        );
    }
}
