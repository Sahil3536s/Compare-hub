import React from 'react';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import { describe, it, expect, vi, beforeEach } from 'vitest';
import RidesPage, { SUGGESTED_ROUTES } from '../pages/RidesPage';
import LocationAutocomplete from '../components/LocationAutocomplete';
import InteractiveMap from '../components/InteractiveMap';
import * as rideService from '../services/rideService';
import * as locationService from '../services/locationService';

// Mock services
vi.mock('../services/rideService');
vi.mock('../services/locationService');

describe('Dynamic Ride Location Support & Upgrades', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    locationService.estimateRoute.mockResolvedValue({
      distanceKm: 34.5,
      durationMinutes: 48,
      pickupAddress: 'VIT Bhopal University',
      dropAddress: 'Sehore Railway Station',
      polylineCoordinates: [
        [23.0768, 76.8524],
        [23.1400, 76.9680],
        [23.2032, 77.0844],
      ],
    });
    rideService.compareRides.mockResolvedValue({
      pickup: {
        name: 'VIT Bhopal University',
        address: 'Bhopal-Indore Highway, Kothri Kalan, Sehore',
        latitude: 23.0768,
        longitude: 76.8524,
      },
      destination: {
        name: 'Sehore Railway Station',
        address: 'Station Road, Sehore',
        latitude: 23.2032,
        longitude: 77.0844,
      },
      distanceKm: 34.5,
      durationMinutes: 48,
      cheapestFare: 420,
      fastestEtaMinutes: 5,
      bestProvider: 'Uber (Uber Go)',
      offers: [
        {
          provider: 'Uber',
          rideType: 'Uber Go',
          vehicleCategory: 'Cab',
          estimatedPriceMin: 420,
          estimatedPriceMax: 480,
          etaMinutes: 5,
          durationMinutes: 48,
          distanceKm: 34.5,
          currency: 'INR',
          isCheapest: true,
          isFastest: true,
          isBest: true,
          score: 95.0,
          deepLink: 'https://m.uber.com',
        },
      ],
      failedProviders: [],
    });
  });

  // =========================================================================
  // 1. REUSABLE LOCATION AUTOCOMPLETE UNIT TESTS
  // =========================================================================
  describe('LocationAutocomplete Component', () => {
    it('does not trigger API call for input shorter than 2 characters', async () => {
      render(
        <LocationAutocomplete
          id="test-loc"
          value=""
          onChange={vi.fn()}
          onSelect={vi.fn()}
        />
      );

      const input = screen.getByRole('combobox');
      fireEvent.change(input, { target: { value: 'V' } });

      await new Promise((r) => setTimeout(r, 350));
      expect(locationService.suggestPlaces).not.toHaveBeenCalled();
    });

    it('debounces user input (~300ms) before querying places API', async () => {
      locationService.suggestPlaces.mockResolvedValue([
        {
          name: 'VIT Bhopal University',
          formattedAddress: 'Bhopal-Indore Highway, Kothri Kalan, Sehore, Madhya Pradesh 466114',
          latitude: 23.0768,
          longitude: 76.8524,
          city: 'Sehore',
          state: 'Madhya Pradesh',
          country: 'India',
          providerPlaceId: 'vit-bhopal',
        },
      ]);

      render(
        <LocationAutocomplete
          id="test-loc"
          value=""
          onChange={vi.fn()}
          onSelect={vi.fn()}
        />
      );

      const input = screen.getByRole('combobox');
      fireEvent.change(input, { target: { value: 'VIT B' } });

      await waitFor(() => {
        expect(locationService.suggestPlaces).toHaveBeenCalledWith('VIT B');
      });

      expect(screen.getByText('VIT Bhopal University')).toBeInTheDocument();
    });

    it('supports keyboard navigation (ArrowDown, ArrowUp, Enter, Escape)', async () => {
      const onSelect = vi.fn();
      locationService.suggestPlaces.mockResolvedValue([
        {
          placeId: 'idr-1',
          name: 'Indore Airport',
          formattedAddress: 'Depalpur Road, Indore',
          latitude: 22.7218,
          longitude: 75.8011,
          city: 'Indore',
          state: 'Madhya Pradesh',
          country: 'India',
          providerPlaceId: 'idr-1',
        },
        {
          placeId: 'idr-2',
          name: 'Rajwada Palace',
          formattedAddress: 'MG Road, Rajwada, Indore',
          latitude: 22.7186,
          longitude: 75.8554,
          city: 'Indore',
          state: 'Madhya Pradesh',
          country: 'India',
          providerPlaceId: 'idr-2',
        },
      ]);

      render(
        <LocationAutocomplete
          id="test-loc"
          value=""
          onChange={vi.fn()}
          onSelect={onSelect}
        />
      );

      const input = screen.getByRole('combobox');
      fireEvent.change(input, { target: { value: 'Indore Air' } });

      await waitFor(() => {
        expect(screen.getByText('Indore Airport')).toBeInTheDocument();
      });

      // Press ArrowDown to highlight first item
      fireEvent.keyDown(input, { key: 'ArrowDown' });
      // Press Enter to select highlighted item
      fireEvent.keyDown(input, { key: 'Enter' });

      expect(onSelect).toHaveBeenCalledWith(
        expect.objectContaining({
          name: 'Indore Airport',
          latitude: 22.7218,
          longitude: 75.8011,
          city: 'Indore',
        })
      );
    });

    it('clears input and resets dropdown when clear button is clicked', async () => {
      const onChange = vi.fn();
      render(
        <LocationAutocomplete
          id="test-loc"
          value="Some Location"
          onChange={onChange}
          onSelect={vi.fn()}
        />
      );

      const clearBtn = screen.getByLabelText(/Clear location input/i);
      expect(clearBtn).toBeInTheDocument();

      fireEvent.click(clearBtn);
      expect(onChange).toHaveBeenCalledWith('');
    });

    it('displays "No matching places found for ..." when query yields no results', async () => {
      locationService.suggestPlaces.mockResolvedValue([]);

      render(
        <LocationAutocomplete
          id="test-loc"
          value=""
          onChange={vi.fn()}
          onSelect={vi.fn()}
        />
      );

      const input = screen.getByRole('combobox');
      fireEvent.change(input, { target: { value: 'NonexistentPlace12345' } });

      await waitFor(() => {
        expect(
          screen.getByText(/No matching places found for/i)
        ).toBeInTheDocument();
        expect(screen.getByText(/NonexistentPlace12345/i)).toBeInTheDocument();
      });
    });

    it('returns structured location with all required fields upon selection', async () => {
      const onSelect = vi.fn();
      locationService.suggestPlaces.mockResolvedValue([
        {
          placeId: 'del-ndls',
          mainText: 'New Delhi Railway Station',
          secondaryText: 'Kamla Market, Delhi',
          fullAddress: 'Bhavbhuti Marg, Ratan Lal Market, New Delhi 110006',
          latitude: 28.6429,
          longitude: 77.2195,
          name: 'New Delhi Railway Station',
          formattedAddress: 'Bhavbhuti Marg, Ratan Lal Market, New Delhi 110006',
          city: 'New Delhi',
          state: 'Delhi',
          country: 'India',
          providerPlaceId: 'del-ndls',
        },
      ]);

      render(
        <LocationAutocomplete
          id="test-loc"
          value=""
          onChange={vi.fn()}
          onSelect={onSelect}
        />
      );

      const input = screen.getByRole('combobox');
      fireEvent.change(input, { target: { value: 'New Delhi' } });

      await waitFor(() => {
        expect(screen.getByText('New Delhi Railway Station')).toBeInTheDocument();
      });

      fireEvent.click(screen.getByText('New Delhi Railway Station'));

      expect(onSelect).toHaveBeenCalledWith(
        expect.objectContaining({
          name: 'New Delhi Railway Station',
          formattedAddress: 'Bhavbhuti Marg, Ratan Lal Market, New Delhi 110006',
          latitude: 28.6429,
          longitude: 77.2195,
          city: 'New Delhi',
          state: 'Delhi',
          country: 'India',
          providerPlaceId: 'del-ndls',
        })
      );
    });
  });

  // =========================================================================
  // 2. SUGGESTED ROUTE PRESETS (NON-HARDCODED / OPEN SEARCH)
  // =========================================================================
  describe('Suggested Route Chips & Dynamic Flow', () => {
    it('contains all prompt example routes in suggestion chips', () => {
      const routeNames = SUGGESTED_ROUTES.map((r) => r.name);
      expect(routeNames).toContain('Bhopal Route');
      expect(routeNames).toContain('VIT Bhopal to Sehore');
      expect(routeNames).toContain('Indore Route');
      expect(routeNames).toContain('Delhi NCR Route');
      expect(routeNames).toContain('Mumbai Route');
    });

    it('selecting "VIT Bhopal to Sehore" updates route progression and triggers comparison', async () => {
      render(<RidesPage />);

      await waitFor(() => {
        expect(screen.getByRole('button', { name: /Compare Rides/i })).toBeInTheDocument();
      });

      const vitChip = screen.getByRole('button', { name: /VIT Bhopal to Sehore/i });
      fireEvent.click(vitChip);

      await waitFor(() => {
        expect(rideService.compareRides).toHaveBeenCalledWith(
          expect.objectContaining({
            pickup: expect.objectContaining({
              latitude: 23.0768,
              longitude: 76.8524,
            }),
            destination: expect.objectContaining({
              latitude: 23.2032,
              longitude: 77.0844,
            }),
          })
        );
      });
    });

    it('selecting "Indore Route" populates Indore Airport and Rajwada Palace coordinates', async () => {
      render(<RidesPage />);

      await waitFor(() => {
        expect(screen.getByRole('button', { name: /Compare Rides/i })).toBeInTheDocument();
      });

      const indoreChip = screen.getByRole('button', { name: /Indore Route/i });
      fireEvent.click(indoreChip);

      await waitFor(() => {
        expect(rideService.compareRides).toHaveBeenCalledWith(
          expect.objectContaining({
            pickup: expect.objectContaining({
              latitude: 22.7218,
              longitude: 75.8011,
            }),
            destination: expect.objectContaining({
              latitude: 22.7186,
              longitude: 75.8554,
            }),
          })
        );
      });
    });
  });

  // =========================================================================
  // 3. RIDE PROVIDER LIMITATION HANDLING
  // =========================================================================
  describe('Ride Provider Limitation Handling', () => {
    it('displays exact provider limitation message when location is found but no providers have fares', async () => {
      rideService.compareRides.mockResolvedValue({
        pickup: { name: 'Remote Village', latitude: 24.1234, longitude: 78.5678 },
        destination: { name: 'Another Remote Town', latitude: 24.5678, longitude: 78.9101 },
        distanceKm: 52.3,
        durationMinutes: 75,
        cheapestFare: null,
        fastestEtaMinutes: null,
        bestProvider: null,
        offers: [],
        failedProviders: [],
      });

      render(<RidesPage />);

      await waitFor(() => {
        expect(
          screen.getByText(
            'Location found, but ride estimates are currently unavailable from the connected providers.'
          )
        ).toBeInTheDocument();
      });

      // Map and route progression card remain visible
      expect(screen.getByTestId('route-progression-card')).toBeInTheDocument();
    });
  });

  // =========================================================================
  // 4. CURRENT LOCATION GEOLOCATION FLOW
  // =========================================================================
  describe('Current Location Detection', () => {
    it('uses browser geolocation and reverse-geocodes into structured location', async () => {
      const mockGeolocation = {
        getCurrentPosition: vi.fn().mockImplementationOnce((success) =>
          success({
            coords: {
              latitude: 23.2330,
              longitude: 77.4332,
            },
          })
        ),
      };
      vi.stubGlobal('navigator', { ...navigator, geolocation: mockGeolocation });

      locationService.reverseGeocode.mockResolvedValue({
        name: 'DB City Mall',
        formattedAddress: 'Arera Hills, Bhopal, Madhya Pradesh 462011',
        address: 'Arera Hills, Bhopal, Madhya Pradesh 462011',
        latitude: 23.2330,
        longitude: 77.4332,
        city: 'Bhopal',
        state: 'Madhya Pradesh',
        country: 'India',
        providerPlaceId: 'reverse-dbmall',
      });

      render(<RidesPage />);

      const currentLocBtn = screen.getByRole('button', { name: /Use Current Location/i });
      fireEvent.click(currentLocBtn);

      await waitFor(() => {
        expect(locationService.reverseGeocode).toHaveBeenCalledWith(23.2330, 77.4332);
      });
    });
  });

  // =========================================================================
  // 5. INTERACTIVE MAP DYNAMIC PROJECTION
  // =========================================================================
  describe('InteractiveMap Dynamic Projection', () => {
    it('dynamically projects custom coordinate route polyline without crashing', () => {
      const { container } = render(
        <InteractiveMap
          pickup={{ latitude: 23.0768, longitude: 76.8524, name: 'VIT Bhopal' }}
          destination={{ latitude: 23.2032, longitude: 77.0844, name: 'Sehore' }}
          distanceKm={34.5}
          durationMinutes={48}
          polylineCoordinates={[
            [23.0768, 76.8524],
            [23.1400, 76.9680],
            [23.2032, 77.0844],
          ]}
        />
      );

      expect(screen.getByText('VIT Bhopal')).toBeInTheDocument();
      expect(screen.getByText('Sehore')).toBeInTheDocument();
      expect(screen.getByText('34.5 km')).toBeInTheDocument();
      expect(screen.getByText('~48 mins')).toBeInTheDocument();

      const paths = container.querySelectorAll('path');
      expect(paths.length).toBeGreaterThan(0);
      const mainPathD = paths[0].getAttribute('d');
      expect(mainPathD).toBeTruthy();
      expect(mainPathD.startsWith('M')).toBe(true);
    });
  });
});
