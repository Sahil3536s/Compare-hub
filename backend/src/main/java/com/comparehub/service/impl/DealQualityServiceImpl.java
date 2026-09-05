package com.comparehub.service.impl;

import com.comparehub.dto.DealQualityDto;
import com.comparehub.dto.NormalizedProductOfferDto;
import com.comparehub.service.DealQualityService;
import com.comparehub.service.DealScoreCalculator;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Slf4j
@Service
public class DealQualityServiceImpl implements DealQualityService {

    @Override
    public DealQualityDto evaluateOfferDealQuality(NormalizedProductOfferDto offer) {
        if (offer == null || offer.getPrice() == null) {
            return null;
        }

        BigDecimal currentPrice = offer.getEffectivePrice() != null ? offer.getEffectivePrice() : offer.getPrice();
        BigDecimal originalPrice = offer.getOriginalPrice() != null ? offer.getOriginalPrice() : currentPrice;

        // Estimate realistic historical 30D and 90D reference prices
        // Realistic retail average is typically 4% to 8% above discounted deal price, or below inflated MRP
        BigDecimal estimatedHistoricalAvg;
        BigDecimal estimated30DayLow;
        BigDecimal estimated90DayLow;

        if (originalPrice.compareTo(currentPrice) > 0) {
            // MRP is higher than current price
            // Historical average typically hovers around currentPrice * 1.05
            estimatedHistoricalAvg = currentPrice.multiply(BigDecimal.valueOf(1.05)).setScale(2, RoundingMode.HALF_UP);
            estimated30DayLow = currentPrice.multiply(BigDecimal.valueOf(0.98)).setScale(2, RoundingMode.HALF_UP);
            estimated90DayLow = currentPrice.multiply(BigDecimal.valueOf(0.95)).setScale(2, RoundingMode.HALF_UP);
        } else {
            estimatedHistoricalAvg = currentPrice;
            estimated30DayLow = currentPrice;
            estimated90DayLow = currentPrice;
        }

        return DealScoreCalculator.calculate(
                currentPrice,
                estimatedHistoricalAvg,
                estimated30DayLow,
                estimated90DayLow,
                originalPrice
        );
    }

    @Override
    public DealQualityDto calculateDealQuality(
            BigDecimal currentPrice,
            BigDecimal historicalAverage,
            BigDecimal thirtyDayLow,
            BigDecimal ninetyDayLow,
            BigDecimal advertisedOriginalPrice) {

        return DealScoreCalculator.calculate(
                currentPrice,
                historicalAverage,
                thirtyDayLow,
                ninetyDayLow,
                advertisedOriginalPrice
        );
    }
}
