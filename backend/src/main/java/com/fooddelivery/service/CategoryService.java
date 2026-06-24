package com.fooddelivery.service;

import com.fooddelivery.entity.Category;
import com.fooddelivery.exception.BusinessException;
import com.fooddelivery.repository.CategoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CategoryService {

    private final CategoryRepository categoryRepository;

    public List<Category> getAllCategories() {
        return categoryRepository.findAllByOrderBySortOrderAsc();
    }

    public Category createCategory(String name, Integer sortOrder) {
        Category category = Category.builder()
                .name(name)
                .sortOrder(sortOrder != null ? sortOrder : 0)
                .build();
        return categoryRepository.save(category);
    }
}
