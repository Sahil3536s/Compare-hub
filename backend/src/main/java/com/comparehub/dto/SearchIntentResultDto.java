package com.comparehub.dto;

import com.comparehub.model.SearchIntent;
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
public class SearchIntentResultDto {

    private SearchIntent intent;
    private String originalQuery;
    private String query; // Cleaned query with extracted terms removed
    private double confidence;

    @Builder.Default
    private Map<String, Object> filters = new HashMap<>();

    @Builder.Default
    private Map<String, String> flightParams = new HashMap<>();

    @Builder.Default
    private Map<String, String> rideParams = new HashMap<>();
}
