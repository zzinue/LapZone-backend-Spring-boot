package com.lapzone.api.product;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.UUID;

public interface ProductRepository extends JpaRepository<Product, UUID> {

    List<Product> findByStockLessThanEqualOrderByStockAsc(Integer stock);

    List<Product> findByOrderByCreatedAtDesc(Pageable pageable);

    @Query("""
            SELECT oi.product
            FROM OrderItem oi
            GROUP BY oi.product
            ORDER BY SUM(oi.quantity) DESC
            """)
    List<Product> findBestSellingProducts(Pageable pageable);
}