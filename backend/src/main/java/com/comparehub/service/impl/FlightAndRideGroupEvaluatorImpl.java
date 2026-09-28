package com.comparehub.service.impl;

import com.comparehub.dto.*;
import com.comparehub.service.FlightComparisonService;
import com.comparehub.service.GroupTransportModeEvaluator;
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
public class FlightAndRideGroupEvaluatorImpl implements GroupTransportModeEvaluator {

    private final FlightComparisonService flightComparisonService;

    @Override
    public GroupTransportMode getSupportedMode() {
        return GroupTransportMode.FLIGHT_AND_RIDE;
    }

    @Override
    public List<GroupTravelOptionDto> evaluate(GroupTravelRequestDto request) {
        List<GroupTravelOptionDto> options = new ArrayList<>();
        int travelers = Math.max(1, request.getNumberOfTravelers());

        try {
            FlightSearchRequestDto flightRequest = FlightSearchRequestDto.builder()
                    .origin(request.getOrigin())
                    .destination(request.getDestination())
                    .departureDate(request.getTravelDate() != null ? request.getTravelDate() : "2026-09-10")
                    .build();

            FlightComparisonResponseDto flightRes = flightComparisonService.compareFlights(flightRequest);
            if (flightRes == null || flightRes.getOffers() == null || flightRes.getOffers().isEmpty()) {
                return options;
            }

            // Local airport transfer cabs needed (4 passengers per standard cab)
            int cabsNeeded = (int) Math.ceil(travelers / 4.0);
            BigDecimal originCabCost = BigDecimal.valueOf(800L * cabsNeeded);
            BigDecimal destCabCost = BigDecimal.valueOf(700L * cabsNeeded);
            BigDecimal totalLocalRidesCost = originCabCost.add(destCabCost);

            // Take top 2 best flight options to create clean multi-modal alternatives
            for (NormalizedFlightOfferDto flight : flightRes.getOffers().stream().limit(2).toList()) {
                BigDecimal ticketPrice = flight.getPrice();
                BigDecimal totalFlightCost = ticketPrice.multiply(BigDecimal.valueOf(travelers));
                BigDecimal totalGroupCost = totalFlightCost.add(totalLocalRidesCost);
                BigDecimal costPerPerson = totalGroupCost.divide(BigDecimal.valueOf(travelers), 2, RoundingMode.HALF_UP);

                // Duration: 45 min origin cab + 90 min airport buffer + flight duration + 45 min dest cab
                int totalDurationMinutes = 45 + 90 + flight.getDurationMinutes() + 45;
                int hours = totalDurationMinutes / 60;
                int mins = totalDurationMinutes % 60;
                String formattedDuration = String.format("%dh %02dm", hours, mins);

                List<GroupTravelLegDto> legs = new ArrayList<>();
                legs.add(GroupTravelLegDto.builder()
                        .legType("RIDE")
                        .title("Airport Drop Cab")
                        .origin(request.getOrigin())
                        .destination(flight.getOrigin() + " Airport")
                        .durationMinutes(45)
                        .cost(originCabCost)
                        .provider("Local Cab / Uber")
                        .notes(cabsNeeded + " cab(s) for " + travelers + " traveler(s)")
                        .build());

                legs.add(GroupTravelLegDto.builder()
                        .legType("FLIGHT")
                        .title("Flight " + flight.getAirline() + " (" + flight.getFlightNumber() + ")")
                        .origin(flight.getOrigin())
                        .destination(flight.getDestination())
                        .durationMinutes(flight.getDurationMinutes())
                        .cost(totalFlightCost)
                        .provider(flight.getAirline())
                        .notes(travelers + " ticket(s) @ ₹" + ticketPrice + " each (" + (flight.getStops() == 0 ? "Non-stop" : flight.getStops() + " Stop") + ")")
                        .build());

                legs.add(GroupTravelLegDto.builder()
                        .legType("RIDE")
                        .title("Airport Pickup to Destination")
                        .origin(flight.getDestination() + " Airport")
                        .destination(request.getDestination())
                        .durationMinutes(45)
                        .cost(destCabCost)
                        .provider("Local Cab / Uber")
                        .notes(cabsNeeded + " cab(s) for " + travelers + " traveler(s)")
                        .build());

                List<GroupCostItemDto> breakdown = new ArrayList<>();
                breakdown.add(GroupCostItemDto.builder()
                        .label(travelers + "x Flight Tickets (" + flight.getAirline() + ")")
                        .amount(totalFlightCost)
                        .category("FARE")
                        .description("₹" + ticketPrice + " per ticket × " + travelers + " passengers")
                        .build());

                breakdown.add(GroupCostItemDto.builder()
                        .label("Airport Drop Transfer (" + cabsNeeded + " cab" + (cabsNeeded > 1 ? "s" : "") + ")")
                        .amount(originCabCost)
                        .category("LOCAL_TRANSFER")
                        .description("Direct ride to departure airport")
                        .build());

                breakdown.add(GroupCostItemDto.builder()
                        .label("Airport Pickup Transfer (" + cabsNeeded + " cab" + (cabsNeeded > 1 ? "s" : "") + ")")
                        .amount(destCabCost)
                        .category("LOCAL_TRANSFER")
                        .description("Direct ride to final destination")
                        .build());

                String optId = "flight_and_rides_" + flight.getAirline().toLowerCase().replaceAll("\\s+", "_") + "_" + flight.getFlightNumber().toLowerCase().replaceAll("[^a-z0-9]", "");

                GroupTravelOptionDto option = GroupTravelOptionDto.builder()
                        .id(optId)
                        .mode(GroupTransportMode.FLIGHT_AND_RIDE)
                        .title("Flight + Airport Cabs (" + flight.getAirline() + ")")
                        .providerName(flight.getAirline() + " + Airport Cabs")
                        .totalCost(totalGroupCost)
                        .costPerPerson(costPerPerson)
                        .estimatedTravelTimeMinutes(totalDurationMinutes)
                        .formattedDuration(formattedDuration)
                        .numberOfTravelers(travelers)
                        .vehicleCapacityNote(travelers + " Flight Seats + " + cabsNeeded + " Airport Cab" + (cabsNeeded > 1 ? "s" : ""))
                        .requiredConnections(2)
                        .legs(legs)
                        .breakdown(breakdown)
                        .build();

                options.add(option);
            }
        } catch (Exception e) {
            log.error("Failed to evaluate Flight + Ride group travel option: {}", e.getMessage());
        }

        return options;
    }
}
