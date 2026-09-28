import React from 'react';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import { describe, it, expect, vi, beforeEach } from 'vitest';
import { BrowserRouter } from 'react-router-dom';
import SmartJourneyPage from '../pages/SmartJourneyPage';
import * as smartJourneyService from '../services/smartJourneyService';

vi.mock('../services/smartJourneyService');

const renderWithRouter = (ui) => {
  return render(<BrowserRouter>{ui}</BrowserRouter>);
};

describe('Smart Journey 2.0 Door-to-Door Optimization Frontend', () => {
  const mockJourneyResponse = {
    origin: 'Saket, South Delhi',
    destination: 'Calangute, North Goa',
    travelers: 1,
    airportBufferMinutes: 90,
    costWeight: 50,
    timeWeight: 50,
    tradeoffSummary: 'IndiGo Express saves ₹1,280 compared to Air India with only 20m extra transit time.',
    executionTimeMs: 14,
    cheapestJourney: {
      id: 'opt-cheapest-2',
      title: 'Akasa Air Value Saver + Airport Shuttle',
      totalCost: 4590,
      totalDurationMinutes: 375,
      formattedTotalDuration: '6h 15m',
      totalWaitingTimeMinutes: 120,
      transferCount: 2,
      travelerCount: 1,
      costPerTraveler: 4590,
      classification: 'CHEAPEST',
      recommendationReason: 'Cheapest door-to-door itinerary across all airlines.',
      isCheapest: true,
      segments: [
        {
          id: 'seg-1',
          type: 'RIDE_TO_AIRPORT',
          typeName: 'Ride to Airport',
          provider: 'Airport Express Shuttle',
          origin: 'Saket, South Delhi',
          destination: 'DEL Airport',
          departureTime: '04:30 AM',
          arrivalTime: '05:25 AM',
          price: 140,
          durationMinutes: 55,
          formattedDuration: '55m',
          icon: '🚌',
        },
        {
          id: 'seg-2',
          type: 'AIRPORT_BUFFER',
          typeName: 'Airport Security & Gate Buffer',
          provider: 'Terminal Security',
          origin: 'DEL Airport',
          destination: 'DEL Airport',
          departureTime: '05:25 AM',
          arrivalTime: '07:00 AM',
          price: 0,
          durationMinutes: 90,
          formattedDuration: '1h 30m',
          icon: '⏳',
        },
        {
          id: 'seg-3',
          type: 'FLIGHT',
          typeName: 'Non-Stop Flight',
          provider: 'Akasa Air QP-1322',
          origin: 'DEL Airport',
          destination: 'GOI Airport',
          departureTime: '07:00 AM',
          arrivalTime: '09:25 AM',
          price: 3900,
          durationMinutes: 145,
          formattedDuration: '2h 25m',
          icon: '✈️',
        },
        {
          id: 'seg-4',
          type: 'ARRIVAL_BUFFER',
          typeName: 'Deboarding & Baggage Collection',
          provider: 'Airport Arrival',
          origin: 'GOI Airport',
          destination: 'GOI Airport',
          departureTime: '09:25 AM',
          arrivalTime: '09:55 AM',
          price: 0,
          durationMinutes: 30,
          formattedDuration: '30m',
          icon: '🧳',
        },
        {
          id: 'seg-5',
          type: 'RIDE_FROM_AIRPORT',
          typeName: 'Ride to Hotel / Destination',
          provider: 'Uber Go',
          origin: 'GOI Airport',
          destination: 'Calangute, North Goa',
          departureTime: '09:55 AM',
          arrivalTime: '10:45 AM',
          price: 550,
          durationMinutes: 50,
          formattedDuration: '50m',
          icon: '🚗',
        },
      ],
    },
    balancedJourney: {
      id: 'opt-balanced-1',
      title: 'IndiGo Express + Uber Connect',
      totalCost: 5820,
      totalDurationMinutes: 350,
      formattedTotalDuration: '5h 50m',
      totalWaitingTimeMinutes: 120,
      transferCount: 2,
      travelerCount: 1,
      costPerTraveler: 5820,
      classification: 'BALANCED',
      recommendationReason: 'Best optimal balance between total price (₹5,820) and transit duration (5h 50m).',
      isBalanced: true,
      segments: [
        {
          id: 'b-seg-1',
          type: 'RIDE_TO_AIRPORT',
          typeName: 'Ride to Airport',
          provider: 'Uber Go',
          origin: 'Saket, South Delhi',
          destination: 'DEL Terminal 3',
          departureTime: '06:00 AM',
          arrivalTime: '06:40 AM',
          price: 350,
          durationMinutes: 40,
          formattedDuration: '40m',
          icon: '🚗',
        },
        {
          id: 'b-seg-2',
          type: 'AIRPORT_BUFFER',
          typeName: 'Airport Security & Gate Buffer',
          provider: 'Terminal Security',
          origin: 'DEL Terminal 3',
          destination: 'DEL Terminal 3',
          departureTime: '06:40 AM',
          arrivalTime: '08:15 AM',
          price: 0,
          durationMinutes: 90,
          formattedDuration: '1h 30m',
          icon: '⏳',
        },
        {
          id: 'b-seg-3',
          type: 'FLIGHT',
          typeName: 'Non-Stop Flight',
          provider: 'IndiGo 6E-204',
          origin: 'DEL Terminal 3',
          destination: 'Goa Dabolim Airport (GOI)',
          departureTime: '08:15 AM',
          arrivalTime: '10:30 AM',
          price: 4850,
          durationMinutes: 135,
          formattedDuration: '2h 15m',
          icon: '✈️',
        },
        {
          id: 'b-seg-4',
          type: 'ARRIVAL_BUFFER',
          typeName: 'Deboarding & Baggage Collection',
          provider: 'Airport Arrival',
          origin: 'Goa Dabolim Airport (GOI)',
          destination: 'Goa Dabolim Airport (GOI)',
          departureTime: '10:30 AM',
          arrivalTime: '11:00 AM',
          price: 0,
          durationMinutes: 30,
          formattedDuration: '30m',
          icon: '🧳',
        },
        {
          id: 'b-seg-5',
          type: 'RIDE_FROM_AIRPORT',
          typeName: 'Ride to Hotel / Destination',
          provider: 'Ola Prime Sedan',
          origin: 'Goa Dabolim Airport (GOI)',
          destination: 'Calangute, North Goa',
          departureTime: '11:00 AM',
          arrivalTime: '11:50 AM',
          price: 620,
          durationMinutes: 50,
          formattedDuration: '50m',
          icon: '🚕',
        },
      ],
    },
    fastestJourney: {
      id: 'opt-fastest-3',
      title: 'Air India Prime Express + Priority Drop',
      totalCost: 7200,
      totalDurationMinutes: 310,
      formattedTotalDuration: '5h 10m',
      totalWaitingTimeMinutes: 120,
      transferCount: 2,
      travelerCount: 1,
      costPerTraveler: 7200,
      classification: 'FASTEST',
      recommendationReason: 'Fastest door-to-door transit time with priority cabs.',
      isFastest: true,
      segments: [],
    },
  };

  mockJourneyResponse.topRecommendedJourney = mockJourneyResponse.balancedJourney;
  mockJourneyResponse.allCombinations = [
    mockJourneyResponse.balancedJourney,
    mockJourneyResponse.cheapestJourney,
    mockJourneyResponse.fastestJourney,
  ];

  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('renders Smart Journey page, highlight cards, and timeline visualizer', async () => {
    smartJourneyService.optimizeSmartJourney.mockResolvedValueOnce(mockJourneyResponse);

    renderWithRouter(<SmartJourneyPage />);

    await waitFor(() => {
      expect(screen.getByTestId('smart-journey-page')).toBeInTheDocument();
    });

    // Check headings
    expect(screen.getAllByText(/Smart Journey/i).length).toBeGreaterThanOrEqual(1);
    expect(screen.getByText('2.0')).toBeInTheDocument();

    // Check highlight cards
    expect(screen.getByText(/Cheapest Itinerary/i)).toBeInTheDocument();
    expect(screen.getByText(/Recommended Balanced/i)).toBeInTheDocument();
    expect(screen.getByText(/Fastest Transit/i)).toBeInTheDocument();

    // Check Timeline Visualizer
    expect(screen.getByTestId('journey-timeline-visualizer')).toBeInTheDocument();
    expect(screen.getAllByText('IndiGo Express + Uber Connect').length).toBeGreaterThanOrEqual(1);
    expect(screen.getAllByText('₹5,820').length).toBeGreaterThanOrEqual(1);
    expect(screen.getByText('Uber Go')).toBeInTheDocument();
    expect(screen.getByText('IndiGo 6E-204')).toBeInTheDocument();
    expect(screen.getByText('Ola Prime Sedan')).toBeInTheDocument();
  });

  it('allows user to change airport buffer and triggers re-optimization', async () => {
    smartJourneyService.optimizeSmartJourney.mockResolvedValueOnce(mockJourneyResponse);
    smartJourneyService.optimizeSmartJourney.mockResolvedValueOnce({
      ...mockJourneyResponse,
      airportBufferMinutes: 60,
    });

    renderWithRouter(<SmartJourneyPage />);

    await waitFor(() => {
      expect(screen.getByText(/60 min \(Express\)/i)).toBeInTheDocument();
    });

    const bufferBtn = screen.getByRole('button', { name: /60 min \(Express\)/i });
    fireEvent.click(bufferBtn);

    await waitFor(() => {
      expect(smartJourneyService.optimizeSmartJourney).toHaveBeenCalledWith(
        expect.objectContaining({
          airportBufferMinutes: 60,
        })
      );
    });
  });

  it('allows user to switch between Cheapest, Balanced, and Fastest presets', async () => {
    smartJourneyService.optimizeSmartJourney.mockResolvedValueOnce(mockJourneyResponse);
    smartJourneyService.optimizeSmartJourney.mockResolvedValueOnce({
      ...mockJourneyResponse,
      costWeight: 100,
    });

    renderWithRouter(<SmartJourneyPage />);

    await waitFor(() => {
      expect(screen.getByRole('button', { name: /Cheapest/i })).toBeInTheDocument();
    });

    const cheapestPreset = screen.getByRole('button', { name: /Cheapest/i });
    fireEvent.click(cheapestPreset);

    await waitFor(() => {
      expect(smartJourneyService.optimizeSmartJourney).toHaveBeenCalledWith(
        expect.objectContaining({
          costWeight: 100,
          timeWeight: 0,
        })
      );
    });
  });

  it('switches selected timeline when an alternative journey card is clicked', async () => {
    smartJourneyService.optimizeSmartJourney.mockResolvedValueOnce(mockJourneyResponse);

    renderWithRouter(<SmartJourneyPage />);

    await waitFor(() => {
      expect(screen.getAllByText('Akasa Air Value Saver + Airport Shuttle').length).toBeGreaterThanOrEqual(1);
    });

    // Click on the cheapest card
    const cheapCardTitle = screen.getAllByText('Akasa Air Value Saver + Airport Shuttle')[0];
    fireEvent.click(cheapCardTitle);

    await waitFor(() => {
      expect(screen.getByText('Akasa Air QP-1322')).toBeInTheDocument();
    });
  });
});
