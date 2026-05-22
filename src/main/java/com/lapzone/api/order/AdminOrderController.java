package com.lapzone.api.order;

import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/admin/orders")
@CrossOrigin(origins = "http://localhost:5173")
public class AdminOrderController {

    private final AdminOrderService adminOrderService;

    public AdminOrderController(AdminOrderService adminOrderService) {
        this.adminOrderService = adminOrderService;
    }

    @GetMapping
    public List<AdminOrderResponse> getAllOrders() {
        return adminOrderService.getAllOrders();
    }

    @GetMapping("/{id}")
    public AdminOrderResponse getOrderById(@PathVariable UUID id) {
        return adminOrderService.getOrderById(id);
    }

    @PatchMapping("/{id}/status")
    public AdminOrderResponse updateOrderStatus(
            @PathVariable UUID id,
            @Valid @RequestBody OrderStatusRequest request
    ) {
        return adminOrderService.updateOrderStatus(id, request);
    }

    @PatchMapping("/{id}/payment-status")
    public AdminOrderResponse updatePaymentStatus(
            @PathVariable UUID id,
            @Valid @RequestBody PaymentStatusRequest request
    ) {
        return adminOrderService.updatePaymentStatus(id, request);
    }
}