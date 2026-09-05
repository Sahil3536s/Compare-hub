import apiClient from './api';

export const getSearchHistory = async () => {
  const response = await apiClient.get('/history');
  return response.data; // [{ id, userId, query, searchType, createdAt }]
};

export const recordSearchHistory = async (query, searchType = 'SHOPPING') => {
  if (!query || !query.trim()) return null;
  try {
    const response = await apiClient.post('/history', {
      query: query.trim(),
      searchType,
    });
    return response.data;
  } catch (e) {
    // Non-critical background telemetry
    return null;
  }
};

export const clearSearchHistory = async () => {
  const response = await apiClient.delete('/history');
  return response.data;
};

export const deleteHistoryItem = async (historyId) => {
  const response = await apiClient.delete(`/history/${historyId}`);
  return response.data;
};

export const getFlightHistory = async () => {
  const response = await apiClient.get('/history/flights');
  return response.data; // [{ id, fromAirport, toAirport, departureDate, returnDate, passengers, cabinClass, createdAt }]
};

export const getRideHistory = async () => {
  const response = await apiClient.get('/history/rides');
  return response.data; // [{ id, pickupLocation, dropLocation, rideType, createdAt }]
};

export default {
  getSearchHistory,
  recordSearchHistory,
  clearSearchHistory,
  deleteHistoryItem,
  getFlightHistory,
  getRideHistory,
};
