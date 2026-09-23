package com.comparehub.service.impl;

import com.comparehub.client.MLServiceClient;
import com.comparehub.dto.MLServiceResponseDto;
import com.comparehub.dto.PricePredictionResponseDto;
import com.comparehub.exception.ResourceNotFoundException;
import com.comparehub.model.ProductPriceHistory;
import com.comparehub.repository.ProductPriceHistoryRepository;
import com.comparehub.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class PricePredictionServiceImpl implements com.comparehub.service.PricePredictionService {

    private final ProductPriceHistoryRepository priceHistoryRepository;
    private final ProductRepository productRepository;
    private final MLServiceClient mlServiceClient;

    @Value("${app.ml.min-history-points:20}")
    private int minHistoryPoints;

    private static final DateTimeFormatter DATE_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd").withZone(ZoneId.systemDefault());

    @Override
    @Transactional(readOnly = true)
    public PricePredictionResponseDto getPricePrediction(Long productId) {
        // 1. Validate product exists
        var product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + productId));

        // 2. Fetch ALL real price history (chronological, no period filter)
        //    We use the full history here — not a windowed query — so the ML service
        //    gets the maximum available data for feature engineering.
        List<ProductPriceHistory> history = priceHistoryRepository
                .findByProductIdOrderByRecordedAtAsc(productId);

        // 3. Check minimum data requirement BEFORE calling ML service
        if (history.size() < minHistoryPoints) {
            log.info("Insufficient price history for product {} ({} points, need {})",
                    productId, history.size(), minHistoryPoints);
            return PricePredictionResponseDto.builder()
                    .status("INSUFFICIENT_DATA")
                    .productId(productId)
                    .productName(product.getName())
                    .dataPointsUsed(history.size())
                    .message(String.format(
                            "More price history is required before a reliable prediction can be generated. "
                            + "Currently have %d observations; minimum required is %d.",
                            history.size(), minHistoryPoints))
                    .build();
        }

        // 4. Map history to price points for ML service
        List<Map<String, Object>> pricePoints = history.stream()
                .map(h -> Map.<String, Object>of(
                        "date", DATE_FORMATTER.format(h.getRecordedAt()),
                        "price", h.getPrice().doubleValue(),
                        "merchant", h.getMerchant()
                ))
                .toList();

        // 5. Call ML service (with Resilience4j circuit breaker via MLServiceClient)
        Optional<MLServiceResponseDto> mlResponseOpt = mlServiceClient.predict(productId, pricePoints);

        // 6. Handle ML service unavailability gracefully
        if (mlResponseOpt.isEmpty()) {
            log.warn("ML service unavailable or circuit open for product {}", productId);
            return PricePredictionResponseDto.builder()
                    .status("ML_UNAVAILABLE")
                    .productId(productId)
                    .productName(product.getName())
                    .message("Price prediction service is temporarily unavailable. " +
                             "All other comparison features continue to work normally.")
                    .build();
        }

        MLServiceResponseDto mlResponse = mlResponseOpt.get();

        // 7. Map ML response to frontend DTO
        return mapToResponseDto(mlResponse, productId, product.getName());
    }

    private PricePredictionResponseDto mapToResponseDto(
            MLServiceResponseDto ml, Long productId, String productName) {

        var builder = PricePredictionResponseDto.builder()
                .status(ml.getStatus())
                .productId(productId)
                .productName(productName)
                .dataPointsUsed(ml.getDataPointsUsed())
                .message(ml.getMessage())
                .modelName(ml.getModelName())
                .modelVersion(ml.getModelVersion())
                .recommendation(ml.getRecommendation())
                .recommendationReason(ml.getRecommendationReason())
                .confidenceLabel(ml.getConfidenceLabel())
                .dealQuality(ml.getDealQuality());

        if (ml.getCurrentPrice() != null) {
            builder.currentPrice(BigDecimal.valueOf(ml.getCurrentPrice()));
        }
        if (ml.getPredictedPrice7d() != null) {
            builder.predictedPrice7d(BigDecimal.valueOf(ml.getPredictedPrice7d()));
        }
        if (ml.getPredictedChange() != null) {
            builder.predictedChange(BigDecimal.valueOf(ml.getPredictedChange()));
        }
        if (ml.getPredictedChangePercent() != null) {
            builder.predictedChangePercent(ml.getPredictedChangePercent());
        }
        if (ml.getPredictedPriceLow() != null) {
            builder.predictedPriceLow(BigDecimal.valueOf(ml.getPredictedPriceLow()));
        }
        if (ml.getPredictedPriceHigh() != null) {
            builder.predictedPriceHigh(BigDecimal.valueOf(ml.getPredictedPriceHigh()));
        }

        return builder.build();
    }
}
