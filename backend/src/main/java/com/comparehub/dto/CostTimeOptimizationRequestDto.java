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
public class CostTimeOptimizationRequestDto {

    @Builder.Default
    private double costWeight = 50.0; // 0 to 100 (% importance of saving money)

    @Builder.Default
    private double timeWeight = 50.0; // 0 to 100 (% importance of saving time)

    @Builder.Default
    private List<CostTimeOptionDto> options = new ArrayList<>();

    @Builder.Default
    private String domain = "GENERAL"; // "FLIGHTS", "RIDES", "GROUP_TRAVEL", "GENERAL"
}
