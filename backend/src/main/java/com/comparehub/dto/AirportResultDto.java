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

    private String id;
    private String name;
    private String iataCode;
    private String cityName;
    private String countryName;
    private String countryCode;
    private String airportType; // "AIRPORT" or "CITY"
    private String type;        // "AIRPORT" or "CITY"
    private Double latitude;
    private Double longitude;
    private String displayName;

    public String getType() {
        return (type != null && !type.isBlank()) ? type : airportType;
    }

    public void setType(String type) {
        this.type = type;
        if (this.airportType == null || this.airportType.isBlank()) {
            this.airportType = type;
        }
    }

    public String getAirportType() {
        return (airportType != null && !airportType.isBlank()) ? airportType : type;
    }

    public void setAirportType(String airportType) {
        this.airportType = airportType;
        if (this.type == null || this.type.isBlank()) {
            this.type = airportType;
        }
    }

    public String getDisplayName() {
        if (displayName != null && !displayName.isBlank()) {
            return displayName;
        }
        if (cityName != null && !cityName.isBlank() && name != null && !name.equalsIgnoreCase(cityName)) {
            return cityName + " — " + name + (iataCode != null ? " — " + iataCode : "");
        }
        if (iataCode != null && !iataCode.isBlank()) {
            return iataCode + " — " + name + (cityName != null ? ", " + cityName : "");
        }
        return name;
    }

    public static class AirportResultDtoBuilder {
        public AirportResultDtoBuilder type(String type) {
            this.type = type;
            if (this.airportType == null) {
                this.airportType = type;
            }
            return this;
        }

        public AirportResultDtoBuilder airportType(String airportType) {
            this.airportType = airportType;
            if (this.type == null) {
                this.type = airportType;
            }
            return this;
        }
    }
}
