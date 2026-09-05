package com.comparehub.repository;

import com.comparehub.model.UserRankingPreference;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRankingPreferenceRepository extends JpaRepository<UserRankingPreference, Long> {

    Optional<UserRankingPreference> findByUserId(Long userId);

    void deleteByUserId(Long userId);
}
