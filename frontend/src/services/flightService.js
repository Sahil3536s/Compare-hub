import apiClient from './api';

export const searchFlights = async ({
  origin = 'DEL',
  destination = 'BOM',
  departureDate = '',
  returnDate = null,
  adults = 1,
  cabinClass = 'ECONOMY',
  maxStops = null,
  airline = 'all',
  maxPrice = null,
  timeOfDay = 'all',
  sortBy = 'best',
} = {}) => {
  const body = {
    origin: origin.trim().toUpperCase(),
    destination: destination.trim().toUpperCase(),
    departureDate: departureDate || new Date(Date.now() + 86400000 * 7).toISOString().split('T')[0],
    returnDate: returnDate || null,
    adults: Number(adults) || 1,
    cabinClass: cabinClass || 'ECONOMY',
    maxStops: maxStops !== null && maxStops !== '' ? Number(maxStops) : null,
    airline: airline && airline !== 'all' ? airline : null,
    maxPrice: maxPrice ? Number(maxPrice) : null,
    timeOfDay: timeOfDay && timeOfDay !== 'all' ? timeOfDay : null,
    sortBy: sortBy || 'best',
  };

  const response = await apiClient.post('/flights/search', body);
  return response.data; // { origin, destination, totalOffers, cheapestPrice, fastestDurationMinutes, bestAirline, offers: [...] }
};

export default {
  searchFlights,
};
