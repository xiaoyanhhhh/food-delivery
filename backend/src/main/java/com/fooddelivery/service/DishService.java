package com.fooddelivery.service;

import com.fooddelivery.dto.DishRequest;
import com.fooddelivery.entity.Category;
import com.fooddelivery.entity.Dish;
import com.fooddelivery.entity.User;
import com.fooddelivery.repository.CategoryRepository;
import com.fooddelivery.repository.DishRepository;
import com.fooddelivery.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class DishService {

    private final DishRepository dishRepository;
    private final CategoryRepository categoryRepository;
    private final UserRepository userRepository;

    public List<Dish> getAllDishes() {
        return dishRepository.findByStatusTrue();
    }

    public List<Dish> getDishesByCategory(Long categoryId) {
        return dishRepository.findByCategoryIdAndStatusTrue(categoryId);
    }

    public Dish getDishById(Long id) {
        return dishRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("菜品不存在"));
    }

    public List<Dish> searchDishes(String keyword) {
        return dishRepository.searchByName(keyword);
    }

    public Dish createDish(DishRequest request, Long merchantId) {
        User merchant = userRepository.findById(merchantId)
                .orElseThrow(() -> new RuntimeException("商家不存在"));

        Category category = categoryRepository.findById(request.getCategoryId())
                .orElseThrow(() -> new RuntimeException("分类不存在"));

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
                .orElseThrow(() -> new RuntimeException("菜品不存在"));

        if (!dish.getMerchant().getId().equals(merchantId)) {
            throw new RuntimeException("无权修改此菜品");
        }

        Category category = categoryRepository.findById(request.getCategoryId())
                .orElseThrow(() -> new RuntimeException("分类不存在"));

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
                .orElseThrow(() -> new RuntimeException("菜品不存在"));

        if (!dish.getMerchant().getId().equals(merchantId)) {
            throw new RuntimeException("无权删除此菜品");
        }

        dish.setStatus(false);
        dishRepository.save(dish);
    }

    public List<Dish> getMerchantDishes(Long merchantId) {
        return dishRepository.findByMerchantIdAndStatusTrue(merchantId);
    }
}
