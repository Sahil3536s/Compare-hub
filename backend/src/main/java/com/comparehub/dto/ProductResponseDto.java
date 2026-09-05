package com.comparehub.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductResponseDto {
    private Long id;
    private String name;
    private String brand;
    private String category;
    private String imageUrl;
    private Instant createdAt;
    private BigDecimal lowestPrice;
    @Builder.Default
    private List<MerchantOfferResponseDto> offers = new ArrayList<>();
}
