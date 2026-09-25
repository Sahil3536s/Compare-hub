package com.comparehub.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NormalizedRideOfferDto {

    private String provider; // "Uber", "Ola", "Rapido"
    private String rideType; // "Uber Go", "Ola Mini", "Rapido Bike"
    private String vehicleCategory; // "Cab", "Auto", "Bike", "Premier"
    private BigDecimal estimatedPriceMin;
    private BigDecimal estimatedPriceMax;
    private Integer etaMinutes;
    private Double distanceKm;
    @Builder.Default
    private String currency = "INR";
    private String deepLink;
    private Integer durationMinutes; // Estimated journey duration
    @Builder.Default
    private Boolean isMock = true; // Indicates demo/simulation mode where authorized API is not active
    @Builder.Default
    private String dataSource = "Demo / Simulated Fare Model";

    // Badges calculated by RideRankingService
    @Builder.Default
    private Boolean isCheapest = false;
    @Builder.Default
    private Boolean isFastest = false; // Quickest ETA
    @Builder.Default
    private Boolean isBest = false; // Best composite balance
    private Double score;
}
