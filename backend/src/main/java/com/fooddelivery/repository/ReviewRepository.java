package com.fooddelivery.repository;

import com.fooddelivery.entity.Review;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ReviewRepository extends JpaRepository<Review, Long> {
    Page<Review> findByStoreIdOrderByCreatedAtDesc(Long storeId, Pageable pageable);
    Page<Review> findByDishIdOrderByCreatedAtDesc(Long dishId, Pageable pageable);
    Page<Review> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);

    @Query("SELECT AVG(r.rating) FROM Review r WHERE r.store.id = :storeId")
    Double getAvgRatingByStore(@Param("storeId") Long storeId);

    @Query("SELECT COUNT(r) FROM Review r WHERE r.store.id = :storeId")
    Long countByStore(@Param("storeId") Long storeId);

    boolean existsByUserIdAndOrderId(Long userId, Long orderId);
}
