package com.fooddelivery.controller;

import com.fooddelivery.dto.ApiResponse;
import com.fooddelivery.dto.PageResponse;
import com.fooddelivery.dto.StoreRequest;
import com.fooddelivery.entity.Store;
import com.fooddelivery.entity.StoreCategory;
import com.fooddelivery.service.StoreService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class StoreController {

    private final StoreService storeService;

    // 公开接口：店铺列表（分页、搜索、分类筛选、排序）
    @GetMapping("/stores")
    public ApiResponse<PageResponse<Store>> getStores(
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) String keyword,
            @PageableDefault(size = 12, sort = "rating", direction = Sort.Direction.DESC) Pageable pageable) {
        return ApiResponse.success(PageResponse.from(storeService.getStores(categoryId, keyword, pageable)));
    }

    // 公开接口：店铺详情
    @GetMapping("/stores/{id}")
    public ApiResponse<Store> getStore(@PathVariable Long id) {
        return ApiResponse.success(storeService.getStoreById(id));
    }

    // 公开接口：店铺分类列表
    @GetMapping("/store-categories")
    public ApiResponse<List<StoreCategory>> getStoreCategories() {
        return ApiResponse.success(storeService.getAllStoreCategories());
    }

    // 商家接口：获取自己的店铺
    @GetMapping("/merchant/store")
    public ApiResponse<Store> getMyStore(Authentication authentication) {
        Long merchantId = (Long) authentication.getPrincipal();
        return ApiResponse.success(storeService.getStoreByMerchantId(merchantId));
    }

    // 商家接口：创建店铺
    @PostMapping("/merchant/store")
    public ApiResponse<Store> createStore(@Valid @RequestBody StoreRequest request,
                                           Authentication authentication) {
        Long merchantId = (Long) authentication.getPrincipal();
        return ApiResponse.success(storeService.createStore(request, merchantId));
    }

    // 商家接口：更新店铺
    @PutMapping("/merchant/store/{id}")
    public ApiResponse<Store> updateStore(@PathVariable Long id,
                                           @Valid @RequestBody StoreRequest request,
                                           Authentication authentication) {
        Long merchantId = (Long) authentication.getPrincipal();
        return ApiResponse.success(storeService.updateStore(id, request, merchantId));
    }

    // 管理员接口：创建店铺分类
    @PostMapping("/store-categories")
    public ApiResponse<StoreCategory> createStoreCategory(@RequestBody Map<String, Object> body) {
        String name = (String) body.get("name");
        String icon = (String) body.get("icon");
        Integer sortOrder = body.get("sortOrder") != null
                ? Integer.valueOf(body.get("sortOrder").toString()) : 0;
        return ApiResponse.success(storeService.createStoreCategory(name, icon, sortOrder));
    }
}
