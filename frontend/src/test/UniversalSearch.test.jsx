import React from 'react';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { describe, it, expect, vi, beforeEach } from 'vitest';
import { BrowserRouter } from 'react-router-dom';
import HomePage from '../pages/HomePage';
import * as searchService from '../services/searchService';

// Mock health check hook
vi.mock('../hooks/useHealthCheck', () => ({
  useHealthCheck: () => ({
    isConnected: true,
    loading: false,
    error: null,
    refetch: vi.fn(),
  }),
}));

// Mock universal search service
vi.mock('../services/searchService', () => ({
  universalSearch: vi.fn(),
}));

describe('Universal Search Frontend Integration', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('renders universal search bar with sample query suggestions', () => {
    render(
      <BrowserRouter>
        <HomePage />
      </BrowserRouter>
    );

    expect(screen.getByPlaceholderText(/Search products, flights/i)).toBeInTheDocument();
    expect(screen.getByRole('button', { name: /Universal Search/i })).toBeInTheDocument();
    expect(screen.getByText(/iPhone 17 256GB/i)).toBeInTheDocument();
    expect(screen.getByText(/Delhi to Mumbai flight tomorrow/i)).toBeInTheDocument();
    expect(screen.getByText(/Ride from VIT Bhopal to Bhopal Airport/i)).toBeInTheDocument();
  });

  it('submits search query and displays detected intent and product results', async () => {
    const mockSearchResponse = {
      intent: 'PRODUCT_SEARCH',
      query: 'Samsung phone under 30000',
      redirectRoute: '/shopping',
      executionTimeMs: 42,
      intentDetails: {
        intent: 'PRODUCT_SEARCH',
        originalQuery: 'Samsung phone under 30000',
        query: 'Samsung phone',
        filters: {
          maxPrice: 30000,
          brand: 'Samsung',
        },
      },
      productResults: {
        totalOffers: 2,
        cheapestPrice: 24999,
        cheapestMerchant: 'Amazon',
        offers: [
          {
            merchant: 'Amazon',
            productName: 'Samsung Galaxy A35 5G',
            price: 24999,
            brand: 'Samsung',
            rating: 4.4,
            inStock: true,
            delivery: 'Free Delivery by Tomorrow',
            isCheapest: true,
          },
        ],
      },
    };

    searchService.universalSearch.mockResolvedValueOnce(mockSearchResponse);

    render(
      <BrowserRouter>
        <HomePage />
      </BrowserRouter>
    );

    const input = screen.getByPlaceholderText(/Search products, flights/i);
    await userEvent.type(input, 'Samsung phone under 30000');

    const searchBtn = screen.getByRole('button', { name: /Universal Search/i });
    fireEvent.click(searchBtn);

    await waitFor(() => {
      expect(searchService.universalSearch).toHaveBeenCalledWith('Samsung phone under 30000');
      expect(screen.getByTestId('universal-results-container')).toBeInTheDocument();
      expect(screen.getByText(/Product Search/i)).toBeInTheDocument();
      expect(screen.getByText(/Samsung Galaxy A35 5G/i)).toBeInTheDocument();
    });
  });

  it('clicking a sample query chip triggers universal search automatically', async () => {
    const mockFlightResponse = {
      intent: 'FLIGHT_SEARCH',
      query: 'Delhi to Mumbai flight tomorrow',
      redirectRoute: '/flights',
      executionTimeMs: 38,
      intentDetails: {
        intent: 'FLIGHT_SEARCH',
        flightParams: {
          origin: 'DEL',
          destination: 'BOM',
        },
      },
      flightResults: {
        origin: 'DEL',
        destination: 'BOM',
        cheapestPrice: 4200,
        offers: [
          {
            airline: 'IndiGo',
            flightNumber: '6E-505',
            price: 4200,
            origin: 'DEL',
            destination: 'BOM',
            departure: '06:00',
            arrival: '08:15',
            durationMinutes: 135,
            stops: 0,
            isCheapest: true,
          },
        ],
      },
    };

    searchService.universalSearch.mockResolvedValueOnce(mockFlightResponse);

    render(
      <BrowserRouter>
        <HomePage />
      </BrowserRouter>
    );

    const sampleChip = screen.getByText(/Delhi to Mumbai flight tomorrow/i);
    fireEvent.click(sampleChip);

    await waitFor(() => {
      expect(searchService.universalSearch).toHaveBeenCalledWith('Delhi to Mumbai flight tomorrow');
      expect(screen.getByText(/Flight Route Comparison/i)).toBeInTheDocument();
      expect(screen.getByText(/IndiGo/i)).toBeInTheDocument();
    });
  });
});
