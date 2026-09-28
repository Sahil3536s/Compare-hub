import apiClient from './api';

export const compareRides = async ({
  pickup = {},
  destination = {},
  rideType = 'all',
  sortBy = 'best',
}) => {
  const body = {
    pickup: {
      name: pickup.name || pickup.mainText,
      formattedAddress: pickup.formattedAddress || pickup.fullAddress || pickup.address,
      address: pickup.address || pickup.formattedAddress || pickup.fullAddress,
      latitude: pickup.latitude,
      longitude: pickup.longitude,
      city: pickup.city,
      state: pickup.state,
      country: pickup.country,
      providerPlaceId: pickup.providerPlaceId || pickup.placeId,
    },
    destination: {
      name: destination.name || destination.mainText,
      formattedAddress: destination.formattedAddress || destination.fullAddress || destination.address,
      address: destination.address || destination.formattedAddress || destination.fullAddress,
      latitude: destination.latitude,
      longitude: destination.longitude,
      city: destination.city,
      state: destination.state,
      country: destination.country,
      providerPlaceId: destination.providerPlaceId || destination.placeId,
    },
    rideType: rideType || 'all',
    sortBy: sortBy || 'best',
  };

  const response = await apiClient.post('/rides/compare', body);
  return response.data; // { pickup, destination, distanceKm, durationMinutes, cheapestFare, fastestEtaMinutes, bestProvider, offers: [...] }
};

export default {
  compareRides,
};

