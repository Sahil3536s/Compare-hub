import apiClient from './api';

export const suggestPlaces = async (query = '') => {
  const response = await apiClient.get('/location/suggest', {
    params: { q: query },
  });
  return response.data; // [{ placeId, mainText, secondaryText, fullAddress, latitude, longitude }]
};

export const geocodeAddress = async (address) => {
  const response = await apiClient.get('/location/geocode', {
    params: { address },
  });
  return response.data; // { latitude, longitude, address, city, state, country }
};

export const reverseGeocode = async (latitude, longitude) => {
  const response = await apiClient.get('/location/reverse-geocode', {
    params: { lat: latitude, lon: longitude },
  });
  return response.data; // { latitude, longitude, address, city, state, country }
};

export const estimateRoute = async (pickup, destination) => {
  const response = await apiClient.post('/location/route-estimate', [pickup, destination]);
  return response.data; // { distanceKm, durationMinutes, pickupAddress, dropAddress, polylineCoordinates: [[lat, lon], ...] }
};

export default {
  suggestPlaces,
  geocodeAddress,
  reverseGeocode,
  estimateRoute,
};
