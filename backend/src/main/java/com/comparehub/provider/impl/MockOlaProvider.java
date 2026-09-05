package com.comparehub.provider.impl;

import com.comparehub.dto.LocationDto;
import com.comparehub.dto.NormalizedRideOfferDto;
import com.comparehub.provider.RideProvider;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

@Component
public class MockOlaProvider implements RideProvider {

    @Override
    public String getProviderName() {
        return "Ola";
    }

    @Override
    public List<NormalizedRideOfferDto> getFareEstimate(LocationDto pickup, LocationDto destination) {
        double distanceKm = calculateApproxDistanceKm(pickup, destination);
        String pLat = String.valueOf(pickup.getLatitude() != null ? pickup.getLatitude() : 28.6139);
        String pLon = String.valueOf(pickup.getLongitude() != null ? pickup.getLongitude() : 77.2090);
        String dLat = String.valueOf(destination.getLatitude() != null ? destination.getLatitude() : 28.5562);
        String dLon = String.valueOf(destination.getLongitude() != null ? destination.getLongitude() : 77.1000);

        String olaDeepLink = String.format(
                "https://book.olacabs.com/?pickup_lat=%s&pickup_lng=%s&drop_lat=%s&drop_lng=%s",
                pLat, pLon, dLat, dLon);

        // Ola Mini (Cab)
        BigDecimal miniBase = BigDecimal.valueOf(45.0 + (distanceKm * 17.5));
        BigDecimal miniMin = miniBase.setScale(0, RoundingMode.HALF_UP);
        BigDecimal miniMax = miniBase.multiply(BigDecimal.valueOf(1.15)).setScale(0, RoundingMode.HALF_UP);

        // Ola Prime Sedan (Cab)
        BigDecimal primeBase = BigDecimal.valueOf(75.0 + (distanceKm * 22.0));
        BigDecimal primeMin = primeBase.setScale(0, RoundingMode.HALF_UP);
        BigDecimal primeMax = primeBase.multiply(BigDecimal.valueOf(1.15)).setScale(0, RoundingMode.HALF_UP);

        // Ola Auto (Auto)
        BigDecimal autoBase = BigDecimal.valueOf(32.0 + (distanceKm * 11.5));
        BigDecimal autoMin = autoBase.setScale(0, RoundingMode.HALF_UP);
        BigDecimal autoMax = autoBase.multiply(BigDecimal.valueOf(1.10)).setScale(0, RoundingMode.HALF_UP);

        // Ola Bike (Bike)
        BigDecimal bikeBase = BigDecimal.valueOf(22.0 + (distanceKm * 8.0));
        BigDecimal bikeMin = bikeBase.setScale(0, RoundingMode.HALF_UP);
        BigDecimal bikeMax = bikeBase.multiply(BigDecimal.valueOf(1.10)).setScale(0, RoundingMode.HALF_UP);

        return List.of(
                NormalizedRideOfferDto.builder()
                        .provider("Ola")
                        .rideType("Ola Mini")
                        .vehicleCategory("Cab")
                        .estimatedPriceMin(miniMin)
                        .estimatedPriceMax(miniMax)
                        .etaMinutes(4)
                        .distanceKm(distanceKm)
                        .currency("INR")
                        .deepLink(olaDeepLink)
                        .build(),
                NormalizedRideOfferDto.builder()
                        .provider("Ola")
                        .rideType("Ola Prime Sedan")
                        .vehicleCategory("Premier")
                        .estimatedPriceMin(primeMin)
                        .estimatedPriceMax(primeMax)
                        .etaMinutes(6)
                        .distanceKm(distanceKm)
                        .currency("INR")
                        .deepLink(olaDeepLink)
                        .build(),
                NormalizedRideOfferDto.builder()
                        .provider("Ola")
                        .rideType("Ola Auto")
                        .vehicleCategory("Auto")
                        .estimatedPriceMin(autoMin)
                        .estimatedPriceMax(autoMax)
                        .etaMinutes(3)
                        .distanceKm(distanceKm)
                        .currency("INR")
                        .deepLink(olaDeepLink)
                        .build(),
                NormalizedRideOfferDto.builder()
                        .provider("Ola")
                        .rideType("Ola Bike")
                        .vehicleCategory("Bike")
                        .estimatedPriceMin(bikeMin)
                        .estimatedPriceMax(bikeMax)
                        .etaMinutes(2)
                        .distanceKm(distanceKm)
                        .currency("INR")
                        .deepLink(olaDeepLink)
                        .build()
        );
    }

    private double calculateApproxDistanceKm(LocationDto p, LocationDto d) {
        if (p == null || d == null || p.getLatitude() == null || d.getLatitude() == null) {
            return 12.5;
        }
        double lat1 = p.getLatitude(), lon1 = p.getLongitude();
        double lat2 = d.getLatitude(), lon2 = d.getLongitude();
        double dist = Math.hypot(lat2 - lat1, lon2 - lon1) * 111.0 * 1.35;
        return Math.max(2.0, Math.round(dist * 10.0) / 10.0);
    }
}
