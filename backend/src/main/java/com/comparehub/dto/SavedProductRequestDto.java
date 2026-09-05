package com.comparehub.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SavedProductRequestDto {
    private Long userId;

    @NotNull(message = "Product ID is required")
    private Long productId;
}
