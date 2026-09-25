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
public class LocationDto {

    private String name;
    private String formattedAddress;
    private Double latitude;
    private Double longitude;
    private String address;
    private String city;
    private String state;
    private String country;
    private String providerPlaceId;

    public String getFormattedAddress() {
        if (formattedAddress != null && !formattedAddress.isBlank()) {
            return formattedAddress;
        }
        return address;
    }

    public String getAddress() {
        if (address != null && !address.isBlank()) {
            return address;
        }
        return formattedAddress;
    }

    public String getName() {
        if (name != null && !name.isBlank()) {
            return name;
        }
        if (address != null && !address.isBlank()) {
            return address.split(",")[0].trim();
        }
        if (formattedAddress != null && !formattedAddress.isBlank()) {
            return formattedAddress.split(",")[0].trim();
        }
        return "Location";
    }
}
