package com.lapzone.api.cart;

import java.math.BigDecimal;
import java.util.UUID;

public record CartItemResponse(
        UUID productId,
        String name,
        String brand,
        String imageUrl,
        BigDecimal price,
        Integer quantity,
        BigDecimal subtotal
) {

    public static CartItemResponse fromEntity(CartItem item) {
        BigDecimal subtotal = item.getUnitPrice()
                .multiply(BigDecimal.valueOf(item.getQuantity()));

        return new CartItemResponse(
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