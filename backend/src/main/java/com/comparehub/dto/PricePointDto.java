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
public class PricePointDto {

    private String date; // "YYYY-MM-DD" or ISO timestamp
    private BigDecimal price;
    private String merchant;
    private Instant recordedAt;
}
