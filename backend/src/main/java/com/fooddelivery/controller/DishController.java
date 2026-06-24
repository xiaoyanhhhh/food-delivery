package com.fooddelivery.controller;

import com.fooddelivery.dto.ApiResponse;
import com.fooddelivery.dto.DishRequest;
import com.fooddelivery.dto.PageResponse;
import com.fooddelivery.entity.Dish;
import com.fooddelivery.service.DishService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class DishController {

    private final DishService dishService;

    // 公开接口：获取所有上架菜品（分页）
    @GetMapping("/dishes")
    public ApiResponse<Object> getAllDishes(
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) Long storeId,
            @RequestParam(required = false) String keyword,
            @PageableDefault(size = 12, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {

        if (keyword != null && !keyword.isEmpty()) {
            if (pageable.isPaged()) {
                return ApiResponse.success(PageResponse.from(dishService.searchDishes(keyword, pageable)));
            }
            return ApiResponse.success(dishService.searchDishes(keyword));
        }
        if (storeId != null && categoryId != null) {
            return ApiResponse.success(PageResponse.from(
                    dishService.getDishesByStoreAndCategory(storeId, categoryId, pageable)));
        }
        if (storeId != null) {
            return ApiResponse.success(PageResponse.from(dishService.getDishesByStore(storeId, pageable)));
        }
        if (categoryId != null) {
            return ApiResponse.success(dishService.getDishesByCategory(categoryId));
        }
        return ApiResponse.success(PageResponse.from(dishService.getAllDishes(pageable)));
    }

    @GetMapping("/dishes/{id}")
    public ApiResponse<Dish> getDish(@PathVariable Long id) {
        return ApiResponse.success(dishService.getDishById(id));
    }

    // 商家接口：创建菜品
    @PostMapping("/merchant/dishes")
    public ApiResponse<Dish> createDish(@Valid @RequestBody DishRequest request,
                                         Authentication authentication) {
        Long merchantId = (Long) authentication.getPrincipal();
        return ApiResponse.success(dishService.createDish(request, merchantId));
    }

    // 商家接口：更新菜品
    @PutMapping("/merchant/dishes/{id}")
    public ApiResponse<Dish> updateDish(@PathVariable Long id,
                                         @Valid @RequestBody DishRequest request,
                                         Authentication authentication) {
        Long merchantId = (Long) authentication.getPrincipal();
        return ApiResponse.success(dishService.updateDish(id, request, merchantId));
    }

    // 商家接口：下架菜品
    @DeleteMapping("/merchant/dishes/{id}")
    public ApiResponse<Void> deleteDish(@PathVariable Long id,
                                         Authentication authentication) {
        Long merchantId = (Long) authentication.getPrincipal();
        dishService.deleteDish(id, merchantId);
        return ApiResponse.success();
    }

    // 获取菜品规格
    @GetMapping("/dishes/{id}/specs")
    public ApiResponse<List<com.fooddelivery.entity.DishSpecification>> getDishSpecs(@PathVariable Long id) {
        return ApiResponse.success(dishService.getSpecsByDishId(id));
    }

    // 商家接口：获取自己的菜品列表（分页）
    @GetMapping("/merchant/dishes")
    public ApiResponse<PageResponse<Dish>> getMerchantDishes(
            Authentication authentication,
            @PageableDefault(size = 20) Pageable pageable) {
        Long merchantId = (Long) authentication.getPrincipal();
        return ApiResponse.success(PageResponse.from(dishService.getMerchantDishes(merchantId, pageable)));
    }
}
