package com.lapzone.api.dashboard;

import java.math.BigDecimal;
import java.util.List;

public record AdminDashboardResponse(
        long totalProducts,
        long totalOrders,
        long pendingOrders,
        long pendingPayments,
        BigDecimal totalSales,
        List<LowStockProductResponse> lowStockProducts
) {
}