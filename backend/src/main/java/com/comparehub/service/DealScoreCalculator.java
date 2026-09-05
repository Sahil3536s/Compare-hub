package com.comparehub.service;

import com.comparehub.dto.DealQualityDto;

import java.math.BigDecimal;
import java.math.RoundingMode;

public class DealScoreCalculator {

    public static DealQualityDto calculate(
            BigDecimal currentPrice,
            BigDecimal historicalAverage,
            BigDecimal thirtyDayLow,
            BigDecimal ninetyDayLow,
            BigDecimal advertisedOriginalPrice) {

        if (currentPrice == null || currentPrice.compareTo(BigDecimal.ZERO) <= 0) {
            return DealQualityDto.builder()
                    .dealScore(50)
                    .classification("AVERAGE_PRICE")
                    .classificationLabel("Normal Market Price")
                    .summary("Insufficient pricing history.")
                    .build();
        }

        BigDecimal avg = historicalAverage != null && historicalAverage.compareTo(BigDecimal.ZERO) > 0
                ? historicalAverage
                : currentPrice;
        BigDecimal low30 = thirtyDayLow != null && thirtyDayLow.compareTo(BigDecimal.ZERO) > 0
                ? thirtyDayLow
                : currentPrice;
        BigDecimal low90 = ninetyDayLow != null && ninetyDayLow.compareTo(BigDecimal.ZERO) > 0
                ? ninetyDayLow
                : low30;

        // 1. Calculate Real Savings vs Historical Average
        BigDecimal realSavings = avg.subtract(currentPrice);
        int realDiscountPercent = 0;
        if (avg.compareTo(BigDecimal.ZERO) > 0) {
            realDiscountPercent = (int) Math.round(
                    (avg.subtract(currentPrice).doubleValue() / avg.doubleValue()) * 100.0);
        }

        // 2. Calculate Advertised Discount (MRP vs Current Price)
        int advertisedDiscountPercent = 0;
        if (advertisedOriginalPrice != null && advertisedOriginalPrice.compareTo(currentPrice) > 0) {
            advertisedDiscountPercent = (int) Math.round(
                    (advertisedOriginalPrice.subtract(currentPrice).doubleValue() / advertisedOriginalPrice.doubleValue()) * 100.0);
        }

        // 3. Compute 0-100 Deal Score
        double score = 50.0;

        // Factor A: Price vs Average (Max +/- 35 pts)
        double diffPct = ((avg.doubleValue() - currentPrice.doubleValue()) / avg.doubleValue()) * 100.0;
        if (diffPct > 0) {
            score += Math.min(35.0, diffPct * 2.0);
        } else {
            score -= Math.min(35.0, Math.abs(diffPct) * 2.5);
        }

        // Factor B: Proximity to Lows (Max +15 pts)
        boolean isAtLowest = false;
        if (currentPrice.compareTo(low90) <= 0) {
            score += 15.0;
            isAtLowest = true;
        } else if (currentPrice.compareTo(low30) <= 0) {
            score += 10.0;
            isAtLowest = true;
        } else if (currentPrice.doubleValue() <= low30.doubleValue() * 1.02) {
            score += 5.0;
        }

        int finalScore = (int) Math.round(Math.max(0.0, Math.min(100.0, score)));

        // 4. Classify
        String classification;
        String classificationLabel;

        if (finalScore >= 90) {
            classification = "EXCEPTIONAL_DEAL";
            classificationLabel = "Exceptional Deal";
        } else if (finalScore >= 75) {
            classification = "GREAT_DEAL";
            classificationLabel = "Great Deal";
        } else if (finalScore >= 60) {
            classification = "GOOD_DEAL";
            classificationLabel = "Good Deal";
        } else if (finalScore >= 40) {
            classification = "AVERAGE_PRICE";
            classificationLabel = "Normal Market Price";
        } else {
            classification = "EXPENSIVE";
            classificationLabel = "Above Average Price";
        }

        // 5. Generate Safe & Objective Explanations
        String summary;
        String disclaimer = null;

        if (isAtLowest) {
            summary = "Current price is at or near its 90-day lowest record.";
        } else if (realDiscountPercent > 5) {
            summary = String.format("Current price is %d%% below the historical market average.", realDiscountPercent);
        } else if (realDiscountPercent < -5) {
            summary = String.format("Current price is %d%% above its recent historical average.", Math.abs(realDiscountPercent));
        } else {
            summary = "Current price is within its normal historical trading range.";
        }

        // Safe disclaimer if advertised discount is substantially higher than actual saving vs market average
        if (advertisedDiscountPercent >= 20 && realDiscountPercent <= 4) {
            disclaimer = String.format(
                    "Advertised discount is high (%d%% vs MRP), but the current price is close to its recent average (real saving: %d%%).",
                    advertisedDiscountPercent, Math.max(0, realDiscountPercent));
        }

        return DealQualityDto.builder()
                .dealScore(finalScore)
                .classification(classification)
                .classificationLabel(classificationLabel)
                .currentPrice(currentPrice)
                .historicalAverage(avg)
                .thirtyDayLow(low30)
                .ninetyDayLow(low90)
                .historicalMedian(avg)
                .advertisedOriginalPrice(advertisedOriginalPrice)
                .advertisedDiscountPercent(advertisedDiscountPercent)
                .realDiscountPercentVsAverage(realDiscountPercent)
                .realSavingsVsAverage(realSavings.max(BigDecimal.ZERO))
                .summary(summary)
                .disclaimer(disclaimer)
                .isAtLowest(isAtLowest)
                .build();
    }
}
