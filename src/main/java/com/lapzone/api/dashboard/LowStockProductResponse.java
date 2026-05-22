package com.lapzone.api.dashboard;

import com.lapzone.api.product.Product;

import java.math.BigDecimal;
import java.util.UUID;

public record LowStockProductResponse(
        UUID id,
        String name,
        String brand,
        BigDecimal price,
        Integer stock,
        Boolean availability
) {

    public static LowStockProductResponse fromProduct(Product product) {
        return new LowStockProductResponse(
                product.getId(),
                product.getName(),
                product.getBrand(),
                product.getPrice(),
                product.getStock(),
                product.getAvailability()
        );
    }
}