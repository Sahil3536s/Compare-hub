package com.comparehub.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GroupTravelRequestDto {

    @NotBlank(message = "Origin is required")
    private String origin;

    @NotBlank(message = "Destination is required")
    private String destination;

    private String travelDate;

    @Min(value = 1, message = "Number of travelers must be at least 1")
    @Builder.Default
    private int numberOfTravelers = 1;

    private Double budget;

    @Builder.Default
    private String priority = "BALANCED"; // CHEAPEST, FASTEST, BALANCED
}
