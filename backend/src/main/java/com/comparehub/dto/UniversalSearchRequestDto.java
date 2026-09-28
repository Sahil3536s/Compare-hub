package com.comparehub.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UniversalSearchRequestDto {

    @NotBlank(message = "Search query must not be blank")
    private String query;

    private Long userId;
}
