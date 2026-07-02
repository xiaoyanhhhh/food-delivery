package com.fooddelivery.service;

import com.fooddelivery.dto.OrderRequest;
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

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final DishRepository dishRepository;
    private final CartItemRepository cartItemRepository;
    private final UserRepository userRepository;
    private final StoreRepository storeRepository;
    private final NotificationService notificationService;
    private final DeliveryService deliveryService;
    private static final BigDecimal NO_RIDER_SURCHARGE = BigDecimal.valueOf(2);

    @Transactional
    public Order createOrder(Long customerId, OrderRequest request) {
        User customer = userRepository.findById(customerId)
                .orElseThrow(() -> new NotFoundException("用户不存在"));

        BigDecimal totalPrice = BigDecimal.ZERO;
        Long merchantId = null;

        for (OrderRequest.OrderItemRequest item : request.getItems()) {
            Dish dish = dishRepository.findById(item.getDishId())
                    .orElseThrow(() -> new NotFoundException("菜品 " + item.getDishId() + " 不存在"));

            if (!dish.getStatus()) {
                throw new BusinessException("菜品 " + dish.getName() + " 已下架");
            }

            if (merchantId == null) {
                merchantId = dish.getMerchant().getId();
            } else if (!merchantId.equals(dish.getMerchant().getId())) {
                throw new BusinessException("不同商家的菜品请分开下单");
            }

            totalPrice = totalPrice.add(dish.getPrice().multiply(BigDecimal.valueOf(item.getQuantity())));
        }

        User merchant = userRepository.findById(merchantId)
                .orElseThrow(() -> new NotFoundException("商家不存在"));
        Store store = storeRepository.findByMerchantId(merchantId).orElse(null);
        Long storeId = store != null ? store.getId() : merchantId;
        boolean pickupOrder = isPickupOrder(request.getDeliveryAddress(), request.getNote());
        DeliveryService.DeliveryInfo deliveryInfo = pickupOrder
                ? new DeliveryService.DeliveryInfo(BigDecimal.ZERO, BigDecimal.ZERO, 0)
                : deliveryService.calculate(storeId, request.getDeliveryAddress());

        String orderNo = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"))
                + UUID.randomUUID().toString().substring(0, 6).toUpperCase();

        Order order = Order.builder()
                .orderNo(orderNo)
                .customer(customer)
                .merchant(merchant)
                .status(Order.OrderStatus.PENDING_PAYMENT)
                .totalPrice(totalPrice)
                .deliveryFee(deliveryInfo.getDeliveryFee())
                .deliveryDistance(deliveryInfo.getDistance())
                .estimatedDeliveryTime(deliveryInfo.getEstimatedMinutes())
                .deliveryAddress(request.getDeliveryAddress())
                .note(request.getNote())
                .build();

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

    public Page<Order> getCustomerOrders(Long customerId, Pageable pageable) {
        User customer = userRepository.findById(customerId)
                .orElseThrow(() -> new NotFoundException("用户不存在"));
        return orderRepository.findByCustomerOrderByCreatedAtDesc(customer, pageable);
    }

    public List<Order> getCustomerOrders(Long customerId) {
        User customer = userRepository.findById(customerId)
                .orElseThrow(() -> new NotFoundException("用户不存在"));
        return orderRepository.findByCustomerOrderByCreatedAtDesc(customer);
    }

    public Page<Order> getMerchantOrders(Long merchantId, Pageable pageable) {
        User merchant = userRepository.findById(merchantId)
                .orElseThrow(() -> new NotFoundException("用户不存在"));
        return orderRepository.findByMerchantOrderByCreatedAtDesc(merchant, pageable);
    }

    public List<Order> getMerchantOrders(Long merchantId) {
        User merchant = userRepository.findById(merchantId)
                .orElseThrow(() -> new NotFoundException("用户不存在"));
        return orderRepository.findByMerchantOrderByCreatedAtDesc(merchant);
    }

    public Page<Order> getRiderOrders(Long riderId, Pageable pageable) {
        User rider = userRepository.findById(riderId)
                .orElseThrow(() -> new NotFoundException("用户不存在"));
        return orderRepository.findRiderWorkOrders(rider, dispatchOfferStatuses(), pageable);
    }

    public List<Order> getRiderOrders(Long riderId) {
        User rider = userRepository.findById(riderId)
                .orElseThrow(() -> new NotFoundException("用户不存在"));
        return orderRepository.findRiderWorkOrders(rider, dispatchOfferStatuses());
    }

    @Transactional
    public Order updateOrderStatus(Long orderId, Order.OrderStatus newStatus, Long userId, String role) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new NotFoundException("订单不存在"));

        switch (role) {
            case "CUSTOMER":
                if (!order.getCustomer().getId().equals(userId)) {
                    throw new ForbiddenException("无权操作此订单");
                }
                // 顾客只能取消未支付的订单
                if (newStatus == Order.OrderStatus.CANCELLED
                        && (order.getStatus() == Order.OrderStatus.PENDING_PAYMENT
                        || order.getStatus() == Order.OrderStatus.PAID
                        || order.getStatus() == Order.OrderStatus.PENDING_ACCEPT)) {
                    order.setStatus(newStatus);
                } else if (newStatus == Order.OrderStatus.COMPLETED
                        && isPickupOrder(order)
                        && (order.getStatus() == Order.OrderStatus.READY
                        || order.getStatus() == Order.OrderStatus.PENDING_PICKUP)) {
                    order.setStatus(newStatus);
                    incrementMonthlySales(order);
                } else {
                    throw new BusinessException("当前状态不允许此操作");
                }
                break;

            case "MERCHANT":
                if (!order.getMerchant().getId().equals(userId)) {
                    throw new ForbiddenException("无权操作此订单");
                }
                // 商家：PAID/PENDING_ACCEPT → PREPARING → READY
                if (newStatus == Order.OrderStatus.PREPARING
                        && (order.getStatus() == Order.OrderStatus.PENDING_ACCEPT
                        || order.getStatus() == Order.OrderStatus.PAID)) {
                    order.setStatus(newStatus);
                    // 设置预估配送时间
                    if (order.getEstimatedDeliveryTime() == null) {
                        order.setEstimatedDeliveryTime(30);
                    }
                } else if (newStatus == Order.OrderStatus.READY
                        && order.getStatus() == Order.OrderStatus.PREPARING) {
                    order.setStatus(newStatus);
                } else if (newStatus == Order.OrderStatus.COMPLETED
                        && isPickupOrder(order)
                        && (order.getStatus() == Order.OrderStatus.READY
                        || order.getStatus() == Order.OrderStatus.PENDING_PICKUP)) {
                    order.setStatus(newStatus);
                    incrementMonthlySales(order);
                } else {
                    throw new BusinessException("当前状态不允许此操作");
                }
                break;

            case "RIDER":
                if (order.getRider() == null || !order.getRider().getId().equals(userId)) {
                    throw new ForbiddenException("无权操作此订单");
                }
                // 骑手：READY → PICKED_UP → DELIVERING → COMPLETED
                if (newStatus == Order.OrderStatus.PICKED_UP
                        && order.getStatus() == Order.OrderStatus.PREPARING) {
                    throw new BusinessException("商家尚未制作完成，暂不能取餐");
                } else if (newStatus == Order.OrderStatus.PICKED_UP
                        && (order.getStatus() == Order.OrderStatus.READY
                        || order.getStatus() == Order.OrderStatus.PENDING_PICKUP)) {
                    order.setStatus(newStatus);
                } else if ((newStatus == Order.OrderStatus.DELIVERING || newStatus == Order.OrderStatus.COMPLETED)
                        && order.getStatus() == Order.OrderStatus.PREPARING) {
                    throw new BusinessException("商家尚未制作完成，暂不能配送或送达");
                } else if (newStatus == Order.OrderStatus.DELIVERING
                        && order.getStatus() == Order.OrderStatus.PICKED_UP) {
                    order.setStatus(newStatus);
                } else if (newStatus == Order.OrderStatus.DELIVERING
                        && (order.getStatus() == Order.OrderStatus.READY
                        || order.getStatus() == Order.OrderStatus.PENDING_PICKUP)) {
                    throw new BusinessException("请先确认取餐后再开始配送");
                } else if (newStatus == Order.OrderStatus.COMPLETED
                        && order.getStatus() == Order.OrderStatus.DELIVERING) {
                    order.setStatus(newStatus);
                    if (order.getDeliveryFee() == null) {
                        order.setDeliveryFee(java.math.BigDecimal.valueOf(3));
                    }
                    incrementMonthlySales(order);
                } else if (newStatus == Order.OrderStatus.COMPLETED) {
                    throw new BusinessException("订单尚未开始配送，不能确认送达");
                } else {
                    throw new BusinessException("当前状态不允许此操作");
                }
                break;
        }

        order = orderRepository.save(order);
        notifyStatusChange(order);
        return order;
    }

    @Transactional
    public Order acceptOrderByRider(Long orderId, Long riderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new NotFoundException("订单不存在"));

        // 骑手可提前接制作中的外卖单；到店自取单不进入骑手流程。
        if (isPickupOrder(order)) {
            throw new BusinessException("到店自取订单无需骑手接单");
        }
        if (order.getStatus() == Order.OrderStatus.PENDING_PAYMENT
                || order.getStatus() == Order.OrderStatus.PAID
                || order.getStatus() == Order.OrderStatus.PENDING_ACCEPT) {
            throw new BusinessException("商家接单后骑手才能接单");
        }
        if (order.getStatus() != Order.OrderStatus.PREPARING
                && order.getStatus() != Order.OrderStatus.READY
                && order.getStatus() != Order.OrderStatus.PENDING_PICKUP) {
            throw new BusinessException("订单状态不允许接单");
        }
        if (order.getRider() != null) {
            throw new BusinessException("该订单已有骑手接单");
        }

        User rider = userRepository.findById(riderId)
                .orElseThrow(() -> new NotFoundException("骑手不存在"));

        order.setRider(rider);
        order.setDispatchCandidateRider(null);
        if (order.getDispatchStatus() == Order.DispatchStatus.REQUESTED
                || order.getDispatchStatus() == Order.DispatchStatus.REJECTED) {
            order.setDispatchStatus(Order.DispatchStatus.ACCEPTED);
        }
        return orderRepository.save(order);
    }

    @Transactional
    public Order requestNoRiderDispatch(Long orderId, Long merchantId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new NotFoundException("订单不存在"));
        if (!order.getMerchant().getId().equals(merchantId)) {
            throw new ForbiddenException("无权操作此订单");
        }
        if (isPickupOrder(order)) {
            throw new BusinessException("到店自取订单无需派送");
        }
        if (order.getRider() != null) {
            throw new BusinessException("该订单已有骑手接单");
        }
        if (order.getStatus() != Order.OrderStatus.PREPARING
                && order.getStatus() != Order.OrderStatus.READY
                && order.getStatus() != Order.OrderStatus.PENDING_PICKUP) {
            throw new BusinessException("商家接单后才能发起无人派送");
        }

        if (order.getDispatchSurcharge() == null || order.getDispatchSurcharge().compareTo(BigDecimal.ZERO) <= 0) {
            BigDecimal currentFee = order.getDeliveryFee() != null ? order.getDeliveryFee() : BigDecimal.ZERO;
            order.setDeliveryFee(currentFee.add(NO_RIDER_SURCHARGE));
            order.setDispatchSurcharge(NO_RIDER_SURCHARGE);
        }

        LocalDateTime now = LocalDateTime.now();
        order.setDispatchStatus(Order.DispatchStatus.REQUESTED);
        order.setDispatchStartedAt(now);
        order.setDispatchDeadlineAt(now.plusSeconds(10));
        if (order.getDispatchRejectCount() == null) {
            order.setDispatchRejectCount(0);
        }
        findFullTimeRider(null).ifPresent(order::setDispatchCandidateRider);

        Order saved = orderRepository.save(order);
        if (saved.getDispatchCandidateRider() != null) {
            notificationService.create(saved.getDispatchCandidateRider().getId(),
                    "系统派单", "订单 " + saved.getOrderNo() + " 已加价，等待您接单", saved.getId());
        }
        return saved;
    }

    @Transactional
    public Order acceptDispatchOffer(Long orderId, Long riderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new NotFoundException("订单不存在"));
        if (order.getRider() != null) {
            throw new BusinessException("该订单已有骑手接单");
        }
        if (order.getDispatchCandidateRider() == null
                || !order.getDispatchCandidateRider().getId().equals(riderId)) {
            throw new ForbiddenException("当前没有派给您的订单");
        }
        User rider = userRepository.findById(riderId)
                .orElseThrow(() -> new NotFoundException("骑手不存在"));
        order.setRider(rider);
        order.setDispatchCandidateRider(null);
        order.setDispatchStatus(Order.DispatchStatus.ACCEPTED);
        order.setStatus(Order.OrderStatus.PENDING_PICKUP);
        notifyStatusChange(order);
        return orderRepository.save(order);
    }

    @Transactional
    public Order rejectDispatchOffer(Long orderId, Long riderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new NotFoundException("订单不存在"));
        if (order.getRider() != null) {
            throw new BusinessException("该订单已有骑手接单");
        }
        if (order.getDispatchCandidateRider() == null
                || !order.getDispatchCandidateRider().getId().equals(riderId)) {
            throw new ForbiddenException("当前没有派给您的订单");
        }

        order.setDispatchRejectCount((order.getDispatchRejectCount() != null ? order.getDispatchRejectCount() : 0) + 1);
        Optional<User> nextRider = findFullTimeRider(riderId);
        if (nextRider.isPresent()) {
            order.setDispatchCandidateRider(nextRider.get());
            order.setDispatchStatus(Order.DispatchStatus.REQUESTED);
            notificationService.create(nextRider.get().getId(),
                    "系统派单", "订单 " + order.getOrderNo() + " 已加价，等待您接单", order.getId());
        } else {
            order.setDispatchCandidateRider(null);
            order.setDispatchStatus(Order.DispatchStatus.REJECTED);
        }
        if (order.getDispatchDeadlineAt() == null) {
            order.setDispatchDeadlineAt(LocalDateTime.now().plusSeconds(10));
        }
        return orderRepository.save(order);
    }

    @Transactional
    public List<Order> forceAssignExpiredDispatchOrders() {
        List<Order> orders = orderRepository.findByDispatchStatusInAndRiderIsNullAndDispatchDeadlineAtBefore(
                dispatchOfferStatuses(), LocalDateTime.now());
        for (Order order : orders) {
            Optional<User> fullTimeRider = Optional.ofNullable(order.getDispatchCandidateRider())
                    .filter(rider -> rider.getRiderType() == User.RiderType.FULL_TIME)
                    .or(() -> findFullTimeRider(null));
            User rider = fullTimeRider.orElseGet(() -> userRepository.findByRole(User.Role.RIDER).stream()
                    .findFirst()
                    .orElse(null));
            if (rider == null) {
                continue;
            }
            order.setRider(rider);
            order.setDispatchCandidateRider(null);
            order.setDispatchStatus(Order.DispatchStatus.FORCE_ASSIGNED);
            order.setStatus(Order.OrderStatus.PENDING_PICKUP);
            orderRepository.save(order);
            notificationService.create(rider.getId(),
                    "系统强制派单", "订单 " + order.getOrderNo() + " 已分配给您，请尽快取餐", order.getId());
            notifyStatusChange(order);
        }
        return orders;
    }

    public Page<Order> getAvailableOrders(Pageable pageable) {
        return orderRepository.findAvailableForRider(riderAvailableStatuses(), pageable);
    }

    public List<Order> getAvailableOrders() {
        return orderRepository.findAvailableForRider(riderAvailableStatuses());
    }

    private List<Order.OrderStatus> riderAvailableStatuses() {
        return List.of(
                Order.OrderStatus.PREPARING,
                Order.OrderStatus.READY,
                Order.OrderStatus.PENDING_PICKUP
        );
    }

    private List<Order.DispatchStatus> dispatchOfferStatuses() {
        return List.of(Order.DispatchStatus.REQUESTED, Order.DispatchStatus.REJECTED);
    }

    private Optional<User> findFullTimeRider(Long excludeRiderId) {
        return userRepository.findByRoleAndRiderType(User.Role.RIDER, User.RiderType.FULL_TIME).stream()
                .filter(rider -> excludeRiderId == null || !rider.getId().equals(excludeRiderId))
                .findFirst();
    }

    @Transactional(readOnly = true)
    public Order getOrderById(Long orderId, Long userId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new NotFoundException("订单不存在"));
        if (!order.getCustomer().getId().equals(userId)
                && !order.getMerchant().getId().equals(userId)
                && (order.getRider() == null || !order.getRider().getId().equals(userId))) {
            throw new ForbiddenException("无权查看此订单");
        }
        return order;
    }

    // 再来一单
    @Transactional
    public Order reorder(Long orderId, Long customerId) {
        Order oldOrder = getOrderById(orderId, customerId);
        if (oldOrder.getStatus() != Order.OrderStatus.COMPLETED && oldOrder.getStatus() != Order.OrderStatus.CANCELLED) {
            throw new BusinessException("只有已完成或已取消的订单才能再来一单");
        }
        // 检查菜品是否仍然在售
        for (OrderItem item : oldOrder.getItems()) {
            Dish dish = dishRepository.findById(item.getDishId()).orElse(null);
            if (dish == null || !dish.getStatus()) {
                throw new BusinessException("菜品 " + item.getDishName() + " 已下架，无法再来一单");
            }
        }

        String orderNo = java.time.LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMddHHmmss"))
                + UUID.randomUUID().toString().substring(0, 6).toUpperCase();

        Order newOrder = Order.builder()
                .orderNo(orderNo)
                .customer(oldOrder.getCustomer())
                .merchant(oldOrder.getMerchant())
                .status(Order.OrderStatus.PENDING_PAYMENT)
                .totalPrice(oldOrder.getTotalPrice())
                .deliveryFee(oldOrder.getDeliveryFee())
                .deliveryDistance(oldOrder.getDeliveryDistance())
                .estimatedDeliveryTime(oldOrder.getEstimatedDeliveryTime())
                .deliveryAddress(oldOrder.getDeliveryAddress())
                .note(oldOrder.getNote())
                .build();

        List<OrderItem> items = oldOrder.getItems().stream().map(item ->
                OrderItem.builder()
                        .order(newOrder)
                        .dishId(item.getDishId())
                        .dishName(item.getDishName())
                        .dishPrice(item.getDishPrice())
                        .quantity(item.getQuantity())
                        .build()
        ).collect(java.util.stream.Collectors.toList());

        newOrder.setItems(items);
        return orderRepository.save(newOrder);
    }

    private void incrementMonthlySales(Order order) {
        int quantity = order.getItems().stream()
                .map(OrderItem::getQuantity)
                .filter(java.util.Objects::nonNull)
                .mapToInt(Integer::intValue)
                .sum();
        storeRepository.findByMerchantId(order.getMerchant().getId()).ifPresent(store -> {
            store.setMonthlySales((store.getMonthlySales() != null ? store.getMonthlySales() : 0) + quantity);
            storeRepository.save(store);
        });
        for (OrderItem item : order.getItems()) {
            dishRepository.findById(item.getDishId()).ifPresent(dish -> {
                int itemQuantity = item.getQuantity() != null ? item.getQuantity() : 0;
                dish.setMonthlySales((dish.getMonthlySales() != null ? dish.getMonthlySales() : 0) + itemQuantity);
                dishRepository.save(dish);
            });
        }
    }

    private boolean isPickupOrder(Order order) {
        return isPickupOrder(order.getDeliveryAddress(), order.getNote());
    }

    private boolean isPickupOrder(String deliveryAddress, String note) {
        return (deliveryAddress != null && deliveryAddress.contains("到店自取"))
                || (note != null && note.contains("到店自取"));
    }

    // 发送通知 + 更新状态
    private void notifyStatusChange(Order order) {
        String statusLabel = getStatusLabel(order.getStatus());
        // 通知顾客
        notificationService.create(order.getCustomer().getId(),
                "订单状态更新", "订单 " + order.getOrderNo() + " 状态变更为：" + statusLabel, order.getId());
        // 通知商家(如果状态是顾客取消)
        if (order.getMerchant() != null && order.getStatus() == Order.OrderStatus.CANCELLED) {
            notificationService.create(order.getMerchant().getId(),
                    "订单取消", "订单 " + order.getOrderNo() + " 已被顾客取消", order.getId());
        }
    }

    private String getStatusLabel(Order.OrderStatus status) {
        return switch (status) {
            case PENDING_PAYMENT -> "待支付";
            case PAID -> "已支付";
            case PENDING_ACCEPT -> "待接单";
            case PREPARING -> "制作中";
            case READY -> "已备好";
            case PENDING_PICKUP -> "待取餐";
            case PICKED_UP -> "已取餐";
            case DELIVERING -> "配送中";
            case COMPLETED -> "已完成";
            case CANCELLED -> "已取消";
        };
    }
}
