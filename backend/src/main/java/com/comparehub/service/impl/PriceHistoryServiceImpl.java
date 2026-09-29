package com.comparehub.service.impl;

import com.comparehub.dto.DealQualityDto;
import com.comparehub.dto.PricePointDto;
import com.comparehub.dto.ProductPriceHistoryResponseDto;
import com.comparehub.dto.PurchaseTimingDto;
import com.comparehub.exception.ResourceNotFoundException;
import com.comparehub.model.Product;
import com.comparehub.model.ProductPriceHistory;
import com.comparehub.repository.ProductPriceHistoryRepository;
import com.comparehub.repository.ProductRepository;
import com.comparehub.service.DealQualityService;
import com.comparehub.service.PriceHistoryService;
import com.comparehub.service.PurchaseTimingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class PriceHistoryServiceImpl implements PriceHistoryService {

    private final ProductPriceHistoryRepository priceHistoryRepository;
    private final ProductRepository productRepository;
    private final DealQualityService dealQualityService;
    private final PurchaseTimingService purchaseTimingService;

    public PriceHistoryServiceImpl(ProductPriceHistoryRepository priceHistoryRepository, ProductRepository productRepository) {
        this(priceHistoryRepository, productRepository, null, null);
    }

    @Override
    @Transactional
    public void recordPriceIfChanged(Product product, String merchant, BigDecimal price, String currency) {
        if (product == null || price == null || merchant == null || merchant.isBlank()) {
            return;
        }

        Optional<ProductPriceHistory> latestOpt = priceHistoryRepository
                .findTopByProductIdAndMerchantOrderByRecordedAtDesc(product.getId(), merchant.trim());

        if (latestOpt.isPresent()) {
            ProductPriceHistory latest = latestOpt.get();
            boolean priceUnchanged = latest.getPrice().compareTo(price) == 0;
            boolean recordedRecently = latest.getRecordedAt().isAfter(Instant.now().minus(24, ChronoUnit.HOURS));

            if (priceUnchanged && recordedRecently) {
                // Deduplication: Price is identical and within 24 hours, skip insert
                return;
            }
        }

        ProductPriceHistory entry = ProductPriceHistory.builder()
                .product(product)
                .merchant(merchant.trim())
                .price(price)
                .currency(currency != null ? currency : "INR")
                .recordedAt(Instant.now())
                .build();

        priceHistoryRepository.save(entry);
        log.debug("Recorded price history for product {} at {}: {}", product.getId(), merchant, price);
    }

    @Override
    @Transactional(readOnly = true)
    public ProductPriceHistoryResponseDto getPriceHistory(Long productId, String period) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + productId));

        String sanitizedPeriod = period != null ? period.toUpperCase().trim() : "90D";
        Integer days = switch (sanitizedPeriod) {
            case "7D" -> 7;
            case "30D" -> 30;
            case "90D" -> 90;
            case "6M" -> 180;
            case "1Y" -> 365;
            case "ALL" -> null;
            default -> {
                sanitizedPeriod = "90D";
                yield 90;
            }
        };

        List<ProductPriceHistory> history;
        if (days != null) {
            Instant since = Instant.now().minus(days, ChronoUnit.DAYS);
            history = priceHistoryRepository
                    .findByProductIdAndRecordedAtGreaterThanEqualOrderByRecordedAtAsc(productId, since);
        } else {
            history = priceHistoryRepository
                    .findByProductIdOrderByRecordedAtAsc(productId);
        }

        List<PricePointDto> pricePoints = new ArrayList<>();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd").withZone(ZoneId.systemDefault());

        if (!history.isEmpty()) {
            pricePoints = history.stream()
                    .map(h -> PricePointDto.builder()
                            .date(formatter.format(h.getRecordedAt()))
                            .price(h.getPrice())
                            .merchant(h.getMerchant())
                            .recordedAt(h.getRecordedAt())
                            .build())
                    .collect(Collectors.toList());
        } else {
            return ProductPriceHistoryResponseDto.builder()
                    .productId(productId)
                    .productName(product.getName())
                    .period(sanitizedPeriod)
                    .currentPrice(product.getOffers() != null && !product.getOffers().isEmpty() ? product.getOffers().get(0).getPrice() : BigDecimal.ZERO)
                    .lowestPrice(BigDecimal.ZERO)
                    .highestPrice(BigDecimal.ZERO)
                    .averagePrice(BigDecimal.ZERO)
                    .currency("INR")
                    .analysisText("Price history is not available yet.")
                    .pricePoints(Collections.emptyList())
                    .build();
        }

        // Calculate statistics
        BigDecimal currentPrice = pricePoints.get(pricePoints.size() - 1).getPrice();
        BigDecimal lowestPrice = pricePoints.stream().map(PricePointDto::getPrice).min(BigDecimal::compareTo).orElse(currentPrice);
        BigDecimal highestPrice = pricePoints.stream().map(PricePointDto::getPrice).max(BigDecimal::compareTo).orElse(currentPrice);

        BigDecimal sum = pricePoints.stream().map(PricePointDto::getPrice).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal averagePrice = sum.divide(BigDecimal.valueOf(pricePoints.size()), 2, RoundingMode.HALF_UP);

        // Generate Analysis Text
        String analysisText = generateAnalysisText(currentPrice, averagePrice, sanitizedPeriod);

        // Calculate Deal Quality
        BigDecimal advertisedOriginalPrice = product.getOffers() != null && !product.getOffers().isEmpty() && product.getOffers().get(0).getOriginalPrice() != null
                ? product.getOffers().get(0).getOriginalPrice()
                : currentPrice.multiply(BigDecimal.valueOf(1.15));

        DealQualityDto dealQuality = dealQualityService.calculateDealQuality(
                currentPrice,
                averagePrice,
                lowestPrice,
                lowestPrice,
                advertisedOriginalPrice
        );

        // Calculate Purchase Timing Decision
        PurchaseTimingDto purchaseTiming = purchaseTimingService.analyzeTiming(currentPrice, pricePoints);

        return ProductPriceHistoryResponseDto.builder()
                .productId(productId)
                .productName(product.getName())
                .period(sanitizedPeriod)
                .currentPrice(currentPrice)
                .lowestPrice(lowestPrice)
                .highestPrice(highestPrice)
                .averagePrice(averagePrice)
                .currency("INR")
                .analysisText(analysisText)
                .pricePoints(pricePoints)
                .dealQuality(dealQuality)
                .purchaseTiming(purchaseTiming)
                .build();
    }

    private String generateAnalysisText(BigDecimal currentPrice, BigDecimal averagePrice, String period) {
        if (averagePrice.compareTo(BigDecimal.ZERO) == 0) {
            return "Current price is stable.";
        }

        double current = currentPrice.doubleValue();
        double avg = averagePrice.doubleValue();
        double diffPct = ((current - avg) / avg) * 100.0;
        long roundedDiff = Math.abs(Math.round(diffPct));

        String periodLabel = switch (period) {
            case "7D" -> "7-day";
            case "30D" -> "30-day";
            case "90D" -> "90-day";
            case "6M" -> "6-month";
            case "1Y" -> "1-year";
            case "ALL" -> "historical";
            default -> "90-day";
        };

        if (diffPct <= -1.5) {
            return String.format("Current price is %d%% below the %s average.", roundedDiff, periodLabel);
        } else if (diffPct >= 1.5) {
            return String.format("Current price is %d%% above the %s average.", roundedDiff, periodLabel);
        } else {
            return String.format("Current price is stable and matches the %s average.", periodLabel);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public com.comparehub.dto.PriceMeterDto calculatePriceMeter(Long productId, BigDecimal currentPrice, String period) {
        String sanitizedPeriod = period != null ? period.toUpperCase().trim() : "90D";
        Integer days = switch (sanitizedPeriod) {
            case "7D" -> 7;
            case "30D" -> 30;
            case "90D" -> 90;
            case "6M" -> 180;
            case "1Y" -> 365;
            case "ALL" -> null;
            default -> {
                sanitizedPeriod = "90D";
                yield 90;
            }
        };

        List<ProductPriceHistory> history;
        if (days != null) {
            Instant since = Instant.now().minus(days, ChronoUnit.DAYS);
            history = priceHistoryRepository
                    .findByProductIdAndRecordedAtGreaterThanEqualOrderByRecordedAtAsc(productId, since);
        } else {
            history = priceHistoryRepository
                    .findByProductIdOrderByRecordedAtAsc(productId);
        }

        if (history == null || history.isEmpty()) {
            return com.comparehub.dto.PriceMeterDto.builder()
                    .classification("INSUFFICIENT_DATA")
                    .classificationLabel("Insufficient Data")
                    .currentPrice(currentPrice)
                    .period(sanitizedPeriod)
                    .observationsCount(0)
                    .hasSufficientData(false)
                    .summaryText("Not enough price history yet to calculate Price Meter.")
                    .build();
        }

        List<BigDecimal> prices = history.stream()
                .map(ProductPriceHistory::getPrice)
                .filter(Objects::nonNull)
                .sorted()
                .collect(Collectors.toList());

        if (prices.isEmpty()) {
            return com.comparehub.dto.PriceMeterDto.builder()
                    .classification("INSUFFICIENT_DATA")
                    .classificationLabel("Insufficient Data")
                    .currentPrice(currentPrice)
                    .period(sanitizedPeriod)
                    .observationsCount(0)
                    .hasSufficientData(false)
                    .summaryText("Not enough price history yet to calculate Price Meter.")
                    .build();
        }

        BigDecimal effectiveCurrent = currentPrice != null ? currentPrice : prices.get(prices.size() - 1);
        BigDecimal min = prices.get(0);
        BigDecimal max = prices.get(prices.size() - 1);

        BigDecimal sum = prices.stream().reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal avg = sum.divide(BigDecimal.valueOf(prices.size()), 2, RoundingMode.HALF_UP);

        BigDecimal median;
        int n = prices.size();
        if (n % 2 == 1) {
            median = prices.get(n / 2);
        } else {
            median = prices.get(n / 2 - 1).add(prices.get(n / 2))
                    .divide(BigDecimal.valueOf(2), 2, RoundingMode.HALF_UP);
        }

        double currVal = effectiveCurrent.doubleValue();
        double avgVal = avg.doubleValue();
        double diffPct = avgVal > 0 ? ((currVal - avgVal) / avgVal) * 100.0 : 0.0;
        double roundedDiffPct = Math.round(diffPct * 10.0) / 10.0;

        String classification;
        String classificationLabel;
        String summaryText;

        String periodLabel = switch (sanitizedPeriod) {
            case "7D" -> "7-day";
            case "30D" -> "30-day";
            case "90D" -> "90-day";
            case "6M" -> "6-month";
            case "1Y" -> "1-year";
            case "ALL" -> "historical";
            default -> "90-day";
        };

        if (diffPct <= -10.0 || effectiveCurrent.compareTo(min) <= 0) {
            classification = "EXCELLENT_DEAL";
            classificationLabel = "Excellent Deal";
            summaryText = String.format("Current price is %.1f%% lower than its %s average.", Math.abs(roundedDiffPct), periodLabel);
        } else if (diffPct <= -3.0) {
            classification = "GOOD_PRICE";
            classificationLabel = "Good Price";
            summaryText = String.format("Current price is %.1f%% lower than its %s average.", Math.abs(roundedDiffPct), periodLabel);
        } else if (diffPct <= 3.0) {
            classification = "AVERAGE_PRICE";
            classificationLabel = "Average Price";
            summaryText = String.format("Current price matches its %s average.", periodLabel);
        } else if (diffPct <= 10.0) {
            classification = "ABOVE_AVERAGE";
            classificationLabel = "Above Average";
            summaryText = String.format("Current price is %.1f%% higher than its %s average.", roundedDiffPct, periodLabel);
        } else {
            classification = "HIGH_PRICE";
            classificationLabel = "High Price";
            summaryText = String.format("Current price is %.1f%% higher than its %s average.", roundedDiffPct, periodLabel);
        }

        return com.comparehub.dto.PriceMeterDto.builder()
                .classification(classification)
                .classificationLabel(classificationLabel)
                .currentPrice(effectiveCurrent)
                .historicalMinimum(min)
                .historicalMaximum(max)
                .historicalAverage(avg)
                .historicalMedian(median)
                .percentDifferenceFromAverage(roundedDiffPct)
                .summaryText(summaryText)
                .period(sanitizedPeriod)
                .observationsCount(prices.size())
                .hasSufficientData(true)
                .build();
    }
}
