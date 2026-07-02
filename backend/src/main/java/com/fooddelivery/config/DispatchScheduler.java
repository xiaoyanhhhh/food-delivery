package com.fooddelivery.config;

import com.fooddelivery.service.OrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class DispatchScheduler {

    private final OrderService orderService;

    @Scheduled(fixedRate = 1000)
    public void forceAssignExpiredDispatchOrders() {
        int count = orderService.forceAssignExpiredDispatchOrders().size();
        if (count > 0) {
            log.info("本次强制派单{}笔订单", count);
        }
    }
}
