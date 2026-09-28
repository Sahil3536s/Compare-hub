package com.comparehub.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties(prefix = "app.ranking")
public class RankingConfigProperties {

    private ProductWeights product = new ProductWeights();
    private FlightWeights flight = new FlightWeights();
    private RideWeights ride = new RideWeights();

    @Data
    public static class ProductWeights {
        private double priceWeight = 0.40;
        private double ratingWeight = 0.20;
        private double discountWeight = 0.15;
        private double deliveryWeight = 0.15;
        private double trustWeight = 0.10;
    }

    @Data
    public static class FlightWeights {
        private double priceWeight = 0.50;
        private double durationWeight = 0.30;
        private double stopsWeight = 0.20;
    }

    @Data
    public static class RideWeights {
        private double fareWeight = 0.60;
        private double etaWeight = 0.40;
    }
}
