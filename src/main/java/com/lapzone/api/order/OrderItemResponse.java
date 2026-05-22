package com.lapzone.api.order;

import java.math.BigDecimal;
import java.util.UUID;

public record OrderItemResponse(
        UUID productId,
        String name,
        String brand,
        String imageUrl,
        BigDecimal price,
        Integer quantity,
        BigDecimal subtotal
) {

    public static OrderItemResponse fromEntity(OrderItem item) {
        BigDecimal subtotal = item.getUnitPrice()
                .multiply(BigDecimal.valueOf(item.getQuantity()));

        return new OrderItemResponse(
                item.getProduct().getId(),
                item.getProduct().getName(),
                item.getProduct().getBrand(),
                item.getProduct().getImageUrl(),
                item.getUnitPrice(),
                item.getQuantity(),
                subtotal
        );
    }
}