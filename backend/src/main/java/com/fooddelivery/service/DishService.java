package com.fooddelivery.service;

import com.fooddelivery.dto.DishRequest;
import com.fooddelivery.entity.Category;
import com.fooddelivery.entity.Dish;
import com.fooddelivery.entity.Store;
import com.fooddelivery.entity.User;
import com.fooddelivery.exception.BusinessException;
import com.fooddelivery.exception.ForbiddenException;
import com.fooddelivery.exception.NotFoundException;
import com.fooddelivery.repository.CategoryRepository;
import com.fooddelivery.repository.DishRepository;
import com.fooddelivery.repository.StoreRepository;
import com.fooddelivery.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class DishService {

    private final DishRepository dishRepository;
    private final CategoryRepository categoryRepository;
    private final UserRepository userRepository;
    private final StoreRepository storeRepository;
    private final com.fooddelivery.repository.DishSpecificationRepository specRepository;

    public Page<Dish> getAllDishes(Pageable pageable) {
        return dishRepository.findByStatusTrue(pageable);
    }

    public List<Dish> getAllDishes() {
        return dishRepository.findByStatusTrue();
    }

    public List<Dish> getDishesByCategory(Long categoryId) {
        return dishRepository.findByCategoryIdAndStatusTrue(categoryId);
    }

    /**
     * 通过店铺ID获取菜品 —— 先查店铺，取其商家用户ID再查菜品
     */
    public Page<Dish> getDishesByStore(Long storeId, Pageable pageable) {
        Store store = storeRepository.findById(storeId)
                .orElseThrow(() -> new NotFoundException("店铺不存在"));
        return dishRepository.findByMerchantIdAndStatusTrue(store.getMerchant().getId(), pageable);
    }

    public Page<Dish> getDishesByStoreAndCategory(Long storeId, Long categoryId, Pageable pageable) {
        Store store = storeRepository.findById(storeId)
                .orElseThrow(() -> new NotFoundException("店铺不存在"));
        return dishRepository.findByMerchantIdAndCategoryIdAndStatusTrue(
                store.getMerchant().getId(), categoryId, pageable);
    }

    public Dish getDishById(Long id) {
        return dishRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("菜品不存在"));
    }

    public Page<Dish> searchDishes(String keyword, Pageable pageable) {
        return dishRepository.searchByName(keyword, pageable);
    }

    public List<Dish> searchDishes(String keyword) {
        return dishRepository.searchByName(keyword);
    }

    public Dish createDish(DishRequest request, Long merchantId) {
        User merchant = userRepository.findById(merchantId)
                .orElseThrow(() -> new NotFoundException("商家不存在"));

        Category category = categoryRepository.findById(request.getCategoryId())
                .orElseThrow(() -> new NotFoundException("分类不存在"));

        Dish dish = Dish.builder()
                .name(request.getName())
                .description(request.getDescription())
                .price(request.getPrice())
                .image(request.getImage())
                .category(category)
                .merchant(merchant)
                .status(request.getStatus() != null ? request.getStatus() : true)
                .build();

        return dishRepository.save(dish);
    }

    public Dish updateDish(Long id, DishRequest request, Long merchantId) {
        Dish dish = dishRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("菜品不存在"));

        if (!dish.getMerchant().getId().equals(merchantId)) {
            throw new ForbiddenException("无权修改此菜品");
        }

        Category category = categoryRepository.findById(request.getCategoryId())
                .orElseThrow(() -> new NotFoundException("分类不存在"));

        dish.setName(request.getName());
        dish.setDescription(request.getDescription());
        dish.setPrice(request.getPrice());
        dish.setImage(request.getImage());
        dish.setCategory(category);
        if (request.getStatus() != null) {
            dish.setStatus(request.getStatus());
        }

        return dishRepository.save(dish);
    }

    public void deleteDish(Long id, Long merchantId) {
        Dish dish = dishRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("菜品不存在"));

        if (!dish.getMerchant().getId().equals(merchantId)) {
            throw new ForbiddenException("无权删除此菜品");
        }

        dish.setStatus(false);
        dishRepository.save(dish);
    }

    public Page<Dish> getMerchantDishes(Long merchantId, Pageable pageable) {
        return dishRepository.findByMerchantIdAndStatusTrue(merchantId, pageable);
    }

    public List<Dish> getMerchantDishes(Long merchantId) {
        return dishRepository.findByMerchantIdAndStatusTrue(merchantId);
    }

    public List<com.fooddelivery.entity.DishSpecification> getSpecsByDishId(Long dishId) {
        return specRepository.findByDishId(dishId);
    }
}
