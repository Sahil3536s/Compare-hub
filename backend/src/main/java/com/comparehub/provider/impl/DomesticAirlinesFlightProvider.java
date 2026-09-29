package com.comparehub.provider.impl;

import com.comparehub.dto.FlightSearchRequestDto;
import com.comparehub.dto.NormalizedFlightOfferDto;
import com.comparehub.provider.FlightProvider;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.Set;

@Slf4j
@Component
public class DomesticAirlinesFlightProvider implements FlightProvider {

    private static final Set<String> DOMESTIC_AIRPORTS = Set.of(
            "DEL", "BOM", "BHO", "IDR", "BLR", "MAA", "CCU", "HYD", "GOI", "GOX",
            "PNQ", "AMD", "JAI", "COK", "LKO", "VNS", "ATQ", "PAT", "SXR", "BBI",
            "GAU", "IXC", "NAG", "CJB", "TRV", "IXE", "TRZ", "VTZ", "BDQ", "STV",
            "RAJ", "RPR", "DED", "UDR", "JDH", "GWL", "JLR"
    );

    @Override
    public String getProviderName() {
        return "Disabled DomesticAirlinesFlightProvider";
    }

    @Override
    @CircuitBreaker(name = "domestic-flights", fallbackMethod = "fallbackSearch")
    @Retry(name = "domestic-flights", fallbackMethod = "fallbackSearch")
    public List<NormalizedFlightOfferDto> searchFlights(FlightSearchRequestDto request) {
        log.info("DomesticAirlinesFlightProvider is disabled; returning no offers.");
        return Collections.emptyList();
    }

    // Keep fallback method for compatibility, also returns empty list
    public List<NormalizedFlightOfferDto> fallbackSearch(FlightSearchRequestDto request, Throwable throwable) {
        log.warn("Fallback for disabled DomesticAirlinesFlightProvider triggered: {}", throwable.getMessage());
        return Collections.emptyList();
    }
}
