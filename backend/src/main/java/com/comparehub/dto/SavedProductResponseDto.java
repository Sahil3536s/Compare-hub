package com.comparehub.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SavedProductResponseDto {
    private Long id;
    private Long userId;
    private Long productId;
    private String productName;
    private String productCategory;
    private String productImageUrl;
    private Instant createdAt;
}
