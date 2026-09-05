package com.comparehub.repository;

import com.comparehub.model.PriceAlert;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;

@Repository
public interface PriceAlertRepository extends JpaRepository<PriceAlert, Long> {

    List<PriceAlert> findByUserId(Long userId);

    List<PriceAlert> findByUserIdAndActiveTrue(Long userId);

    @Query("SELECT pa FROM PriceAlert pa WHERE pa.product.id = :productId AND pa.active = true AND pa.targetPrice >= :currentPrice")
    List<PriceAlert> findTriggerableAlerts(@Param("productId") Long productId, @Param("currentPrice") BigDecimal currentPrice);

    long countByUserId(Long userId);
}
