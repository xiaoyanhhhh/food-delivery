package com.fooddelivery.repository;

import com.fooddelivery.entity.CartItem;
import com.fooddelivery.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface CartItemRepository extends JpaRepository<CartItem, Long> {
    List<CartItem> findByUser(User user);
    Optional<CartItem> findByUserAndDishId(User user, Long dishId);
    void deleteByUser(User user);
}
