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
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class StoreController {

    private final StoreService storeService;

    @GetMapping("/stores")
    public ApiResponse<PageResponse<Store>> getStores(
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) String keyword,
            @PageableDefault(size = 12, sort = "rating", direction = Sort.Direction.DESC) Pageable pageable) {
        return ApiResponse.success(PageResponse.from(storeService.getStores(categoryId, keyword, pageable)));
    }

    @GetMapping("/stores/{id}")
    public ApiResponse<Store> getStore(@PathVariable Long id) {
        return ApiResponse.success(storeService.getStoreById(id));
    }

    @GetMapping("/store-categories")
    public ApiResponse<List<StoreCategory>> getStoreCategories() {
        return ApiResponse.success(storeService.getAllStoreCategories());
    }

    @GetMapping("/merchant/store")
    public ApiResponse<Store> getMyStore(Authentication authentication) {
        Long merchantId = (Long) authentication.getPrincipal();
        return ApiResponse.success(storeService.getStoreByMerchantId(merchantId));
    }

    @PostMapping("/merchant/store")
    public ApiResponse<Store> createStore(@Valid @RequestBody StoreRequest request,
                                           Authentication authentication) {
        Long merchantId = (Long) authentication.getPrincipal();
        return ApiResponse.success(storeService.createStore(request, merchantId));
    }

    @PutMapping("/merchant/store/{id}")
    public ApiResponse<Store> updateStore(@PathVariable Long id,
                                           @Valid @RequestBody StoreRequest request,
                                           Authentication authentication) {
        Long merchantId = (Long) authentication.getPrincipal();
        return ApiResponse.success(storeService.updateStore(id, request, merchantId));
    }

    @PostMapping("/store-categories")
    public ApiResponse<StoreCategory> createStoreCategory(@RequestBody Map<String, Object> body) {
        String name = (String) body.get("name");
        String icon = (String) body.get("icon");
        Integer sortOrder = body.get("sortOrder") != null
                ? Integer.valueOf(body.get("sortOrder").toString()) : 0;
        return ApiResponse.success(storeService.createStoreCategory(name, icon, sortOrder));
    }
}
