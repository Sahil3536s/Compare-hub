package com.comparehub.provider;

import com.comparehub.dto.FlightSearchRequestDto;
import com.comparehub.dto.NormalizedFlightOfferDto;

import java.util.List;

public interface FlightProvider {

    String getProviderName();

    List<NormalizedFlightOfferDto> searchFlights(FlightSearchRequestDto request);
}
