import React from 'react';
import { render, screen, fireEvent, waitFor, within } from '@testing-library/react';
import { describe, it, expect, vi, beforeEach } from 'vitest';
import { MemoryRouter } from 'react-router-dom';
import DashboardPage from '../pages/DashboardPage';
import HistoryPage from '../pages/HistoryPage';
import SavedProductCard from '../components/saved/SavedProductCard';
import * as dashboardService from '../services/dashboardService';
import * as savedService from '../services/savedService';
import * as alertService from '../services/alertService';
import * as historyService from '../services/historyService';
import * as AuthContext from '../context/AuthContext';

vi.mock('../services/dashboardService');
vi.mock('../services/savedService');
vi.mock('../services/alertService');
vi.mock('../services/historyService');

describe('Part 6: User Dashboard, Saved Watchlist & Activity History', () => {
  const mockUser = {
    id: 1,
    name: 'Sahil',
    email: 'sahil@example.com',
  };

  const mockDashboardData = {
    summary: {
      userId: 1,
      userName: 'Sahil',
      savedProductsCount: 3,
      activeAlertsCount: 2,
      recentComparisonsCount: 5,
      priceDropsCount: 1,
      potentialSavings: 5000.0,
    },
    recentPriceDrops: [
      {
        id: 101,
        productId: 1,
        productName: 'Sony WH-1000XM5 Wireless Headphones',
        productCategory: 'Electronics',
        productImageUrl: 'https://example.com/sony.jpg',
        savedPrice: 29990.0,
        savedMerchant: 'Amazon',
        currentPrice: 24990.0,
        currentProvider: 'Amazon',
        isPriceDropped: true,
        priceDropAmount: 5000.0,
        priceDropPercentage: 16.67,
        priceChange: -5000.0,
        hasActiveAlert: true,
        alertTargetPrice: 25000.0,
        productUrl: 'https://amazon.in/dp/sony',
        createdAt: '2026-09-20T10:00:00Z',
      },
    ],
    savedProducts: [
      {
        id: 101,
        productId: 1,
        productName: 'Sony WH-1000XM5 Wireless Headphones',
        productCategory: 'Electronics',
        productImageUrl: 'https://example.com/sony.jpg',
        savedPrice: 29990.0,
        savedMerchant: 'Amazon',
        currentPrice: 24990.0,
        currentProvider: 'Amazon',
        isPriceDropped: true,
        priceDropAmount: 5000.0,
        priceDropPercentage: 16.67,
        priceChange: -5000.0,
        hasActiveAlert: true,
        alertTargetPrice: 25000.0,
        productUrl: 'https://amazon.in/dp/sony',
        createdAt: '2026-09-20T10:00:00Z',
      },
      {
        id: 102,
        productId: 2,
        productName: 'Apple iPhone 15 128GB Blue',
        productCategory: 'Electronics',
        productImageUrl: 'https://example.com/iphone15.jpg',
        savedPrice: 71999.0,
        savedMerchant: 'Flipkart',
        currentPrice: 71999.0,
        currentProvider: 'Flipkart',
        isPriceDropped: false,
        priceDropAmount: 0.0,
        priceDropPercentage: 0.0,
        priceChange: 0.0,
        hasActiveAlert: false,
        productUrl: 'https://flipkart.com/dp/iphone15',
        createdAt: '2026-09-21T11:00:00Z',
      },
    ],
    activeAlerts: [
      {
        id: 201,
        productId: 1,
        productName: 'Sony WH-1000XM5 Wireless Headphones',
        targetPrice: 25000.0,
        active: true,
        createdAt: '2026-09-20T12:00:00Z',
      },
      {
        id: 202,
        productId: 3,
        productName: 'Samsung Galaxy S24 256GB',
        targetPrice: 74999.0,
        active: true,
        createdAt: '2026-09-22T08:00:00Z',
      },
    ],
    recentActivity: [
      {
        id: 301,
        activityType: 'PRODUCT_COMPARE',
        title: 'Sony WH-1000XM5 Wireless Headphones',
        description: 'Amazon vs Flipkart vs Croma',
        actionLabel: 'Compare Again',
        actionUrl: '/shopping?q=Sony%20WH-1000XM5',
        createdAt: '2026-09-24T10:00:00Z',
      },
      {
        id: 302,
        activityType: 'FLIGHT_SEARCH',
        title: 'DEL → BOM',
        description: 'Round Trip • ECONOMY',
        actionLabel: 'Re-search Flights',
        actionUrl: '/flights',
        createdAt: '2026-09-23T14:00:00Z',
      },
    ],
    recentComparisons: [
      {
        id: 401,
        title: 'Sony WH-1000XM5',
        merchants: 'Amazon vs Flipkart vs Croma',
        compareUrl: '/shopping?q=Sony%20WH-1000XM5',
        lastCompared: '2026-09-24T10:00:00Z',
      },
    ],
  };

  beforeEach(() => {
    vi.clearAllMocks();
    vi.spyOn(AuthContext, 'useAuth').mockReturnValue({
      isAuthenticated: true,
      user: mockUser,
      token: 'mock-jwt-token',
    });
  });

  // 1. SavedProductCard Unit Tests
  describe('SavedProductCard Component', () => {
    const droppedProduct = mockDashboardData.savedProducts[0];
    const unchangedProduct = mockDashboardData.savedProducts[1];

    it('displays product title, provider, saved price, and current price', () => {
      render(
        <MemoryRouter>
          <SavedProductCard product={droppedProduct} />
        </MemoryRouter>
      );

      expect(screen.getByText('Sony WH-1000XM5 Wireless Headphones')).toBeInTheDocument();
      expect(screen.getByText('Amazon')).toBeInTheDocument();
      expect(screen.getByText('29,990', { exact: false })).toBeInTheDocument();
      expect(screen.getByText('24,990', { exact: false })).toBeInTheDocument();
    });

    it('shows PRICE DROPPED badge and savings amount when price has dropped', () => {
      render(
        <MemoryRouter>
          <SavedProductCard product={droppedProduct} />
        </MemoryRouter>
      );

      expect(screen.getByText('PRICE DROPPED')).toBeInTheDocument();
      expect(screen.getByText(/\b5,000\b/)).toBeInTheDocument();
      expect(screen.getByText(/16.67%/)).toBeInTheDocument();
    });

    it('does NOT show PRICE DROPPED badge when current price is equal or higher than saved price', () => {
      render(
        <MemoryRouter>
          <SavedProductCard product={unchangedProduct} />
        </MemoryRouter>
      );

      expect(screen.queryByText('PRICE DROPPED')).not.toBeInTheDocument();
    });

    it('triggers onRemove callback when Remove button is clicked', () => {
      const handleRemove = vi.fn();
      render(
        <MemoryRouter>
          <SavedProductCard product={droppedProduct} onRemove={handleRemove} />
        </MemoryRouter>
      );

      const removeBtn = screen.getByRole('button', { name: /Remove/i });
      fireEvent.click(removeBtn);

      expect(handleRemove).toHaveBeenCalledWith(droppedProduct.id);
    });

    it('triggers onSetAlert callback when Alert button is clicked', () => {
      const handleAlert = vi.fn();
      render(
        <MemoryRouter>
          <SavedProductCard product={unchangedProduct} onSetAlert={handleAlert} />
        </MemoryRouter>
      );

      const alertBtn = screen.getByRole('button', { name: /Set Price Alert/i });
      fireEvent.click(alertBtn);

      expect(handleAlert).toHaveBeenCalledWith({
        id: unchangedProduct.productId,
        productId: unchangedProduct.productId,
        productName: unchangedProduct.productName,
        price: unchangedProduct.currentPrice,
      });
    });
  });

  // 2. User Dashboard Page Tests
  describe('User Dashboard Page', () => {
    it('shows login prompt when user is not authenticated', () => {
      vi.spyOn(AuthContext, 'useAuth').mockReturnValue({
        isAuthenticated: false,
        user: null,
      });

      render(
        <MemoryRouter>
          <DashboardPage />
        </MemoryRouter>
      );

      expect(screen.getByText('Sign In to Access My CompareHub')).toBeInTheDocument();
      expect(screen.getByText('Sign In / Create Account')).toBeInTheDocument();
    });

    it('renders dashboard summary cards with real calculated metrics', async () => {
      dashboardService.getDashboardData.mockResolvedValue(mockDashboardData);

      render(
        <MemoryRouter>
          <DashboardPage />
        </MemoryRouter>
      );

      await waitFor(() => {
        expect(screen.getByText('Welcome back, Sahil 👋')).toBeInTheDocument();
      });

      // 5 Summary Metric Cards
      const summaryContainer = screen.getByTestId('dashboard-summary-cards');
      expect(summaryContainer).toBeInTheDocument();
      expect(within(summaryContainer).getByText('Saved')).toBeInTheDocument();
      expect(within(summaryContainer).getByText('Active Alerts')).toBeInTheDocument();
      expect(within(summaryContainer).getByText('Comparisons')).toBeInTheDocument();
      expect(within(summaryContainer).getByText('Price Drops')).toBeInTheDocument();
      expect(within(summaryContainer).getByText('Potential Savings')).toBeInTheDocument();
      expect(within(summaryContainer).getByText(/5,000/)).toBeInTheDocument();
    });

    it('renders Recent Price Drops section with verified savings', async () => {
      dashboardService.getDashboardData.mockResolvedValue(mockDashboardData);

      render(
        <MemoryRouter>
          <DashboardPage />
        </MemoryRouter>
      );

      await waitFor(() => {
        expect(screen.getByText('Recent Price Drops')).toBeInTheDocument();
      });

      expect(screen.getByTestId('section-price-drops')).toBeInTheDocument();
      expect(screen.getAllByText('PRICE DROPPED').length).toBeGreaterThan(0);
    });

    it('renders Quick Reopen Recent Comparisons section with Compare Again link', async () => {
      dashboardService.getDashboardData.mockResolvedValue(mockDashboardData);

      render(
        <MemoryRouter>
          <DashboardPage />
        </MemoryRouter>
      );

      await waitFor(() => {
        expect(screen.getByTestId('section-recent-comparisons')).toBeInTheDocument();
      });

      const compSection = screen.getByTestId('section-recent-comparisons');
      expect(within(compSection).getByText('Sony WH-1000XM5')).toBeInTheDocument();
      expect(within(compSection).getByText('Compare Again')).toBeInTheDocument();
    });

    it('handles alert pause/resume and deletion', async () => {
      dashboardService.getDashboardData.mockResolvedValue(mockDashboardData);
      alertService.togglePriceAlertStatus.mockResolvedValue({ id: 201, active: false });
      alertService.deletePriceAlert.mockResolvedValue({ success: true });

      render(
        <MemoryRouter>
          <DashboardPage />
        </MemoryRouter>
      );

      await waitFor(() => {
        expect(screen.getByText('Active Price Alerts')).toBeInTheDocument();
      });

      const pauseButtons = screen.getAllByText('Pause');
      expect(pauseButtons.length).toBeGreaterThan(0);
      fireEvent.click(pauseButtons[0]);

      await waitFor(() => {
        expect(alertService.togglePriceAlertStatus).toHaveBeenCalledWith(201, false);
      });

      const deleteButtons = screen.getAllByText('Delete');
      fireEvent.click(deleteButtons[0]);

      await waitFor(() => {
        expect(alertService.deletePriceAlert).toHaveBeenCalledWith(201);
      });
    });

    it('displays empty states when user has no saved items or alerts', async () => {
      const emptyDashboard = {
        summary: {
          userId: 1,
          userName: 'Sahil',
          savedProductsCount: 0,
          activeAlertsCount: 0,
          recentComparisonsCount: 0,
          priceDropsCount: 0,
          potentialSavings: 0.0,
        },
        recentPriceDrops: [],
        savedProducts: [],
        activeAlerts: [],
        recentActivity: [],
        recentComparisons: [],
      };

      dashboardService.getDashboardData.mockResolvedValue(emptyDashboard);

      render(
        <MemoryRouter>
          <DashboardPage />
        </MemoryRouter>
      );

      await waitFor(() => {
        expect(screen.getByText('No saved products yet.')).toBeInTheDocument();
      });

      expect(screen.getByText("You don't have any active price alerts.")).toBeInTheDocument();
      expect(screen.getByText('Your recent comparisons will appear here.')).toBeInTheDocument();
    });
  });

  // 3. Search & Activity History Page Tests
  describe('History Page', () => {
    const mockHistory = [
      {
        id: 501,
        query: 'Samsung S24 vs OnePlus 12',
        searchType: 'PRODUCT_COMPARE',
        details: 'Amazon vs Flipkart vs Croma',
        targetUrl: '/shopping?q=Samsung%20S24',
        createdAt: '2026-09-24T10:00:00Z',
      },
      {
        id: 502,
        query: 'MacBook Air M3',
        searchType: 'SHOPPING',
        details: 'Product Search',
        targetUrl: '/shopping?q=MacBook%20Air%20M3',
        createdAt: '2026-09-23T12:00:00Z',
      },
    ];

    it('renders recent comparisons section and allows one-click comparison reopen', async () => {
      historyService.getSearchHistory.mockResolvedValue(mockHistory);
      historyService.getFlightHistory.mockResolvedValue([]);
      historyService.getRideHistory.mockResolvedValue([]);

      render(
        <MemoryRouter>
          <HistoryPage />
        </MemoryRouter>
      );

      await waitFor(() => {
        expect(screen.getByText('Search & Activity History')).toBeInTheDocument();
      });

      const compSection = screen.getByTestId('recent-comparisons-section');
      expect(within(compSection).getByText('Samsung S24 vs OnePlus 12')).toBeInTheDocument();
      expect(within(compSection).getByText('Reopen Comparison')).toBeInTheDocument();
    });

    it('renders category filter tabs and filters items', async () => {
      historyService.getSearchHistory.mockResolvedValue(mockHistory);
      historyService.getFlightHistory.mockResolvedValue([]);
      historyService.getRideHistory.mockResolvedValue([]);

      render(
        <MemoryRouter>
          <HistoryPage />
        </MemoryRouter>
      );

      await waitFor(() => {
        expect(screen.getByRole('tab', { name: /Comparisons/i })).toBeInTheDocument();
      });

      const compTab = screen.getByRole('tab', { name: /Comparisons/i });
      fireEvent.click(compTab);

      expect(screen.getAllByText('Samsung S24 vs OnePlus 12').length).toBeGreaterThan(0);
      expect(screen.queryByText('MacBook Air M3')).not.toBeInTheDocument();
    });

    it('shows clean empty state when no comparisons exist', async () => {
      historyService.getSearchHistory.mockResolvedValue([]);
      historyService.getFlightHistory.mockResolvedValue([]);
      historyService.getRideHistory.mockResolvedValue([]);

      render(
        <MemoryRouter>
          <HistoryPage />
        </MemoryRouter>
      );

      await waitFor(() => {
        expect(screen.getByText('Your recent comparisons will appear here.')).toBeInTheDocument();
      });

      expect(screen.getByRole('link', { name: /Compare Products/i })).toBeInTheDocument();
    });
  });
});
