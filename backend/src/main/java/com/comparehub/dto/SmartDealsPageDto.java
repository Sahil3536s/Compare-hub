package com.comparehub.dto;

import com.comparehub.model.SmartDealCategory;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SmartDealsPageDto {

    private List<SmartDealDto> deals;
    private int currentPage;
    private int totalPages;
    private long totalElements;
    private SmartDealCategory activeCategory;
    private Map<String, Long> categoryCounts;
}
