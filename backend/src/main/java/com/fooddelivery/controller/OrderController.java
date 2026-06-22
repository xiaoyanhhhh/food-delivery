package com.fooddelivery.controller;

import com.fooddelivery.dto.ApiResponse;
import com.fooddelivery.dto.OrderRequest;
import com.fooddelivery.entity.Order;
import com.fooddelivery.service.OrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    // 顾客：创建订单
    @PostMapping
    public ApiResponse<Order> createOrder(@Valid @RequestBody OrderRequest request,
                                           Authentication authentication) {
        Long customerId = (Long) authentication.getPrincipal();
        try {
            return ApiResponse.success(orderService.createOrder(customerId, request));
        } catch (RuntimeException e) {
            return ApiResponse.error(400, e.getMessage());
        }
    }

    // 顾客：查看自己的订单
    @GetMapping("/customer")
    public ApiResponse<List<Order>> getCustomerOrders(Authentication authentication) {
        Long customerId = (Long) authentication.getPrincipal();
        return ApiResponse.success(orderService.getCustomerOrders(customerId));
    }

    // 商家：查看自己的订单
    @GetMapping("/merchant")
    public ApiResponse<List<Order>> getMerchantOrders(Authentication authentication) {
        Long merchantId = (Long) authentication.getPrincipal();
        return ApiResponse.success(orderService.getMerchantOrders(merchantId));
    }

    // 骑手：查看自己的订单
    @GetMapping("/rider")
    public ApiResponse<List<Order>> getRiderOrders(Authentication authentication) {
        Long riderId = (Long) authentication.getPrincipal();
        return ApiResponse.success(orderService.getRiderOrders(riderId));
    }

    // 骑手：查看可接订单
    @GetMapping("/available")
    public ApiResponse<List<Order>> getAvailableOrders() {
        return ApiResponse.success(orderService.getAvailableOrders());
    }

    // 更新订单状态
    @PutMapping("/{id}/status")
    public ApiResponse<Order> updateOrderStatus(@PathVariable Long id,
                                                 @RequestBody Map<String, String> body,
                                                 Authentication authentication) {
        Long userId = (Long) authentication.getPrincipal();
        String role = body.get("role");
        Order.OrderStatus newStatus = Order.OrderStatus.valueOf(body.get("status"));
        try {
            return ApiResponse.success(orderService.updateOrderStatus(id, newStatus, userId, role));
        } catch (RuntimeException e) {
            return ApiResponse.error(400, e.getMessage());
        }
    }

    // 骑手：接单
    @PostMapping("/{id}/accept")
    public ApiResponse<Order> acceptOrder(@PathVariable Long id,
                                           Authentication authentication) {
        Long riderId = (Long) authentication.getPrincipal();
        try {
            return ApiResponse.success(orderService.acceptOrderByRider(id, riderId));
        } catch (RuntimeException e) {
            return ApiResponse.error(400, e.getMessage());
        }
    }
}
