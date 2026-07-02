package com.fooddelivery.repository;

import com.fooddelivery.entity.Order;
import com.fooddelivery.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface OrderRepository extends JpaRepository<Order, Long> {

    List<Order> findByCustomerOrderByCreatedAtDesc(User customer);

    Page<Order> findByCustomerOrderByCreatedAtDesc(User customer, Pageable pageable);

    List<Order> findByMerchantOrderByCreatedAtDesc(User merchant);

    Page<Order> findByMerchantOrderByCreatedAtDesc(User merchant, Pageable pageable);

    List<Order> findByRiderOrderByCreatedAtDesc(User rider);

    Page<Order> findByRiderOrderByCreatedAtDesc(User rider, Pageable pageable);

    @Query("""
            SELECT o FROM Order o
            WHERE o.rider = :rider
               OR (o.rider IS NULL
                   AND o.dispatchCandidateRider = :rider
                   AND o.dispatchStatus IN :dispatchStatuses)
            ORDER BY o.createdAt DESC
            """)
    List<Order> findRiderWorkOrders(@Param("rider") User rider,
                                    @Param("dispatchStatuses") List<Order.DispatchStatus> dispatchStatuses);

    @Query("""
            SELECT o FROM Order o
            WHERE o.rider = :rider
               OR (o.rider IS NULL
                   AND o.dispatchCandidateRider = :rider
                   AND o.dispatchStatus IN :dispatchStatuses)
            ORDER BY o.createdAt DESC
            """)
    Page<Order> findRiderWorkOrders(@Param("rider") User rider,
                                    @Param("dispatchStatuses") List<Order.DispatchStatus> dispatchStatuses,
                                    Pageable pageable);

    List<Order> findByStatusAndRiderIsNull(Order.OrderStatus status);

    Page<Order> findByStatusAndRiderIsNull(Order.OrderStatus status, Pageable pageable);

    @Query("""
            SELECT o FROM Order o
            WHERE o.rider IS NULL
              AND o.status IN :statuses
              AND (o.deliveryAddress IS NULL OR o.deliveryAddress NOT LIKE '%到店自取%')
              AND (o.note IS NULL OR o.note NOT LIKE '%到店自取%')
            """)
    Page<Order> findAvailableForRider(@Param("statuses") List<Order.OrderStatus> statuses, Pageable pageable);

    @Query("""
            SELECT o FROM Order o
            WHERE o.rider IS NULL
              AND o.status IN :statuses
              AND (o.deliveryAddress IS NULL OR o.deliveryAddress NOT LIKE '%到店自取%')
              AND (o.note IS NULL OR o.note NOT LIKE '%到店自取%')
            """)
    List<Order> findAvailableForRider(@Param("statuses") List<Order.OrderStatus> statuses);

    List<Order> findByStatusAndCreatedAtBefore(Order.OrderStatus status, LocalDateTime time);

    List<Order> findByDispatchStatusInAndRiderIsNullAndDispatchDeadlineAtBefore(
            List<Order.DispatchStatus> dispatchStatuses, LocalDateTime time);

    @Query("""
            SELECT COALESCE(SUM(i.quantity), 0)
            FROM Order o JOIN o.items i
            WHERE o.status = :status
              AND o.merchant.id = :merchantId
              AND o.updatedAt >= :since
            """)
    Long sumCompletedQuantityByMerchantSince(@Param("merchantId") Long merchantId,
                                             @Param("status") Order.OrderStatus status,
                                             @Param("since") LocalDateTime since);

    @Query("""
            SELECT COALESCE(SUM(i.quantity), 0)
            FROM Order o JOIN o.items i
            WHERE o.status = :status
              AND i.dishId = :dishId
              AND o.updatedAt >= :since
            """)
    Long sumCompletedQuantityByDishSince(@Param("dishId") Long dishId,
                                         @Param("status") Order.OrderStatus status,
                                         @Param("since") LocalDateTime since);
}
