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
public class PriceAlertResponseDto {
    private Long id;
    private Long userId;
    private String userName;
    private String userEmail;
    private Long productId;
    private String productName;
    private BigDecimal targetPrice;
    private Boolean active;
    private Instant createdAt;
}
