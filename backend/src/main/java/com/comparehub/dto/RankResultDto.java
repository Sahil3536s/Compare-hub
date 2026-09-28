package com.comparehub.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RankResultDto {

    private Integer rank;
    private Double score;
    private String label; // "BEST_VALUE", "CHEAPEST", "HIGHEST_RATED", "FASTEST_DELIVERY"

    @Builder.Default
    private List<String> reasons = new ArrayList<>();

    private String explanation;

    // Transparency scores (0.0 to 1.0)
    private Double priceScore;
    private Double ratingScore;
    private Double deliveryScore;
    private Double discountScore;
    private Double availabilityScore;
    private Double finalScore;
}
