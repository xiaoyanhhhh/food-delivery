package com.fooddelivery.controller;

import com.fooddelivery.dto.ApiResponse;
import com.fooddelivery.dto.OrderRequest;
import com.fooddelivery.dto.PageResponse;
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
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;
    private final PaymentService paymentService;
    private final DeliveryService deliveryService;

    @PostMapping
    public ApiResponse<Order> createOrder(@Valid @RequestBody OrderRequest request,
                                           Authentication authentication) {
        Long customerId = (Long) authentication.getPrincipal();
        return ApiResponse.success(orderService.createOrder(customerId, request));
    }

    @GetMapping("/delivery-estimate")
    public ApiResponse<Map<String, Object>> estimateDelivery(
            @RequestParam(required = false, defaultValue = "1") Long storeId,
            @RequestParam(required = false, defaultValue = "") String address) {
        DeliveryService.DeliveryInfo info = deliveryService.calculate(storeId, address);
        return ApiResponse.success(Map.of(
                "distance", info.getDistance(),
                "deliveryFee", info.getDeliveryFee(),
                "estimatedMinutes", info.getEstimatedMinutes()
        ));
    }

    @PostMapping("/{id}/pay")
    public ApiResponse<PaymentService.PaymentResult> payOrder(
            @PathVariable Long id,
            @RequestBody(required = false) Map<String, String> body,
            Authentication authentication) {
        Long userId = (Long) authentication.getPrincipal();
        String method = body != null ? body.getOrDefault("paymentMethod", "WECHAT") : "WECHAT";
        return ApiResponse.success(paymentService.pay(id, userId, method));
    }

    @GetMapping("/customer")
    public ApiResponse<PageResponse<Order>> getCustomerOrders(
            Authentication authentication,
            @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        Long customerId = (Long) authentication.getPrincipal();
        return ApiResponse.success(PageResponse.from(orderService.getCustomerOrders(customerId, pageable)));
    }

    @GetMapping("/merchant")
    public ApiResponse<PageResponse<Order>> getMerchantOrders(
            Authentication authentication,
            @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        Long merchantId = (Long) authentication.getPrincipal();
        return ApiResponse.success(PageResponse.from(orderService.getMerchantOrders(merchantId, pageable)));
    }

    @GetMapping("/rider")
    public ApiResponse<PageResponse<Order>> getRiderOrders(
            Authentication authentication,
            @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        Long riderId = (Long) authentication.getPrincipal();
        return ApiResponse.success(PageResponse.from(orderService.getRiderOrders(riderId, pageable)));
    }

    @GetMapping("/available")
    public ApiResponse<PageResponse<Order>> getAvailableOrders(
            @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return ApiResponse.success(PageResponse.from(orderService.getAvailableOrders(pageable)));
    }

    @GetMapping("/{id}")
    public ApiResponse<Order> getOrder(@PathVariable Long id, Authentication authentication) {
        Long userId = (Long) authentication.getPrincipal();
        return ApiResponse.success(orderService.getOrderById(id, userId));
    }

    @PutMapping("/{id}/status")
    public ApiResponse<Order> updateOrderStatus(@PathVariable Long id,
                                                 @RequestBody Map<String, String> body,
                                                 Authentication authentication) {
        Long userId = (Long) authentication.getPrincipal();
        String role = body.get("role");
        Order.OrderStatus newStatus = Order.OrderStatus.valueOf(body.get("status"));
        return ApiResponse.success(orderService.updateOrderStatus(id, newStatus, userId, role));
    }

    @PostMapping("/{id}/accept")
    public ApiResponse<Order> acceptOrder(@PathVariable Long id,
                                           Authentication authentication) {
        Long riderId = (Long) authentication.getPrincipal();
        return ApiResponse.success(orderService.acceptOrderByRider(id, riderId));
    }

    @PostMapping("/{id}/no-rider")
    public ApiResponse<Order> requestNoRiderDispatch(@PathVariable Long id,
                                                      Authentication authentication) {
        Long merchantId = (Long) authentication.getPrincipal();
        return ApiResponse.success(orderService.requestNoRiderDispatch(id, merchantId));
    }

    @PostMapping("/{id}/dispatch/accept")
    public ApiResponse<Order> acceptDispatchOffer(@PathVariable Long id,
                                                  Authentication authentication) {
        Long riderId = (Long) authentication.getPrincipal();
        return ApiResponse.success(orderService.acceptDispatchOffer(id, riderId));
    }

    @PostMapping("/{id}/dispatch/reject")
    public ApiResponse<Order> rejectDispatchOffer(@PathVariable Long id,
                                                  Authentication authentication) {
        Long riderId = (Long) authentication.getPrincipal();
        return ApiResponse.success(orderService.rejectDispatchOffer(id, riderId));
    }

    @PostMapping("/{id}/reorder")
    public ApiResponse<Order> reorder(@PathVariable Long id, Authentication authentication) {
        Long customerId = (Long) authentication.getPrincipal();
        return ApiResponse.success(orderService.reorder(id, customerId));
    }
}
