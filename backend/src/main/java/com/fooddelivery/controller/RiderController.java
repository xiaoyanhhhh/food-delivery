package com.fooddelivery.controller;

import com.fooddelivery.dto.ApiResponse;
import com.fooddelivery.entity.Order;
import com.fooddelivery.entity.User;
import com.fooddelivery.repository.OrderRepository;
import com.fooddelivery.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@RestController
@RequestMapping("/api/rider")
@RequiredArgsConstructor
public class RiderController {
    private final OrderRepository orderRepository;
    private final UserRepository userRepository;

    // 骑手位置模拟数据（内存中）
    private final Map<Long, BigDecimal[]> riderPositions = new ConcurrentHashMap<>();

    @GetMapping("/earnings")
    public ApiResponse<Map<String, Object>> getEarnings(Authentication auth) {
        Long riderId = (Long) auth.getPrincipal();
        User rider = userRepository.findById(riderId).orElse(null);
        if (rider == null) return ApiResponse.error(404, "骑手不存在");

        List<Order> orders = orderRepository.findByRiderOrderByCreatedAtDesc(rider);
        BigDecimal todayEarnings = BigDecimal.ZERO, totalEarnings = BigDecimal.ZERO;
        int todayCompleted = 0, totalCompleted = 0;
        String today = LocalDate.now().toString();

        for (Order o : orders) {
            if (o.getStatus() == Order.OrderStatus.COMPLETED) {
                BigDecimal fee = o.getDeliveryFee() != null ? o.getDeliveryFee() : BigDecimal.valueOf(3);
                totalEarnings = totalEarnings.add(fee);
                totalCompleted++;
                if (o.getUpdatedAt() != null && o.getUpdatedAt().toLocalDate().toString().equals(today)) {
                    todayEarnings = todayEarnings.add(fee);
                    todayCompleted++;
                }
            }
        }
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("todayEarnings", todayEarnings);
        result.put("totalEarnings", totalEarnings);
        result.put("todayCompleted", todayCompleted);
        result.put("totalCompleted", totalCompleted);
        return ApiResponse.success(result);
    }

    // 更新骑手位置
    @PutMapping("/location")
    public ApiResponse<Void> updateLocation(@RequestBody Map<String, Object> body, Authentication auth) {
        Long riderId = (Long) auth.getPrincipal();
        double lat = Double.parseDouble(body.get("lat").toString());
        double lng = Double.parseDouble(body.get("lng").toString());
        riderPositions.put(riderId, new BigDecimal[]{BigDecimal.valueOf(lat), BigDecimal.valueOf(lng)});
        // Also update DB
        User rider = userRepository.findById(riderId).orElse(null);
        if (rider != null) {
            rider.setCurrentLat(BigDecimal.valueOf(lat));
            rider.setCurrentLng(BigDecimal.valueOf(lng));
            userRepository.save(rider);
        }
        return ApiResponse.success();
    }

    // 获取骑手位置
    @GetMapping("/location")
    public ApiResponse<Map<String, Object>> getLocation(@RequestParam Long riderId) {
        BigDecimal[] pos = riderPositions.get(riderId);
        if (pos == null) {
            User rider = userRepository.findById(riderId).orElse(null);
            if (rider != null && rider.getCurrentLat() != null) {
                pos = new BigDecimal[]{rider.getCurrentLat(), rider.getCurrentLng()};
            }
        }
        Map<String, Object> result = new HashMap<>();
        result.put("lat", pos != null ? pos[0] : BigDecimal.valueOf(39.9042));
        result.put("lng", pos != null ? pos[1] : BigDecimal.valueOf(116.4074));
        return ApiResponse.success(result);
    }

    // 开始骑手位置模拟（从商家走向顾客）
    @PostMapping("/simulate")
    public ApiResponse<Void> startSimulation(@RequestBody Map<String, Long> body) {
        Long riderId = body.get("riderId");
        Long orderId = body.get("orderId");
        Order order = orderRepository.findById(orderId).orElse(null);
        if (order == null) return ApiResponse.error(404, "订单不存在");

        // 起点：商家店铺坐标（默认国贸附近）
        double startLat = 39.9087, startLng = 116.4605;
        // 终点：顾客坐标
        double endLat = order.getCustomerLat() != null ? order.getCustomerLat().doubleValue() : 39.9542;
        double endLng = order.getCustomerLng() != null ? order.getCustomerLng().doubleValue() : 116.4074;

        riderPositions.put(riderId, new BigDecimal[]{BigDecimal.valueOf(startLat), BigDecimal.valueOf(startLng)});

        // 后台模拟：分10步从起点走向终点
        new Thread(() -> {
            for (int step = 1; step <= 10; step++) {
                try { Thread.sleep(5000); } catch (InterruptedException e) { break; }
                double progress = step / 10.0;
                double lat = startLat + (endLat - startLat) * progress;
                double lng = startLng + (endLng - startLng) * progress;
                riderPositions.put(riderId, new BigDecimal[]{
                        BigDecimal.valueOf(lat).setScale(6, RoundingMode.HALF_UP),
                        BigDecimal.valueOf(lng).setScale(6, RoundingMode.HALF_UP)
                });
            }
        }).start();

        return ApiResponse.success();
    }
}
