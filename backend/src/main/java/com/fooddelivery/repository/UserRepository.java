package com.fooddelivery.repository;

import com.fooddelivery.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByUsername(String username);
    Optional<User> findByPhone(String phone);
    List<User> findByRole(User.Role role);
    List<User> findByRoleAndRiderType(User.Role role, User.RiderType riderType);
    boolean existsByUsername(String username);
    boolean existsByPhone(String phone);
}
