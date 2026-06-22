package com.fooddelivery.controller;

import com.fooddelivery.dto.ApiResponse;
import com.fooddelivery.dto.DishRequest;
import com.fooddelivery.entity.Dish;
import com.fooddelivery.service.DishService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class DishController {

    private final DishService dishService;

    // 公开接口：获取所有上架菜品
    @GetMapping("/dishes")
    public ApiResponse<List<Dish>> getAllDishes(
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) String keyword) {
        List<Dish> dishes;
        if (keyword != null && !keyword.isEmpty()) {
            dishes = dishService.searchDishes(keyword);
        } else if (categoryId != null) {
            dishes = dishService.getDishesByCategory(categoryId);
        } else {
            dishes = dishService.getAllDishes();
        }
        return ApiResponse.success(dishes);
    }

    @GetMapping("/dishes/{id}")
    public ApiResponse<Dish> getDish(@PathVariable Long id) {
        try {
            return ApiResponse.success(dishService.getDishById(id));
        } catch (RuntimeException e) {
            return ApiResponse.error(404, e.getMessage());
        }
    }

    // 商家接口：创建菜品
    @PostMapping("/merchant/dishes")
    public ApiResponse<Dish> createDish(@Valid @RequestBody DishRequest request,
                                         Authentication authentication) {
        try {
            Long merchantId = (Long) authentication.getPrincipal();
            return ApiResponse.success(dishService.createDish(request, merchantId));
        } catch (RuntimeException e) {
            return ApiResponse.error(400, e.getMessage());
        }
    }

    // 商家接口：更新菜品
    @PutMapping("/merchant/dishes/{id}")
    public ApiResponse<Dish> updateDish(@PathVariable Long id,
                                         @Valid @RequestBody DishRequest request,
                                         Authentication authentication) {
        try {
            Long merchantId = (Long) authentication.getPrincipal();
            return ApiResponse.success(dishService.updateDish(id, request, merchantId));
        } catch (RuntimeException e) {
            return ApiResponse.error(400, e.getMessage());
        }
    }

    // 商家接口：下架菜品
    @DeleteMapping("/merchant/dishes/{id}")
    public ApiResponse<Void> deleteDish(@PathVariable Long id,
                                         Authentication authentication) {
        try {
            Long merchantId = (Long) authentication.getPrincipal();
            dishService.deleteDish(id, merchantId);
            return ApiResponse.success();
        } catch (RuntimeException e) {
            return ApiResponse.error(400, e.getMessage());
        }
    }

    // 商家接口：获取自己的菜品列表
    @GetMapping("/merchant/dishes")
    public ApiResponse<List<Dish>> getMerchantDishes(Authentication authentication) {
        Long merchantId = (Long) authentication.getPrincipal();
        return ApiResponse.success(dishService.getMerchantDishes(merchantId));
    }
}
