package com.fooddelivery.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import java.math.BigDecimal;

@Data
public class StoreRequest {

    @NotBlank(message = "店铺名称不能为空")
    private String name;

    private String logo;
    private String description;
    private BigDecimal minOrderAmount;
    private String businessHours;
    private String announcement;
    private String address;
    private String phone;
    private BigDecimal lat;
    private BigDecimal lng;
    private Long categoryId;
    private Boolean status;
}
