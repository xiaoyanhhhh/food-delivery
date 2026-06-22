package com.fooddelivery.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.math.BigDecimal;

@Data
public class DishRequest {

    @NotBlank(message = "菜品名不能为空")
    private String name;

    private String description;

    @NotNull(message = "价格不能为空")
    @Min(value = 0, message = "价格不能为负数")
    private BigDecimal price;

    private String image;

    @NotNull(message = "分类不能为空")
    private Long categoryId;

    private Boolean status;
}
