import apiClient from './api';

export const searchAirports = async (query = '') => {
  const response = await apiClient.get('/airports/search', {
    params: { q: query },
  });
  return response.data; // [{ name, iataCode, cityName, countryName, airportType, latitude, longitude, displayName }]
};

export const suggestAirports = async (query = '') => {
  const response = await apiClient.get('/airports/suggest', {
    params: { q: query },
  });
  return response.data;
};

export default {
  searchAirports,
  suggestAirports,
};
