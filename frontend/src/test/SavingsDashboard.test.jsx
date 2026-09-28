import React from 'react';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import { describe, it, expect, vi, beforeEach } from 'vitest';
import SavingsDashboardPage from '../pages/SavingsDashboardPage';
import * as savingsService from '../services/savingsService';

vi.mock('../services/savingsService');

describe('Personal Savings Dashboard Frontend', () => {
  const mockDashboardData = {
    thisMonthPotentialSavings: 4280,
    thisMonthConfirmedSavings: 7200,
    totalComparisons: 37,
    dealsFoundCount: 12,
    priceAlertsCount: 4,
    triggeredAlertsCount: 3,
    largestSaving: {
      title: 'Asus ROG Zephyrus G16 Gaming Laptop',
      amount: 7200,
      merchant: 'Amazon India',
      category: 'Electronics',
      comparisonContext: 'Saved vs Croma Baseline ₹1,54,990',
    },
    monthlyHistory: [
      { month: 'Apr', confirmedAmount: 3200, potentialAmount: 2100 },
      { month: 'May', confirmedAmount: 4800, potentialAmount: 3400 },
      { month: 'Jun', confirmedAmount: 5100, potentialAmount: 3900 },
      { month: 'Jul', confirmedAmount: 6400, potentialAmount: 4100 },
      { month: 'Aug', confirmedAmount: 5900, potentialAmount: 3800 },
      { month: 'Sep', confirmedAmount: 7200, potentialAmount: 4280 },
    ],
    categoryBreakdowns: [
      { category: 'Travel & Flights', savingAmount: 14600, percentage: 42.0 },
      { category: 'Electronics', savingAmount: 11480, percentage: 33.0 },
      { category: 'Cabs & Rides', savingAmount: 8700, percentage: 25.0 },
    ],
    priceAlertSuccess: {
      configuredCount: 4,
      triggeredCount: 3,
      successRate: 75.0,
      averageDropAmount: 2150,
    },
    recentEvents: [
      {
        id: 1,
        title: 'Asus ROG Zephyrus G16 Gaming Laptop',
        category: 'Electronics',
        merchantOrProvider: 'Amazon India',
        savingAmount: 7200,
        eventType: 'CONFIRMED',
        notes: 'Price drop alert matched ₹1,47,790 vs Croma ₹1,54,990',
      },
      {
        id: 2,
        title: 'Samsung Galaxy S24 256GB',
        category: 'Electronics',
        merchantOrProvider: 'Flipkart',
        savingAmount: 4280,
        eventType: 'POTENTIAL',
        notes: 'Best eligible card discount & merchant price gap',
      },
      {
        id: 3,
        title: 'Delhi to Jaipur Group Travel (4 Persons)',
        category: 'Travel & Flights',
        merchantOrProvider: 'Uber XL Group Cab',
        savingAmount: 12200,
        eventType: 'POTENTIAL',
        notes: 'Group cab ₹6,400 vs 4 flight tickets + local rides ₹18,600',
      },
    ],
  };

  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('renders loading state initially and then displays savings dashboard', async () => {
    savingsService.getSavingsDashboard.mockResolvedValueOnce(mockDashboardData);

    render(<SavingsDashboardPage />);

    await waitFor(() => {
      expect(screen.getByTestId('savings-dashboard')).toBeInTheDocument();
    });

    expect(screen.getAllByText('₹4,280').length).toBeGreaterThanOrEqual(1);
    expect(screen.getAllByText('₹7,200').length).toBeGreaterThanOrEqual(1);
    expect(screen.getByText('37')).toBeInTheDocument();
    expect(screen.getByText('Deals & Triggered Alerts')).toBeInTheDocument();
  });

  it('renders biggest single saving spotlight card correctly', async () => {
    savingsService.getSavingsDashboard.mockResolvedValueOnce(mockDashboardData);

    render(<SavingsDashboardPage />);

    await waitFor(() => {
      expect(screen.getByText(/Biggest Single Saving/i)).toBeInTheDocument();
    });

    expect(screen.getAllByText('Asus ROG Zephyrus G16 Gaming Laptop').length).toBeGreaterThanOrEqual(1);
    expect(screen.getByText(/Saved vs Croma Baseline/i)).toBeInTheDocument();
    expect(screen.getAllByText('Amazon India').length).toBeGreaterThanOrEqual(1);
  });

  it('renders category distributions and price alert success widget', async () => {
    savingsService.getSavingsDashboard.mockResolvedValueOnce(mockDashboardData);

    render(<SavingsDashboardPage />);

    await waitFor(() => {
      expect(screen.getByText('Savings by Category')).toBeInTheDocument();
    });

    expect(screen.getAllByText('Travel & Flights').length).toBeGreaterThanOrEqual(1);
    expect(screen.getByText('₹14,600')).toBeInTheDocument();
    expect(screen.getByText('Price Alert Success')).toBeInTheDocument();
    expect(screen.getByText('75%')).toBeInTheDocument();
  });

  it('allows user to confirm purchase on potential savings event and updates state', async () => {
    savingsService.getSavingsDashboard.mockResolvedValueOnce(mockDashboardData);
    savingsService.confirmSavingsEvent.mockResolvedValueOnce({
      ...mockDashboardData.recentEvents[1],
      eventType: 'CONFIRMED',
    });

    render(<SavingsDashboardPage />);

    await waitFor(() => {
      expect(screen.getByText('Samsung Galaxy S24 256GB')).toBeInTheDocument();
    });

    // Find "Confirm Purchase" buttons (for event 2 and 3)
    const confirmButtons = screen.getAllByRole('button', { name: /Confirm Purchase/i });
    expect(confirmButtons.length).toBe(2);

    // Click confirm for Samsung Galaxy S24
    fireEvent.click(confirmButtons[0]);

    await waitFor(() => {
      expect(savingsService.confirmSavingsEvent).toHaveBeenCalledWith(2);
    });
  });

  it('handles error state with retry button', async () => {
    savingsService.getSavingsDashboard.mockRejectedValueOnce(new Error('Network connectivity issue'));

    render(<SavingsDashboardPage />);

    await waitFor(() => {
      expect(screen.getByText('Dashboard Unavailable')).toBeInTheDocument();
      expect(screen.getByText('Network connectivity issue')).toBeInTheDocument();
    });

    savingsService.getSavingsDashboard.mockResolvedValueOnce(mockDashboardData);
    const retryBtn = screen.getByRole('button', { name: /Retry Loading/i });
    fireEvent.click(retryBtn);

    await waitFor(() => {
      expect(screen.getByTestId('savings-dashboard')).toBeInTheDocument();
    });
  });
});
