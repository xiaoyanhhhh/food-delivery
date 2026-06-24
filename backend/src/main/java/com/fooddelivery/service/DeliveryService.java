package com.fooddelivery.service;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Service
public class DeliveryService {

    @Value("${app.delivery.base-fee:3.0}")
    private double baseFee;

    @Value("${app.delivery.per-km-rate:1.5}")
    private double perKmRate;

    @Value("${app.delivery.free-distance:1.0}")
    private double freeDistance;

    public DeliveryInfo calculate(Long storeId, String deliveryAddress) {
        // 模拟距离：0.5 - 5.0 km
        double distance = 0.5 + Math.random() * 4.5;
        distance = Math.round(distance * 100.0) / 100.0;  // 保留2位小数

        // 配送费：基础费 + 超出部分×费率
        double fee = distance <= freeDistance ? baseFee : baseFee + (distance - freeDistance) * perKmRate;
        fee = Math.round(fee * 100.0) / 100.0;

        // ETA：15分钟 + 每公里5分钟
        int etaMinutes = (int) (15 + distance * 5);

        return new DeliveryInfo(
                BigDecimal.valueOf(distance),
                BigDecimal.valueOf(fee),
                etaMinutes
        );
    }

    @Getter
    public static class DeliveryInfo {
        private final BigDecimal distance;    // 距离(km)
        private final BigDecimal deliveryFee; // 配送费(元)
        private final int estimatedMinutes;    // 预计送达时间(分钟)

        public DeliveryInfo(BigDecimal distance, BigDecimal deliveryFee, int estimatedMinutes) {
            this.distance = distance;
            this.deliveryFee = deliveryFee;
            this.estimatedMinutes = estimatedMinutes;
        }
    }
}
