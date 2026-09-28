package com.comparehub.repository;

import com.comparehub.model.FlightSearch;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface FlightSearchRepository extends JpaRepository<FlightSearch, Long> {

    List<FlightSearch> findByUserIdOrderByCreatedAtDesc(Long userId);

    boolean existsByIdAndUserId(Long id, Long userId);

    void deleteByIdAndUserId(Long id, Long userId);
}
