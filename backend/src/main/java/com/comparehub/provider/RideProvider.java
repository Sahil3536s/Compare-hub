package com.comparehub.provider;

import com.comparehub.dto.LocationDto;
import com.comparehub.dto.NormalizedRideOfferDto;

import java.util.List;

public interface RideProvider {

    String getProviderName();

    List<NormalizedRideOfferDto> getFareEstimate(LocationDto pickup, LocationDto destination);
}
