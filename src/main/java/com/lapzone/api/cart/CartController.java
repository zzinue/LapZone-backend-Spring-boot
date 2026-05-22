package com.lapzone.api.cart;

import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/cart")
@CrossOrigin(origins = "http://localhost:5173")
public class CartController {

    private final CartService cartService;

    public CartController(CartService cartService) {
        this.cartService = cartService;
    }

    @GetMapping
    public CartResponse getCart(Authentication authentication) {
        return cartService.getCart(authentication);
    }

    @PostMapping("/items")
    public CartResponse addItem(
            Authentication authentication,
            @Valid @RequestBody CartItemRequest request
    ) {
        return cartService.addItem(authentication, request);
    }

    @PutMapping("/items/{productId}")
    public CartResponse updateItem(
            Authentication authentication,
            @PathVariable UUID productId,
            @Valid @RequestBody CartItemRequest request
    ) {
        return cartService.updateItem(authentication, productId, request);
    }

    @DeleteMapping("/items/{productId}")
    public CartResponse removeItem(
            Authentication authentication,
            @PathVariable UUID productId
    ) {
        return cartService.removeItem(authentication, productId);
    }

    @DeleteMapping
    public CartResponse clearCart(Authentication authentication) {
        return cartService.clearCart(authentication);
    }
}