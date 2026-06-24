package com.fooddelivery.config;

import com.fooddelivery.entity.Order;
import com.fooddelivery.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class

OrderTimeoutScheduler {

    private final OrderRepository orderRepository;

    @Value("${app.payment.timeout-minutes:15}")
    private int timeoutMinutes;

    @Scheduled(fixedRate = 60000)  // 每分钟执行一次
    public void cancelTimeoutOrders() {
        LocalDateTime timeout = LocalDateTime.now().minusMinutes(timeoutMinutes);
        List<Order> timeoutOrders = orderRepository.findByStatusAndCreatedAtBefore(
                Order.OrderStatus.PENDING_PAYMENT, timeout);

        for (Order order : timeoutOrders) {
            order.setStatus(Order.OrderStatus.CANCELLED);
            orderRepository.save(order);
            log.info("订单超时自动取消: 订单号={}, 创建时间={}", order.getOrderNo(), order.getCreatedAt());
        }

        if (!timeoutOrders.isEmpty()) {
            log.info("本次超时取消{}笔订单", timeoutOrders.size());
        }
    }
}
