package com.comparehub.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Historical price point observation sent in MLPredictionRequestDto.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class MLPricePointDto {

    @JsonProperty("date")
    private String date;

    @JsonProperty("price")
    private Double price;

    @JsonProperty("merchant")
    private String merchant;
}
