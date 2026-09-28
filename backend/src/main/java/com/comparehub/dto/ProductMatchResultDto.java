package com.comparehub.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductMatchResultDto {

    private boolean matched;
    private double score;

    @Builder.Default
    private List<String> reasons = new ArrayList<>();
}
