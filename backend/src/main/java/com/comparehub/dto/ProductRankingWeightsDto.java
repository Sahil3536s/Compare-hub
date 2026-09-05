package com.comparehub.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductRankingWeightsDto {

    @Builder.Default
    private int price = 40;

    @Builder.Default
    private int rating = 20;

    @Builder.Default
    private int discount = 15;

    @Builder.Default
    private int delivery = 15;

    @Builder.Default
    private int reliability = 10;

    public void normalizeWeights() {
        int total = price + rating + discount + delivery + reliability;
        if (total != 100 && total > 0) {
            price = (int) Math.round((double) price * 100 / total);
            rating = (int) Math.round((double) rating * 100 / total);
            discount = (int) Math.round((double) discount * 100 / total);
            delivery = (int) Math.round((double) delivery * 100 / total);
            reliability = Math.max(0, 100 - (price + rating + discount + delivery));
        }
    }
}
