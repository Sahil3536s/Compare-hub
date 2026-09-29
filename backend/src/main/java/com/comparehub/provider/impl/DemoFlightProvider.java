package com.comparehub.provider.impl;

import com.comparehub.dto.FlightSearchRequestDto;
import com.comparehub.dto.NormalizedFlightOfferDto;
import com.comparehub.provider.FlightProvider;
import lombok.Builder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Set;

@Slf4j
@Component
public class DemoFlightProvider implements FlightProvider {

    private static final Set<String> DOMESTIC_AIRPORTS = Set.of(
            "DEL", "BOM", "BHO", "IDR", "BLR", "MAA", "CCU", "HYD", "GOI", "GOX",
            "PNQ", "AMD", "JAI", "COK", "LKO", "VNS", "ATQ", "PAT", "SXR", "BBI",
            "GAU", "IXC", "NAG", "CJB", "TRV", "IXE", "TRZ", "VTZ", "BDQ", "STV",
            "RAJ", "RPR", "DED", "UDR", "JDH", "GWL", "JLR"
    );

    private static final List<String> DEMO_CARRIERS = List.of(
            "DemoAir", "Sample Airways", "SkyDemo", "Compare Air"
    );

    @Override
    public String getProviderName() {
        return "CompareHub Demo Flight Provider";
    }

    @Override
    public List<NormalizedFlightOfferDto> searchFlights(FlightSearchRequestDto request) {
        if (request == null || request.getOrigin() == null || request.getDestination() == null) {
            return Collections.emptyList();
        }
        String origin = request.getOrigin().trim().toUpperCase();
        String destination = request.getDestination().trim().toUpperCase();
        if (origin.equals(destination)) {
            return Collections.emptyList();
        }
        if (!DOMESTIC_AIRPORTS.contains(origin) || !DOMESTIC_AIRPORTS.contains(destination)) {
            log.debug("Demo provider does not support non‑domestic route {} -> {}", origin, destination);
            return Collections.emptyList();
        }
        // Deterministic seed based on route, date, cabin class and adults
        String dateStr = request.getDepartureDate();
        int adults = request.getAdults() != null ? request.getAdults() : 1;
        String cabin = request.getCabinClass() != null ? request.getCabinClass() : "ECONOMY";
        int seed = Math.abs(Objects.hash(origin, destination, dateStr, cabin, adults));

        // Base duration (minutes) - deterministic variation up to ±30 min
        int baseDuration = 120 + (seed % 61) - 30; // range 90‑150
        // Base price (INR) - deterministic variation up to ±1500
        BigDecimal basePrice = new BigDecimal(4000 + (seed % 3001)); // 4000‑7000

        // Generate 4‑6 offers
        int offerCount = 4 + (seed % 3); // 4‑6 offers
        List<NormalizedFlightOfferDto> offers = new java.util.ArrayList<>();
        for (int i = 0; i < offerCount; i++) {
            String carrier = DEMO_CARRIERS.get(i % DEMO_CARRIERS.size());
            String airlineLogo = carrier.replaceAll(" ", "").substring(0, Math.min(2, carrier.length())).toUpperCase();
            // Flight number deterministic
            int flightNum = 100 + ((seed + i) % 9000);
            String flightNumber = airlineLogo + "-" + flightNum;
            // Departure time start at 06:00, add deterministic minutes per index
            int depMinutes = (6 * 60) + (i * 60) + (seed % 60);
            int depHour = (depMinutes / 60) % 24;
            int depMin = depMinutes % 60;
            String departure = String.format("%02d:%02d", depHour, depMin);
            // Arrival based on duration (some offers may have a stop adding 30 min)
            int extraStop = (i % 2 == 0) ? 0 : 30; // every second offer has a stop
            int duration = baseDuration + extraStop;
            String arrival = calculateArrivalTime(departure, duration);
            // Price variation per offer
            BigDecimal price = basePrice.add(new BigDecimal((i * 250) + (seed % 200)));
            // Stops field
            int stops = extraStop == 0 ? 0 : 1;

            offers.add(NormalizedFlightOfferDto.builder()
                    .provider(getProviderName())
                    .airline(carrier)
                    .airlineLogo(airlineLogo)
                    .flightNumber(flightNumber)
                    .origin(origin)
                    .destination(destination)
                    .departure(departure)
                    .arrival(arrival)
                    .durationMinutes(duration)
                    .stops(stops)
                    .price(price.setScale(2, java.math.RoundingMode.HALF_UP))
                    .currency("INR")
                    .cabinBaggage("7 kg Cabin")
                    .checkInBaggage("15 kg Check-in")
                    .bookingUrl(null) // no real booking link
                    .dataSource("DEMO")
                    .live(false)
                    .build());
        }
        return offers;
    }

    /**
     * Simple deterministic arrival calculation – adds minutes and rolls over 24h.
     */
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
