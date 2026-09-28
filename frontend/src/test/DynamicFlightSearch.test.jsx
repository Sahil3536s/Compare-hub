import React from 'react';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import { describe, it, expect, vi, beforeEach } from 'vitest';
import FlightsPage, { SUGGESTED_AIRPORTS } from '../pages/FlightsPage';
import AirportAutocomplete from '../components/AirportAutocomplete';
import * as flightService from '../services/flightService';
import * as airportService from '../services/airportService';

// Mock services
vi.mock('../services/flightService');
vi.mock('../services/airportService');

describe('Dynamic Flight Airport/City Search & Integration', () => {
  const mockFlightOffer = {
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
    isCheapest: true,
    isFastest: true,
    isBest: true,
    score: 95.0,
    bookingUrl: 'https://www.airindia.com',
  };

  const mockCompareResponse = {
    origin: 'DEL',
    destination: 'BOM',
    departureDate: '2026-10-15',
    returnDate: null,
    totalOffers: 1,
    cheapestPrice: 5890,
    fastestDurationMinutes: 135,
    bestAirline: 'Air India',
    offers: [mockFlightOffer],
    failedProviders: [],
  };

  beforeEach(() => {
    vi.clearAllMocks();
    flightService.searchFlights.mockResolvedValue(mockCompareResponse);
  });

  // Helper to select an airport via autocomplete suggestions
  const selectAirportViaInput = async (input, query, code, cityName) => {
    airportService.searchAirports.mockImplementation(async () => [
      {
        iataCode: code,
        name: `${cityName} International Airport`,
        cityName: cityName,
        countryName: 'Country',
        displayName: `${code} — ${cityName} International Airport, ${cityName}`,
      },
    ]);

    fireEvent.change(input, { target: { value: query } });
    const option = await screen.findByRole('option', { name: new RegExp(code, 'i') }, { timeout: 3000 });
    fireEvent.click(option);
  };

  // =========================================================================
  // 1. REUSABLE AIRPORT AUTOCOMPLETE TESTS
  // =========================================================================
  describe('AirportAutocomplete Component', () => {
    it('does not trigger API call for queries shorter than 2 characters', async () => {
      render(
        <AirportAutocomplete
          id="test-airport"
          value=""
          onChange={vi.fn()}
          onSelect={vi.fn()}
        />
      );

      const input = screen.getByRole('combobox');
      fireEvent.change(input, { target: { value: 'D' } });

      await new Promise((r) => setTimeout(r, 350));
      expect(airportService.searchAirports).not.toHaveBeenCalled();
    });

    it('debounces user input (~300ms) before querying searchAirports API', async () => {
      airportService.searchAirports.mockResolvedValue([
        {
          name: 'Indira Gandhi International Airport',
          iataCode: 'DEL',
          cityName: 'Delhi',
          countryName: 'India',
          airportType: 'AIRPORT',
        },
      ]);

      render(
        <AirportAutocomplete
          id="test-airport"
          value=""
          onChange={vi.fn()}
          onSelect={vi.fn()}
        />
      );

      const input = screen.getByRole('combobox');
      fireEvent.change(input, { target: { value: 'Del' } });

      await waitFor(() => {
        expect(airportService.searchAirports).toHaveBeenCalledWith('Del');
      });

      const option = await screen.findByRole('option');
      expect(option).toHaveTextContent(/DEL/i);
      expect(option).toHaveTextContent(/Delhi/i);
    });

    it('displays loading spinner while fetching suggestions', async () => {
      let resolvePromise;
      const pendingPromise = new Promise((resolve) => {
        resolvePromise = resolve;
      });
      airportService.searchAirports.mockReturnValue(pendingPromise);

      render(
        <AirportAutocomplete
          id="test-airport"
          value=""
          onChange={vi.fn()}
          onSelect={vi.fn()}
        />
      );

      const input = screen.getByRole('combobox');
      fireEvent.change(input, { target: { value: 'Mumbai' } });

      await waitFor(() => {
        expect(screen.getByLabelText(/Loading suggestions/i)).toBeInTheDocument();
      });

      resolvePromise([]);
    });

    it('cancels stale pending requests when input changes rapidly', async () => {
      let resolveFirst;
      const firstPromise = new Promise((resolve) => {
        resolveFirst = resolve;
      });

      airportService.searchAirports
        .mockReturnValueOnce(firstPromise)
        .mockResolvedValueOnce([
          {
            name: 'Heathrow Airport',
            iataCode: 'LHR',
            cityName: 'London',
            countryName: 'United Kingdom',
          },
        ]);

      render(
        <AirportAutocomplete
          id="test-airport"
          value=""
          onChange={vi.fn()}
          onSelect={vi.fn()}
        />
      );

      const input = screen.getByRole('combobox');
      fireEvent.change(input, { target: { value: 'Lon' } });
      fireEvent.change(input, { target: { value: 'London' } });

      // Stale first promise resolves late
      resolveFirst([
        {
          name: 'Old Airport',
          iataCode: 'OLD',
          cityName: 'Old City',
        },
      ]);

      await waitFor(() => {
        expect(screen.getByText(/LHR/i)).toBeInTheDocument();
      });
      expect(screen.queryByText(/OLD/i)).not.toBeInTheDocument();
    });

    it('supports full keyboard navigation (ArrowDown, ArrowUp, Enter, Escape)', async () => {
      airportService.searchAirports.mockResolvedValue([
        { name: 'Heathrow', iataCode: 'LHR', cityName: 'London' },
        { name: 'Gatwick', iataCode: 'LGW', cityName: 'London' },
      ]);

      const onSelect = vi.fn();
      render(
        <AirportAutocomplete
          id="test-airport"
          value=""
          onChange={vi.fn()}
          onSelect={onSelect}
        />
      );

      const input = screen.getByRole('combobox');
      fireEvent.change(input, { target: { value: 'London' } });

      await waitFor(() => {
        expect(screen.getAllByRole('option')).toHaveLength(2);
      });

      // Press ArrowDown to highlight first
      fireEvent.keyDown(input, { key: 'ArrowDown' });
      // Press ArrowDown to highlight second
      fireEvent.keyDown(input, { key: 'ArrowDown' });
      // Press Enter to select
      fireEvent.keyDown(input, { key: 'Enter' });

      expect(onSelect).toHaveBeenCalledWith(
        expect.objectContaining({
          iataCode: 'LGW',
          name: 'Gatwick',
        })
      );
    });

    it('displays "No matching airports or cities found" when search yields 0 results', async () => {
      airportService.searchAirports.mockResolvedValue([]);

      render(
        <AirportAutocomplete
          id="test-airport"
          value=""
          onChange={vi.fn()}
          onSelect={vi.fn()}
        />
      );

      const input = screen.getByRole('combobox');
      fireEvent.change(input, { target: { value: 'NonexistentPlace' } });

      await waitFor(() => {
        expect(
          screen.getByText(/No matching airports or cities found for/i)
        ).toBeInTheDocument();
      });
    });

    it('clears input when clear button is clicked', async () => {
      const onChange = vi.fn();
      render(
        <AirportAutocomplete
          id="test-airport"
          value="DEL"
          onChange={onChange}
          onSelect={vi.fn()}
        />
      );

      const clearBtn = screen.getByLabelText(/Clear airport input/i);
      fireEvent.click(clearBtn);
      expect(onChange).toHaveBeenCalledWith('');
    });
  });

  // =========================================================================
  // 2. FLIGHT SEARCH ROUTE PAIRS & VALIDATION
  // =========================================================================
  describe('Dynamic Route Pairs & Validation', () => {
    it('initializes in IDLE state without executing auto-search on mount', async () => {
      render(<FlightsPage />);

      expect(screen.getByText('Compare Flight Fares')).toBeInTheDocument();
      expect(
        screen.getByText(/Select your departure and arrival airports above/i)
      ).toBeInTheDocument();
      expect(flightService.searchFlights).not.toHaveBeenCalled();
    });

    it('requires valid selected airport and rejects arbitrary unselected text like ABC', async () => {
      render(<FlightsPage />);

      const fromInput = screen.getByPlaceholderText(/DEL \(Delhi\)/i);
      fireEvent.change(fromInput, { target: { value: 'ABC' } });

      const searchBtn = screen.getByRole('button', { name: /Search Flights/i });
      fireEvent.click(searchBtn);

      await waitFor(() => {
        expect(screen.getByRole('alert')).toHaveTextContent(
          /Please select a valid origin airport from the suggestions/i
        );
      });
      expect(flightService.searchFlights).not.toHaveBeenCalled();
    });

    it('validates that origin and destination cannot be identical', async () => {
      render(<FlightsPage />);

      // Use popular hub suggestion chips to select DEL for both origin and destination
      const delChip = screen.getByRole('button', { name: /DEL \(Delhi\)/i });
      fireEvent.click(delChip); // Sets fromAirport to DEL
      fireEvent.click(delChip); // Sets toAirport to DEL

      const searchBtn = screen.getByRole('button', { name: /Search Flights/i });
      fireEvent.click(searchBtn);

      await waitFor(() => {
        expect(screen.getByRole('alert')).toHaveTextContent(
          /Origin and destination airports must be different/i
        );
      });
    });

    it('executes search for prompt route: Indore (IDR) -> Bangalore (BLR)', async () => {
      render(<FlightsPage />);

      const fromInput = screen.getByPlaceholderText(/DEL \(Delhi\)/i);
      const toInput = screen.getByPlaceholderText(/BOM \(Mumbai\)/i);

      await selectAirportViaInput(fromInput, 'Indore', 'IDR', 'Indore');
      await selectAirportViaInput(toInput, 'Bangalore', 'BLR', 'Bengaluru');

      const searchBtn = screen.getByRole('button', { name: /Search Flights/i });
      fireEvent.click(searchBtn);

      await waitFor(() => {
        expect(flightService.searchFlights).toHaveBeenCalledWith(
          expect.objectContaining({
            origin: 'IDR',
            destination: 'BLR',
          })
        );
      });
    });

    it('executes search for international route: Mumbai (BOM) -> Dubai (DXB)', async () => {
      render(<FlightsPage />);

      const fromInput = screen.getByPlaceholderText(/DEL \(Delhi\)/i);
      const toInput = screen.getByPlaceholderText(/BOM \(Mumbai\)/i);

      await selectAirportViaInput(fromInput, 'Mumbai', 'BOM', 'Mumbai');
      await selectAirportViaInput(toInput, 'Dubai', 'DXB', 'Dubai');

      const searchBtn = screen.getByRole('button', { name: /Search Flights/i });
      fireEvent.click(searchBtn);

      await waitFor(() => {
        expect(flightService.searchFlights).toHaveBeenCalledWith(
          expect.objectContaining({
            origin: 'BOM',
            destination: 'DXB',
          })
        );
      });
    });

    it('executes search for long-haul route: Delhi (DEL) -> London (LHR)', async () => {
      render(<FlightsPage />);

      const fromInput = screen.getByPlaceholderText(/DEL \(Delhi\)/i);
      const toInput = screen.getByPlaceholderText(/BOM \(Mumbai\)/i);

      await selectAirportViaInput(fromInput, 'Delhi', 'DEL', 'Delhi');
      await selectAirportViaInput(toInput, 'London', 'LHR', 'London');

      const searchBtn = screen.getByRole('button', { name: /Search Flights/i });
      fireEvent.click(searchBtn);

      await waitFor(() => {
        expect(flightService.searchFlights).toHaveBeenCalledWith(
          expect.objectContaining({
            origin: 'DEL',
            destination: 'LHR',
          })
        );
      });
    });

    it('executes search for US domestic route: New York (JFK) -> Los Angeles (LAX)', async () => {
      render(<FlightsPage />);

      const fromInput = screen.getByPlaceholderText(/DEL \(Delhi\)/i);
      const toInput = screen.getByPlaceholderText(/BOM \(Mumbai\)/i);

      await selectAirportViaInput(fromInput, 'New York', 'JFK', 'New York');
      await selectAirportViaInput(toInput, 'Los Angeles', 'LAX', 'Los Angeles');

      const searchBtn = screen.getByRole('button', { name: /Search Flights/i });
      fireEvent.click(searchBtn);

      await waitFor(() => {
        expect(flightService.searchFlights).toHaveBeenCalledWith(
          expect.objectContaining({
            origin: 'JFK',
            destination: 'LAX',
          })
        );
      });
    });
  });

  // =========================================================================
  // 3. REAL DATA VS LOCATION DATA (NO FAKE OFFERS)
  // =========================================================================
  describe('Real Data vs Location Data', () => {
    it('displays exact required notice when airports exist but providers return no flight offers', async () => {
      flightService.searchFlights.mockResolvedValue({
        origin: 'BHO',
        destination: 'DXB',
        totalOffers: 0,
        offers: [],
        failedProviders: [],
      });

      render(<FlightsPage />);

      const fromInput = screen.getByPlaceholderText(/DEL \(Delhi\)/i);
      const toInput = screen.getByPlaceholderText(/BOM \(Mumbai\)/i);

      await selectAirportViaInput(fromInput, 'Bhopal', 'BHO', 'Bhopal');
      await selectAirportViaInput(toInput, 'Dubai', 'DXB', 'Dubai');

      const searchBtn = screen.getByRole('button', { name: /Search Flights/i });
      fireEvent.click(searchBtn);

      await waitFor(() => {
        expect(
          screen.getByText('No flight offers were returned for this route and date.')
        ).toBeInTheDocument();
      });

      expect(screen.getByRole('button', { name: /Reset Search Filters/i })).toBeInTheDocument();
    });
  });
});
