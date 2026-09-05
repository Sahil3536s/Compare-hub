package com.comparehub.repository;

import com.comparehub.model.SavedProduct;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SavedProductRepository extends JpaRepository<SavedProduct, Long> {

    List<SavedProduct> findByUserId(Long userId);

    Optional<SavedProduct> findByUserIdAndProductId(Long userId, Long productId);

    boolean existsByUserIdAndProductId(Long userId, Long productId);

    Optional<SavedProduct> findByIdAndUserId(Long id, Long userId);

    void deleteByUserIdAndProductId(Long userId, Long productId);

    void deleteByIdAndUserId(Long id, Long userId);

    long countByUserId(Long userId);
}
