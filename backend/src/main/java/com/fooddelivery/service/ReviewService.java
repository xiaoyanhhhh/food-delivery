package com.fooddelivery.service;

import com.fooddelivery.dto.ReviewRequest;
import com.fooddelivery.entity.*;
import com.fooddelivery.exception.BusinessException;
import com.fooddelivery.exception.ForbiddenException;
import com.fooddelivery.exception.NotFoundException;
import com.fooddelivery.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ReviewService {

    private final ReviewRepository reviewRepository;
    private final StoreRepository storeRepository;
    private final DishRepository dishRepository;
    private final OrderRepository orderRepository;
    private final UserRepository userRepository;

    @Transactional
    public Review createReview(ReviewRequest request, Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("用户不存在"));

        Store store = storeRepository.findById(request.getStoreId())
                .orElseThrow(() -> new NotFoundException("店铺不存在"));

        // 检查是否已评价该订单
        if (request.getOrderId() != null
                && reviewRepository.existsByUserIdAndOrderId(userId, request.getOrderId())) {
            throw new BusinessException("您已评价过此订单");
        }

        Review review = Review.builder()
                .rating(request.getRating())
                .content(request.getContent())
                .images(request.getImages())
                .user(user)
                .store(store)
                .build();

        if (request.getDishId() != null) {
            Dish dish = dishRepository.findById(request.getDishId())
                    .orElseThrow(() -> new NotFoundException("菜品不存在"));
            review.setDish(dish);
        }

        if (request.getOrderId() != null) {
            Order order = orderRepository.findById(request.getOrderId())
                    .orElseThrow(() -> new NotFoundException("订单不存在"));
            review.setOrder(order);
        }

        Review saved = reviewRepository.save(review);

        // 更新店铺评分
        Double avgRating = reviewRepository.getAvgRatingByStore(store.getId());
        Long count = reviewRepository.countByStore(store.getId());
        if (avgRating != null) {
            store.setRating(java.math.BigDecimal.valueOf(Math.round(avgRating * 10) / 10.0));
            storeRepository.save(store);
        }

        return saved;
    }

    public Page<Review> getStoreReviews(Long storeId, Pageable pageable) {
        return reviewRepository.findByStoreIdOrderByCreatedAtDesc(storeId, pageable);
    }

    public Page<Review> getDishReviews(Long dishId, Pageable pageable) {
        return reviewRepository.findByDishIdOrderByCreatedAtDesc(dishId, pageable);
    }

    public Page<Review> getMyReviews(Long userId, Pageable pageable) {
        return reviewRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable);
    }

    @Transactional
    public Review replyReview(Long reviewId, String reply, Long merchantId) {
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new NotFoundException("评价不存在"));

        if (!review.getStore().getMerchant().getId().equals(merchantId)) {
            throw new ForbiddenException("无权回复此评价");
        }

        review.setMerchantReply(reply);
        return reviewRepository.save(review);
    }
}
