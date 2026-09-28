import React from 'react';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import { describe, it, expect, vi, beforeEach } from 'vitest';
import { ProductComparisonModal, areOffersEquivalent } from '../components/ProductComparisonModal';
import { PriceHistoryModal } from '../components/PriceHistoryModal';
import { PriceAlertModal } from '../components/PriceAlertModal';
import { ProductOfferCard } from '../components/ProductOfferCard';
import * as productService from '../services/productService';
import * as alertService from '../services/alertService';
import AuthContext from '../context/AuthContext';

// Mock services
vi.mock('../services/productService');
vi.mock('../services/alertService');

describe('Part 3 UI Upgrades: Comparison, Price History, and Price Alerts', () => {

  const mockS24_128GB_Amazon = {
    productId: 101,
    productName: 'Samsung Galaxy S24 5G (128GB)',
    merchant: 'Amazon',
    price: 74999,
    effectivePrice: 72499,
    rating: 4.8,
    discountPercent: 10,
    delivery: 'Free Delivery Tomorrow',
    inStock: true,
    brand: 'Samsung',
    canonicalKey: 'samsung_s24_128gb',
    attributes: {
      brand: 'Samsung',
      model: 'Galaxy S24',
      storage: '128GB',
      ram: '8GB',
      color: 'Onyx Black',
    },
  };

  const mockS24_128GB_Flipkart = {
    productId: 102,
    productName: 'Samsung Galaxy S24 5G (128GB)',
    merchant: 'Flipkart',
    price: 75499,
    effectivePrice: 74499,
    rating: 4.6,
    discountPercent: 8,
    delivery: 'Standard 2 Days',
    inStock: true,
    brand: 'Samsung',
    canonicalKey: 'samsung_s24_128gb',
    attributes: {
      brand: 'Samsung',
      model: 'Galaxy S24',
      storage: '128GB',
      ram: '8GB',
      color: 'Marble Gray',
    },
  };

  const mockS24_128GB_Croma = {
    productId: 103,
    productName: 'Samsung Galaxy S24 5G (128GB)',
    merchant: 'Croma',
    price: 79999,
    effectivePrice: 79999,
    rating: 4.7,
    discountPercent: 5,
    delivery: 'Same Day Store Pickup',
    inStock: true,
    brand: 'Samsung',
    canonicalKey: 'samsung_s24_128gb',
    attributes: {
      brand: 'Samsung',
      model: 'Galaxy S24',
      storage: '128GB',
      ram: '8GB',
      color: 'Amber Yellow',
    },
  };

  const mockS24_256GB_Amazon = {
    productId: 104,
    productName: 'Samsung Galaxy S24 5G (256GB)',
    merchant: 'Amazon',
    price: 81999,
    effectivePrice: 79999,
    rating: 4.9,
    discountPercent: 7,
    delivery: 'Free Delivery Tomorrow',
    inStock: true,
    brand: 'Samsung',
    canonicalKey: 'samsung_s24_256gb',
    attributes: {
      brand: 'Samsung',
      model: 'Galaxy S24',
      storage: '256GB',
      ram: '8GB',
      color: 'Cobalt Violet',
    },
  };

  beforeEach(() => {
    vi.clearAllMocks();
  });

  // =========================================================================
  // 1. PRODUCT COMPARISON TESTS
  // =========================================================================

  describe('Feature 1: Product Comparison & Variant Separation', () => {

    it('CRITICAL: strictly isolates Samsung S24 128GB from Samsung S24 256GB', () => {
      // Direct equivalence unit check
      const equivalent = areOffersEquivalent(mockS24_128GB_Amazon, mockS24_256GB_Amazon);
      expect(equivalent).toBe(false);

      const sameVariant = areOffersEquivalent(mockS24_128GB_Amazon, mockS24_128GB_Flipkart);
      expect(sameVariant).toBe(true);
    });

    it('renders desktop comparison table with Amazon, Flipkart, Croma columns and all 9 specification rows', () => {
      const allOffers = [
        mockS24_128GB_Amazon,
        mockS24_128GB_Flipkart,
        mockS24_128GB_Croma,
        mockS24_256GB_Amazon,
      ];

      render(
        <ProductComparisonModal
          isOpen={true}
          onClose={vi.fn()}
          selectedOffer={mockS24_128GB_Amazon}
          allOffers={allOffers}
        />
      );

      // Verify Stores in Columns
      expect(screen.getByRole('columnheader', { name: /Amazon/i })).toBeInTheDocument();
      expect(screen.getByRole('columnheader', { name: /Flipkart/i })).toBeInTheDocument();
      expect(screen.getByRole('columnheader', { name: /Croma/i })).toBeInTheDocument();

      // Verify all 9 rows exist
      expect(screen.getByRole('rowheader', { name: /^Price$/i })).toBeInTheDocument();
      expect(screen.getByRole('rowheader', { name: /^Effective Price$/i })).toBeInTheDocument();
      expect(screen.getByRole('rowheader', { name: /^Rating$/i })).toBeInTheDocument();
      expect(screen.getByRole('rowheader', { name: /^Discount$/i })).toBeInTheDocument();
      expect(screen.getByRole('rowheader', { name: /^Delivery$/i })).toBeInTheDocument();
      expect(screen.getByRole('rowheader', { name: /^Availability$/i })).toBeInTheDocument();
      expect(screen.getByRole('rowheader', { name: /^RAM$/i })).toBeInTheDocument();
      expect(screen.getByRole('rowheader', { name: /^Storage$/i })).toBeInTheDocument();
      expect(screen.getByRole('rowheader', { name: /^Color$/i })).toBeInTheDocument();

      // Verify values for RAM & Storage
      expect(screen.getAllByText('8GB').length).toBeGreaterThanOrEqual(1);
      expect(screen.getAllByText('128GB').length).toBeGreaterThanOrEqual(1);
    });

    it('highlights meaningful best values: Lowest Price, Best Rating, and Fastest Delivery', () => {
      const allOffers = [
        mockS24_128GB_Amazon,
        mockS24_128GB_Flipkart,
        mockS24_128GB_Croma,
      ];

      render(
        <ProductComparisonModal
          isOpen={true}
          onClose={vi.fn()}
          selectedOffer={mockS24_128GB_Amazon}
          allOffers={allOffers}
        />
      );

      // Lowest Price: Amazon has effective price 72,499
      expect(screen.getAllByText(/Lowest Price/i).length).toBeGreaterThanOrEqual(1);

      // Best Rating: Amazon has rating 4.8
      expect(screen.getAllByText(/Best Rating/i).length).toBeGreaterThanOrEqual(1);

      // Fastest Delivery: Croma has Same Day Store Pickup
      expect(screen.getAllByText(/Fastest Delivery/i).length).toBeGreaterThanOrEqual(1);
    });

    it('switches between 128GB and 256GB variants via variant selector tabs without mixing', () => {
      const allOffers = [
        mockS24_128GB_Amazon,
        mockS24_128GB_Flipkart,
        mockS24_256GB_Amazon,
      ];

      render(
        <ProductComparisonModal
          isOpen={true}
          onClose={vi.fn()}
          selectedOffer={mockS24_128GB_Amazon}
          allOffers={allOffers}
        />
      );

      // Verify variant tabs exist
      expect(screen.getByText(/Switch Hardware Variant:/i)).toBeInTheDocument();
      const variant256Tab = screen.getByRole('button', { name: /256GB/i });
      expect(variant256Tab).toBeInTheDocument();

      // Click to switch to 256GB variant
      fireEvent.click(variant256Tab);

      // Header now reflects 256GB
      expect(screen.getByText(/Samsung Galaxy S24 5G \(256GB\)/i)).toBeInTheDocument();
    });

    it('supports mobile view toggling between cards and table', () => {
      render(
        <ProductComparisonModal
          isOpen={true}
          onClose={vi.fn()}
          selectedOffer={mockS24_128GB_Amazon}
          allOffers={[mockS24_128GB_Amazon, mockS24_128GB_Flipkart]}
        />
      );

      const tableBtn = screen.getByRole('button', { name: /Table View/i });
      fireEvent.click(tableBtn);
      expect(tableBtn).toHaveClass('bg-white');

      const cardsBtn = screen.getByRole('button', { name: /Cards View/i });
      fireEvent.click(cardsBtn);
      expect(cardsBtn).toHaveClass('bg-white');
    });

  });

  // =========================================================================
  // 2. PRICE HISTORY TESTS
  // =========================================================================

  describe('Feature 2: Price History Upgrade', () => {

    const mockHistoryData = {
      productId: 101,
      productName: 'Samsung Galaxy S24 128GB',
      period: '30D',
      currentPrice: 72499,
      lowestPrice: 71999,
      highestPrice: 79999,
      averagePrice: 74999,
      pricePoints: [
        { date: '2026-08-25', price: 79999, merchant: 'Croma' },
        { date: '2026-09-05', price: 74999, merchant: 'Flipkart' },
        { date: '2026-09-20', price: 71999, merchant: 'Amazon' },
        { date: '2026-09-24', price: 72499, merchant: 'Amazon' },
      ],
    };

    it('renders period tabs (7D, 30D, 90D) and switches period', async () => {
      productService.getProductPriceHistory.mockResolvedValue(mockHistoryData);

      render(
        <PriceHistoryModal
          isOpen={true}
          onClose={vi.fn()}
          product={mockS24_128GB_Amazon}
        />
      );

      expect(screen.getByRole('button', { name: '7D' })).toBeInTheDocument();
      expect(screen.getByRole('button', { name: '30D' })).toBeInTheDocument();
      expect(screen.getByRole('button', { name: '90D' })).toBeInTheDocument();

      // Click 90D tab
      fireEvent.click(screen.getByRole('button', { name: '90D' }));
      await waitFor(() => {
        expect(productService.getProductPriceHistory).toHaveBeenCalledWith(expect.anything(), '90D');
      });
    });

    it('renders Summary with Current Price, Average Price, Lowest Price, Highest Price', async () => {
      productService.getProductPriceHistory.mockResolvedValue(mockHistoryData);

      render(
        <PriceHistoryModal
          isOpen={true}
          onClose={vi.fn()}
          product={mockS24_128GB_Amazon}
        />
      );

      await waitFor(() => {
        expect(screen.getByText('Current Price')).toBeInTheDocument();
        expect(screen.getByText('Average Price')).toBeInTheDocument();
        expect(screen.getByText('Lowest Price')).toBeInTheDocument();
        expect(screen.getByText('Highest Price')).toBeInTheDocument();
        expect(screen.getAllByText(/72,499/).length).toBeGreaterThanOrEqual(1);
        expect(screen.getAllByText(/74,999/).length).toBeGreaterThanOrEqual(1);
      });
    });

    it('displays "Price history is not available yet." when no history exists', async () => {
      productService.getProductPriceHistory.mockResolvedValue({
        productId: 101,
        productName: 'Samsung Galaxy S24 128GB',
        period: '30D',
        currentPrice: 72499,
        lowestPrice: 0,
        highestPrice: 0,
        averagePrice: 0,
        pricePoints: [],
      });

      render(
        <PriceHistoryModal
          isOpen={true}
          onClose={vi.fn()}
          product={mockS24_128GB_Amazon}
        />
      );

      await waitFor(() => {
        expect(screen.getByText('Price history is not available yet.')).toBeInTheDocument();
      });
    });

  });

  // =========================================================================
  // 3. PRICE ALERTS TESTS
  // =========================================================================

  describe('Feature 3: Price Alerts Modal & Accidental Duplicate Prevention', () => {

    const renderWithAuth = (ui, authState) => {
      return render(
        <AuthContext.Provider value={authState}>
          {ui}
        </AuthContext.Provider>
      );
    };

    it('triggers auth gate when unauthenticated user attempts to set price alert', () => {
      const mockOpenAuth = vi.fn();
      renderWithAuth(
        <PriceAlertModal
          isOpen={true}
          onClose={vi.fn()}
          product={mockS24_128GB_Amazon}
          onOpenAuthModal={mockOpenAuth}
        />,
        { isAuthenticated: false, user: null }
      );

      expect(screen.getByText('Sign In to Set Price Alerts')).toBeInTheDocument();
      const signInBtn = screen.getByRole('button', { name: /Sign In \/ Create Account/i });
      fireEvent.click(signInBtn);
      expect(mockOpenAuth).toHaveBeenCalledTimes(1);
    });

    it('renders current price, target price input, and "Notify me when the price reaches your target."', async () => {
      alertService.getPriceAlerts.mockResolvedValue([]);

      renderWithAuth(
        <PriceAlertModal
          isOpen={true}
          onClose={vi.fn()}
          product={mockS24_128GB_Amazon}
        />,
        { isAuthenticated: true, user: { name: 'Sahil', email: 'sahil@example.com' } }
      );

      expect(screen.getByText('Notify me when the price reaches your target.')).toBeInTheDocument();
      expect(screen.getByText(/72,499/)).toBeInTheDocument();

      const input = screen.getByLabelText(/Target Price/i);
      expect(input).toBeInTheDocument();
      expect(screen.getByRole('button', { name: /Create Alert/i })).toBeInTheDocument();
      expect(screen.getByRole('button', { name: /Cancel/i })).toBeInTheDocument();
    });

    it('blocks accidental duplicate alert creation when an active alert already exists', async () => {
      alertService.getPriceAlerts.mockResolvedValue([
        {
          id: 501,
          productId: 101,
          productName: 'Samsung Galaxy S24 5G (128GB)',
          targetPrice: 65000,
          active: true,
        },
      ]);

      renderWithAuth(
        <PriceAlertModal
          isOpen={true}
          onClose={vi.fn()}
          product={mockS24_128GB_Amazon}
        />,
        { isAuthenticated: true, user: { name: 'Sahil', email: 'sahil@example.com' } }
      );

      await waitFor(() => {
        expect(screen.getByText(/Active alert already set/i)).toBeInTheDocument();
      });

      const createBtn = screen.getByRole('button', { name: /Create Alert/i });
      expect(createBtn).toBeDisabled();
    });

    it('creates alert successfully for valid target price and shows feedback', async () => {
      alertService.getPriceAlerts.mockResolvedValue([]);
      alertService.createPriceAlert.mockResolvedValue({
        id: 701,
        productId: 101,
        productName: 'Samsung Galaxy S24 5G (128GB)',
        targetPrice: 65000,
        active: true,
      });

      const mockOnAlertCreated = vi.fn();

      renderWithAuth(
        <PriceAlertModal
          isOpen={true}
          onClose={vi.fn()}
          product={mockS24_128GB_Amazon}
          onAlertCreated={mockOnAlertCreated}
        />,
        { isAuthenticated: true, user: { name: 'Sahil', email: 'sahil@example.com' } }
      );

      const input = screen.getByLabelText(/Target Price/i);
      fireEvent.change(input, { target: { value: '65000' } });

      const createBtn = screen.getByRole('button', { name: /Create Alert/i });
      await waitFor(() => {
        expect(createBtn).not.toBeDisabled();
      });

      fireEvent.click(createBtn);

      await waitFor(() => {
        expect(alertService.createPriceAlert).toHaveBeenCalledWith({
          productId: expect.anything(),
          productName: 'Samsung Galaxy S24 5G (128GB)',
          targetPrice: 65000,
          merchant: 'Amazon',
        });
        expect(screen.getByText(/Price alert set!/i)).toBeInTheDocument();
        expect(mockOnAlertCreated).toHaveBeenCalled();
      });
    });

    it('renders "Set Price Alert" button on ProductOfferCard and invokes callback', () => {
      const mockSetAlert = vi.fn();
      render(
        <ProductOfferCard
          offer={mockS24_128GB_Amazon}
          onSetPriceAlert={mockSetAlert}
        />
      );

      const alertBtn = screen.getByTestId('set-price-alert-btn');
      expect(alertBtn).toBeInTheDocument();
      expect(alertBtn).toHaveTextContent(/Set Price Alert/i);

      fireEvent.click(alertBtn);
      expect(mockSetAlert).toHaveBeenCalledWith(mockS24_128GB_Amazon);
    });

  });

});
