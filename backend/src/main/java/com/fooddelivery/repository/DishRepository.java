package com.fooddelivery.repository;

import com.fooddelivery.entity.Dish;
import com.fooddelivery.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;

public interface DishRepository extends JpaRepository<Dish, Long> {

    List<Dish> findByStatusTrue();

    List<Dish> findByCategoryIdAndStatusTrue(Long categoryId);

    List<Dish> findByMerchantAndStatusTrue(User merchant);

    List<Dish> findByMerchant(User merchant);

    @Query("SELECT d FROM Dish d WHERE d.status = true AND d.name LIKE %:keyword%")
    List<Dish> searchByName(@Param("keyword") String keyword);

    List<Dish> findByMerchantIdAndStatusTrue(Long merchantId);
}
