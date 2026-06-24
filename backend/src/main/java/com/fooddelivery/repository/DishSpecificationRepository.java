package com.fooddelivery.repository;

import com.fooddelivery.entity.DishSpecification;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface DishSpecificationRepository extends JpaRepository<DishSpecification, Long> {
    List<DishSpecification> findByDishId(Long dishId);
    void deleteByDishId(Long dishId);
}
