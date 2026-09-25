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
public class AirportResultDto {

    private String name;
    private String iataCode;
    private String cityName;
    private String countryName;
    private String airportType; // "AIRPORT" or "CITY"
    private Double latitude;
    private Double longitude;

    public String getDisplayName() {
        if (cityName != null && !cityName.isBlank() && iataCode != null && !iataCode.isBlank()) {
            return cityName + " (" + iataCode + ") - " + name;
        }
        return name;
    }
}
