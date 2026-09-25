import React from 'react';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import { describe, it, expect, vi, beforeEach } from 'vitest';
import FlightsPage from '../pages/FlightsPage';
import FlightOfferCard from '../components/FlightOfferCard';
import FlightFilterPanel from '../components/flights/FlightFilterPanel';
import FlightCardSkeleton from '../components/FlightCardSkeleton';
import * as flightService from '../services/flightService';

// Mock flightService
vi.mock('../services/flightService');

describe('Part 4 UI Upgrades: Flights Search, Results, Filters & States', () => {
  const mockOffers = [
    {
      airline: 'IndiGo',
      flightNumber: '6E-5012',
      origin: 'DEL',
      destination: 'BOM',
      departure: '06:15',
      arrival: '08:25',
      durationMinutes: 130,
      stops: 0,
      price: 4999,
      currency: 'INR',
      provider: 'Domestic Direct',
      cabinBaggage: '7 kg Cabin',
      checkInBaggage: '15 kg Check-in',
      isCheapest: true,
      isFastest: true,
      isBest: true,
      score: 98.5,
      bookingUrl: 'https://www.goindigo.in',
    },
    {
      airline: 'Air India',
      flightNumber: 'AI-865',
      origin: 'DEL',
      destination: 'BOM',
      departure: '10:00',
      arrival: '12:15',
      durationMinutes: 135,
      stops: 0,
      price: 5890,
      currency: 'INR',
      provider: 'Amadeus GDS',
      cabinBaggage: '7 kg Cabin',
      checkInBaggage: '15 kg Check-in',
      isCheapest: false,
      isFastest: false,
      isBest: false,
      score: 85.0,
      bookingUrl: 'https://www.airindia.com',
    },
    {
      airline: 'SpiceJet',
      flightNumber: 'SG-8169',
      origin: 'DEL',
      destination: 'BOM',
      departure: '21:40',
      arrival: '23:55',
      durationMinutes: 135,
      stops: 1,
      price: 4750,
      currency: 'INR',
      provider: 'Domestic Direct',
      cabinBaggage: '7 kg Cabin',
      checkInBaggage: '15 kg Check-in',
      isCheapest: false,
      isFastest: false,
      isBest: false,
      score: 82.0,
      bookingUrl: 'https://www.spicejet.com',
    },
  ];

  const mockResponse = {
    origin: 'DEL',
    destination: 'BOM',
    departureDate: '2026-10-01',
    returnDate: null,
    totalOffers: 3,
    cheapestPrice: 4750,
    fastestDurationMinutes: 130,
    bestAirline: 'IndiGo',
    offers: mockOffers,
    failedProviders: [],
  };

  beforeEach(() => {
    vi.clearAllMocks();
  });

  // =========================================================================
  // 1. FLIGHT SEARCH UI TESTS
  // =========================================================================
  describe('1. Flight Search UI', () => {
    it('renders Trip Type toggle (One Way / Round Trip) and toggles return date field', async () => {
      flightService.searchFlights.mockResolvedValue(mockResponse);
      render(<FlightsPage />);

      const oneWayBtn = screen.getByRole('button', { name: /One Way/i });
      const roundTripBtn = screen.getByRole('button', { name: /Round Trip/i });
      expect(oneWayBtn).toBeInTheDocument();
      expect(roundTripBtn).toBeInTheDocument();

      const returnInput = screen.getByPlaceholderText(/One Way trip/i);
      expect(returnInput).toBeDisabled();

      // Switch to round trip
      fireEvent.click(roundTripBtn);
      expect(returnInput).not.toBeDisabled();
    });

    it('renders FROM, TO, DEPARTURE, TRAVELLERS fields with Search Flights button', async () => {
      flightService.searchFlights.mockResolvedValue(mockResponse);
      render(<FlightsPage />);

      await waitFor(() => {
        expect(screen.getByRole('button', { name: /Search Flights/i })).toBeInTheDocument();
      });

      expect(screen.getByPlaceholderText(/DEL \(Delhi\)/i)).toBeInTheDocument();
      expect(screen.getByPlaceholderText(/BOM \(Mumbai\)/i)).toBeInTheDocument();
      expect(screen.getByLabelText(/Departure Date/i)).toBeInTheDocument();
    });

    it('swaps origin and destination when the swap button is clicked', async () => {
      flightService.searchFlights.mockResolvedValue(mockResponse);
      render(<FlightsPage />);

      await waitFor(() => {
        expect(screen.getByRole('button', { name: /Search Flights/i })).toBeInTheDocument();
      });

      const fromInput = screen.getByPlaceholderText(/DEL \(Delhi\)/i);
      const toInput = screen.getByPlaceholderText(/BOM \(Mumbai\)/i);
      expect(fromInput.value).toBe('DEL');
      expect(toInput.value).toBe('BOM');

      const swapBtn = screen.getByLabelText(/Swap origin and destination/i);
      fireEvent.click(swapBtn);

      expect(fromInput.value).toBe('BOM');
      expect(toInput.value).toBe('DEL');
    });

    it('validates that origin and destination cannot be identical', async () => {
      flightService.searchFlights.mockResolvedValue(mockResponse);
      render(<FlightsPage />);

      await waitFor(() => {
        expect(screen.getByRole('button', { name: /Search Flights/i })).toBeInTheDocument();
      });

      const toInput = screen.getByPlaceholderText(/BOM \(Mumbai\)/i);
      fireEvent.change(toInput, { target: { value: 'DEL' } });

      const searchBtn = screen.getByRole('button', { name: /Search Flights/i });
      fireEvent.click(searchBtn);

      await waitFor(() => {
        expect(screen.getByRole('alert')).toHaveTextContent(
          /Origin and destination airports must be different/i
        );
      });
    });

    it('validates that departure date cannot be in the past', async () => {
      flightService.searchFlights.mockResolvedValue(mockResponse);
      render(<FlightsPage />);

      await waitFor(() => {
        expect(screen.getByRole('button', { name: /Search Flights/i })).toBeInTheDocument();
      });

      const depInput = screen.getByLabelText(/Departure Date/i);
      fireEvent.change(depInput, { target: { value: '2020-01-01' } });

      const searchBtn = screen.getByRole('button', { name: /Search Flights/i });
      fireEvent.click(searchBtn);

      await waitFor(() => {
        expect(screen.getByRole('alert')).toHaveTextContent(
          /Departure date cannot be in the past/i
        );
      });
    });

    it('validates that return date must be on or after departure date for round trips', async () => {
      flightService.searchFlights.mockResolvedValue(mockResponse);
      render(<FlightsPage />);

      await waitFor(() => {
        expect(screen.getByRole('button', { name: /Search Flights/i })).toBeInTheDocument();
      });

      // Switch to round trip
      fireEvent.click(screen.getByRole('button', { name: /Round Trip/i }));

      const depInput = screen.getByLabelText(/Departure Date/i);
      const retInput = screen.getByLabelText(/Return Date/i);

      fireEvent.change(depInput, { target: { value: '2026-10-15' } });
      fireEvent.change(retInput, { target: { value: '2026-10-10' } });

      fireEvent.click(screen.getByRole('button', { name: /Search Flights/i }));

      await waitFor(() => {
        expect(screen.getByRole('alert')).toHaveTextContent(
          /Return date must be on or after the departure date/i
        );
      });
    });
  });

  // =========================================================================
  // 2. FLIGHT RESULTS & CARDS TESTS
  // =========================================================================
  describe('2. Flight Results & Ranking Tabs', () => {
    it('displays route loading context banner and skeletons during fetch', async () => {
      flightService.searchFlights.mockReturnValue(new Promise(() => {})); // pending promise
      render(<FlightsPage />);

      expect(
        screen.getByText(/Finding the best fares from/i)
      ).toBeInTheDocument();
      expect(screen.getByTestId('flight-skeleton-list')).toBeInTheDocument();
    });

    it('renders FlightOfferCard with airline badge, flight number, baggage, stops, and price', () => {
      render(<FlightOfferCard flight={mockOffers[0]} />);

      expect(screen.getByText('IndiGo')).toBeInTheDocument();
      expect(screen.getByText('(6E-5012)')).toBeInTheDocument();
      expect(screen.getByText('6E')).toBeInTheDocument(); // Airline badge
      expect(screen.getByText('Non-stop')).toBeInTheDocument();
      expect(screen.getByText('7 kg Cabin')).toBeInTheDocument();
      expect(screen.getByText('15 kg Check-in')).toBeInTheDocument();
      expect(screen.getByText(/4,999/)).toBeInTheDocument();
      expect(screen.getByText(/Domestic Direct/i)).toBeInTheDocument();
      expect(screen.getByText(/Best Value/i)).toBeInTheDocument();
      expect(screen.getByText(/Cheapest Fare/i)).toBeInTheDocument();
      expect(screen.getByText(/Fastest Flight/i)).toBeInTheDocument();
    });

    it('renders 3 ranking tabs (Cheapest, Fastest, Best Value) and triggers backend sorting', async () => {
      flightService.searchFlights.mockResolvedValue(mockResponse);
      render(<FlightsPage />);

      await waitFor(() => {
        expect(screen.getByRole('button', { name: /Cheapest Lowest Available Fare/i })).toBeInTheDocument();
        expect(screen.getByRole('button', { name: /Fastest Shortest Flight Time/i })).toBeInTheDocument();
        expect(screen.getByRole('button', { name: /Best Value Optimal Price & Duration/i })).toBeInTheDocument();
      });

      // Click Cheapest tab
      const cheapestTab = screen.getByRole('button', { name: /Cheapest Lowest Available Fare/i });
      fireEvent.click(cheapestTab);

      await waitFor(() => {
        expect(flightService.searchFlights).toHaveBeenCalledWith(
          expect.objectContaining({ sortBy: 'cheapest' })
        );
      });

      // Click Fastest tab
      const fastestTab = screen.getByRole('button', { name: /Fastest Shortest Flight Time/i });
      fireEvent.click(fastestTab);

      await waitFor(() => {
        expect(flightService.searchFlights).toHaveBeenCalledWith(
          expect.objectContaining({ sortBy: 'fastest' })
        );
      });
    });
  });

  // =========================================================================
  // 3. FLIGHT FILTERS TESTS
  // =========================================================================
  describe('3. Flight Filters (Desktop & Mobile Drawer)', () => {
    it('renders stops, departure time, price slider, duration slider, and airlines in FlightFilterPanel', () => {
      const mockSetStops = vi.fn();
      const mockSetTime = vi.fn();
      const mockSetAirline = vi.fn();
      const mockReset = vi.fn();

      render(
        <FlightFilterPanel
          stops=""
          setStops={mockSetStops}
          departureTime="all"
          setDepartureTime={mockSetTime}
          maxPrice={35000}
          setMaxPrice={vi.fn()}
          selectedAirline="all"
          setSelectedAirline={mockSetAirline}
          maxDuration={720}
          setMaxDuration={vi.fn()}
          onResetFilters={mockReset}
        />
      );

      expect(screen.getByRole('button', { name: /Non-stop/i })).toBeInTheDocument();
      expect(screen.getByRole('button', { name: /1 Stop/i })).toBeInTheDocument();
      expect(screen.getByRole('button', { name: /Before 6 AM/i })).toBeInTheDocument();
      expect(screen.getByRole('button', { name: /6 AM – 12 PM/i })).toBeInTheDocument();
      expect(screen.getByRole('button', { name: /12 PM – 6 PM/i })).toBeInTheDocument();
      expect(screen.getByRole('button', { name: /After 6 PM/i })).toBeInTheDocument();
      expect(screen.getByText(/Max Flight Fare/i)).toBeInTheDocument();
      expect(screen.getByText(/Max Flight Duration/i)).toBeInTheDocument();

      // Click Non-stop
      fireEvent.click(screen.getByRole('button', { name: /Non-stop/i }));
      expect(mockSetStops).toHaveBeenCalledWith('0');

      // Click Before 6 AM
      fireEvent.click(screen.getByRole('button', { name: /Before 6 AM/i }));
      expect(mockSetTime).toHaveBeenCalledWith('before_6am');
    });

    it('opens mobile filter drawer when mobile filter button is triggered', async () => {
      flightService.searchFlights.mockResolvedValue(mockResponse);
      render(<FlightsPage />);

      await waitFor(() => {
        expect(screen.getByRole('button', { name: /Filter Flights/i })).toBeInTheDocument();
      });

      const filterBtn = screen.getByRole('button', { name: /Filter Flights/i });
      fireEvent.click(filterBtn);

      // Verify drawer opens with Close button and Apply Filters button
      expect(screen.getByRole('button', { name: /Apply Filters/i })).toBeInTheDocument();
      expect(screen.getByLabelText(/Close filters drawer/i)).toBeInTheDocument();

      // Click Close
      fireEvent.click(screen.getByLabelText(/Close filters drawer/i));
      expect(screen.queryByLabelText(/Close filters drawer/i)).not.toBeInTheDocument();
    });
  });

  // =========================================================================
  // 4. STATES: SUCCESS, PARTIAL_SUCCESS, EMPTY, ERROR
  // =========================================================================
  describe('4. States Management', () => {
    it('displays PARTIAL_SUCCESS banner when failedProviders is populated', async () => {
      flightService.searchFlights.mockResolvedValue({
        ...mockResponse,
        failedProviders: ['Amadeus GDS'],
      });

      render(<FlightsPage />);

      await waitFor(() => {
        expect(screen.getByRole('status')).toHaveTextContent(
          /Partial Provider Availability/i
        );
        expect(screen.getByRole('status')).toHaveTextContent(
          /Live offers from Amadeus GDS were temporarily unavailable/i
        );
      });
    });

    it('displays EMPTY state with reset filters button when no flights match', async () => {
      flightService.searchFlights.mockResolvedValue({
        ...mockResponse,
        offers: [],
        totalOffers: 0,
      });

      render(<FlightsPage />);

      await waitFor(() => {
        expect(screen.getByText(/No flights found for this route/i)).toBeInTheDocument();
        expect(screen.getByRole('button', { name: /Reset Search Filters/i })).toBeInTheDocument();
      });
    });

    it('displays user-friendly ERROR state with retry button on network/API failure', async () => {
      flightService.searchFlights.mockRejectedValue(new Error('Connection timeout to flight gateway'));

      render(<FlightsPage />);

      await waitFor(() => {
        expect(screen.getByText(/Flight Comparison Engine Notice/i)).toBeInTheDocument();
        expect(screen.getByText(/Connection timeout to flight gateway/i)).toBeInTheDocument();
        expect(screen.getByRole('button', { name: /Try Again/i })).toBeInTheDocument();
      });
    });
  });
});
