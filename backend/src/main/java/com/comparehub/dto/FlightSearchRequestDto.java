package com.comparehub.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FlightSearchRequestDto {

    private Long userId;

    @NotBlank(message = "Origin airport code is required")
    private String origin; // e.g. "DEL"

    @NotBlank(message = "Destination airport code is required")
    private String destination; // e.g. "BOM"

    @NotBlank(message = "Departure date is required")
    private String departureDate; // "YYYY-MM-DD"

    private String returnDate;

    @Builder.Default
    private Integer adults = 1;

    @Builder.Default
    private String cabinClass = "ECONOMY"; // "ECONOMY", "PREMIUM_ECONOMY", "BUSINESS"

    // Optional Filter & Sorting Parameters
    private Integer maxStops; // 0 for non-stop, 1, 2
    private String airline; // "IndiGo", "Air India", "Vistara", "SpiceJet", "all"
    private BigDecimal maxPrice;
    private Integer maxDurationMinutes;
    private String timeOfDay; // "all", "morning", "afternoon", "evening", "night", "before_6am", "6am_12pm", "12pm_6pm", "after_6pm"
    @Builder.Default
    private String sortBy = "best"; // "cheapest", "fastest", "best", "price_asc", "price_desc"

    // Alias methods for compatibility with SearchHistoryService
    public String getFromAirport() {
        return origin;
    }

    public String getToAirport() {
        return destination;
    }

    public Integer getPassengers() {
        return adults;
    }
}
