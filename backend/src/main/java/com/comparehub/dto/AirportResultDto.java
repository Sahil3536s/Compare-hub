package com.comparehub.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class AirportResultDto implements Serializable {

    private String name;
    private String iataCode;
    private String cityName;
    private String countryName;
    private String airportType; // "AIRPORT" or "CITY"
    private Double latitude;
    private Double longitude;
    private String displayName;

    public String getDisplayName() {
        if (displayName != null && !displayName.isBlank()) {
            return displayName;
        }
        if (iataCode != null && !iataCode.isBlank()) {
            return iataCode + " — " + name + (cityName != null ? ", " + cityName : "");
        }
        return name;
    }
}
