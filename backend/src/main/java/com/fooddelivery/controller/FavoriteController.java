package com.fooddelivery.controller;

import com.fooddelivery.dto.ApiResponse;
import com.fooddelivery.dto.PageResponse;
import com.fooddelivery.entity.Favorite;
import com.fooddelivery.service.FavoriteService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/favorites")
@RequiredArgsConstructor
public class FavoriteController {
    private final FavoriteService favoriteService;

    @GetMapping
    public ApiResponse<PageResponse<Favorite>> getFavorites(Authentication auth, Pageable pageable) {
        Long userId = (Long) auth.getPrincipal();
        return ApiResponse.success(PageResponse.from(favoriteService.getFavorites(userId, pageable)));
    }

    @GetMapping("/check/{storeId}")
    public ApiResponse<Boolean> checkFavorite(@PathVariable Long storeId, Authentication auth) {
        Long userId = (Long) auth.getPrincipal();
        return ApiResponse.success(favoriteService.isFavorite(userId, storeId));
    }

    @PostMapping("/{storeId}")
    public ApiResponse<Favorite> addFavorite(@PathVariable Long storeId, Authentication auth) {
        Long userId = (Long) auth.getPrincipal();
        return ApiResponse.success(favoriteService.addFavorite(userId, storeId));
    }

    @DeleteMapping("/{storeId}")
    public ApiResponse<Void> removeFavorite(@PathVariable Long storeId, Authentication auth) {
        Long userId = (Long) auth.getPrincipal();
        favoriteService.removeFavorite(userId, storeId);
        return ApiResponse.success();
    }
}
