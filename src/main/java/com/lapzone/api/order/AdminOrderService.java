package com.lapzone.api.order;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
public class AdminOrderService {

    private static final Set<String> VALID_ORDER_STATUSES = Set.of(
            "PENDIENTE",
            "PAGADO",
            "ENVIADO",
            "ENTREGADO",
            "CANCELADO"
    );

    private static final Set<String> VALID_PAYMENT_STATUSES = Set.of(
            "PENDIENTE",
            "APROBADO",
            "RECHAZADO",
            "REEMBOLSADO"
    );

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final PaymentRepository paymentRepository;

    public AdminOrderService(
            OrderRepository orderRepository,
            OrderItemRepository orderItemRepository,
            PaymentRepository paymentRepository
    ) {
        this.orderRepository = orderRepository;
        this.orderItemRepository = orderItemRepository;
        this.paymentRepository = paymentRepository;
    }

    public List<AdminOrderResponse> getAllOrders() {
        return orderRepository.findAllByOrderByCreatedAtDesc()
                .stream()
                .map(this::toAdminOrderResponse)
                .toList();
    }

    public AdminOrderResponse getOrderById(UUID orderId) {
        Order order = getOrder(orderId);
        return toAdminOrderResponse(order);
    }

    @Transactional
    public AdminOrderResponse updateOrderStatus(UUID orderId, OrderStatusRequest request) {
        String newStatus = request.status().toUpperCase();

        if (!VALID_ORDER_STATUSES.contains(newStatus)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Invalid order status"
            );
        }

        Order order = getOrder(orderId);
        order.setStatus(newStatus);

        Order savedOrder = orderRepository.save(order);

        return toAdminOrderResponse(savedOrder);
    }

    @Transactional
    public AdminOrderResponse updatePaymentStatus(UUID orderId, PaymentStatusRequest request) {
        String newPaymentStatus = request.paymentStatus().toUpperCase();

        if (!VALID_PAYMENT_STATUSES.contains(newPaymentStatus)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Invalid payment status"
            );
        }

        Order order = getOrder(orderId);

        Payment payment = paymentRepository.findByOrder(order)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.INTERNAL_SERVER_ERROR,
                        "Payment was not found"
                ));

        payment.setPaymentStatus(newPaymentStatus);

        /*
         * Regla práctica:
         * Si el pago se aprueba, marcamos el pedido como PAGADO.
         * Si el pago se rechaza, dejamos el pedido como PENDIENTE o podrías cambiarlo a CANCELADO.
         */
        if ("APROBADO".equals(newPaymentStatus)) {
            order.setStatus("PAGADO");
            orderRepository.save(order);
        }

        paymentRepository.save(payment);

        return toAdminOrderResponse(order);
    }

    private AdminOrderResponse toAdminOrderResponse(Order order) {
        Payment payment = paymentRepository.findByOrder(order)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.INTERNAL_SERVER_ERROR,
                        "Payment was not found"
                ));

        List<OrderItem> items = orderItemRepository.findByOrder(order);

        return AdminOrderResponse.of(order, payment, items);
    }

    private Order getOrder(UUID orderId) {
        return orderRepository.findById(orderId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Order was not found"
                ));
    }
}