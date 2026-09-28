package com.comparehub.config;

import jakarta.annotation.PostConstruct;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Slf4j
@Data
@Configuration
@ConfigurationProperties(prefix = "comparison.ranking")
public class ComparisonRankingProperties {

    private double priceWeight = 0.45;
    private double ratingWeight = 0.25;
    private double deliveryWeight = 0.15;
    private double discountWeight = 0.10;
    private double availabilityWeight = 0.05;

    // Prior rating and confidence threshold for Bayesian rating damping
    private double priorRating = 3.5;
    private double confidenceThreshold = 50.0;

    @PostConstruct
    public void validateWeights() {
        double sum = priceWeight + ratingWeight + deliveryWeight + discountWeight + availabilityWeight;
        if (Math.abs(sum - 1.0) > 0.02) {
            log.warn("Comparison ranking weights do not sum to 1.0 (actual sum: {}). Re-normalizing weights.", sum);
            priceWeight = priceWeight / sum;
            ratingWeight = ratingWeight / sum;
            deliveryWeight = deliveryWeight / sum;
            discountWeight = discountWeight / sum;
            availabilityWeight = availabilityWeight / sum;
        }
        log.info("Comparison ranking weights initialized: Price={}% Rating={}% Delivery={}% Discount={}% Availability={}%",
                (int) Math.round(priceWeight * 100),
                (int) Math.round(ratingWeight * 100),
                (int) Math.round(deliveryWeight * 100),
                (int) Math.round(discountWeight * 100),
                (int) Math.round(availabilityWeight * 100));
    }
}
