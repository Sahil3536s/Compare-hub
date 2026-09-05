package com.comparehub.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PlaceSuggestionDto {

    private String placeId;
    private String mainText;
    private String secondaryText;
    private String fullAddress;
    private Double latitude;
    private Double longitude;
}
