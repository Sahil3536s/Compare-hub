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
public class AiRecommendationDto {

    private String bestOverall;
    private String cheapest;
    private String recommendation;
    @Builder.Default
    private List<String> reasoningPoints = new ArrayList<>();
}
