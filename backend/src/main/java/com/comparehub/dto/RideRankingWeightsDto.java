package com.comparehub.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RideRankingWeightsDto {

    @Builder.Default
    private int fare = 60;

    @Builder.Default
    private int eta = 40;

    public void normalizeWeights() {
        int total = fare + eta;
        if (total != 100 && total > 0) {
            fare = (int) Math.round((double) fare * 100 / total);
            eta = Math.max(0, 100 - fare);
        }
    }
}
