package com.comparehub.dto;

import com.comparehub.model.JourneySegmentType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class JourneySegmentDto {

    private String id;
    private JourneySegmentType type;
    private String typeName;
    private String provider;
    private String origin;
    private String destination;
    private String departureTime;
    private String arrivalTime;
    private BigDecimal price;
    private int durationMinutes;
    private String formattedDuration;
    private String icon;
    private Map<String, String> metadata;
}
