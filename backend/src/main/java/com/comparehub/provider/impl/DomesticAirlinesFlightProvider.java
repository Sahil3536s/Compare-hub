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
        return "Domestic Direct Connect";
    }

    @Override
    @CircuitBreaker(name = "domestic-flights", fallbackMethod = "fallbackSearch")
    @Retry(name = "domestic-flights", fallbackMethod = "fallbackSearch")
    public List<NormalizedFlightOfferDto> searchFlights(FlightSearchRequestDto request) {
        if (request == null || request.getOrigin() == null || request.getDestination() == null) {
            return Collections.emptyList();
        }

        String origin = request.getOrigin().trim().toUpperCase();
        String destination = request.getDestination().trim().toUpperCase();

        if (origin.equals(destination)) {
            return Collections.emptyList();
        }

        // Domestic carrier connects only domestic Indian commercial airports
        if (!DOMESTIC_AIRPORTS.contains(origin) || !DOMESTIC_AIRPORTS.contains(destination)) {
            log.debug("Domestic Direct Connect does not operate non-domestic route {} -> {}", origin, destination);
            return Collections.emptyList();
        }

        // Calculate dynamic flight duration and pricing based on route
        int baseDuration = estimateFlightDuration(origin, destination);
        BigDecimal basePrice = estimateBasePrice(origin, destination);

        return List.of(
                NormalizedFlightOfferDto.builder()
                        .provider("IndiGo Direct")
                        .airline("IndiGo")
                        .airlineLogo("6E")
                        .flightNumber("6E-" + (1000 + Math.abs((origin + destination).hashCode() % 8000)))
                        .origin(origin)
                        .destination(destination)
                        .departure("06:15")
                        .arrival(calculateArrivalTime("06:15", baseDuration))
                        .durationMinutes(baseDuration)
                        .stops(0)
                        .price(basePrice.setScale(2, java.math.RoundingMode.HALF_UP))
                        .currency("INR")
                        .cabinBaggage("7 kg Cabin")
                        .checkInBaggage("15 kg Check-in")
                        .bookingUrl("https://www.goindigo.in/booking/search.html?from=" + origin + "&to=" + destination)
                        .build(),
                NormalizedFlightOfferDto.builder()
                        .provider("SpiceJet Direct")
                        .airline("SpiceJet")
                        .airlineLogo("SG")
                        .flightNumber("SG-" + (2000 + Math.abs((origin + destination).hashCode() % 7000)))
                        .origin(origin)
                        .destination(destination)
                        .departure("14:30")
                        .arrival(calculateArrivalTime("14:30", baseDuration + 10))
                        .durationMinutes(baseDuration + 10)
                        .stops(0)
                        .price(basePrice.subtract(new BigDecimal("350.00")).setScale(2, java.math.RoundingMode.HALF_UP))
                        .currency("INR")
                        .cabinBaggage("7 kg Cabin")
                        .checkInBaggage("15 kg Check-in")
                        .bookingUrl("https://www.spicejet.com/search?orig=" + origin + "&dest=" + destination)
                        .build(),
                NormalizedFlightOfferDto.builder()
                        .provider("Akasa Air Direct")
                        .airline("Akasa Air")
                        .airlineLogo("QP")
                        .flightNumber("QP-" + (1100 + Math.abs((origin + destination).hashCode() % 6000)))
                        .origin(origin)
                        .destination(destination)
                        .departure("18:45")
                        .arrival(calculateArrivalTime("18:45", baseDuration + 5))
                        .durationMinutes(baseDuration + 5)
                        .stops(0)
                        .price(basePrice.add(new BigDecimal("220.00")).setScale(2, java.math.RoundingMode.HALF_UP))
                        .currency("INR")
                        .cabinBaggage("7 kg Cabin")
                        .checkInBaggage("15 kg Check-in")
                        .bookingUrl("https://www.akasaair.com/book-flight?orig=" + origin + "&dest=" + destination)
                        .build()
        );
    }

    public List<NormalizedFlightOfferDto> fallbackSearch(FlightSearchRequestDto request, Throwable throwable) {
        log.warn("Domestic flight direct connect fallback triggered: {}", throwable.getMessage());
        return searchFlights(request);
    }

    private int estimateFlightDuration(String orig, String dest) {
        // Known route durations (minutes)
        if ((orig.equals("DEL") && dest.equals("BOM")) || (orig.equals("BOM") && dest.equals("DEL"))) return 130;
        if ((orig.equals("BHO") && dest.equals("DEL")) || (orig.equals("DEL") && dest.equals("BHO"))) return 85;
        if ((orig.equals("IDR") && dest.equals("BLR")) || (orig.equals("BLR") && dest.equals("IDR"))) return 125;
        if ((orig.equals("BLR") && dest.equals("DEL")) || (orig.equals("DEL") && dest.equals("BLR"))) return 165;
        if ((orig.equals("BOM") && dest.equals("BLR")) || (orig.equals("BLR") && dest.equals("BOM"))) return 100;
        return 120; // Default domestic flight duration
    }

    private BigDecimal estimateBasePrice(String orig, String dest) {
        if ((orig.equals("DEL") && dest.equals("BOM")) || (orig.equals("BOM") && dest.equals("DEL"))) {
            return new BigDecimal("4999.00");
        }
        if ((orig.equals("BHO") && dest.equals("DEL")) || (orig.equals("DEL") && dest.equals("BHO"))) {
            return new BigDecimal("3850.00");
        }
        if ((orig.equals("IDR") && dest.equals("BLR")) || (orig.equals("BLR") && dest.equals("IDR"))) {
            return new BigDecimal("4620.00");
        }
        return new BigDecimal("4800.00");
    }

    private String calculateArrivalTime(String dep, int durationMins) {
        try {
            String[] parts = dep.split(":");
            int hour = Integer.parseInt(parts[0]);
            int min = Integer.parseInt(parts[1]);
            int total = hour * 60 + min + durationMins;
            int arrH = (total / 60) % 24;
            int arrM = total % 60;
            return String.format("%02d:%02d", arrH, arrM);
        } catch (Exception e) {
            return "12:00";
        }
    }
}
