package com.fooddelivery.repository;

import com.fooddelivery.entity.Store;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.Optional;

public interface StoreRepository extends JpaRepository<Store, Long> {
    Optional<Store> findByMerchantId(Long merchantId);
    Page<Store> findByStatusTrue(Pageable pageable);
    Page<Store> findByStatusTrueAndCategoryId(Long categoryId, Pageable pageable);

    @Query("SELECT s FROM Store s WHERE s.status = true AND s.name LIKE %:keyword%")
    Page<Store> searchByName(@Param("keyword") String keyword, Pageable pageable);

    @Query("SELECT s FROM Store s WHERE s.status = true AND s.category.id = :categoryId AND s.name LIKE %:keyword%")
    Page<Store> searchByCategoryAndName(@Param("categoryId") Long categoryId, @Param("keyword") String keyword, Pageable pageable);
}
