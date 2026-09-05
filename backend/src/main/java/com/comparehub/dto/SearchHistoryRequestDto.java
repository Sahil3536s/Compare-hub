package com.comparehub.dto;

import com.comparehub.model.SearchType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SearchHistoryRequestDto {
    private Long userId;

    @NotBlank(message = "Query is required")
    private String query;

    @NotNull(message = "Search type is required")
    private SearchType searchType;
}
