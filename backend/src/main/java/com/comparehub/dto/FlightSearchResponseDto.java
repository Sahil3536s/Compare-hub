package com.comparehub.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FlightSearchResponseDto {
    private Long id;
    private Long userId;
    private String fromAirport;
    private String toAirport;
    private LocalDate departureDate;
    private LocalDate returnDate;
    private Integer passengers;
    private String cabinClass;
    private Instant createdAt;
}
