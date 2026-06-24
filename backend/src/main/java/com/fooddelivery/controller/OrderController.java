package com.fooddelivery.controller;

import com.fooddelivery.dto.ApiResponse;
import com.fooddelivery.dto.OrderRequest;
import com.fooddelivery.dto.PageResponse;
import com.fooddelivery.dto.PaymentRequest;
import com.fooddelivery.entity.Order;
import com.fooddelivery.service.DeliveryService;
import com.fooddelivery.service.OrderService;
import com.fooddelivery.service.PaymentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;
    private final PaymentService paymentService;
    private final DeliveryService deliveryService;

    // 顾客：创建订单
    @PostMapping
    public ApiResponse<Order> createOrder(@Valid @RequestBody OrderRequest request,
                                           Authentication authentication) {
        Long customerId = (Long) authentication.getPrincipal();
        return ApiResponse.success(orderService.createOrder(customerId, request));
    }

    // 顾客：预估配送费
    @GetMapping("/delivery-estimate")
    public ApiResponse<Map<String, Object>> estimateDelivery(
            @RequestParam(required = false, defaultValue = "1") Long storeId) {
        DeliveryService.DeliveryInfo info = deliveryService.calculate(storeId, "");
        return ApiResponse.success(Map.of(
                "distance", info.getDistance(),
                "deliveryFee", info.getDeliveryFee(),
                "estimatedMinutes", info.getEstimatedMinutes()
        ));
    }

    // 顾客：支付订单
    @PostMapping("/{id}/pay")
    public ApiResponse<PaymentService.PaymentResult> payOrder(
            @PathVariable Long id,
            @RequestBody(required = false) Map<String, String> body,
            Authentication authentication) {
        Long userId = (Long) authentication.getPrincipal();
        String method = body != null ? body.getOrDefault("paymentMethod", "WECHAT") : "WECHAT";
        return ApiResponse.success(paymentService.pay(id, userId, method));
    }

    // 顾客：查看自己的订单（分页）
    @GetMapping("/customer")
    public ApiResponse<PageResponse<Order>> getCustomerOrders(
            Authentication authentication,
            @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        Long customerId = (Long) authentication.getPrincipal();
        return ApiResponse.success(PageResponse.from(orderService.getCustomerOrders(customerId, pageable)));
    }

    // 商家：查看自己的订单（分页）
    @GetMapping("/merchant")
    public ApiResponse<PageResponse<Order>> getMerchantOrders(
            Authentication authentication,
            @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        Long merchantId = (Long) authentication.getPrincipal();
        return ApiResponse.success(PageResponse.from(orderService.getMerchantOrders(merchantId, pageable)));
    }

    // 骑手：查看自己的订单（分页）
    @GetMapping("/rider")
    public ApiResponse<PageResponse<Order>> getRiderOrders(
            Authentication authentication,
            @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        Long riderId = (Long) authentication.getPrincipal();
        return ApiResponse.success(PageResponse.from(orderService.getRiderOrders(riderId, pageable)));
    }

    // 骑手：查看可接订单（分页）
    @GetMapping("/available")
    public ApiResponse<PageResponse<Order>> getAvailableOrders(
            @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return ApiResponse.success(PageResponse.from(orderService.getAvailableOrders(pageable)));
    }

    // 获取单个订单详情
    @GetMapping("/{id}")
    public ApiResponse<Order> getOrder(@PathVariable Long id, Authentication authentication) {
        Long userId = (Long) authentication.getPrincipal();
        return ApiResponse.success(orderService.getOrderById(id, userId));
    }

    // 更新订单状态
    @PutMapping("/{id}/status")
    public ApiResponse<Order> updateOrderStatus(@PathVariable Long id,
                                                 @RequestBody Map<String, String> body,
                                                 Authentication authentication) {
        Long userId = (Long) authentication.getPrincipal();
        String role = body.get("role");
        Order.OrderStatus newStatus = Order.OrderStatus.valueOf(body.get("status"));
        return ApiResponse.success(orderService.updateOrderStatus(id, newStatus, userId, role));
    }

    // 骑手：接单
    @PostMapping("/{id}/accept")
    public ApiResponse<Order> acceptOrder(@PathVariable Long id,
                                           Authentication authentication) {
        Long riderId = (Long) authentication.getPrincipal();
        return ApiResponse.success(orderService.acceptOrderByRider(id, riderId));
    }

    // 顾客：再来一单
    @PostMapping("/{id}/reorder")
    public ApiResponse<Order> reorder(@PathVariable Long id, Authentication authentication) {
        Long customerId = (Long) authentication.getPrincipal();
        return ApiResponse.success(orderService.reorder(id, customerId));
    }
}
