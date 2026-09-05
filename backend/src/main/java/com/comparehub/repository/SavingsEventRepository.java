package com.comparehub.repository;

import com.comparehub.model.SavingsEvent;
import com.comparehub.model.SavingsEventType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

@Repository
public interface SavingsEventRepository extends JpaRepository<SavingsEvent, Long> {

    List<SavingsEvent> findByUserIdOrderByCreatedAtDesc(Long userId);

    List<SavingsEvent> findByUserIdAndEventType(Long userId, SavingsEventType eventType);

    List<SavingsEvent> findByUserIdAndCreatedAtAfter(Long userId, Instant after);

    List<SavingsEvent> findAllByOrderByCreatedAtDesc();

    long countByUserId(Long userId);
}
