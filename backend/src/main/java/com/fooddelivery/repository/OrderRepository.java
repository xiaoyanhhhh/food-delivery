package com.fooddelivery.repository;

import com.fooddelivery.entity.Order;
import com.fooddelivery.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface OrderRepository extends JpaRepository<Order, Long> {

    List<Order> findByCustomerOrderByCreatedAtDesc(User customer);

    List<Order> findByMerchantOrderByCreatedAtDesc(User merchant);

    List<Order> findByRiderOrderByCreatedAtDesc(User rider);

    // 骑手可接的订单（待取餐状态且未分配骑手）
    List<Order> findByStatusAndRiderIsNull(Order.OrderStatus status);
}
