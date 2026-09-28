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

describe('Universal Search & Homepage Integration', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('renders redesigned homepage with verbatim headings and upgraded placeholder', () => {
    render(
      <BrowserRouter>
        <HomePage />
      </BrowserRouter>
    );

    // Headings
    expect(screen.getByText(/Compare prices\./i)).toBeInTheDocument();
    expect(screen.getByText(/Decide smarter\./i)).toBeInTheDocument();
    expect(screen.getByText(/Compare products, flights and rides in one place\./i)).toBeInTheDocument();

    // Universal Search Bar
    expect(screen.getByPlaceholderText('Search a product or paste a product link')).toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Search' })).toBeInTheDocument();

    // 3 Category Cards
    expect(screen.getByRole('heading', { name: 'Shopping', level: 3 })).toBeInTheDocument();
    expect(screen.getByRole('heading', { name: 'Flights', level: 3 })).toBeInTheDocument();
    expect(screen.getByRole('heading', { name: 'Rides', level: 3 })).toBeInTheDocument();
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

    const input = screen.getByPlaceholderText('Search a product or paste a product link');
    await userEvent.type(input, 'Samsung phone under 30000');

    const searchBtn = screen.getByRole('button', { name: 'Search' });
    fireEvent.click(searchBtn);

    await waitFor(() => {
      expect(searchService.universalSearch).toHaveBeenCalledWith('Samsung phone under 30000');
      expect(screen.getByTestId('universal-results-container')).toBeInTheDocument();
      expect(screen.getByText(/Product Search/i)).toBeInTheDocument();
      expect(screen.getByText(/Samsung Galaxy A35 5G/i)).toBeInTheDocument();
    });
  });

  it('handles product link pasting and triggers URL search', async () => {
    const amazonUrl = 'https://www.amazon.in/Apple-iPhone-15-128-GB/dp/B0CHX1W1XY/';
    const mockUrlResponse = {
      intent: 'PRODUCT_SEARCH',
      query: amazonUrl,
      redirectRoute: '/shopping',
      executionTimeMs: 45,
      productResults: {
        totalOffers: 1,
        cheapestPrice: 69999,
        cheapestMerchant: 'Amazon',
        offers: [
          {
            merchant: 'Amazon',
            productName: 'Apple iPhone 15 128GB',
            price: 69999,
            brand: 'Apple',
            rating: 4.8,
            inStock: true,
            isCheapest: true,
          },
        ],
      },
    };

    searchService.universalSearch.mockResolvedValueOnce(mockUrlResponse);

    render(
      <BrowserRouter>
        <HomePage />
      </BrowserRouter>
    );

    const input = screen.getByPlaceholderText('Search a product or paste a product link');
    await userEvent.type(input, amazonUrl);

    const searchBtn = screen.getByRole('button', { name: 'Search' });
    fireEvent.click(searchBtn);

    await waitFor(() => {
      expect(searchService.universalSearch).toHaveBeenCalledWith(amazonUrl);
      expect(screen.getByText(/Apple iPhone 15 128GB/i)).toBeInTheDocument();
    });
  });

  it('shows clear button when text is entered and clears input on click', async () => {
    render(
      <BrowserRouter>
        <HomePage />
      </BrowserRouter>
    );

    const input = screen.getByPlaceholderText('Search a product or paste a product link');
    expect(screen.queryByLabelText('Clear search input')).not.toBeInTheDocument();

    await userEvent.type(input, 'MacBook');
    const clearBtn = screen.getByLabelText('Clear search input');
    expect(clearBtn).toBeInTheDocument();

    fireEvent.click(clearBtn);
    expect(input.value).toBe('');
    expect(screen.queryByLabelText('Clear search input')).not.toBeInTheDocument();
  });

  it('displays empty state card when 0 comparison offers are found', async () => {
    const emptyResponse = {
      intent: 'PRODUCT_SEARCH',
      query: 'NonExistentGadgetXYZ999',
      productResults: {
        totalOffers: 0,
        offers: [],
      },
    };

    searchService.universalSearch.mockResolvedValueOnce(emptyResponse);

    render(
      <BrowserRouter>
        <HomePage />
      </BrowserRouter>
    );

    const input = screen.getByPlaceholderText('Search a product or paste a product link');
    await userEvent.type(input, 'NonExistentGadgetXYZ999');

    const searchBtn = screen.getByRole('button', { name: 'Search' });
    fireEvent.click(searchBtn);

    await waitFor(() => {
      expect(screen.getByTestId('empty-search-state')).toBeInTheDocument();
      expect(screen.getByText(/No comparisons found/i)).toBeInTheDocument();
    });
  });

  it('displays error banner with retry button on search failure', async () => {
    searchService.universalSearch.mockRejectedValueOnce(new Error('Network connection timeout'));

    render(
      <BrowserRouter>
        <HomePage />
      </BrowserRouter>
    );

    const input = screen.getByPlaceholderText('Search a product or paste a product link');
    await userEvent.type(input, 'Gaming Laptop');

    const searchBtn = screen.getByRole('button', { name: 'Search' });
    fireEvent.click(searchBtn);

    await waitFor(() => {
      expect(screen.getByRole('alert')).toBeInTheDocument();
      expect(screen.getByText(/Network connection timeout/i)).toBeInTheDocument();
      expect(screen.getByRole('button', { name: /Try Again/i })).toBeInTheDocument();
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

    const sampleChip = screen.getByText(/Delhi to Mumbai flight/i);
    fireEvent.click(sampleChip);

    await waitFor(() => {
      expect(searchService.universalSearch).toHaveBeenCalledWith('Delhi to Mumbai flight tomorrow');
      expect(screen.getByText(/Flight Route Comparison/i)).toBeInTheDocument();
      expect(screen.getByText(/IndiGo/i)).toBeInTheDocument();
    });
  });
});
