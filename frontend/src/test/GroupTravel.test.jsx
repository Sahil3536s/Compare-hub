import React from 'react';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { describe, it, expect, vi, beforeEach } from 'vitest';
import GroupTravelOptimizer from '../components/GroupTravelOptimizer';
import * as groupTravelService from '../services/groupTravelService';

vi.mock('../services/groupTravelService', () => ({
  optimizeGroupTravel: vi.fn(),
}));

describe('Phase 33: Group Travel Optimization Component', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('renders input fields, traveler stepper, and priority options', () => {
    render(<GroupTravelOptimizer initialOrigin="Delhi" initialDestination="Jaipur" />);

    expect(screen.getByTestId('group-travel-optimizer')).toBeInTheDocument();
    expect(screen.getByDisplayValue('Delhi')).toBeInTheDocument();
    expect(screen.getByDisplayValue('Jaipur')).toBeInTheDocument();
    expect(screen.getByText('4 People')).toBeInTheDocument();
    expect(screen.getByRole('button', { name: /💰 Cheapest/i })).toBeInTheDocument();
    expect(screen.getByRole('button', { name: /⚡ Fastest/i })).toBeInTheDocument();
    expect(screen.getByRole('button', { name: /⚖️ Balanced/i })).toBeInTheDocument();
  });

  it('increments and decrements traveler count stepper correctly', async () => {
    render(<GroupTravelOptimizer initialOrigin="Delhi" initialDestination="Jaipur" />);

    const incBtn = screen.getByRole('button', { name: /Increase travelers/i });
    const decBtn = screen.getByRole('button', { name: /Decrease travelers/i });

    fireEvent.click(incBtn);
    expect(screen.getByText('5 People')).toBeInTheDocument();

    fireEvent.click(decBtn);
    expect(screen.getByText('4 People')).toBeInTheDocument();
  });

  it('submits group travel query and displays comparison results and cost breakdown', async () => {
    const mockResponse = {
      origin: 'Delhi',
      destination: 'Jaipur',
      numberOfTravelers: 4,
      priority: 'CHEAPEST',
      groupInsights: 'For 4 travelers, hiring a Direct Cab saves ₹12,200 overall (₹1,600/person vs ₹4,650/person by air).',
      options: [
        {
          id: 'direct_cab_uber_xl',
          mode: 'DIRECT_RIDE',
          title: 'Direct Cab (Uber XL)',
          providerName: 'Uber (XL)',
          totalCost: 6400,
          costPerPerson: 1600,
          estimatedTravelTimeMinutes: 300,
          formattedDuration: '5h 00m',
          numberOfTravelers: 4,
          vehicleCapacityNote: '1 SUV / XL (Capacity: 6 pax)',
          requiredConnections: 0,
          score: 95.0,
          isRecommended: true,
          classification: 'BEST_GROUP_VALUE',
          recommendationReason: 'Lowest overall cost for 4 traveler(s) at ₹6400 (₹1600/person).',
          breakdown: [
            {
              label: '1x Uber XL Cab Fare',
              amount: 6400,
              description: 'Shared by 4 people',
            },
          ],
          legs: [
            {
              legType: 'RIDE',
              title: 'Direct Door-to-Door Cab (Uber XL)',
              origin: 'Delhi',
              destination: 'Jaipur',
              durationMinutes: 300,
              cost: 6400,
              provider: 'Uber',
            },
          ],
        },
        {
          id: 'flight_and_rides_indigo',
          mode: 'FLIGHT_AND_RIDE',
          title: 'Flight + Airport Cabs (IndiGo)',
          providerName: 'IndiGo + Airport Cabs',
          totalCost: 18600,
          costPerPerson: 4650,
          estimatedTravelTimeMinutes: 180,
          formattedDuration: '3h 00m',
          numberOfTravelers: 4,
          vehicleCapacityNote: '4 Flight Seats + 1 Airport Cab',
          requiredConnections: 2,
          score: 72.0,
          isRecommended: false,
          classification: 'FASTEST_OPTION',
          breakdown: [
            {
              label: '4x Flight Tickets (IndiGo)',
              amount: 17100,
            },
            {
              label: 'Airport Drop Transfer',
              amount: 800,
            },
            {
              label: 'Airport Pickup Transfer',
              amount: 700,
            },
          ],
        },
      ],
    };

    groupTravelService.optimizeGroupTravel.mockResolvedValueOnce(mockResponse);

    render(<GroupTravelOptimizer initialOrigin="Delhi" initialDestination="Jaipur" />);

    const compareBtn = screen.getByRole('button', { name: /Compare Group Modes/i });
    fireEvent.click(compareBtn);

    await waitFor(() => {
      expect(groupTravelService.optimizeGroupTravel).toHaveBeenCalledWith({
        origin: 'Delhi',
        destination: 'Jaipur',
        travelDate: '2026-09-10',
        numberOfTravelers: 4,
        budget: null,
        priority: 'CHEAPEST',
      });

      expect(screen.getByTestId('group-travel-results')).toBeInTheDocument();
      expect(screen.getByText(/Group Economy Analysis/i)).toBeInTheDocument();
      expect(screen.getByText(/Direct Cab \(Uber XL\)/i)).toBeInTheDocument();
      expect(screen.getByText('₹6,400')).toBeInTheDocument();
      expect(screen.getByText('₹1,600 / person')).toBeInTheDocument();
      expect(screen.getByText(/Top Recommendation \(CHEAPEST\)/i)).toBeInTheDocument();
    });

    // Expand itemized breakdown on first option
    const toggleBtns = screen.getAllByRole('button', { name: /View Itemized Breakdown & Legs/i });
    fireEvent.click(toggleBtns[0]);

    await waitFor(() => {
      expect(screen.getByText(/Cost Breakdown \(4 Travelers\)/i)).toBeInTheDocument();
      expect(screen.getByText(/1x Uber XL Cab Fare/i)).toBeInTheDocument();
      expect(screen.getByText(/Journey Steps & Multi-Modal Transfers/i)).toBeInTheDocument();
    });
  });
});
