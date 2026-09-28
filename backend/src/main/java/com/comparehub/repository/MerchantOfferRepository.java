package com.comparehub.repository;

import com.comparehub.model.MerchantOffer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MerchantOfferRepository extends JpaRepository<MerchantOffer, Long> {

    List<MerchantOffer> findByProductIdOrderByPriceAsc(Long productId);

    List<MerchantOffer> findByProductIdAndInStockTrueOrderByPriceAsc(Long productId);
}
