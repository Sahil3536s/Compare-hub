package com.comparehub.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RideQueryEntitiesDto {

    private String pickup; // e.g. "VIT Bhopal", "Connaught Place"
    private String destination; // e.g. "Bhopal Airport"
    private String rideType; // "bike", "auto", "cab", "all"
    private String sortPreference; // "PRICE", "ETA", "CHEAPEST"
}
