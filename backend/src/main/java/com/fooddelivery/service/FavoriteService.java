package com.fooddelivery.service;

import com.fooddelivery.entity.Favorite;
import com.fooddelivery.entity.Store;
import com.fooddelivery.entity.User;
import com.fooddelivery.exception.BusinessException;
import com.fooddelivery.exception.NotFoundException;
import com.fooddelivery.repository.FavoriteRepository;
import com.fooddelivery.repository.StoreRepository;
import com.fooddelivery.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class FavoriteService {
    private final FavoriteRepository favoriteRepository;
    private final UserRepository userRepository;
    private final StoreRepository storeRepository;

    @Transactional
    public Favorite addFavorite(Long userId, Long storeId) {
        User user = userRepository.findById(userId).orElseThrow(() -> new NotFoundException("用户不存在"));
        Store store = storeRepository.findById(storeId).orElseThrow(() -> new NotFoundException("店铺不存在"));
        if (favoriteRepository.existsByUserIdAndStoreId(userId, storeId)) {
            throw new BusinessException("已收藏该店铺");
        }
        return favoriteRepository.save(Favorite.builder().user(user).store(store).build());
    }

    @Transactional
    public void removeFavorite(Long userId, Long storeId) {
        if (!favoriteRepository.existsByUserIdAndStoreId(userId, storeId)) {
            throw new NotFoundException("未收藏该店铺");
        }
        favoriteRepository.deleteByUserIdAndStoreId(userId, storeId);
    }

    public Page<Favorite> getFavorites(Long userId, Pageable pageable) {
        return favoriteRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable);
    }

    public List<Long> getFavoriteStoreIds(Long userId) {
        return favoriteRepository.findByUserId(userId).stream()
                .map(f -> f.getStore().getId()).collect(Collectors.toList());
    }

    public boolean isFavorite(Long userId, Long storeId) {
        return favoriteRepository.existsByUserIdAndStoreId(userId, storeId);
    }
}
