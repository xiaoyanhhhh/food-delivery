package com.fooddelivery.repository;

import com.fooddelivery.entity.Dish;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;

public interface DishRepository extends JpaRepository<Dish, Long> {

    List<Dish> findByStatusTrue();

    Page<Dish> findByStatusTrue(Pageable pageable);

    List<Dish> findByCategoryIdAndStatusTrue(Long categoryId);

    List<Dish> findByMerchantAndStatusTrue(com.fooddelivery.entity.User merchant);

    List<Dish> findByMerchant(com.fooddelivery.entity.User merchant);

    @Query("SELECT d FROM Dish d WHERE d.status = true AND d.name LIKE %:keyword%")
    List<Dish> searchByName(@Param("keyword") String keyword);

    @Query("SELECT d FROM Dish d WHERE d.status = true AND d.name LIKE %:keyword%")
    Page<Dish> searchByName(@Param("keyword") String keyword, Pageable pageable);

    Page<Dish> findByMerchantIdAndStatusTrue(Long merchantId, Pageable pageable);

    List<Dish> findByMerchantIdAndStatusTrue(Long merchantId);

    @Query("SELECT d FROM Dish d WHERE d.merchant.id = :merchantId AND d.category.id = :categoryId AND d.status = true")
    Page<Dish> findByMerchantIdAndCategoryIdAndStatusTrue(
            @Param("merchantId") Long merchantId,
            @Param("categoryId") Long categoryId,
            Pageable pageable);
}
