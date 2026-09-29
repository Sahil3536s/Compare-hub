import React from 'react';
import { render, screen, waitFor } from '@testing-library/react';
import { describe, it, expect, vi, beforeEach } from 'vitest';
import { MemoryRouter, Route, Routes } from 'react-router-dom';
import CanonicalProductCard from '../components/shopping/CanonicalProductCard';
import ProductDetailsPage from '../pages/ProductDetailsPage';
import * as productService from '../services/productService';

vi.mock('../services/productService');
vi.mock('../services/alertService', () => ({
  createPriceAlert: vi.fn().mockResolvedValue({ id: 1, active: true }),
}));
vi.mock('../services/savedService', () => ({
  saveProduct: vi.fn().mockResolvedValue({ id: 1 }),
}));
vi.mock('../context/AuthContext', () => ({
  useAuth: () => ({ isAuthenticated: true, user: { id: 1, name: 'Test User' } }),
}));

vi.mock('chart.js', () => ({
  Chart: { register: vi.fn() },
  CategoryScale: vi.fn(),
  LinearScale: vi.fn(),
  PointElement: vi.fn(),
  LineElement: vi.fn(),
  Title: vi.fn(),
  Tooltip: vi.fn(),
  Legend: vi.fn(),
  Filler: vi.fn(),
}));

// Mock react-chartjs-2 Line component
vi.mock('react-chartjs-2', () => ({
  Line: () => <div data-testid="mock-price-line-chart">Line Chart</div>,
}));

describe('CanonicalProductCard Component', () => {
  const mockGroup = {
    productId: 101,
    canonicalTitle: 'Samsung Galaxy S24 Ultra 256GB',
    brand: 'Samsung',
    category: 'Smartphones',
    lowestPrice: 119999,
    highestPrice: 129999,
    priceSpread: 10000,
    cheapestMerchant: 'Amazon',
    rating: 4.7,
    reviewCount: 1540,
    offers: [
      { merchant: 'Amazon', price: 119999, live: true, dataSource: 'LIVE' },
      { merchant: 'Flipkart', price: 124999, live: true, dataSource: 'LIVE' },
      { merchant: 'Croma', price: 129999, live: true, dataSource: 'LIVE' },
    ],
  };

  it('renders canonical title, store count, lowest price, and savings badge', () => {
    render(
      <MemoryRouter>
        <CanonicalProductCard product={mockGroup} />
      </MemoryRouter>
    );

    expect(screen.getByText('Samsung Galaxy S24 Ultra 256GB')).toBeInTheDocument();
    expect(screen.getByText('3 Stores')).toBeInTheDocument();
    expect(screen.getByText('₹1,19,999')).toBeInTheDocument();
    expect(screen.getByText(/Save up to ₹10,000/)).toBeInTheDocument();
    expect(screen.getByText(/Compare Prices & Trends/i)).toBeInTheDocument();
  });
});

describe('ProductDetailsPage Component', () => {
  const mockDetail = {
    id: 101,
    name: 'Samsung Galaxy S24 Ultra 256GB',
    brand: 'Samsung',
    category: 'Smartphones',
    imageUrl: 'https://example.com/s24.png',
    rating: 4.7,
    reviewCount: 1540,
    currentLowestPrice: 119999,
    cheapestMerchant: 'Amazon',
    priceMeter: {
      status: 'CALCULATED',
      classification: 'EXCELLENT_DEAL',
      classificationLabel: 'Excellent Deal',
      hasSufficientData: true,
      score: 95,
      currentPrice: 119999,
      historicalMinimum: 119999,
      historicalLowest: 119999,
      historicalAverage: 128500,
      historicalMaximum: 134999,
      historicalHighest: 134999,
      percentDifferenceFromAverage: -6.6,
      differencePercentage: -6.6,
      differenceFromAvg: -8501,
      advice: 'Current price is at its 90-day lowest. Excellent time to purchase.',
      summaryText: 'Current price is at its 90-day lowest. Excellent time to purchase.',
      percentile: 5,
    },
    offers: [
      {
        id: 1,
        merchant: 'Amazon',
        price: 119999,
        originalPrice: 134999,
        deliveryText: 'Free 1-Day Delivery',
        inStock: true,
        rating: 4.8,
        productUrl: 'https://amazon.in/s24',
        live: true,
        dataSource: 'LIVE',
      },
      {
        id: 2,
        merchant: 'Flipkart',
        price: 124999,
        originalPrice: 134999,
        deliveryText: 'Delivery in 2 days',
        inStock: true,
        rating: 4.6,
        productUrl: 'https://flipkart.com/s24',
        live: true,
        dataSource: 'LIVE',
      },
    ],
    priceHistory: {
      productId: 101,
      period: '90D',
      pricePoints: [
        { date: '2026-08-01', price: 134999, merchant: 'Amazon' },
        { date: '2026-08-15', price: 129999, merchant: 'Flipkart' },
        { date: '2026-09-01', price: 119999, merchant: 'Amazon' },
      ],
    },
    specifications: {
      Brand: 'Samsung',
      Model: 'Galaxy S24 Ultra',
      Storage: '256GB',
      RAM: '12GB',
    },
    mlPrediction: {
      predictedPrice: 119500,
      predictedTrend: 'STABLE',
      confidenceScore: 0.85,
      summary: 'Price is anticipated to remain stable over the next 14 days.',
    },
    alternatives: [],
  };

  beforeEach(() => {
    vi.clearAllMocks();
    productService.getProductDetails.mockResolvedValue(mockDetail);
    productService.getProductPriceHistory.mockResolvedValue(mockDetail.priceHistory);
    productService.getProductPriceMeter.mockResolvedValue(mockDetail.priceMeter);
  });

  it('loads and renders product hero, price meter, merchant offers, and price trend', async () => {
    render(
      <MemoryRouter initialEntries={['/products/101']}>
        <Routes>
          <Route path="/products/:productId" element={<ProductDetailsPage />} />
        </Routes>
      </MemoryRouter>
    );

    // Verify loading completes and title appears
    await waitFor(() => {
      expect(screen.getByRole('heading', { level: 1, name: 'Samsung Galaxy S24 Ultra 256GB' })).toBeInTheDocument();
    });

    // Verify Price Meter section and classifications
    expect(screen.getByText('Statistical Price Meter')).toBeInTheDocument();
    expect(screen.getAllByText(/Excellent Deal/i).length).toBeGreaterThan(0);
    expect(screen.getAllByText(/Current price is at its 90-day lowest/i).length).toBeGreaterThan(0);

    // Verify Merchant Comparison offers
    expect(screen.getByText('Compare Merchant Prices')).toBeInTheDocument();
    expect(screen.getByText('Amazon')).toBeInTheDocument();
    expect(screen.getByText('Flipkart')).toBeInTheDocument();

    // Verify Specifications
    expect(screen.getByText('Product Specifications')).toBeInTheDocument();
    expect(screen.getByText('Galaxy S24 Ultra')).toBeInTheDocument();

    // Verify Price History Chart is mounted
    expect(screen.getByTestId('mock-price-line-chart')).toBeInTheDocument();

    // Verify ML Price Prediction & Insights
    expect(screen.getByText('Price Insights & ML Prediction')).toBeInTheDocument();
    expect(screen.getByText(/Expected Trend: STABLE/i)).toBeInTheDocument();
    expect(screen.getByText(/Confidence: 85%/i)).toBeInTheDocument();
  });
});
