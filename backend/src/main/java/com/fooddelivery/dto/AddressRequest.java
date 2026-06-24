package com.fooddelivery.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class AddressRequest {
    @NotBlank(message = "标签不能为空")
    private String label;

    @NotBlank(message = "详细地址不能为空")
    private String detail;

    @NotBlank(message = "联系人不能为空")
    private String contactName;

    @NotBlank(message = "联系电话不能为空")
    private String contactPhone;

    private Double lat;
    private Double lng;
    private Boolean isDefault;
}
