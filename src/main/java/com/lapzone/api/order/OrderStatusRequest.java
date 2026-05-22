package com.lapzone.api.order;

import jakarta.validation.constraints.NotBlank;

public record OrderStatusRequest(
        @NotBlank(message = "Status is required")
        String status
) {
}