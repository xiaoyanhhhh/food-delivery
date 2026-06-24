package com.fooddelivery.service;

import com.fooddelivery.dto.AddressRequest;
import com.fooddelivery.entity.Address;
import com.fooddelivery.entity.User;
import com.fooddelivery.exception.ForbiddenException;
import com.fooddelivery.exception.NotFoundException;
import com.fooddelivery.repository.AddressRepository;
import com.fooddelivery.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AddressService {

    private final AddressRepository addressRepository;
    private final UserRepository userRepository;

    public List<Address> getUserAddresses(Long userId) {
        return addressRepository.findByUserIdOrderByIsDefaultDescCreatedAtDesc(userId);
    }

    @Transactional
    public Address createAddress(AddressRequest request, Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("用户不存在"));

        // 如果是第一个地址，自动设为默认
        boolean isFirst = addressRepository.countByUserId(userId) == 0;
        boolean isDefault = isFirst || (request.getIsDefault() != null && request.getIsDefault());

        // 如果设为默认，取消其他默认地址
        if (isDefault) {
            addressRepository.findByUserIdAndIsDefaultTrue(userId).ifPresent(addr -> {
                addr.setIsDefault(false);
                addressRepository.save(addr);
            });
        }

        Address address = Address.builder()
                .label(request.getLabel())
                .detail(request.getDetail())
                .contactName(request.getContactName())
                .contactPhone(request.getContactPhone())
                .lat(request.getLat())
                .lng(request.getLng())
                .isDefault(isDefault)
                .user(user)
                .build();

        return addressRepository.save(address);
    }

    @Transactional
    public Address updateAddress(Long addressId, AddressRequest request, Long userId) {
        Address address = addressRepository.findById(addressId)
                .orElseThrow(() -> new NotFoundException("地址不存在"));

        if (!address.getUser().getId().equals(userId)) {
            throw new ForbiddenException("无权操作此地址");
        }

        if (request.getLabel() != null) address.setLabel(request.getLabel());
        if (request.getDetail() != null) address.setDetail(request.getDetail());
        if (request.getContactName() != null) address.setContactName(request.getContactName());
        if (request.getContactPhone() != null) address.setContactPhone(request.getContactPhone());
        if (request.getLat() != null) address.setLat(request.getLat());
        if (request.getLng() != null) address.setLng(request.getLng());
        if (request.getIsDefault() != null && request.getIsDefault()) {
            addressRepository.findByUserIdAndIsDefaultTrue(userId).ifPresent(addr -> {
                addr.setIsDefault(false);
                addressRepository.save(addr);
            });
            address.setIsDefault(true);
        }

        return addressRepository.save(address);
    }

    public void deleteAddress(Long addressId, Long userId) {
        Address address = addressRepository.findById(addressId)
                .orElseThrow(() -> new NotFoundException("地址不存在"));

        if (!address.getUser().getId().equals(userId)) {
            throw new ForbiddenException("无权操作此地址");
        }

        addressRepository.delete(address);
    }

    public Address getAddressById(Long addressId, Long userId) {
        Address address = addressRepository.findById(addressId)
                .orElseThrow(() -> new NotFoundException("地址不存在"));

        if (!address.getUser().getId().equals(userId)) {
            throw new ForbiddenException("无权查看此地址");
        }

        return address;
    }
}
