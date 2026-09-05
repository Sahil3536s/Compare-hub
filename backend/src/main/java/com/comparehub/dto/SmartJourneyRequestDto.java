package com.comparehub.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SmartJourneyRequestDto {

    private String origin;
    private String destination;
    private String originCity;
    private String destinationCity;
    private String travelDate;
    private String preferredDepartureTime;
    @Builder.Default
    private int travelers = 1;
    @Builder.Default
    private int airportBufferMinutes = 90;
    @Builder.Default
    private double costWeight = 50.0;
    @Builder.Default
    private double timeWeight = 50.0;
}
