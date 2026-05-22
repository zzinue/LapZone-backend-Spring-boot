package com.lapzone.api.product;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;

public record ProductRequest(
        @NotBlank(message = "Product name is required")
        @Size(max = 150, message = "Product name must have less than 150 characters")
        String name,

        @NotBlank(message = "Brand is required")
        @Size(max = 100, message = "Brand must have less than 100 characters")
        String brand,

        String description,

        @NotNull(message = "Price is required")
        @DecimalMin(value = "0.0", inclusive = true, message = "Price must be zero or greater")
        BigDecimal price,

        @NotNull(message = "Stock is required")
        @Min(value = 0, message = "Stock must be zero or greater")
        Integer stock,

        Boolean availability,

        String imageUrl
) {
}