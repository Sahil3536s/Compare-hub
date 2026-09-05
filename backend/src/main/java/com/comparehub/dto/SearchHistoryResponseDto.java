package com.comparehub.dto;

import com.comparehub.model.SearchType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SearchHistoryResponseDto {
    private Long id;
    private Long userId;
    private String query;
    private SearchType searchType;
    private Instant createdAt;
}
