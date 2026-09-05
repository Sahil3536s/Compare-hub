package com.comparehub.provider.impl;

import com.comparehub.dto.LocationDto;
import com.comparehub.dto.NormalizedRideOfferDto;
import com.comparehub.provider.RideProvider;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

@Component
public class MockRapidoProvider implements RideProvider {

    @Override
    public String getProviderName() {
        return "Rapido";
    }

    @Override
    public List<NormalizedRideOfferDto> getFareEstimate(LocationDto pickup, LocationDto destination) {
        double distanceKm = calculateApproxDistanceKm(pickup, destination);
        String pLat = String.valueOf(pickup.getLatitude() != null ? pickup.getLatitude() : 28.6139);
        String pLon = String.valueOf(pickup.getLongitude() != null ? pickup.getLongitude() : 77.2090);
        String dLat = String.valueOf(destination.getLatitude() != null ? destination.getLatitude() : 28.5562);
        String dLon = String.valueOf(destination.getLongitude() != null ? destination.getLongitude() : 77.1000);

        String rapidoDeepLink = String.format(
                "https://www.rapido.bike/app?lat=%s&lng=%s&drop_lat=%s&drop_lng=%s",
                pLat, pLon, dLat, dLon);

        // Rapido Bike (Bike)
        BigDecimal bikeBase = BigDecimal.valueOf(20.0 + (distanceKm * 7.5));
        BigDecimal bikeMin = bikeBase.setScale(0, RoundingMode.HALF_UP);
        BigDecimal bikeMax = bikeBase.multiply(BigDecimal.valueOf(1.10)).setScale(0, RoundingMode.HALF_UP);

        // Rapido Auto (Auto)
        BigDecimal autoBase = BigDecimal.valueOf(30.0 + (distanceKm * 11.0));
        BigDecimal autoMin = autoBase.setScale(0, RoundingMode.HALF_UP);
        BigDecimal autoMax = autoBase.multiply(BigDecimal.valueOf(1.10)).setScale(0, RoundingMode.HALF_UP);

        // Rapido Cab Economy (Cab)
        BigDecimal cabBase = BigDecimal.valueOf(42.0 + (distanceKm * 16.5));
        BigDecimal cabMin = cabBase.setScale(0, RoundingMode.HALF_UP);
        BigDecimal cabMax = cabBase.multiply(BigDecimal.valueOf(1.15)).setScale(0, RoundingMode.HALF_UP);

        return List.of(
                NormalizedRideOfferDto.builder()
                        .provider("Rapido")
                        .rideType("Rapido Bike")
                        .vehicleCategory("Bike")
                        .estimatedPriceMin(bikeMin)
                        .estimatedPriceMax(bikeMax)
                        .etaMinutes(2)
                        .distanceKm(distanceKm)
                        .currency("INR")
                        .deepLink(rapidoDeepLink)
                        .build(),
                NormalizedRideOfferDto.builder()
                        .provider("Rapido")
                        .rideType("Rapido Auto")
                        .vehicleCategory("Auto")
                        .estimatedPriceMin(autoMin)
                        .estimatedPriceMax(autoMax)
                        .etaMinutes(3)
                        .distanceKm(distanceKm)
                        .currency("INR")
                        .deepLink(rapidoDeepLink)
                        .build(),
                NormalizedRideOfferDto.builder()
                        .provider("Rapido")
                        .rideType("Rapido Cab")
                        .vehicleCategory("Cab")
                        .estimatedPriceMin(cabMin)
                        .estimatedPriceMax(cabMax)
                        .etaMinutes(5)
                        .distanceKm(distanceKm)
                        .currency("INR")
                        .deepLink(rapidoDeepLink)
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
