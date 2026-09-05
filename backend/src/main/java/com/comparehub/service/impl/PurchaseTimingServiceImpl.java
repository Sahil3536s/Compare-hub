package com.comparehub.service.impl;

import com.comparehub.dto.NormalizedProductOfferDto;
import com.comparehub.dto.PricePointDto;
import com.comparehub.dto.PurchaseTimingDto;
import com.comparehub.service.PurchaseTimingService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
public class PurchaseTimingServiceImpl implements PurchaseTimingService {

    @Override
    public PurchaseTimingDto analyzeTiming(BigDecimal currentPrice, List<PricePointDto> pricePoints) {
        if (currentPrice == null || pricePoints == null || pricePoints.size() < 2) {
            return PurchaseTimingDto.builder()
                    .score(50)
                    .status("INSUFFICIENT_DATA")
                    .statusLabel("Insufficient Data")
                    .confidence("LOW")
                    .currentPrice(currentPrice)
                    .reasons(List.of("Insufficient historical price records to determine timing confidence."))
                    .build();
        }

        // Sort price points chronologically
        List<PricePointDto> sorted = pricePoints.stream()
                .filter(p -> p.getPrice() != null && p.getRecordedAt() != null)
                .sorted(Comparator.comparing(PricePointDto::getRecordedAt))
                .collect(Collectors.toList());

        if (sorted.size() < 2) {
            return PurchaseTimingDto.builder()
                    .score(50)
                    .status("INSUFFICIENT_DATA")
                    .statusLabel("Insufficient Data")
                    .confidence("LOW")
                    .currentPrice(currentPrice)
                    .reasons(List.of("Insufficient historical price records to determine timing confidence."))
                    .build();
        }

        Instant now = Instant.now();
        Instant sevenDaysAgo = now.minus(7, ChronoUnit.DAYS).minus(5, ChronoUnit.MINUTES);
        Instant thirtyDaysAgo = now.minus(30, ChronoUnit.DAYS).minus(5, ChronoUnit.MINUTES);

        // 1. Averages and Lows
        List<PricePointDto> last30 = sorted.stream()
                .filter(p -> p.getRecordedAt().isAfter(thirtyDaysAgo) || p.getRecordedAt().equals(thirtyDaysAgo))
                .collect(Collectors.toList());

        if (last30.isEmpty()) last30 = sorted;

        BigDecimal sum30 = last30.stream().map(PricePointDto::getPrice).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal thirtyDayAvg = sum30.divide(BigDecimal.valueOf(last30.size()), 2, RoundingMode.HALF_UP);

        List<PricePointDto> last7 = sorted.stream()
                .filter(p -> p.getRecordedAt().isAfter(sevenDaysAgo) || p.getRecordedAt().equals(sevenDaysAgo))
                .collect(Collectors.toList());
        if (last7.isEmpty()) last7 = last30;

        BigDecimal sum7 = last7.stream().map(PricePointDto::getPrice).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal sevenDayAvg = sum7.divide(BigDecimal.valueOf(last7.size()), 2, RoundingMode.HALF_UP);

        BigDecimal ninetyDayLow = sorted.stream().map(PricePointDto::getPrice).min(BigDecimal::compareTo).orElse(currentPrice);

        // 2. Trend & Movement
        PricePointDto baselineRecent = last7.get(0);
        BigDecimal recentStartPrice = baselineRecent.getPrice();
        String priceTrend;
        double trendDiffPct = 0;

        if (recentStartPrice.compareTo(BigDecimal.ZERO) > 0) {
            trendDiffPct = ((currentPrice.doubleValue() - recentStartPrice.doubleValue()) / recentStartPrice.doubleValue()) * 100.0;
        }

        if (trendDiffPct <= -1.5) {
            priceTrend = "DECREASING";
        } else if (trendDiffPct >= 1.5) {
            priceTrend = "INCREASING";
        } else {
            priceTrend = "STABLE";
        }

        // 3. Compute Score & Reasons
        double score = 50.0;
        List<String> reasons = new ArrayList<>();

        // Comparison vs 30-day average
        if (thirtyDayAvg.compareTo(BigDecimal.ZERO) > 0) {
            double diffVs30Avg = ((thirtyDayAvg.doubleValue() - currentPrice.doubleValue()) / thirtyDayAvg.doubleValue()) * 100.0;
            int roundedDiff = (int) Math.round(Math.abs(diffVs30Avg));

            if (diffVs30Avg >= 5.0) {
                score += Math.min(25.0, diffVs30Avg * 2.0);
                reasons.add(String.format("%d%% below the 30-day average price", roundedDiff));
            } else if (diffVs30Avg <= -5.0) {
                score -= Math.min(25.0, Math.abs(diffVs30Avg) * 2.0);
                reasons.add(String.format("%d%% above the recent 30-day average", roundedDiff));
            } else {
                reasons.add("Trading within normal 30-day price band");
            }
        }

        // Proximity to 90-day low
        if (currentPrice.compareTo(ninetyDayLow) <= 0) {
            score += 20.0;
            reasons.add("At or near 90-day lowest price");
        } else if (currentPrice.doubleValue() <= ninetyDayLow.doubleValue() * 1.03) {
            score += 10.0;
            reasons.add("Close to 90-day low");
        }

        // Weekly Movement
        if ("DECREASING".equals(priceTrend)) {
            score += 10.0;
            reasons.add("Price decreased during the last week");
        } else if ("INCREASING".equals(priceTrend)) {
            score -= 15.0;
            reasons.add("Price recently spiked upwards");
        }

        int finalScore = (int) Math.round(Math.max(0.0, Math.min(100.0, score)));

        // 4. Status
        String status;
        String statusLabel;

        if (finalScore >= 85) {
            status = "STRONG_BUY_PRICE";
            statusLabel = "Strong Buy Price";
        } else if (finalScore >= 70) {
            status = "GOOD_TIME_TO_BUY";
            statusLabel = "Good Time to Buy";
        } else if (finalScore >= 50) {
            status = "NEUTRAL";
            statusLabel = "Fair Market Price";
        } else {
            status = "CONSIDER_WAITING";
            statusLabel = "Consider Waiting";
        }

        // 5. Confidence
        String confidence = sorted.size() >= 10 ? "HIGH" : sorted.size() >= 4 ? "MEDIUM" : "LOW";

        return PurchaseTimingDto.builder()
                .score(finalScore)
                .status(status)
                .statusLabel(statusLabel)
                .confidence(confidence)
                .currentPrice(currentPrice)
                .sevenDayAvg(sevenDayAvg)
                .thirtyDayAvg(thirtyDayAvg)
                .ninetyDayLow(ninetyDayLow)
                .priceTrend(priceTrend)
                .reasons(reasons)
                .build();
    }

    @Override
    public PurchaseTimingDto evaluateOfferTiming(NormalizedProductOfferDto offer) {
        if (offer == null || offer.getPrice() == null) {
            return null;
        }

        BigDecimal current = offer.getEffectivePrice() != null ? offer.getEffectivePrice() : offer.getPrice();
        BigDecimal original = offer.getOriginalPrice() != null ? offer.getOriginalPrice() : current;

        // Construct baseline points for ad-hoc offers
        Instant now = Instant.now();
        List<PricePointDto> points = new ArrayList<>();
        points.add(PricePointDto.builder().price(original.compareTo(current) > 0 ? current.multiply(BigDecimal.valueOf(1.08)) : current).recordedAt(now.minus(14, ChronoUnit.DAYS)).build());
        points.add(PricePointDto.builder().price(original.compareTo(current) > 0 ? current.multiply(BigDecimal.valueOf(1.04)) : current).recordedAt(now.minus(7, ChronoUnit.DAYS)).build());
        points.add(PricePointDto.builder().price(current).recordedAt(now).build());

        return analyzeTiming(current, points);
    }
}
