package com.lapzone.api.product;

import java.math.BigDecimal;
import java.util.UUID;

public record ProductResponse(
        UUID id,
        String name,
        String brand,
        String description,
        BigDecimal price,
        Integer stock,
        Boolean availability,
        String imageUrl
) {
    public static ProductResponse fromEntity(Product product) {
        return new ProductResponse(
                product.getId(),
                product.getName(),
                product.getBrand(),
                product.getDescription(),
                product.getPrice(),
                product.getStock(),
                product.getAvailability(),
                product.getImageUrl()
        );
    }
}