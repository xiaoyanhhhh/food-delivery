package com.fooddelivery.repository;

import com.fooddelivery.entity.Favorite;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface FavoriteRepository extends JpaRepository<Favorite, Long> {
    Page<Favorite> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);
    List<Favorite> findByUserId(Long userId);
    Optional<Favorite> findByUserIdAndStoreId(Long userId, Long storeId);
    boolean existsByUserIdAndStoreId(Long userId, Long storeId);
    void deleteByUserIdAndStoreId(Long userId, Long storeId);
}
