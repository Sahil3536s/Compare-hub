package com.comparehub.dto;

import com.comparehub.model.BudgetPreference;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BudgetConstraint {

    private String origin;
    private String destination;
    private String travelDate;
    @Builder.Default
    private int travelers = 1;
    private BigDecimal maxBudget;
    @Builder.Default
    private BudgetPreference preference = BudgetPreference.BALANCED;
    @Builder.Default
    private int airportBufferMinutes = 90;
    private String naturalLanguageQuery;
}
