package com.fooddelivery.controller;

import com.fooddelivery.dto.ApiResponse;
import com.fooddelivery.dto.PageResponse;
import com.fooddelivery.dto.ReviewRequest;
import com.fooddelivery.entity.Review;
import com.fooddelivery.service.ReviewService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class ReviewController {

    private final ReviewService reviewService;

    // 公开：店铺评价列表
    @GetMapping("/reviews/store/{storeId}")
    public ApiResponse<PageResponse<Review>> getStoreReviews(
            @PathVariable Long storeId,
            @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return ApiResponse.success(PageResponse.from(reviewService.getStoreReviews(storeId, pageable)));
    }

    // 公开：菜品评价列表
    @GetMapping("/reviews/dish/{dishId}")
    public ApiResponse<PageResponse<Review>> getDishReviews(
            @PathVariable Long dishId,
            @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return ApiResponse.success(PageResponse.from(reviewService.getDishReviews(dishId, pageable)));
    }

    // 我的评价
    @GetMapping("/reviews/my")
    public ApiResponse<PageResponse<Review>> getMyReviews(
            Authentication authentication,
            @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        Long userId = (Long) authentication.getPrincipal();
        return ApiResponse.success(PageResponse.from(reviewService.getMyReviews(userId, pageable)));
    }

    // 发表评价
    @PostMapping("/reviews")
    public ApiResponse<Review> createReview(@Valid @RequestBody ReviewRequest request,
                                             Authentication authentication) {
        Long userId = (Long) authentication.getPrincipal();
        return ApiResponse.success(reviewService.createReview(request, userId));
    }

    // 商家回复评价
    @PutMapping("/reviews/{id}/reply")
    public ApiResponse<Review> replyReview(@PathVariable Long id,
                                            @RequestBody Map<String, String> body,
                                            Authentication authentication) {
        Long merchantId = (Long) authentication.getPrincipal();
        return ApiResponse.success(reviewService.replyReview(id, body.get("reply"), merchantId));
    }
}
