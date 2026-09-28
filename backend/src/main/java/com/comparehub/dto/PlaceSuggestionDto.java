package com.comparehub.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class PlaceSuggestionDto {

    private String placeId;
    private String mainText;
    private String secondaryText;
    private String fullAddress;
    private Double latitude;
    private Double longitude;

    // Structured fields matching LocationDto
    private String name;
    private String formattedAddress;
    private String city;
    private String state;
    private String country;
    private String providerPlaceId;

    public String getName() {
        return (name != null && !name.isBlank()) ? name : mainText;
    }

    public String getFormattedAddress() {
        return (formattedAddress != null && !formattedAddress.isBlank()) ? formattedAddress : fullAddress;
    }

    public String getProviderPlaceId() {
        return (providerPlaceId != null && !providerPlaceId.isBlank()) ? providerPlaceId : placeId;
    }
}
