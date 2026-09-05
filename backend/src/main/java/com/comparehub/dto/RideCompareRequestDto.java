package com.comparehub.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RideCompareRequestDto {

    @Valid
    @NotNull(message = "Pickup location is required")
    private LocationDto pickup;

    @Valid
    @NotNull(message = "Destination location is required")
    private LocationDto destination;

    @Builder.Default
    private String rideType = "all"; // "all", "cab", "auto", "bike"

    @Builder.Default
    private String sortBy = "best"; // "best", "cheapest", "fastest", "price_asc", "eta"
}
