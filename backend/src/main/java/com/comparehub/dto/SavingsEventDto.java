package com.comparehub.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SavingsEventDto {

    private Long id;
    private String title;
    private String category;
    private String eventType; // "POTENTIAL" or "CONFIRMED"
    private BigDecimal selectedPrice;
    private BigDecimal baselinePrice;
    private BigDecimal savingAmount;
    private String merchantOrProvider;
    private String notes;
    private Instant createdAt;
}
