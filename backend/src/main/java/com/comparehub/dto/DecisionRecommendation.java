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
public class DecisionRecommendation {

    private String recommendedOption;
    private String summary;
    @Builder.Default
    private List<String> reasons = new ArrayList<>();
    @Builder.Default
    private List<String> tradeoffs = new ArrayList<>();
    private String confidence; // HIGH, MEDIUM, LOW
    @Builder.Default
    private boolean deterministic = true;
    private String contextType;
}
