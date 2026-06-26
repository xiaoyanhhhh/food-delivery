package com.fooddelivery.service;

import com.fooddelivery.entity.Order;
import com.fooddelivery.exception.BusinessException;
import com.fooddelivery.exception.NotFoundException;
import com.fooddelivery.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentService {

    private final OrderRepository orderRepository;

    @Transactional
    public PaymentResult pay(Long orderId, Long userId, String paymentMethod) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new NotFoundException("订单不存在"));

        if (!order.getCustomer().getId().equals(userId)) {
            throw new BusinessException("无权操作此订单");
        }

        if (order.getStatus() != Order.OrderStatus.PENDING_PAYMENT) {
            throw new BusinessException("订单状态不支持支付");
        }

        // 模拟支付：生成交易号
        String transactionId = "PAY" + LocalDateTime.now().format(
                java.time.format.DateTimeFormatter.ofPattern("yyyyMMddHHmmss"))
                + UUID.randomUUID().toString().substring(0, 8).toUpperCase();

        // 支付后自动转为待接单状态
        order.setStatus(Order.OrderStatus.PENDING_ACCEPT);
        order.setPaidAt(LocalDateTime.now());
        orderRepository.save(order);
        BigDecimal payableAmount = order.getTotalPrice()
                .add(order.getDeliveryFee() != null ? order.getDeliveryFee() : BigDecimal.ZERO);

        log.info("模拟支付成功: 订单号={}, 金额={}, 交易号={}, 支付方式={}",
                order.getOrderNo(), payableAmount, transactionId, paymentMethod);

        return new PaymentResult(transactionId, payableAmount, paymentMethod, LocalDateTime.now());
    }

    public record PaymentResult(
            String transactionId,
            java.math.BigDecimal amount,
            String paymentMethod,
            LocalDateTime paidAt
    ) {}
}
