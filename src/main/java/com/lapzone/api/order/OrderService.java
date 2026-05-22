package com.lapzone.api.order;

import com.lapzone.api.cart.Cart;
import com.lapzone.api.cart.CartItem;
import com.lapzone.api.cart.CartItemRepository;
import com.lapzone.api.cart.CartRepository;
import com.lapzone.api.product.Product;
import com.lapzone.api.product.ProductRepository;
import com.lapzone.api.user.AppUser;
import com.lapzone.api.user.AppUserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Service
public class OrderService {

    private final AppUserRepository appUserRepository;
    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final ProductRepository productRepository;
    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final PaymentRepository paymentRepository;

    public OrderService(
            AppUserRepository appUserRepository,
            CartRepository cartRepository,
            CartItemRepository cartItemRepository,
            ProductRepository productRepository,
            OrderRepository orderRepository,
            OrderItemRepository orderItemRepository,
            PaymentRepository paymentRepository
    ) {
        this.appUserRepository = appUserRepository;
        this.cartRepository = cartRepository;
        this.cartItemRepository = cartItemRepository;
        this.productRepository = productRepository;
        this.orderRepository = orderRepository;
        this.orderItemRepository = orderItemRepository;
        this.paymentRepository = paymentRepository;
    }

    @Transactional
    public OrderResponse checkout(Authentication authentication, CheckoutRequest request) {
        AppUser user = getAuthenticatedUser(authentication);

        Cart cart = cartRepository.findByUserAndStatus(user, "ACTIVO")
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "Active cart was not found"
                ));

        List<CartItem> cartItems = cartItemRepository.findByCart(cart);

        if (cartItems.isEmpty()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Cart is empty"
            );
        }

        BigDecimal total = cartItems.stream()
                .map(item -> item.getUnitPrice().multiply(BigDecimal.valueOf(item.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        Order order = new Order();
        order.setUser(user);
        order.setCart(cart);
        order.setTotal(total);
        order.setStatus("PENDIENTE");

        Order savedOrder = orderRepository.save(order);

        for (CartItem cartItem : cartItems) {
            Product product = cartItem.getProduct();

            if (cartItem.getQuantity() > product.getStock()) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "Insufficient stock for product: " + product.getName()
                );
            }

            OrderItem orderItem = new OrderItem();
            orderItem.setOrder(savedOrder);
            orderItem.setProduct(product);
            orderItem.setQuantity(cartItem.getQuantity());
            orderItem.setUnitPrice(cartItem.getUnitPrice());

            orderItemRepository.save(orderItem);

            product.setStock(product.getStock() - cartItem.getQuantity());

            if (product.getStock() == 0) {
                product.setAvailability(false);
            }

            productRepository.save(product);
        }

        Payment payment = new Payment();
        payment.setOrder(savedOrder);
        payment.setPaymentMethod(request.paymentMethod());
        payment.setPaymentStatus("PENDIENTE");
        payment.setAmount(total);
        payment.setPaymentReference("PENDING-" + savedOrder.getId());

        Payment savedPayment = paymentRepository.save(payment);

        cart.setStatus("COMPRADO");
        cartRepository.save(cart);

        List<OrderItem> orderItems = orderItemRepository.findByOrder(savedOrder);

        return OrderResponse.of(savedOrder, savedPayment, orderItems);
    }

    public List<OrderResponse> getMyOrders(Authentication authentication) {
        AppUser user = getAuthenticatedUser(authentication);

        return orderRepository.findByUserOrderByCreatedAtDesc(user)
                .stream()
                .map(order -> {
                    Payment payment = paymentRepository.findByOrder(order)
                            .orElseThrow(() -> new ResponseStatusException(
                                    HttpStatus.INTERNAL_SERVER_ERROR,
                                    "Payment was not found"
                            ));

                    List<OrderItem> items = orderItemRepository.findByOrder(order);

                    return OrderResponse.of(order, payment, items);
                })
                .toList();
    }

    public OrderResponse getOrderById(Authentication authentication, UUID orderId) {
        AppUser user = getAuthenticatedUser(authentication);

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Order was not found"
                ));

        if (!order.getUser().getId().equals(user.getId())) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "You cannot access this order"
            );
        }

        Payment payment = paymentRepository.findByOrder(order)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.INTERNAL_SERVER_ERROR,
                        "Payment was not found"
                ));

        List<OrderItem> items = orderItemRepository.findByOrder(order);

        return OrderResponse.of(order, payment, items);
    }

    private AppUser getAuthenticatedUser(Authentication authentication) {
        if (authentication == null || authentication.getName() == null) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "User is not authenticated"
            );
        }

        return appUserRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.UNAUTHORIZED,
                        "Authenticated user was not found"
                ));
    }
}