package com.comparehub.provider.impl;

import com.comparehub.dto.FlightSearchRequestDto;
import com.comparehub.dto.NormalizedFlightOfferDto;
import com.comparehub.provider.FlightProvider;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;

@Slf4j
@Component
public class DomesticAirlinesFlightProvider implements FlightProvider {

    @Override
    public String getProviderName() {
        return "Domestic Direct Connect";
    }

    @Override
    @CircuitBreaker(name = "domestic-flights", fallbackMethod = "fallbackSearch")
    @Retry(name = "domestic-flights", fallbackMethod = "fallbackSearch")
    public List<NormalizedFlightOfferDto> searchFlights(FlightSearchRequestDto request) {
        String origin = request.getOrigin() != null ? request.getOrigin().toUpperCase() : "DEL";
        String destination = request.getDestination() != null ? request.getDestination().toUpperCase() : "BOM";

        return List.of(
                NormalizedFlightOfferDto.builder()
                        .provider("IndiGo Direct")
                        .airline("IndiGo")
                        .flightNumber("6E-5012")
                        .origin(origin)
                        .destination(destination)
                        .departure("06:15")
                        .arrival("08:25")
                        .durationMinutes(130)
                        .stops(0)
                        .price(new BigDecimal("4999.00"))
                        .currency("INR")
                        .bookingUrl("https://www.goindigo.in/booking/search.html?from=" + origin + "&to=" + destination)
                        .build(),
                NormalizedFlightOfferDto.builder()
                        .provider("SpiceJet Direct")
                        .airline("SpiceJet")
                        .flightNumber("SG-8169")
                        .origin(origin)
                        .destination(destination)
                        .departure("21:40")
                        .arrival("23:55")
                        .durationMinutes(135)
                        .stops(0)
                        .price(new BigDecimal("4750.00"))
                        .currency("INR")
                        .bookingUrl("https://www.spicejet.com/search?orig=" + origin + "&dest=" + destination)
                        .build(),
                NormalizedFlightOfferDto.builder()
                        .provider("Akasa Air Direct")
                        .airline("Akasa Air")
                        .flightNumber("QP-1354")
                        .origin(origin)
                        .destination(destination)
                        .departure("16:00")
                        .arrival("18:15")
                        .durationMinutes(135)
                        .stops(0)
                        .price(new BigDecimal("5190.00"))
                        .currency("INR")
                        .bookingUrl("https://www.akasaair.com/book-flight?orig=" + origin + "&dest=" + destination)
                        .build(),
                NormalizedFlightOfferDto.builder()
                        .provider("IndiGo Direct")
                        .airline("IndiGo")
                        .flightNumber("6E-204")
                        .origin(origin)
                        .destination(destination)
                        .departure("12:00")
                        .arrival("17:40")
                        .durationMinutes(340)
                        .stops(1)
                        .price(new BigDecimal("4200.00"))
                        .currency("INR")
                        .bookingUrl("https://www.goindigo.in/booking/search.html?from=" + origin + "&to=" + destination)
                        .build()
        );
    }

    public List<NormalizedFlightOfferDto> fallbackSearch(FlightSearchRequestDto request, Throwable throwable) {
        log.warn("Domestic flight direct connect fallback triggered: {}", throwable.getMessage());
        return searchFlights(request);
    }
}
