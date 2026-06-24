package com.fooddelivery.repository;

import com.fooddelivery.entity.StoreCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface StoreCategoryRepository extends JpaRepository<StoreCategory, Long> {
    List<StoreCategory> findAllByOrderBySortOrderAsc();
}
