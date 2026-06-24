package com.fooddelivery.controller;

import com.fooddelivery.dto.AddressRequest;
import com.fooddelivery.dto.ApiResponse;
import com.fooddelivery.entity.Address;
import com.fooddelivery.service.AddressService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/addresses")
@RequiredArgsConstructor
public class AddressController {

    private final AddressService addressService;

    @GetMapping
    public ApiResponse<List<Address>> getAddresses(Authentication authentication) {
        Long userId = (Long) authentication.getPrincipal();
        return ApiResponse.success(addressService.getUserAddresses(userId));
    }

    @GetMapping("/{id}")
    public ApiResponse<Address> getAddress(@PathVariable Long id, Authentication authentication) {
        Long userId = (Long) authentication.getPrincipal();
        return ApiResponse.success(addressService.getAddressById(id, userId));
    }

    @PostMapping
    public ApiResponse<Address> createAddress(@Valid @RequestBody AddressRequest request,
                                               Authentication authentication) {
        Long userId = (Long) authentication.getPrincipal();
        return ApiResponse.success(addressService.createAddress(request, userId));
    }

    @PutMapping("/{id}")
    public ApiResponse<Address> updateAddress(@PathVariable Long id,
                                               @RequestBody AddressRequest request,
                                               Authentication authentication) {
        Long userId = (Long) authentication.getPrincipal();
        return ApiResponse.success(addressService.updateAddress(id, request, userId));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> deleteAddress(@PathVariable Long id, Authentication authentication) {
        Long userId = (Long) authentication.getPrincipal();
        addressService.deleteAddress(id, userId);
        return ApiResponse.success();
    }
}
