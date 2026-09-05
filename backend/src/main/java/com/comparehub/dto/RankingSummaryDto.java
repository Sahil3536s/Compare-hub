package com.comparehub.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.HashMap;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RankingSummaryDto {

    private String cheapest;
    private String bestValue;
    private String highestRated;
    private String fastest;
    @Builder.Default
    private Map<String, Double> weights = new HashMap<>();
    private String explanation;
}
