package com.fooddelivery.service;

import com.fooddelivery.dto.OrderRequest;
import com.fooddelivery.entity.*;
import com.fooddelivery.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final DishRepository dishRepository;
    private final CartItemRepository cartItemRepository;
    private final UserRepository userRepository;

    @Transactional
    public Order createOrder(Long customerId, OrderRequest request) {
        User customer = userRepository.findById(customerId)
                .orElseThrow(() -> new RuntimeException("用户不存在"));

        BigDecimal totalPrice = BigDecimal.ZERO;
        Long merchantId = null;

        // 验证每项菜品并计算总价
        for (OrderRequest.OrderItemRequest item : request.getItems()) {
            Dish dish = dishRepository.findById(item.getDishId())
                    .orElseThrow(() -> new RuntimeException("菜品 " + item.getDishId() + " 不存在"));

            if (!dish.getStatus()) {
                throw new RuntimeException("菜品 " + dish.getName() + " 已下架");
            }

            // 确保同一订单的菜品来自同一商家
            if (merchantId == null) {
                merchantId = dish.getMerchant().getId();
            } else if (!merchantId.equals(dish.getMerchant().getId())) {
                throw new RuntimeException("不同商家的菜品请分开下单");
            }

            totalPrice = totalPrice.add(dish.getPrice().multiply(BigDecimal.valueOf(item.getQuantity())));
        }

        User merchant = userRepository.findById(merchantId)
                .orElseThrow(() -> new RuntimeException("商家不存在"));

        // 生成订单号
        String orderNo = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"))
                + UUID.randomUUID().toString().substring(0, 6).toUpperCase();

        Order order = Order.builder()
                .orderNo(orderNo)
                .customer(customer)
                .merchant(merchant)
                .status(Order.OrderStatus.PENDING_PAYMENT)
                .totalPrice(totalPrice)
                .deliveryAddress(request.getDeliveryAddress())
                .note(request.getNote())
                .build();

        // 创建订单项
        List<OrderItem> items = request.getItems().stream().map(item -> {
            Dish dish = dishRepository.findById(item.getDishId()).get();
            return OrderItem.builder()
                    .order(order)
                    .dishId(dish.getId())
                    .dishName(dish.getName())
                    .dishPrice(dish.getPrice())
                    .quantity(item.getQuantity())
                    .build();
        }).collect(Collectors.toList());

        order.setItems(items);
        Order savedOrder = orderRepository.save(order);

        // 清空购物车
        cartItemRepository.deleteByUser(customer);

        return savedOrder;
    }

    public List<Order> getCustomerOrders(Long customerId) {
        User customer = userRepository.findById(customerId)
                .orElseThrow(() -> new RuntimeException("用户不存在"));
        return orderRepository.findByCustomerOrderByCreatedAtDesc(customer);
    }

    public List<Order> getMerchantOrders(Long merchantId) {
        User merchant = userRepository.findById(merchantId)
                .orElseThrow(() -> new RuntimeException("用户不存在"));
        return orderRepository.findByMerchantOrderByCreatedAtDesc(merchant);
    }

    public List<Order> getRiderOrders(Long riderId) {
        User rider = userRepository.findById(riderId)
                .orElseThrow(() -> new RuntimeException("用户不存在"));
        return orderRepository.findByRiderOrderByCreatedAtDesc(rider);
    }

    public Order updateOrderStatus(Long orderId, Order.OrderStatus newStatus, Long userId, String role) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("订单不存在"));

        // 权限验证
        switch (role) {
            case "CUSTOMER":
                if (!order.getCustomer().getId().equals(userId)) {
                    throw new RuntimeException("无权操作此订单");
                }
                // 顾客只能取消订单
                if (newStatus == Order.OrderStatus.CANCELLED
                        && (order.getStatus() == Order.OrderStatus.PENDING_PAYMENT
                        || order.getStatus() == Order.OrderStatus.PENDING_ACCEPT)) {
                    order.setStatus(newStatus);
                } else {
                    throw new RuntimeException("当前状态不允许此操作");
                }
                break;

            case "MERCHANT":
                if (!order.getMerchant().getId().equals(userId)) {
                    throw new RuntimeException("无权操作此订单");
                }
                // 商家可以接单、开始制作、完成制作
                if (newStatus == Order.OrderStatus.PREPARING
                        && order.getStatus() == Order.OrderStatus.PENDING_ACCEPT) {
                    order.setStatus(newStatus);
                } else if (newStatus == Order.OrderStatus.PENDING_PICKUP
                        && order.getStatus() == Order.OrderStatus.PREPARING) {
                    order.setStatus(newStatus);
                } else {
                    throw new RuntimeException("当前状态不允许此操作");
                }
                break;

            case "RIDER":
                if (!order.getRider().getId().equals(userId)) {
                    throw new RuntimeException("无权操作此订单");
                }
                // 骑手可以接单（如果订单状态为待取餐且无骑手）、开始配送、完成配送
                if (newStatus == Order.OrderStatus.DELIVERING
                        && order.getStatus() == Order.OrderStatus.PENDING_PICKUP) {
                    order.setStatus(newStatus);
                } else if (newStatus == Order.OrderStatus.COMPLETED
                        && order.getStatus() == Order.OrderStatus.DELIVERING) {
                    order.setStatus(newStatus);
                } else {
                    throw new RuntimeException("当前状态不允许此操作");
                }
                break;
        }

        return orderRepository.save(order);
    }

    // 骑手接单
    public Order acceptOrderByRider(Long orderId, Long riderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("订单不存在"));

        if (order.getStatus() != Order.OrderStatus.PENDING_PICKUP) {
            throw new RuntimeException("订单状态不允许接单");
        }
        if (order.getRider() != null) {
            throw new RuntimeException("该订单已有骑手接单");
        }

        User rider = userRepository.findById(riderId)
                .orElseThrow(() -> new RuntimeException("骑手不存在"));

        order.setRider(rider);
        return orderRepository.save(order);
    }

    // 骑手可接订单列表
    public List<Order> getAvailableOrders() {
        return orderRepository.findByStatusAndRiderIsNull(Order.OrderStatus.PENDING_PICKUP);
    }
}
