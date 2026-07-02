package com.fooddelivery.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "users")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 50)
    private String username;

    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    @Column(nullable = false)
    private String password;

    @Column(nullable = false, unique = true, length = 20)
    private String phone;

    @Column(length = 255)
    private String address;

    @Column(name = "current_lat", precision = 10, scale = 6)
    private java.math.BigDecimal currentLat;

    @Column(name = "current_lng", precision = 10, scale = 6)
    private java.math.BigDecimal currentLng;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10, columnDefinition = "VARCHAR(10)")
    private Role role;

    @Enumerated(EnumType.STRING)
    @Column(name = "rider_type", length = 20, columnDefinition = "VARCHAR(20)")
    private RiderType riderType;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public enum Role {
        CUSTOMER,   // 顾客
        MERCHANT,   // 商家
        RIDER       // 骑手
    }

    public enum RiderType {
        FULL_TIME,  // 全职
        PART_TIME   // 兼职
    }

    @PrePersist
    protected void onCreate() {
        if (role == Role.RIDER && riderType == null) {
            riderType = RiderType.PART_TIME;
        }
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
