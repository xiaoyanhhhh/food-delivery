package com.fooddelivery.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;

@Entity
@Table(name = "dish_specifications")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DishSpecification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "spec_group", nullable = false, length = 50)
    private String specGroup;  // 规格组: size/spice_level/temperature

    @Column(name = "spec_name", nullable = false, length = 50)
    private String specName;   // 规格名: 大份/中辣/热

    @Column(name = "price_delta", precision = 10, scale = 2)
    @Builder.Default
    private BigDecimal priceDelta = BigDecimal.ZERO;  // 加价

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "dish_id", nullable = false)
    private Dish dish;
}
