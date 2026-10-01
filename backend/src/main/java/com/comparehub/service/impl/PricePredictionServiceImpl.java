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

        // 2a. Exclude mock/demo merchants from history
        List<ProductPriceHistory> genuineHistory = history.stream()
                .filter(h -> {
                    String merchant = h.getMerchant().toLowerCase();
                    return !(merchant.contains("amazon mock")
                            || merchant.contains("flipkart mock")
                            || merchant.contains("croma mock")
                            || merchant.contains("dummyjson")
                            || merchant.contains("openproductdata"));
                })
                .toList();

        // 3. Check minimum data requirement BEFORE calling ML service
        if (genuineHistory.size() < minHistoryPoints) {
            log.info("Insufficient genuine price history for product {} ({} points, need {})",
                    productId, genuineHistory.size(), minHistoryPoints);
            return PricePredictionResponseDto.builder()
                    .status("INSUFFICIENT_DATA")
                    .productId(productId)
                    .productName(product.getName())
                    .dataPointsUsed(genuineHistory.size())
                    .message(String.format(
                            "More price history is required before a reliable prediction can be generated. "
                            + "Currently have %d genuine observations; minimum required is %d.",
                            genuineHistory.size(), minHistoryPoints))
                    .build();
        }

        // 4. Map history to price points for ML service
        List<com.comparehub.dto.MLPricePointDto> pricePoints = genuineHistory.stream()
                .map(h -> com.comparehub.dto.MLPricePointDto.builder()
                        .date(DATE_FORMATTER.format(h.getRecordedAt()))
                        .price(h.getPrice().doubleValue())
                        .merchant(h.getMerchant())
                        .build())
                .toList();

        // 5. Call ML service (with Resilience4j circuit breaker via MLServiceClient)
        Optional<MLServiceResponseDto> mlResponseOpt = mlServiceClient.predict(productId, pricePoints);

        // 6. Handle ML service unavailability gracefully
        if (mlResponseOpt.isEmpty()) {
            log.warn("ML service unavailable or circuit open for product {}", productId);
            return PricePredictionResponseDto.builder()
                    .status("TEMPORARILY_UNAVAILABLE")
                    .productId(productId)
                    .productName(product.getName())
                    .message("Price prediction is temporarily unavailable. " +
                             "All other comparison features continue to work normally.")
                    .build();
        }

        MLServiceResponseDto mlResponse = mlResponseOpt.get();

        // 7. Validate ML response
        if ("INSUFFICIENT_DATA".equalsIgnoreCase(mlResponse.getStatus())) {
            return PricePredictionResponseDto.builder()
                    .status("INSUFFICIENT_DATA")
                    .productId(productId)
                    .productName(product.getName())
                    .dataPointsUsed(mlResponse.getDataPointsUsed())
                    .message("More price history is required before a reliable prediction can be generated.")
                    .build();
        }

        if (!"SUCCESS".equalsIgnoreCase(mlResponse.getStatus())) {
            log.warn("ML service returned non-successful status '{}' for product {}", mlResponse.getStatus(), productId);
            return PricePredictionResponseDto.builder()
                    .status("INVALID_RESPONSE")
                    .productId(productId)
                    .productName(product.getName())
                    .message("Invalid response received from prediction service.")
                    .build();
        }

        // Validate numerical integrity for SUCCESS status
        if (!isValidMLResponse(mlResponse)) {
            log.warn("ML service response failed numerical integrity checks for product {}: {}", productId, mlResponse);
            return PricePredictionResponseDto.builder()
                    .status("INVALID_RESPONSE")
                    .productId(productId)
                    .productName(product.getName())
                    .message("Prediction returned invalid or malformed data.")
                    .build();
        }

        // 8. Map ML response to frontend DTO
        return mapToResponseDto(mlResponse, productId, product.getName());
    }

    private boolean isValidMLResponse(MLServiceResponseDto ml) {
        // New contract validation – ensure required numeric fields are present and sensible
        if (ml.getStatus() == null) return false;
        if ("SUCCESS".equalsIgnoreCase(ml.getStatus())) {
            if (ml.getCurrentPrice() == null || ml.getPredictedPrice() == null) return false;
            if (ml.getCurrentPrice() <= 0 || ml.getPredictedPrice() <= 0) return false;
            if (Double.isNaN(ml.getCurrentPrice()) || Double.isInfinite(ml.getCurrentPrice())) return false;
            if (Double.isNaN(ml.getPredictedPrice()) || Double.isInfinite(ml.getPredictedPrice())) return false;
            if (ml.getPredictedChange() != null && (Double.isNaN(ml.getPredictedChange()) || Double.isInfinite(ml.getPredictedChange()))) return false;
            if (ml.getPredictedChangePercent() != null && (Double.isNaN(ml.getPredictedChangePercent()) || Double.isInfinite(ml.getPredictedChangePercent()))) return false;
        }
        return true;
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
                .recommendationReason(ml.getRecommendationReason());

        if (ml.getCurrentPrice() != null) {
            builder.currentPrice(java.math.BigDecimal.valueOf(ml.getCurrentPrice()));
        }
        if (ml.getPredictedPrice() != null) {
            builder.predictedPrice(java.math.BigDecimal.valueOf(ml.getPredictedPrice()));
        }
        if (ml.getPredictedChange() != null) {
            builder.predictedChange(java.math.BigDecimal.valueOf(ml.getPredictedChange()));
        }
        if (ml.getPredictedChangePercent() != null) {
            builder.predictedChangePercent(ml.getPredictedChangePercent());
        }
        return builder.build();
    }
}
