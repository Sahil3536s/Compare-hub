package com.comparehub.provider.impl;

import com.comparehub.dto.LocationDto;
import com.comparehub.dto.NormalizedRideOfferDto;
import com.comparehub.provider.RideProvider;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

@Component
public class MockUberProvider implements RideProvider {

    @Override
    public String getProviderName() {
        return "Uber";
    }

    @Override
    public List<NormalizedRideOfferDto> getFareEstimate(LocationDto pickup, LocationDto destination) {
        double distanceKm = calculateApproxDistanceKm(pickup, destination);
        String pLat = String.valueOf(pickup.getLatitude() != null ? pickup.getLatitude() : 28.6139);
        String pLon = String.valueOf(pickup.getLongitude() != null ? pickup.getLongitude() : 77.2090);
        String dLat = String.valueOf(destination.getLatitude() != null ? destination.getLatitude() : 28.5562);
        String dLon = String.valueOf(destination.getLongitude() != null ? destination.getLongitude() : 77.1000);

        String uberDeepLink = String.format(
                "https://m.uber.com/ul/?action=setPickup&pickup[latitude]=%s&pickup[longitude]=%s&dropoff[latitude]=%s&dropoff[longitude]=%s",
                pLat, pLon, dLat, dLon);

        // Uber Go (Cab)
        BigDecimal uberGoBase = BigDecimal.valueOf(50.0 + (distanceKm * 18.5));
        BigDecimal uberGoMin = uberGoBase.setScale(0, RoundingMode.HALF_UP);
        BigDecimal uberGoMax = uberGoBase.multiply(BigDecimal.valueOf(1.15)).setScale(0, RoundingMode.HALF_UP);

        // Uber Premier (Cab)
        BigDecimal premierBase = BigDecimal.valueOf(80.0 + (distanceKm * 24.0));
        BigDecimal premierMin = premierBase.setScale(0, RoundingMode.HALF_UP);
        BigDecimal premierMax = premierBase.multiply(BigDecimal.valueOf(1.15)).setScale(0, RoundingMode.HALF_UP);

        // Uber Auto (Auto)
        BigDecimal autoBase = BigDecimal.valueOf(35.0 + (distanceKm * 12.0));
        BigDecimal autoMin = autoBase.setScale(0, RoundingMode.HALF_UP);
        BigDecimal autoMax = autoBase.multiply(BigDecimal.valueOf(1.10)).setScale(0, RoundingMode.HALF_UP);

        // Uber Moto (Bike)
        BigDecimal motoBase = BigDecimal.valueOf(25.0 + (distanceKm * 8.5));
        BigDecimal motoMin = motoBase.setScale(0, RoundingMode.HALF_UP);
        BigDecimal motoMax = motoBase.multiply(BigDecimal.valueOf(1.10)).setScale(0, RoundingMode.HALF_UP);

        return List.of(
                NormalizedRideOfferDto.builder()
                        .provider("Uber")
                        .rideType("Uber Go")
                        .vehicleCategory("Cab")
                        .estimatedPriceMin(uberGoMin)
                        .estimatedPriceMax(uberGoMax)
                        .etaMinutes(3)
                        .distanceKm(distanceKm)
                        .currency("INR")
                        .deepLink(uberDeepLink)
                        .build(),
                NormalizedRideOfferDto.builder()
                        .provider("Uber")
                        .rideType("Uber Premier")
                        .vehicleCategory("Premier")
                        .estimatedPriceMin(premierMin)
                        .estimatedPriceMax(premierMax)
                        .etaMinutes(5)
                        .distanceKm(distanceKm)
                        .currency("INR")
                        .deepLink(uberDeepLink)
                        .build(),
                NormalizedRideOfferDto.builder()
                        .provider("Uber")
                        .rideType("Uber Auto")
                        .vehicleCategory("Auto")
                        .estimatedPriceMin(autoMin)
                        .estimatedPriceMax(autoMax)
                        .etaMinutes(4)
                        .distanceKm(distanceKm)
                        .currency("INR")
                        .deepLink(uberDeepLink)
                        .build(),
                NormalizedRideOfferDto.builder()
                        .provider("Uber")
                        .rideType("Uber Moto")
                        .vehicleCategory("Bike")
                        .estimatedPriceMin(motoMin)
                        .estimatedPriceMax(motoMax)
                        .etaMinutes(2)
                        .distanceKm(distanceKm)
                        .currency("INR")
                        .deepLink(uberDeepLink)
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
