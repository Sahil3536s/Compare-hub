import React from 'react';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import { describe, it, expect, vi, beforeEach } from 'vitest';
import { BrowserRouter } from 'react-router-dom';
import BudgetAssistantPage from '../pages/BudgetAssistantPage';
import * as budgetService from '../services/budgetService';

vi.mock('../services/budgetService');

const renderWithRouter = (ui) => {
  return render(<BrowserRouter>{ui}</BrowserRouter>);
};

describe('Constraint-Based Budget Assistant 2.0 Frontend', () => {
  const mockBudgetResponse = {
    constraint: {
      origin: 'Delhi',
      destination: 'Goa',
      travelers: 3,
      maxBudget: 25000,
      preference: 'BALANCED',
    },
    maxBudget: 25000,
    exceededBudgetOptionsCount: 1,
    lowestAvailablePrice: 15800,
    summaryText: 'Budget: ₹25,000. Found 3 valid itineraries for 3 travelers. IndiGo Express is recommended (₹7,800 remaining).',
    executionTimeMs: 18,
    bestPlan: {
      id: 'plan-opt-balanced-1',
      title: 'IndiGo Express + Uber Connect',
      totalCost: 17200,
      costPerTraveler: 5733.33,
      remainingBudget: 7800,
      budgetUtilizationPercent: 68.8,
      durationMinutes: 350,
      formattedDuration: '5h 50m',
      transferCount: 2,
      comfortLevel: 'MEDIUM',
      classification: 'BEST_VALUE',
      recommendationReason: 'Optimal balance of convenience and price, utilizing 68.8% of budget.',
      isWithinBudget: true,
      journeyOption: {
        id: 'opt-balanced-1',
        title: 'IndiGo Express + Uber Connect',
        totalCost: 17200,
        formattedTotalDuration: '5h 50m',
        transferCount: 2,
        costPerTraveler: 5733.33,
        classification: 'BALANCED',
        segments: [
          {
            id: 'seg-1',
            type: 'RIDE_TO_AIRPORT',
            typeName: 'Ride to Airport',
            provider: 'Uber Go',
            origin: 'Delhi',
            destination: 'DEL Airport',
            departureTime: '06:00 AM',
            arrivalTime: '06:40 AM',
            price: 350,
            durationMinutes: 40,
            formattedDuration: '40m',
            icon: '🚗',
          },
          {
            id: 'seg-2',
            type: 'AIRPORT_BUFFER',
            typeName: 'Airport Security & Gate Buffer',
            provider: 'Terminal Security',
            origin: 'DEL Airport',
            destination: 'DEL Airport',
            departureTime: '06:40 AM',
            arrivalTime: '08:15 AM',
            price: 0,
            durationMinutes: 90,
            formattedDuration: '1h 30m',
            icon: '⏳',
          },
          {
            id: 'seg-3',
            type: 'FLIGHT',
            typeName: 'Non-Stop Flight',
            provider: 'IndiGo 6E-204',
            origin: 'DEL Airport',
            destination: 'GOI Airport',
            departureTime: '08:15 AM',
            arrivalTime: '10:30 AM',
            price: 16230,
            durationMinutes: 135,
            formattedDuration: '2h 15m',
            icon: '✈️',
          },
          {
            id: 'seg-4',
            type: 'ARRIVAL_BUFFER',
            typeName: 'Deboarding & Baggage Collection',
            provider: 'Airport Arrival',
            origin: 'GOI Airport',
            destination: 'GOI Airport',
            departureTime: '10:30 AM',
            arrivalTime: '11:00 AM',
            price: 0,
            durationMinutes: 30,
            formattedDuration: '30m',
            icon: '🧳',
          },
          {
            id: 'seg-5',
            type: 'RIDE_FROM_AIRPORT',
            typeName: 'Ride to Hotel / Destination',
            provider: 'Ola Prime Sedan',
            origin: 'GOI Airport',
            destination: 'Goa',
            departureTime: '11:00 AM',
            arrivalTime: '11:50 AM',
            price: 620,
            durationMinutes: 50,
            formattedDuration: '50m',
            icon: '🚕',
          },
        ],
      },
    },
    fastestPlan: {
      id: 'plan-opt-fastest-3',
      title: 'Air India Prime Express + Priority Drop',
      totalCost: 19500,
      costPerTraveler: 6500,
      remainingBudget: 5500,
      budgetUtilizationPercent: 78.0,
      durationMinutes: 310,
      formattedDuration: '5h 10m',
      classification: 'FASTEST',
      recommendationReason: 'Fastest door-to-door transit (5h 10m) within budget.',
      isWithinBudget: true,
      journeyOption: null,
    },
    cheapestPlan: {
      id: 'plan-opt-cheapest-2',
      title: 'Akasa Air Value Saver + Airport Shuttle',
      totalCost: 15800,
      costPerTraveler: 5266.67,
      remainingBudget: 9200,
      budgetUtilizationPercent: 63.2,
      durationMinutes: 375,
      formattedDuration: '6h 15m',
      classification: 'CHEAPEST',
      recommendationReason: 'Lowest cost itinerary at ₹15,800, leaving ₹9,200 in reserve.',
      isWithinBudget: true,
      journeyOption: null,
    },
  };

  mockBudgetResponse.bestValuePlan = mockBudgetResponse.bestPlan;
  mockBudgetResponse.plans = [
    mockBudgetResponse.bestPlan,
    mockBudgetResponse.fastestPlan,
    mockBudgetResponse.cheapestPlan,
  ];

  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('renders Budget Assistant 2.0 with metrics, recommended plans, and timeline', async () => {
    budgetService.optimizeBudgetPlan.mockResolvedValueOnce(mockBudgetResponse);

    renderWithRouter(<BudgetAssistantPage />);

    await waitFor(() => {
      expect(screen.getByTestId('budget-assistant-page')).toBeInTheDocument();
    });

    expect(screen.getByText('Constraint-Based')).toBeInTheDocument();
    expect(screen.getByText('Budget Assistant')).toBeInTheDocument();

    // Check financial health metrics
    expect(screen.getByText('₹25,000')).toBeInTheDocument();
    expect(screen.getAllByText('₹17,200').length).toBeGreaterThanOrEqual(1);
    expect(screen.getAllByText('₹7,800').length).toBeGreaterThanOrEqual(1);

    // Check options A, B, C
    expect(screen.getByText(/Option A: ⚡ Fastest/i)).toBeInTheDocument();
    expect(screen.getByText(/Option B: ⚖️ Best Value/i)).toBeInTheDocument();
    expect(screen.getByText(/Option C: 💰 Cheapest/i)).toBeInTheDocument();

    // Check Timeline Visualizer for selected best plan
    expect(screen.getByTestId('journey-timeline-visualizer')).toBeInTheDocument();
    expect(screen.getAllByText('IndiGo Express + Uber Connect').length).toBeGreaterThanOrEqual(1);
  });

  it('parses natural language query and updates inputs on Apply Query', async () => {
    budgetService.optimizeBudgetPlan.mockResolvedValueOnce(mockBudgetResponse);
    budgetService.parseBudgetQuery.mockResolvedValueOnce({
      origin: 'Mumbai',
      destination: 'Bangalore',
      travelers: 2,
      maxBudget: 18000,
      preference: 'FASTEST',
    });
    budgetService.optimizeBudgetPlan.mockResolvedValueOnce({
      ...mockBudgetResponse,
      maxBudget: 18000,
    });

    renderWithRouter(<BudgetAssistantPage />);

    await waitFor(() => {
      expect(screen.getByText(/Apply Query →/i)).toBeInTheDocument();
    });

    const applyBtn = screen.getByRole('button', { name: /Apply Query →/i });
    fireEvent.click(applyBtn);

    await waitFor(() => {
      expect(budgetService.parseBudgetQuery).toHaveBeenCalled();
    });
  });

  it('handles over-budget state when budget is too low', async () => {
    budgetService.optimizeBudgetPlan.mockResolvedValueOnce({
      constraint: { travelers: 3, maxBudget: 2000 },
      maxBudget: 2000,
      plans: [],
      exceededBudgetOptionsCount: 3,
      lowestAvailablePrice: 15800,
      summaryText: 'No available travel combinations found under ₹2,000.',
    });

    renderWithRouter(<BudgetAssistantPage />);

    await waitFor(() => {
      expect(screen.getByText(/Budget Limit Exceeded/i)).toBeInTheDocument();
      expect(screen.getByText(/Lowest verified rate starts at/i)).toBeInTheDocument();
    });
  });
});
