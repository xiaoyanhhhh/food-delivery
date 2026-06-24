package com.fooddelivery.repository;

import com.fooddelivery.entity.Order;
import com.fooddelivery.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface OrderRepository extends JpaRepository<Order, Long> {

    List<Order> findByCustomerOrderByCreatedAtDesc(User customer);
    Page<Order> findByCustomerOrderByCreatedAtDesc(User customer, Pageable pageable);

    List<Order> findByMerchantOrderByCreatedAtDesc(User merchant);
    Page<Order> findByMerchantOrderByCreatedAtDesc(User merchant, Pageable pageable);

    List<Order> findByRiderOrderByCreatedAtDesc(User rider);
    Page<Order> findByRiderOrderByCreatedAtDesc(User rider, Pageable pageable);

    List<Order> findByStatusAndRiderIsNull(Order.OrderStatus status);
    Page<Order> findByStatusAndRiderIsNull(Order.OrderStatus status, Pageable pageable);

    // 超时未支付订单查询
    List<Order> findByStatusAndCreatedAtBefore(Order.OrderStatus status, java.time.LocalDateTime time);
}
