package com.fooddelivery.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "orders")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "order_no", nullable = false, unique = true, length = 32)
    private String orderNo;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id", nullable = false)
    private User customer;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "merchant_id", nullable = false)
    private User merchant;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "rider_id")
    private User rider;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20, columnDefinition = "VARCHAR(20)")
    private OrderStatus status;

    @Column(name = "total_price", nullable = false, precision = 10, scale = 2)
    private BigDecimal totalPrice;

    @Column(name = "delivery_address", length = 255)
    private String deliveryAddress;

    @Column(length = 255)
    private String note;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<OrderItem> items = new ArrayList<>();

    @Column(name = "delivery_fee", precision = 10, scale = 2)
    private java.math.BigDecimal deliveryFee;

    @Column(name = "estimated_delivery_time")
    private Integer estimatedDeliveryTime;  // 预计送达分钟数

    @Column(name = "paid_at")
    private LocalDateTime paidAt;

    @Column(name = "address_snapshot", columnDefinition = "TEXT")
    private String addressSnapshot;  // 下单时的地址快照(JSON)

    @Column(name = "delivery_distance", precision = 10, scale = 2)
    private java.math.BigDecimal deliveryDistance;

    @Column(name = "customer_lat", precision = 10, scale = 6)
    private java.math.BigDecimal customerLat;

    @Column(name = "customer_lng", precision = 10, scale = 6)
    private java.math.BigDecimal customerLng;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public enum OrderStatus {
        PENDING_PAYMENT,  // 待支付
        PAID,             // 已支付（待商家接单）
        PENDING_ACCEPT,   // 待商家接单
        PREPARING,        // 制作中
        READY,            // 已备好（待取餐）
        PENDING_PICKUP,   // 待取餐
        PICKED_UP,        // 已取餐（配送中）
        DELIVERING,       // 配送中
        COMPLETED,        // 已完成
        CANCELLED         // 已取消
    }

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
