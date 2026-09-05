package com.comparehub.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FlightRankingWeightsDto {

    @Builder.Default
    private int price = 50;

    @Builder.Default
    private int duration = 30;

    @Builder.Default
    private int stops = 20;

    public void normalizeWeights() {
        int total = price + duration + stops;
        if (total != 100 && total > 0) {
            price = (int) Math.round((double) price * 100 / total);
            duration = (int) Math.round((double) duration * 100 / total);
            stops = Math.max(0, 100 - (price + duration));
        }
    }
}
