import React from 'react';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import { describe, it, expect, vi, beforeEach } from 'vitest';
import { BrowserRouter } from 'react-router-dom';
import SmartDealsSection from '../components/SmartDealsSection';
import * as dealDiscoveryService from '../services/dealDiscoveryService';

vi.mock('../services/dealDiscoveryService');

const renderWithRouter = (ui) => {
  return render(<BrowserRouter>{ui}</BrowserRouter>);
};

describe('Personalized Smart Deals Feed Frontend', () => {
  const mockDealsPageAll = {
    deals: [
      {
        id: 'prod-101-501',
        dealType: 'PRODUCT',
        productId: 101,
        title: 'Gaming Laptop RTX 4070 16GB',
        category: 'Electronics',
        dealCategory: 'EXCEPTIONAL_DEALS',
        merchantOrProvider: 'Amazon India',
        currentPrice: 67999,
        historicalTypicalPrice: 82999,
        originalAdvertisedPrice: 89999,
        realSavingsAmount: 15000,
        realDiscountPercent: 18,
        dealScore: 91,
        dealClassification: 'EXCEPTIONAL_DEAL',
        dealLabel: 'Exceptional Deal',
        reason: '18% below 30-day average price (₹82,999).',
        watchlistMatch: false,
        alertTriggered: false,
      },
      {
        id: 'prod-102-502',
        dealType: 'PRODUCT',
        productId: 102,
        title: 'Samsung Galaxy S24 256GB',
        category: 'Smartphones',
        dealCategory: 'WATCHLIST_DEALS',
        merchantOrProvider: 'Flipkart',
        currentPrice: 59999,
        historicalTypicalPrice: 66999,
        originalAdvertisedPrice: 74999,
        realSavingsAmount: 7000,
        realDiscountPercent: 10,
        dealScore: 84,
        dealClassification: 'GREAT_DEAL',
        dealLabel: 'Price Alert Target Reached',
        reason: 'Price dropped to ₹59,999, matching your target alert of ₹60,000.',
        watchlistMatch: true,
        alertTriggered: true,
      },
      {
        id: 'travel-fl-del-bom',
        dealType: 'TRAVEL',
        productId: null,
        title: 'Delhi (DEL) → Mumbai (BOM) Non-Stop Flight',
        category: 'Flights',
        dealCategory: 'TRAVEL_DEALS',
        merchantOrProvider: 'IndiGo Airlines',
        currentPrice: 3450,
        historicalTypicalPrice: 5200,
        originalAdvertisedPrice: 5800,
        realSavingsAmount: 1750,
        realDiscountPercent: 34,
        dealScore: 94,
        dealClassification: 'EXCEPTIONAL_DEAL',
        dealLabel: 'Airfare Drop',
        reason: '34% lower than the 30-day average route fare of ₹5,200.',
        watchlistMatch: false,
        alertTriggered: false,
        linkUrl: '/flights?origin=DEL&destination=BOM',
      },
    ],
    currentPage: 0,
    totalPages: 2,
    totalElements: 12,
    activeCategory: 'ALL',
    categoryCounts: {
      ALL: 12,
      EXCEPTIONAL_DEALS: 5,
      PRICE_DROPS: 4,
      WATCHLIST_DEALS: 1,
      TRAVEL_DEALS: 2,
    },
  };

  const mockDealsPageExceptional = {
    deals: [mockDealsPageAll.deals[0]],
    currentPage: 0,
    totalPages: 1,
    totalElements: 1,
    activeCategory: 'EXCEPTIONAL_DEALS',
    categoryCounts: mockDealsPageAll.categoryCounts,
  };

  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('renders Today Smart Deals heading, counter badges, and deal cards', async () => {
    dealDiscoveryService.getSmartDeals.mockResolvedValueOnce(mockDealsPageAll);

    renderWithRouter(<SmartDealsSection />);

    await waitFor(() => {
      expect(screen.getByTestId('smart-deals-section')).toBeInTheDocument();
    });

    expect(screen.getByText("Today's")).toBeInTheDocument();
    expect(screen.getByText('Smart Deals')).toBeInTheDocument();

    // Verify Deal cards rendered
    expect(screen.getByText('Gaming Laptop RTX 4070 16GB')).toBeInTheDocument();
    expect(screen.getByText('91 Deal Score')).toBeInTheDocument();
    expect(screen.getByText(/18% below 30-day average/i)).toBeInTheDocument();
    expect(screen.getByText('₹67,999')).toBeInTheDocument();
    expect(screen.getByText('18% OFF')).toBeInTheDocument();

    // Verify Watchlist alert badge
    expect(screen.getAllByText(/Target Alert/i).length).toBeGreaterThanOrEqual(1);

    // Verify Travel deal card
    expect(screen.getByText('Delhi (DEL) → Mumbai (BOM) Non-Stop Flight')).toBeInTheDocument();
    expect(screen.getByText('94 Deal Score')).toBeInTheDocument();
  });

  it('filters deals when user clicks a category tab', async () => {
    dealDiscoveryService.getSmartDeals.mockResolvedValueOnce(mockDealsPageAll);
    dealDiscoveryService.getSmartDeals.mockResolvedValueOnce(mockDealsPageExceptional);

    renderWithRouter(<SmartDealsSection />);

    await waitFor(() => {
      expect(screen.getByText('Gaming Laptop RTX 4070 16GB')).toBeInTheDocument();
    });

    // Click "Exceptional Deals" tab
    const exceptionalTab = screen.getByRole('button', { name: /Exceptional Deals/i });
    fireEvent.click(exceptionalTab);

    await waitFor(() => {
      expect(dealDiscoveryService.getSmartDeals).toHaveBeenCalledWith({
        category: 'EXCEPTIONAL_DEALS',
        page: 0,
        size: 8,
      });
    });
  });

  it('handles pagination navigation', async () => {
    dealDiscoveryService.getSmartDeals.mockResolvedValueOnce(mockDealsPageAll);
    dealDiscoveryService.getSmartDeals.mockResolvedValueOnce({
      ...mockDealsPageAll,
      currentPage: 1,
    });

    renderWithRouter(<SmartDealsSection />);

    await waitFor(() => {
      expect(screen.getByText(/Next →/i)).toBeInTheDocument();
    });

    const nextBtn = screen.getByRole('button', { name: /Next →/i });
    fireEvent.click(nextBtn);

    await waitFor(() => {
      expect(dealDiscoveryService.getSmartDeals).toHaveBeenCalledWith({
        category: 'ALL',
        page: 1,
        size: 8,
      });
    });
  });

  it('renders empty state when no deals are found in category', async () => {
    dealDiscoveryService.getSmartDeals.mockResolvedValueOnce({
      deals: [],
      currentPage: 0,
      totalPages: 0,
      totalElements: 0,
      activeCategory: 'WATCHLIST_DEALS',
      categoryCounts: { ALL: 0, EXCEPTIONAL_DEALS: 0, PRICE_DROPS: 0, WATCHLIST_DEALS: 0, TRAVEL_DEALS: 0 },
    });

    renderWithRouter(<SmartDealsSection />);

    await waitFor(() => {
      expect(screen.getByText(/No deals found in this category/i)).toBeInTheDocument();
    });
  });
});
