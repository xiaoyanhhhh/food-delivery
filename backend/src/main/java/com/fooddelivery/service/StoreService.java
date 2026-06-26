package com.fooddelivery.service;

import com.fooddelivery.dto.StoreRequest;
import com.fooddelivery.entity.Store;
import com.fooddelivery.entity.StoreCategory;
import com.fooddelivery.entity.User;
import com.fooddelivery.exception.BusinessException;
import com.fooddelivery.exception.ForbiddenException;
import com.fooddelivery.exception.NotFoundException;
import com.fooddelivery.repository.StoreCategoryRepository;
import com.fooddelivery.repository.StoreRepository;
import com.fooddelivery.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class StoreService {

    private final StoreRepository storeRepository;
    private final StoreCategoryRepository storeCategoryRepository;
    private final UserRepository userRepository;
    private final com.fooddelivery.repository.OrderRepository orderRepository;

    public Page<Store> getStores(Long categoryId, String keyword, Pageable pageable) {
        if (keyword != null && !keyword.isEmpty() && categoryId != null) {
            return storeRepository.searchByCategoryAndName(categoryId, keyword, pageable).map(this::withActualMonthlySales);
        }
        if (keyword != null && !keyword.isEmpty()) {
            return storeRepository.searchByName(keyword, pageable).map(this::withActualMonthlySales);
        }
        if (categoryId != null) {
            return storeRepository.findByStatusTrueAndCategoryId(categoryId, pageable).map(this::withActualMonthlySales);
        }
        return storeRepository.findByStatusTrue(pageable).map(this::withActualMonthlySales);
    }

    public Store getStoreById(Long id) {
        Store store = storeRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("店铺不存在"));
        return withActualMonthlySales(store);
    }

    public Store getStoreByMerchantId(Long merchantId) {
        Store store = storeRepository.findByMerchantId(merchantId)
                .orElseThrow(() -> new NotFoundException("您还没有创建店铺"));
        return withActualMonthlySales(store);
    }

    public Store createStore(StoreRequest request, Long merchantId) {
        if (storeRepository.findByMerchantId(merchantId).isPresent()) {
            throw new BusinessException("您已经创建过店铺");
        }

        User merchant = userRepository.findById(merchantId)
                .orElseThrow(() -> new NotFoundException("商家不存在"));

        StoreCategory category = null;
        if (request.getCategoryId() != null) {
            category = storeCategoryRepository.findById(request.getCategoryId())
                    .orElseThrow(() -> new NotFoundException("店铺分类不存在"));
        }

        Store store = Store.builder()
                .name(request.getName())
                .logo(request.getLogo())
                .description(request.getDescription())
                .minOrderAmount(request.getMinOrderAmount() != null ? request.getMinOrderAmount() : java.math.BigDecimal.valueOf(20))
                .businessHours(request.getBusinessHours() != null ? request.getBusinessHours() : "09:00-22:00")
                .announcement(request.getAnnouncement())
                .address(request.getAddress())
                .phone(request.getPhone())
                .lat(request.getLat())
                .lng(request.getLng())
                .category(category)
                .merchant(merchant)
                .build();

        return storeRepository.save(store);
    }

    public Store updateStore(Long storeId, StoreRequest request, Long merchantId) {
        Store store = storeRepository.findById(storeId)
                .orElseThrow(() -> new NotFoundException("店铺不存在"));

        if (!store.getMerchant().getId().equals(merchantId)) {
            throw new ForbiddenException("无权操作此店铺");
        }

        if (request.getName() != null) store.setName(request.getName());
        if (request.getLogo() != null) store.setLogo(request.getLogo());
        if (request.getDescription() != null) store.setDescription(request.getDescription());
        if (request.getMinOrderAmount() != null) store.setMinOrderAmount(request.getMinOrderAmount());
        if (request.getBusinessHours() != null) store.setBusinessHours(request.getBusinessHours());
        if (request.getAnnouncement() != null) store.setAnnouncement(request.getAnnouncement());
        if (request.getAddress() != null) store.setAddress(request.getAddress());
        if (request.getPhone() != null) store.setPhone(request.getPhone());
        if (request.getLat() != null) store.setLat(request.getLat());
        if (request.getLng() != null) store.setLng(request.getLng());
        if (request.getStatus() != null) store.setStatus(request.getStatus());
        if (request.getCategoryId() != null) {
            StoreCategory category = storeCategoryRepository.findById(request.getCategoryId())
                    .orElseThrow(() -> new NotFoundException("店铺分类不存在"));
            store.setCategory(category);
        }

        return storeRepository.save(store);
    }

    public List<StoreCategory> getAllStoreCategories() {
        return storeCategoryRepository.findAllByOrderBySortOrderAsc();
    }

    public StoreCategory createStoreCategory(String name, String icon, Integer sortOrder) {
        StoreCategory category = StoreCategory.builder()
                .name(name)
                .icon(icon)
                .sortOrder(sortOrder != null ? sortOrder : 0)
                .build();
        return storeCategoryRepository.save(category);
    }

    private Store withActualMonthlySales(Store store) {
        if (store.getMerchant() == null || store.getMerchant().getId() == null) {
            store.setMonthlySales(0);
            return store;
        }
        Long sales = orderRepository.sumCompletedQuantityByMerchantSince(
                store.getMerchant().getId(),
                com.fooddelivery.entity.Order.OrderStatus.COMPLETED,
                LocalDateTime.now().minusDays(30)
        );
        store.setMonthlySales(sales != null ? sales.intValue() : 0);
        return store;
    }
}
