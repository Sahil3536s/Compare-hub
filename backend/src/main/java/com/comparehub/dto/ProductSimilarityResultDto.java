package com.comparehub.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductSimilarityResultDto {

    private int score; // 0 to 100
    private String matchQuality; // "STRONG_MATCH", "PROBABLE_MATCH", "SEPARATE_LISTING"
    private boolean isCompatible;
    private String explanation;
}
