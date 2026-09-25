package com.comparehub.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NormalizedFlightOfferDto {

    private String provider;
    private String airline;
    private String flightNumber;
    private String origin;
    private String destination;
    private String departure; // e.g. "06:15" or ISO timestamp
    private String arrival; // e.g. "08:30"
    private Integer durationMinutes;
    private Integer stops;
    private BigDecimal price;
    @Builder.Default
    private String currency = "INR";
    private String bookingUrl;
    private String cabinBaggage; // e.g. "7 kg Cabin"
    private String checkInBaggage; // e.g. "15 kg Check-in"
    private String airlineLogo; // e.g. "IndiGo" or SVG/URL

    // Badges calculated by FlightRankingService
    @Builder.Default
    private Boolean isCheapest = false;
    @Builder.Default
    private Boolean isFastest = false;
    @Builder.Default
    private Boolean isBest = false;
    private Double score;
}
