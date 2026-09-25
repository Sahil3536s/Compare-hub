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

      expect(screen.getByText('Indira Gandhi International Airport')).toBeInTheDocument();
      expect(screen.getByText('DEL')).toBeInTheDocument();
    });

    it('supports search by City Name, Airport Name, and IATA code', async () => {
      airportService.searchAirports.mockResolvedValue([
        {
          name: 'Raja Bhoj Airport',
          iataCode: 'BHO',
          cityName: 'Bhopal',
          countryName: 'India',
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
      fireEvent.change(input, { target: { value: 'Bho' } });

      await waitFor(() => {
        expect(screen.getByText('Raja Bhoj Airport')).toBeInTheDocument();
        expect(screen.getByText('BHO')).toBeInTheDocument();
      });
    });

    it('handles multiple airports in the same city (e.g. London: LHR, LGW, STN)', async () => {
      airportService.searchAirports.mockResolvedValue([
        {
          name: 'London Heathrow Airport',
          iataCode: 'LHR',
          cityName: 'London',
          countryName: 'United Kingdom',
        },
        {
          name: 'London Gatwick Airport',
          iataCode: 'LGW',
          cityName: 'London',
          countryName: 'United Kingdom',
        },
        {
          name: 'London Stansted Airport',
          iataCode: 'STN',
          cityName: 'London',
          countryName: 'United Kingdom',
        },
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
        expect(screen.getByText('LHR')).toBeInTheDocument();
        expect(screen.getByText('LGW')).toBeInTheDocument();
        expect(screen.getByText('STN')).toBeInTheDocument();
      });

      // User selects specific airport Gatwick
      fireEvent.click(screen.getByText('London Gatwick Airport'));
      expect(onSelect).toHaveBeenCalledWith(
        expect.objectContaining({
          iataCode: 'LGW',
          cityName: 'London',
          name: 'London Gatwick Airport',
        })
      );
    });

    it('supports keyboard navigation (ArrowDown, ArrowUp, Enter to select)', async () => {
      const onSelect = vi.fn();
      airportService.searchAirports.mockResolvedValue([
        {
          name: 'John F. Kennedy International Airport',
          iataCode: 'JFK',
          cityName: 'New York',
          countryName: 'United States',
        },
        {
          name: 'LaGuardia Airport',
          iataCode: 'LGA',
          cityName: 'New York',
          countryName: 'United States',
        },
      ]);

      render(
        <AirportAutocomplete
          id="test-airport"
          value=""
          onChange={vi.fn()}
          onSelect={onSelect}
        />
      );

      const input = screen.getByRole('combobox');
      fireEvent.change(input, { target: { value: 'New York' } });

      await waitFor(() => {
        expect(screen.getByText('JFK')).toBeInTheDocument();
      });

      // Arrow down to highlight first item (JFK), then select with Enter
      fireEvent.keyDown(input, { key: 'ArrowDown' });
      fireEvent.keyDown(input, { key: 'Enter' });

      expect(onSelect).toHaveBeenCalledWith(
        expect.objectContaining({
          iataCode: 'JFK',
          cityName: 'New York',
        })
      );
    });

    it('displays "No matching airports or cities found for ..." on empty results', async () => {
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
      fireEvent.change(input, { target: { value: 'NonexistentPlace123' } });

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
    it('validates that origin and destination cannot be identical', async () => {
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

    it('executes search for prompt route: Indore (IDR) -> Bangalore (BLR)', async () => {
      render(<FlightsPage />);

      await waitFor(() => {
        expect(screen.getByRole('button', { name: /Search Flights/i })).toBeInTheDocument();
      });

      const fromInput = screen.getByPlaceholderText(/DEL \(Delhi\)/i);
      const toInput = screen.getByPlaceholderText(/BOM \(Mumbai\)/i);

      fireEvent.change(fromInput, { target: { value: 'IDR' } });
      fireEvent.change(toInput, { target: { value: 'BLR' } });

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

      await waitFor(() => {
        expect(screen.getByRole('button', { name: /Search Flights/i })).toBeInTheDocument();
      });

      const fromInput = screen.getByPlaceholderText(/DEL \(Delhi\)/i);
      const toInput = screen.getByPlaceholderText(/BOM \(Mumbai\)/i);

      fireEvent.change(fromInput, { target: { value: 'BOM' } });
      fireEvent.change(toInput, { target: { value: 'DXB' } });

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

      await waitFor(() => {
        expect(screen.getByRole('button', { name: /Search Flights/i })).toBeInTheDocument();
      });

      const fromInput = screen.getByPlaceholderText(/DEL \(Delhi\)/i);
      const toInput = screen.getByPlaceholderText(/BOM \(Mumbai\)/i);

      fireEvent.change(fromInput, { target: { value: 'DEL' } });
      fireEvent.change(toInput, { target: { value: 'LHR' } });

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

      await waitFor(() => {
        expect(screen.getByRole('button', { name: /Search Flights/i })).toBeInTheDocument();
      });

      const fromInput = screen.getByPlaceholderText(/DEL \(Delhi\)/i);
      const toInput = screen.getByPlaceholderText(/BOM \(Mumbai\)/i);

      fireEvent.change(fromInput, { target: { value: 'JFK' } });
      fireEvent.change(toInput, { target: { value: 'LAX' } });

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

      await waitFor(() => {
        expect(
          screen.getByText('No flight offers were returned for this route and date.')
        ).toBeInTheDocument();
      });

      expect(screen.getByRole('button', { name: /Reset Search Filters/i })).toBeInTheDocument();
    });
  });
});
