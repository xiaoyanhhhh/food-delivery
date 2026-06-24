package com.fooddelivery.repository;

import com.fooddelivery.entity.Address;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface AddressRepository extends JpaRepository<Address, Long> {
    List<Address> findByUserIdOrderByIsDefaultDescCreatedAtDesc(Long userId);
    Optional<Address> findByUserIdAndIsDefaultTrue(Long userId);
    long countByUserId(Long userId);
}
