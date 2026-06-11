package com.lapzone.api.order;

import com.lapzone.api.cart.Cart;
import com.lapzone.api.cart.CartItem;
import com.lapzone.api.cart.CartItemRepository;
import com.lapzone.api.cart.CartRepository;
import com.lapzone.api.product.Product;
import com.lapzone.api.product.ProductRepository;
import com.lapzone.api.user.AppUser;
import com.lapzone.api.user.AppUserRepository;
import com.stripe.Stripe;
import com.stripe.exception.StripeException;
import com.stripe.model.checkout.Session;
import com.stripe.param.checkout.SessionCreateParams;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.math.RoundingMode;
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
    private final String stripeSecretKey;
    private final String frontendUrl;

    public OrderService(
            AppUserRepository appUserRepository,
            CartRepository cartRepository,
            CartItemRepository cartItemRepository,
            ProductRepository productRepository,
            OrderRepository orderRepository,
            OrderItemRepository orderItemRepository,
            PaymentRepository paymentRepository,
            @Value("${app.stripe.secret-key}") String stripeSecretKey,
            @Value("${app.frontend-url}") String frontendUrl
    ) {
        this.appUserRepository = appUserRepository;
        this.cartRepository = cartRepository;
        this.cartItemRepository = cartItemRepository;
        this.productRepository = productRepository;
        this.orderRepository = orderRepository;
        this.orderItemRepository = orderItemRepository;
        this.paymentRepository = paymentRepository;
        this.stripeSecretKey = stripeSecretKey;
        this.frontendUrl = frontendUrl;
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

        OrderResponse response = OrderResponse.of(savedOrder, savedPayment, orderItems);

        if ("STRIPE".equals(request.paymentMethod())) {
            Session stripeSession = createStripeCheckoutSession(savedOrder, savedPayment, orderItems);
            savedPayment.setPaymentReference(stripeSession.getId());
            paymentRepository.save(savedPayment);

            return response.withStripeCheckoutUrl(stripeSession.getUrl());
        }

        return response;
    }

    @Transactional
    public OrderResponse confirmStripePayment(Authentication authentication, String sessionId) {
        AppUser user = getAuthenticatedUser(authentication);

        if (sessionId == null || sessionId.isBlank()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Stripe session id is required"
            );
        }

        configureStripe();

        Session session;

        try {
            session = Session.retrieve(sessionId);
        } catch (StripeException exception) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_GATEWAY,
                    "Stripe payment session could not be verified",
                    exception
            );
        }

        Payment payment = paymentRepository.findByPaymentReference(sessionId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Payment was not found"
                ));

        Order order = payment.getOrder();

        if (!order.getUser().getId().equals(user.getId())) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "You cannot confirm this payment"
            );
        }

        if ("paid".equals(session.getPaymentStatus())) {
            payment.setPaymentStatus("APROBADO");
            order.setStatus("PAGADO");
        } else {
            payment.setPaymentStatus("PENDIENTE");
            order.setStatus("PENDIENTE");
        }

        paymentRepository.save(payment);
        orderRepository.save(order);

        List<OrderItem> items = orderItemRepository.findByOrder(order);

        return OrderResponse.of(order, payment, items);
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

    private Session createStripeCheckoutSession(
            Order order,
            Payment payment,
            List<OrderItem> orderItems
    ) {
        configureStripe();

        SessionCreateParams.Builder params = SessionCreateParams.builder()
                .setMode(SessionCreateParams.Mode.PAYMENT)
                .setSuccessUrl(frontendUrl + "/stripe/success?session_id={CHECKOUT_SESSION_ID}")
                .setCancelUrl(frontendUrl + "/checkout")
                .putMetadata("orderId", order.getId().toString())
                .putMetadata("paymentId", payment.getId().toString());

        for (OrderItem item : orderItems) {
            params.addLineItem(
                    SessionCreateParams.LineItem.builder()
                            .setQuantity(item.getQuantity().longValue())
                            .setPriceData(
                                    SessionCreateParams.LineItem.PriceData.builder()
                                            .setCurrency("mxn")
                                            .setUnitAmount(toStripeAmount(item.getUnitPrice()))
                                            .setProductData(
                                                    SessionCreateParams.LineItem.PriceData.ProductData.builder()
                                                            .setName(item.getProduct().getName())
                                                            .build()
                                            )
                                            .build()
                            )
                            .build()
            );
        }

        try {
            return Session.create(params.build());
        } catch (StripeException exception) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_GATEWAY,
                    "Stripe checkout session could not be created",
                    exception
            );
        }
    }

    private void configureStripe() {
        if (stripeSecretKey == null || stripeSecretKey.isBlank()) {
            throw new ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "Stripe secret key is not configured"
            );
        }

        Stripe.apiKey = stripeSecretKey;
    }

    private long toStripeAmount(BigDecimal amount) {
        return amount
                .multiply(BigDecimal.valueOf(100))
                .setScale(0, RoundingMode.HALF_UP)
                .longValueExact();
    }
}
