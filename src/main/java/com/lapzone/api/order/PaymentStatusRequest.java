package com.lapzone.api.order;

import jakarta.validation.constraints.NotBlank;

public record PaymentStatusRequest(
        @NotBlank(message = "Payment status is required")
        String paymentStatus
) {
}