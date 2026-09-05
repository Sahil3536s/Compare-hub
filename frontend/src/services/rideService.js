import apiClient from './api';

export const compareRides = async ({
  pickup,
  destination,
  rideType = 'all',
  sortBy = 'best',
}) => {
  const body = {
    pickup: {
      latitude: pickup.latitude || 28.6315,
      longitude: pickup.longitude || 77.2167,
      address: pickup.address || 'Connaught Place, New Delhi',
      city: pickup.city || 'New Delhi',
    },
    destination: {
      latitude: destination.latitude || 28.5562,
      longitude: destination.longitude || 77.1000,
      address: destination.address || 'IGI Airport, New Delhi',
      city: destination.city || 'New Delhi',
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
