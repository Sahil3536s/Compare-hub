package com.comparehub.repository;

import com.comparehub.model.UserPaymentPreference;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserPaymentPreferenceRepository extends JpaRepository<UserPaymentPreference, Long> {

    Optional<UserPaymentPreference> findByUserId(Long userId);
}
