package com.comparehub.repository;

import com.comparehub.model.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {

    List<Product> findByCategoryIgnoreCase(String category);

    List<Product> findByNameContainingIgnoreCase(String keyword);

    @Query("SELECT DISTINCT p FROM Product p LEFT JOIN FETCH p.offers WHERE p.id = :id")
    Optional<Product> findByIdWithOffers(@Param("id") Long id);

    @Query("SELECT DISTINCT p FROM Product p LEFT JOIN FETCH p.offers")
    List<Product> findAllWithOffers();

    @Query(
            value = "SELECT DISTINCT p FROM Product p LEFT JOIN FETCH p.offers",
            countQuery = "SELECT count(p) FROM Product p"
    )
    Page<Product> findAllWithOffers(Pageable pageable);

    @Query(
            value = "SELECT DISTINCT p FROM Product p LEFT JOIN FETCH p.offers WHERE LOWER(p.category) = LOWER(:category)",
            countQuery = "SELECT count(p) FROM Product p WHERE LOWER(p.category) = LOWER(:category)"
    )
    Page<Product> findByCategoryWithOffers(@Param("category") String category, Pageable pageable);
}
