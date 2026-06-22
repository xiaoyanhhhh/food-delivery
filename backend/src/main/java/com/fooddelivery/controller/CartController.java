package com.fooddelivery.controller;

import com.fooddelivery.dto.ApiResponse;
import com.fooddelivery.entity.CartItem;
import com.fooddelivery.service.CartService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/cart")
@RequiredArgsConstructor
public class CartController {

    private final CartService cartService;

    @GetMapping
    public ApiResponse<List<CartItem>> getCart(Authentication authentication) {
        Long userId = (Long) authentication.getPrincipal();
        return ApiResponse.success(cartService.getCart(userId));
    }

    @PostMapping
    public ApiResponse<CartItem> addToCart(@RequestBody Map<String, Object> body,
                                            Authentication authentication) {
        Long userId = (Long) authentication.getPrincipal();
        Long dishId = Long.valueOf(body.get("dishId").toString());
        Integer quantity = body.containsKey("quantity")
                ? Integer.valueOf(body.get("quantity").toString()) : 1;
        try {
            return ApiResponse.success(cartService.addToCart(userId, dishId, quantity));
        } catch (RuntimeException e) {
            return ApiResponse.error(400, e.getMessage());
        }
    }

    @PutMapping("/{id}")
    public ApiResponse<CartItem> updateQuantity(@PathVariable Long id,
                                                 @RequestBody Map<String, Integer> body,
                                                 Authentication authentication) {
        Long userId = (Long) authentication.getPrincipal();
        try {
            CartItem item = cartService.updateQuantity(id, body.get("quantity"), userId);
            return ApiResponse.success(item);
        } catch (RuntimeException e) {
            return ApiResponse.error(400, e.getMessage());
        }
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> removeFromCart(@PathVariable Long id,
                                             Authentication authentication) {
        Long userId = (Long) authentication.getPrincipal();
        try {
            cartService.removeFromCart(id, userId);
            return ApiResponse.success();
        } catch (RuntimeException e) {
            return ApiResponse.error(400, e.getMessage());
        }
    }

    @DeleteMapping
    public ApiResponse<Void> clearCart(Authentication authentication) {
        Long userId = (Long) authentication.getPrincipal();
        cartService.clearCart(userId);
        return ApiResponse.success();
    }
}
