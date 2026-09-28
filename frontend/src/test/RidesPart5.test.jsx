import React from 'react';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import { describe, it, expect, vi, beforeEach } from 'vitest';
import RidesPage from '../pages/RidesPage';
import RideOfferCard from '../components/RideOfferCard';
import RideCardSkeleton from '../components/RideCardSkeleton';
import RideComparisonModal from '../components/rides/RideComparisonModal';
import InteractiveMap from '../components/InteractiveMap';
import * as rideService from '../services/rideService';
import * as locationService from '../services/locationService';

// Mock services
vi.mock('../services/rideService');
vi.mock('../services/locationService');

describe('Part 5 UI Upgrades: Ride Search, Comparison & Map Integration', () => {
  const mockRides = [
    {
      provider: 'Uber',
      rideType: 'Uber Go',
      vehicleCategory: 'Cab',
      estimatedPriceMin: 280,
      estimatedPriceMax: 320,
      etaMinutes: 3,
      durationMinutes: 28,
      distanceKm: 12.4,
      currency: 'INR',
      isCheapest: false,
      isFastest: false,
      isBest: true,
      score: 95.0,
      deepLink: 'https://m.uber.com',
    },
    {
      provider: 'Ola',
      rideType: 'Ola Mini',
      vehicleCategory: 'Cab',
      estimatedPriceMin: 265,
      estimatedPriceMax: 310,
      etaMinutes: 4,
      durationMinutes: 28,
      distanceKm: 12.4,
      currency: 'INR',
      isCheapest: true,
      isFastest: false,
      isBest: false,
      score: 91.0,
      deepLink: 'https://book.olacabs.com',
    },
    {
      provider: 'Rapido',
      rideType: 'Rapido Cab',
      vehicleCategory: 'Cab',
      estimatedPriceMin: 270,
      estimatedPriceMax: 315,
      etaMinutes: 2,
      durationMinutes: 28,
      distanceKm: 12.4,
      currency: 'INR',
      isCheapest: false,
      isFastest: true,
      isBest: false,
      score: 93.0,
      deepLink: 'https://www.rapido.bike',
    },
  ];

  const mockCompareResponse = {
    pickup: { address: 'Bhopal Railway Station, Bhopal', latitude: 23.2599, longitude: 77.4126 },
    destination: { address: 'Raja Bhoj Airport, Bhopal', latitude: 23.2875, longitude: 77.3378 },
    distanceKm: 12.4,
    durationMinutes: 28,
    cheapestFare: 265,
    fastestEtaMinutes: 2,
    bestProvider: 'Uber (Uber Go)',
    offers: mockRides,
    failedProviders: [],
  };

  const mockRouteResponse = {
    distanceKm: 12.4,
    durationMinutes: 28,
    pickupAddress: 'Bhopal Railway Station, Bhopal',
    dropAddress: 'Raja Bhoj Airport, Bhopal',
    polylineCoordinates: [
      [23.2599, 77.4126],
      [23.2875, 77.3378],
    ],
  };

  beforeEach(() => {
    vi.clearAllMocks();
    locationService.estimateRoute.mockResolvedValue(mockRouteResponse);
    rideService.compareRides.mockResolvedValue(mockCompareResponse);
  });

  // =========================================================================
  // 1. RIDE SEARCH & INPUT VALIDATIONS
  // =========================================================================
  describe('1. Ride Search & Input Validation', () => {
    it('renders pickup, destination, and Compare Rides button', async () => {
      render(<RidesPage />);

      await waitFor(() => {
        expect(screen.getByRole('button', { name: /Compare Rides/i })).toBeInTheDocument();
      });

      expect(screen.getByLabelText(/Pickup Location/i)).toBeInTheDocument();
      expect(screen.getByLabelText(/^Destination$/i)).toBeInTheDocument();
      expect(screen.getByRole('button', { name: /Use Current Location/i })).toBeInTheDocument();
    });

    it('swaps pickup and destination when swap button is clicked', async () => {
      render(<RidesPage />);

      await waitFor(() => {
        expect(screen.getByRole('button', { name: /Compare Rides/i })).toBeInTheDocument();
      });

      const pickupInput = screen.getByLabelText(/Pickup Location/i);
      const dropInput = screen.getByLabelText(/^Destination$/i);

      expect(pickupInput.value).toBe('Bhopal Railway Station');
      expect(dropInput.value).toBe('Bhopal Airport');

      const swapBtn = screen.getByLabelText(/Swap pickup and destination/i);
      fireEvent.click(swapBtn);

      expect(pickupInput.value).toBe('Bhopal Airport');
      expect(dropInput.value).toBe('Bhopal Railway Station');
    });

    it('validates that pickup and destination cannot be identical', async () => {
      render(<RidesPage />);

      await waitFor(() => {
        expect(screen.getByRole('button', { name: /Compare Rides/i })).toBeInTheDocument();
      });

      const dropInput = screen.getByLabelText(/^Destination$/i);
      fireEvent.change(dropInput, { target: { value: 'Bhopal Railway Station' } });

      const compareBtn = screen.getByRole('button', { name: /Compare Rides/i });
      fireEvent.click(compareBtn);

      await waitFor(() => {
        expect(screen.getByRole('alert')).toHaveTextContent(
          /Pickup and destination cannot be the same location/i
        );
      });
    });

    it('validates that pickup is required', async () => {
      render(<RidesPage />);

      await waitFor(() => {
        expect(screen.getByRole('button', { name: /Compare Rides/i })).toBeInTheDocument();
      });

      const pickupInput = screen.getByLabelText(/Pickup Location/i);
      fireEvent.change(pickupInput, { target: { value: '' } });

      const compareBtn = screen.getByRole('button', { name: /Compare Rides/i });
      fireEvent.click(compareBtn);

      await waitFor(() => {
        expect(screen.getByRole('alert')).toHaveTextContent(/Pickup location is required/i);
      });
    });

    it('fetches and displays location suggestions on user input', async () => {
      locationService.suggestPlaces.mockResolvedValue([
        {
          placeId: 'bho-dbmall',
          mainText: 'DB City Mall',
          secondaryText: 'Arera Hills, Bhopal, Madhya Pradesh',
          fullAddress: 'DB City Mall, Arera Hills, Bhopal 462011',
          latitude: 23.2330,
          longitude: 77.4332,
        },
      ]);

      render(<RidesPage />);

      const pickupInput = screen.getByLabelText(/Pickup Location/i);
      fireEvent.change(pickupInput, { target: { value: 'DB' } });

      await waitFor(() => {
        expect(locationService.suggestPlaces).toHaveBeenCalledWith('DB');
        expect(screen.getByText('DB City Mall')).toBeInTheDocument();
      });

      // Click suggestion
      fireEvent.click(screen.getByText('DB City Mall'));
      expect(pickupInput.value).toBe('DB City Mall');
    });
  });

  // =========================================================================
  // 2. ROUTE INFORMATION & MAP INTEGRATION
  // =========================================================================
  describe('2. Route Information & Map Integration', () => {
    it('displays route progression card with pickup, distance, duration, and destination', async () => {
      render(<RidesPage />);

      await waitFor(() => {
        expect(screen.getByTestId('route-progression-card')).toBeInTheDocument();
      });

      const routeCard = screen.getByTestId('route-progression-card');
      expect(routeCard).toHaveTextContent(/Bhopal Railway Station/i);
      expect(routeCard).toHaveTextContent(/12.4 km/i);
      expect(routeCard).toHaveTextContent(/28 min travel/i);
      expect(routeCard).toHaveTextContent(/Bhopal Airport/i);
    });

    it('renders InteractiveMap with pickup marker, destination marker, distance, and duration', () => {
      render(
        <InteractiveMap
          pickup={{ address: 'Bhopal Railway Station' }}
          destination={{ address: 'Bhopal Airport' }}
          distanceKm={12.4}
          durationMinutes={28}
        />
      );

      expect(screen.getByText(/12.4 km/i)).toBeInTheDocument();
      expect(screen.getByText(/~28 mins/i)).toBeInTheDocument();
      expect(screen.getByText(/Bhopal Railway Station/i)).toBeInTheDocument();
      expect(screen.getByText(/Bhopal Airport/i)).toBeInTheDocument();
      expect(screen.getByLabelText(/Zoom in map/i)).toBeInTheDocument();
      expect(screen.getByLabelText(/Zoom out map/i)).toBeInTheDocument();
    });

    it('gracefully degrades map rendering if map API fails without crashing', () => {
      render(
        <InteractiveMap
          pickup={{ address: 'Bhopal Station' }}
          destination={{ address: 'Bhopal Airport' }}
          hasError={true}
        />
      );

      expect(screen.getByTestId('map-fallback')).toBeInTheDocument();
      expect(screen.getByText(/Live Map View Unavailable/i)).toBeInTheDocument();
    });
  });

  // =========================================================================
  // 3. RIDE RESULTS & COMPARISON CARDS
  // =========================================================================
  describe('3. Ride Results & Ranking Tabs', () => {
    it('renders RideOfferCard with provider badge, vehicle, fare, ETA, duration, and deep link', () => {
      render(<RideOfferCard ride={mockRides[0]} />);

      expect(screen.getByText('UBER')).toBeInTheDocument();
      expect(screen.getByText('Uber Go')).toBeInTheDocument();
      expect(screen.getByText(/Cab/i)).toBeInTheDocument();
      expect(screen.getByText('3 mins')).toBeInTheDocument();
      expect(screen.getByText('28 min')).toBeInTheDocument();
      expect(screen.getByText('12.4 km')).toBeInTheDocument();
      expect(screen.getByText(/280/)).toBeInTheDocument();
      expect(screen.getByText(/320/)).toBeInTheDocument();
      expect(screen.getByText(/Best Value/i)).toBeInTheDocument();
      expect(screen.getByRole('link', { name: /Open in Uber/i })).toHaveAttribute(
        'href',
        'https://m.uber.com'
      );
    });

    it('renders 3 ranking tabs (Cheapest, Fastest Pickup, Best Value) and triggers backend sorting', async () => {
      render(<RidesPage />);

      await waitFor(() => {
        expect(screen.getByRole('button', { name: /Cheapest Lowest Estimated Fare/i })).toBeInTheDocument();
        expect(screen.getByRole('button', { name: /Fastest Pickup Quickest Driver ETA/i })).toBeInTheDocument();
        expect(screen.getByRole('button', { name: /Best Value Weighted Fare & ETA/i })).toBeInTheDocument();
      });

      // Click Cheapest tab
      fireEvent.click(screen.getByRole('button', { name: /Cheapest Lowest Estimated Fare/i }));

      await waitFor(() => {
        expect(rideService.compareRides).toHaveBeenCalledWith(
          expect.objectContaining({ sortBy: 'cheapest' })
        );
      });

      // Click Fastest Pickup tab
      fireEvent.click(screen.getByRole('button', { name: /Fastest Pickup Quickest Driver ETA/i }));

      await waitFor(() => {
        expect(rideService.compareRides).toHaveBeenCalledWith(
          expect.objectContaining({ sortBy: 'fastest' })
        );
      });
    });

    it('renders side-by-side comparison modal with Uber, Ola, Rapido and highlights best values', async () => {
      render(
        <RideComparisonModal
          isOpen={true}
          onClose={vi.fn()}
          rides={mockRides}
          pickupAddress="Bhopal Station"
          destinationAddress="Bhopal Airport"
        />
      );

      // Verify Provider Columns
      expect(screen.getByRole('columnheader', { name: /Uber/i })).toBeInTheDocument();
      expect(screen.getByRole('columnheader', { name: /Ola/i })).toBeInTheDocument();
      expect(screen.getByRole('columnheader', { name: /Rapido/i })).toBeInTheDocument();

      // Verify Highlight Badges
      expect(screen.getByText(/Lowest Fare/i)).toBeInTheDocument();
      expect(screen.getByText(/Fastest Pickup/i)).toBeInTheDocument();
      expect(screen.getAllByText(/Best Value/i).length).toBeGreaterThan(0);

      // Verify Attributes
      expect(screen.getByText(/Estimated Fare/i)).toBeInTheDocument();
      expect(screen.getByText(/Pickup ETA/i)).toBeInTheDocument();
      expect(screen.getByText(/Vehicle Type/i)).toBeInTheDocument();
      expect(screen.getByText(/Est. Duration/i)).toBeInTheDocument();
      expect(screen.getAllByText(/Distance/i).length).toBeGreaterThan(0);
    });
  });

  // =========================================================================
  // 4. STATES: LOADING, PARTIAL FAILURE, EMPTY, ERROR
  // =========================================================================
  describe('4. Lifecycle & Partial Failure States', () => {
    it('shows loading message "Finding available ride options..." and skeleton cards during search', () => {
      rideService.compareRides.mockReturnValue(new Promise(() => {})); // pending
      render(<RidesPage />);

      expect(screen.getByText(/Finding available ride options.../i)).toBeInTheDocument();
      expect(screen.getByTestId('ride-skeleton-list')).toBeInTheDocument();
    });

    it('displays partial failure notice "Some providers couldn\'t be reached." when failedProviders is present', async () => {
      rideService.compareRides.mockResolvedValue({
        ...mockCompareResponse,
        failedProviders: ['Rapido'],
      });

      render(<RidesPage />);

      await waitFor(() => {
        expect(screen.getByRole('status')).toHaveTextContent(
          /Some providers couldn't be reached/i
        );
        expect(screen.getByRole('status')).toHaveTextContent(
          /Live estimates from Rapido were temporarily unavailable/i
        );
      });
    });

    it('displays empty state with action when no rides match filter', async () => {
      rideService.compareRides.mockResolvedValue({
        ...mockCompareResponse,
        offers: [],
      });

      render(<RidesPage />);

      await waitFor(() => {
        expect(screen.getByText(/No ride options available/i)).toBeInTheDocument();
        expect(screen.getByRole('button', { name: /Show All Rides/i })).toBeInTheDocument();
      });
    });

    it('displays friendly error state on network or provider exception', async () => {
      rideService.compareRides.mockRejectedValue(new Error('Gateway timeout from cab dispatch server'));

      render(<RidesPage />);

      await waitFor(() => {
        expect(screen.getByText(/Ride Comparison Engine Error/i)).toBeInTheDocument();
        expect(screen.getByText(/Gateway timeout from cab dispatch server/i)).toBeInTheDocument();
        expect(screen.getByRole('button', { name: /Try Again/i })).toBeInTheDocument();
      });
    });
  });
});
