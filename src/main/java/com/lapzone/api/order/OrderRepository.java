package com.lapzone.api.order;

import com.lapzone.api.user.AppUser;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface OrderRepository extends JpaRepository<Order, UUID> {

    List<Order> findByUserOrderByCreatedAtDesc(AppUser user);

    List<Order> findAllByOrderByCreatedAtDesc();
    long countByStatus(String status);
}