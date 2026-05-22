package com.lapzone.api.cart;

import com.lapzone.api.user.AppUser;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface CartRepository extends JpaRepository<Cart, UUID> {

    Optional<Cart> findByUserAndStatus(AppUser user, String status);
}