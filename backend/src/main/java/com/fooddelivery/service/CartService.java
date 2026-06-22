package com.fooddelivery.service;

import com.fooddelivery.entity.CartItem;
import com.fooddelivery.entity.Dish;
import com.fooddelivery.entity.User;
import com.fooddelivery.repository.CartItemRepository;
import com.fooddelivery.repository.DishRepository;
import com.fooddelivery.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class CartService {

    private final CartItemRepository cartItemRepository;
    private final DishRepository dishRepository;
    private final UserRepository userRepository;

    public List<CartItem> getCart(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("用户不存在"));
        return cartItemRepository.findByUser(user);
    }

    public CartItem addToCart(Long userId, Long dishId, Integer quantity) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("用户不存在"));

        Dish dish = dishRepository.findById(dishId)
                .orElseThrow(() -> new RuntimeException("菜品不存在"));

        if (!dish.getStatus()) {
            throw new RuntimeException("该菜品已下架");
        }

        // 检查购物车中是否已有该菜品
        Optional<CartItem> existing = cartItemRepository.findByUserAndDishId(user, dishId);
        if (existing.isPresent()) {
            CartItem item = existing.get();
            item.setQuantity(item.getQuantity() + quantity);
            return cartItemRepository.save(item);
        }

        CartItem item = CartItem.builder()
                .user(user)
                .dish(dish)
                .quantity(quantity)
                .build();

        return cartItemRepository.save(item);
    }

    public CartItem updateQuantity(Long cartItemId, Integer quantity, Long userId) {
        CartItem item = cartItemRepository.findById(cartItemId)
                .orElseThrow(() -> new RuntimeException("购物车项不存在"));

        if (!item.getUser().getId().equals(userId)) {
            throw new RuntimeException("无权操作此购物车");
        }

        if (quantity <= 0) {
            cartItemRepository.delete(item);
            return null;
        }

        item.setQuantity(quantity);
        return cartItemRepository.save(item);
    }

    public void removeFromCart(Long cartItemId, Long userId) {
        CartItem item = cartItemRepository.findById(cartItemId)
                .orElseThrow(() -> new RuntimeException("购物车项不存在"));

        if (!item.getUser().getId().equals(userId)) {
            throw new RuntimeException("无权操作此购物车");
        }

        cartItemRepository.delete(item);
    }

    public void clearCart(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("用户不存在"));
        cartItemRepository.deleteByUser(user);
    }
}
