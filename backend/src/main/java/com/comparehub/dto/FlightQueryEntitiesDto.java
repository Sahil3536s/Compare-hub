package com.comparehub.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FlightQueryEntitiesDto {

    private String origin; // e.g. "DEL"
    private String destination; // e.g. "BLR"
    private String departureDate; // e.g. "2026-09-11"
    private Integer stops; // 0 for non-stop, 1 for 1 stop
    private String timePreference; // "MORNING", "AFTERNOON", "EVENING", "NIGHT", "ANY"
    private String sortPreference; // "PRICE", "DURATION", "BEST"
}
