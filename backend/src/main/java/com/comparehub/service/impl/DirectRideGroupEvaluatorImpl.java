package com.comparehub.service.impl;

import com.comparehub.dto.*;
import com.comparehub.service.GroupTransportModeEvaluator;
import com.comparehub.service.LocationService;
import com.comparehub.service.RideComparisonService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class DirectRideGroupEvaluatorImpl implements GroupTransportModeEvaluator {

    private final RideComparisonService rideComparisonService;
    private final LocationService locationService;

    @Override
    public GroupTransportMode getSupportedMode() {
        return GroupTransportMode.DIRECT_RIDE;
    }

    @Override
    public List<GroupTravelOptionDto> evaluate(GroupTravelRequestDto request) {
        List<GroupTravelOptionDto> options = new ArrayList<>();
        int travelers = Math.max(1, request.getNumberOfTravelers());

        try {
            LocationDto pickup = locationService.geocodeAddress(request.getOrigin());
            LocationDto destination = locationService.geocodeAddress(request.getDestination());

            if (pickup == null) {
                pickup = LocationDto.builder()
                        .address(request.getOrigin())
                        .latitude(28.6139)
                        .longitude(77.2090)
                        .build();
            }

            if (destination == null) {
                destination = LocationDto.builder()
                        .address(request.getDestination())
                        .latitude(26.9124)
                        .longitude(75.7873)
                        .build();
            }

            RideCompareRequestDto rideRequest = RideCompareRequestDto.builder()
                    .pickup(pickup)
                    .destination(destination)
                    .sortBy("PRICE")
                    .build();

            RideComparisonResponseDto rideRes = rideComparisonService.compareRides(rideRequest);
            if (rideRes == null || rideRes.getOffers() == null || rideRes.getOffers().isEmpty()) {
                return options;
            }

            int routeDuration = (rideRes.getDurationMinutes() != null && rideRes.getDurationMinutes() > 0)
                    ? rideRes.getDurationMinutes()
                    : 270;

            // Evaluate up to 3 distinct ride types (e.g. XL/SUV, Sedan/Prime, Standard)
            for (NormalizedRideOfferDto ride : rideRes.getOffers().stream().limit(3).toList()) {
                String rideType = ride.getRideType() != null ? ride.getRideType().toUpperCase() : "STANDARD";
                boolean isSuvOrXl = rideType.contains("XL") || rideType.contains("SUV") || rideType.contains("MAX") || "Premier".equalsIgnoreCase(ride.getVehicleCategory());
                
                int vehicleCapacity = isSuvOrXl ? 6 : 4;
                int vehiclesNeeded = (int) Math.ceil((double) travelers / vehicleCapacity);

                BigDecimal baseVehicleFare = ride.getEstimatedPriceMin() != null
                        ? ride.getEstimatedPriceMin()
                        : (ride.getEstimatedPriceMax() != null ? ride.getEstimatedPriceMax() : BigDecimal.valueOf(3200));

                BigDecimal totalGroupCost = baseVehicleFare.multiply(BigDecimal.valueOf(vehiclesNeeded));
                BigDecimal costPerPerson = totalGroupCost.divide(BigDecimal.valueOf(travelers), 2, RoundingMode.HALF_UP);

                int hours = routeDuration / 60;
                int mins = routeDuration % 60;
                String formattedDuration = String.format("%dh %02dm", hours, mins);

                String capacityNote = vehiclesNeeded == 1
                        ? "1 " + (isSuvOrXl ? "SUV / XL" : "Sedan / Hatchback") + " (Capacity: " + vehicleCapacity + " pax)"
                        : vehiclesNeeded + " Vehicles (" + (isSuvOrXl ? "SUVs" : "Sedans") + ") needed for " + travelers + " travelers";

                List<GroupTravelLegDto> legs = new ArrayList<>();
                legs.add(GroupTravelLegDto.builder()
                        .legType("RIDE")
                        .title("Direct Door-to-Door Cab (" + ride.getProvider() + " " + ride.getRideType() + ")")
                        .origin(request.getOrigin())
                        .destination(request.getDestination())
                        .durationMinutes(routeDuration)
                        .cost(totalGroupCost)
                        .provider(ride.getProvider())
                        .notes(capacityNote + " • Zero connections")
                        .build());

                List<GroupCostItemDto> breakdown = new ArrayList<>();
                breakdown.add(GroupCostItemDto.builder()
                        .label(vehiclesNeeded + "x " + ride.getProvider() + " " + ride.getRideType() + " Cab Fare")
                        .amount(totalGroupCost)
                        .category("FARE")
                        .description("₹" + baseVehicleFare + " per vehicle × " + vehiclesNeeded + " vehicle" + (vehiclesNeeded > 1 ? "s" : "") + " (Shared by " + travelers + " people)")
                        .build());

                String optId = "direct_ride_" + ride.getProvider().toLowerCase().replaceAll("\\s+", "_") + "_" + ride.getRideType().toLowerCase().replaceAll("[^a-z0-9]", "");

                GroupTravelOptionDto option = GroupTravelOptionDto.builder()
                        .id(optId)
                        .mode(GroupTransportMode.DIRECT_RIDE)
                        .title("Direct Cab (" + ride.getProvider() + " " + ride.getRideType() + ")")
                        .providerName(ride.getProvider() + " (" + ride.getRideType() + ")")
                        .totalCost(totalGroupCost)
                        .costPerPerson(costPerPerson)
                        .estimatedTravelTimeMinutes(routeDuration)
                        .formattedDuration(formattedDuration)
                        .numberOfTravelers(travelers)
                        .vehicleCapacityNote(capacityNote)
                        .requiredConnections(0)
                        .legs(legs)
                        .breakdown(breakdown)
                        .build();

                options.add(option);
            }
        } catch (Exception e) {
            log.error("Failed to evaluate Direct Ride group travel option: {}", e.getMessage());
        }

        return options;
    }
}
