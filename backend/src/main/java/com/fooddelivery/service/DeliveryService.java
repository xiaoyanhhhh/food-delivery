package com.fooddelivery.service;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
public class DeliveryService {

    @Value("${app.delivery.base-fee:3.0}")
    private double baseFee;

    @Value("${app.delivery.per-km-rate:1.5}")
    private double perKmRate;

    @Value("${app.delivery.free-distance:1.0}")
    private double freeDistance;

    public DeliveryInfo calculate(Long storeId, String deliveryAddress) {
        int seed = Math.abs((String.valueOf(storeId) + "|" + String.valueOf(deliveryAddress)).hashCode());
        double distance = 0.5 + (seed % 451) / 100.0;
        distance = Math.round(distance * 100.0) / 100.0;

        double fee = distance <= freeDistance ? baseFee : baseFee + (distance - freeDistance) * perKmRate;
        fee = Math.round(fee * 100.0) / 100.0;

        int etaMinutes = (int) (15 + distance * 5);

        return new DeliveryInfo(
                BigDecimal.valueOf(distance),
                BigDecimal.valueOf(fee),
                etaMinutes
        );
    }

    @Getter
    public static class DeliveryInfo {
        private final BigDecimal distance;
        private final BigDecimal deliveryFee;
        private final int estimatedMinutes;

        public DeliveryInfo(BigDecimal distance, BigDecimal deliveryFee, int estimatedMinutes) {
            this.distance = distance;
            this.deliveryFee = deliveryFee;
            this.estimatedMinutes = estimatedMinutes;
        }
    }
}
