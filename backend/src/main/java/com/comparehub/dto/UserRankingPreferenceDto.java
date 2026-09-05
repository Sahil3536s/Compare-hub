package com.comparehub.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserRankingPreferenceDto {

    private Long userId;

    @Builder.Default
    private String preset = "BALANCED"; // "BALANCED", "CHEAPEST", "FASTEST", "BEST_RATED", "CUSTOM"

    @Builder.Default
    private ProductRankingWeightsDto product = new ProductRankingWeightsDto();

    @Builder.Default
    private FlightRankingWeightsDto flight = new FlightRankingWeightsDto();

    @Builder.Default
    private RideRankingWeightsDto ride = new RideRankingWeightsDto();
}
