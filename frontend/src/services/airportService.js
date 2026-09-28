import apiClient from './api';

export const searchAirports = async (query = '', signal) => {
  const trimmed = typeof query === 'string' ? query.trim() : '';
  const response = await apiClient.get('/flights/locations', {
    params: { query: trimmed, q: trimmed },
    signal,
  });
  return response.data; // [{ id, name, iataCode, cityName, countryName, countryCode, airportType, type, latitude, longitude, displayName }]
};

export const suggestAirports = async (query = '', signal) => {
  const trimmed = typeof query === 'string' ? query.trim() : '';
  const response = await apiClient.get('/flights/locations', {
    params: { query: trimmed, q: trimmed },
    signal,
  });
  return response.data;
};

export const searchLocations = searchAirports;

export default {
  searchAirports,
  suggestAirports,
  searchLocations,
};
