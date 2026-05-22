package com.lapzone.api.cart;

import com.lapzone.api.product.Product;
import com.lapzone.api.product.ProductRepository;
import com.lapzone.api.user.AppUser;
import com.lapzone.api.user.AppUserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;

@Service
public class CartService {

    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final ProductRepository productRepository;
    private final AppUserRepository appUserRepository;

    public CartService(
            CartRepository cartRepository,
            CartItemRepository cartItemRepository,
            ProductRepository productRepository,
            AppUserRepository appUserRepository
    ) {
        this.cartRepository = cartRepository;
        this.cartItemRepository = cartItemRepository;
        this.productRepository = productRepository;
        this.appUserRepository = appUserRepository;
    }

    public CartResponse getCart(Authentication authentication) {
        AppUser user = getAuthenticatedUser(authentication);
        Cart cart = getOrCreateActiveCart(user);
        List<CartItem> items = cartItemRepository.findByCart(cart);

        return CartResponse.of(cart, items);
    }

    @Transactional
    public CartResponse addItem(Authentication authentication, CartItemRequest request) {
        AppUser user = getAuthenticatedUser(authentication);
        Cart cart = getOrCreateActiveCart(user);
        Product product = getProduct(request.productId());

        if (request.quantity() > product.getStock()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Quantity exceeds available stock"
            );
        }

        CartItem item = cartItemRepository.findByCartAndProduct(cart, product)
                .orElse(null);

        if (item == null) {
            item = new CartItem();
            item.setCart(cart);
            item.setProduct(product);
            item.setQuantity(request.quantity());
            item.setUnitPrice(product.getPrice());
        } else {
            int newQuantity = item.getQuantity() + request.quantity();

            if (newQuantity > product.getStock()) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "Quantity exceeds available stock"
                );
            }

            item.setQuantity(newQuantity);
        }

        cartItemRepository.save(item);

        return getCart(authentication);
    }

    @Transactional
    public CartResponse updateItem(
            Authentication authentication,
            UUID productId,
            CartItemRequest request
    ) {
        AppUser user = getAuthenticatedUser(authentication);
        Cart cart = getOrCreateActiveCart(user);
        Product product = getProduct(productId);

        if (request.quantity() > product.getStock()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Quantity exceeds available stock"
            );
        }

        CartItem item = cartItemRepository.findByCartAndProduct(cart, product)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Product was not found in cart"
                ));

        item.setQuantity(request.quantity());
        cartItemRepository.save(item);

        return getCart(authentication);
    }

    @Transactional
    public CartResponse removeItem(Authentication authentication, UUID productId) {
        AppUser user = getAuthenticatedUser(authentication);
        Cart cart = getOrCreateActiveCart(user);
        Product product = getProduct(productId);

        cartItemRepository.deleteByCartAndProduct(cart, product);

        return getCart(authentication);
    }

    @Transactional
    public CartResponse clearCart(Authentication authentication) {
        AppUser user = getAuthenticatedUser(authentication);
        Cart cart = getOrCreateActiveCart(user);

        cartItemRepository.deleteByCart(cart);

        return getCart(authentication);
    }

    private AppUser getAuthenticatedUser(Authentication authentication) {
        if (authentication == null || authentication.getName() == null) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "User is not authenticated"
            );
        }

        String email = authentication.getName();

        return appUserRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.UNAUTHORIZED,
                        "Authenticated user was not found"
                ));
    }

    private Cart getOrCreateActiveCart(AppUser user) {
        return cartRepository.findByUserAndStatus(user, "ACTIVO")
                .orElseGet(() -> {
                    Cart cart = new Cart();
                    cart.setUser(user);
                    cart.setStatus("ACTIVO");
                    return cartRepository.save(cart);
                });
    }

    private Product getProduct(UUID productId) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Product was not found"
                ));

        if (!Boolean.TRUE.equals(product.getAvailability())) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Product is not available"
            );
        }

        return product;
    }
}