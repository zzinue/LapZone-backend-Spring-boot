package com.lapzone.api.dashboard;

import com.lapzone.api.order.OrderRepository;
import com.lapzone.api.order.PaymentRepository;
import com.lapzone.api.product.ProductRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

@Service
public class AdminDashboardService {

    private static final int LOW_STOCK_LIMIT = 3;

    private final ProductRepository productRepository;
    private final OrderRepository orderRepository;
    private final PaymentRepository paymentRepository;

    public AdminDashboardService(
            ProductRepository productRepository,
            OrderRepository orderRepository,
            PaymentRepository paymentRepository
    ) {
        this.productRepository = productRepository;
        this.orderRepository = orderRepository;
        this.paymentRepository = paymentRepository;
    }

    public AdminDashboardResponse getDashboardData() {
        long totalProducts = productRepository.count();
        long totalOrders = orderRepository.count();
        long pendingOrders = orderRepository.countByStatus("PENDIENTE");
        long pendingPayments = paymentRepository.countByPaymentStatus("PENDIENTE");

        BigDecimal totalSales = paymentRepository.sumApprovedPayments();

        List<LowStockProductResponse> lowStockProducts = productRepository
                .findByStockLessThanEqualOrderByStockAsc(LOW_STOCK_LIMIT)
                .stream()
                .map(LowStockProductResponse::fromProduct)
                .toList();

        return new AdminDashboardResponse(
                totalProducts,
                totalOrders,
                pendingOrders,
                pendingPayments,
                totalSales,
                lowStockProducts
        );
    }
}