package com.comparehub.repository;

import com.comparehub.model.ProductPriceHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
public interface ProductPriceHistoryRepository extends JpaRepository<ProductPriceHistory, Long> {

    List<ProductPriceHistory> findByProductIdOrderByRecordedAtAsc(Long productId);

    List<ProductPriceHistory> findByProductIdAndRecordedAtGreaterThanEqualOrderByRecordedAtAsc(
            Long productId, Instant recordedAt);

    Optional<ProductPriceHistory> findTopByProductIdAndMerchantOrderByRecordedAtDesc(
            Long productId, String merchant);

    Optional<ProductPriceHistory> findTopByProductIdOrderByRecordedAtDesc(Long productId);
}
