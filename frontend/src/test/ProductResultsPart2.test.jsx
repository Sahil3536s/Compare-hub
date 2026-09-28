import React from 'react';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import { describe, it, expect, vi, beforeEach } from 'vitest';
import { MemoryRouter } from 'react-router-dom';
import { ShoppingPage } from '../pages/ShoppingPage';
import ProductOfferCard from '../components/ProductOfferCard';
import * as ProductServiceModule from '../services/productService';

describe('Part 2 UI Upgrade: Product Results, Filters, and Product Cards', () => {
  const mockOffer = {
    productName: 'Samsung Galaxy S24 Ultra',
    merchant: 'Amazon',
    price: 129999,
    originalPrice: 139999,
    effectivePrice: 124999,
    currency: 'INR',
    rating: 4.9,
    delivery: 'Same Day Delivery',
    inStock: true,
    discountPercent: 7,
    isCheapest: true,
    isBestValue: true,
    isHighestRated: true,
    isFastestDelivery: true,
    rankingScore: 96.5,
    attributes: {
      ram: '12GB',
      storage: '256GB',
      variant: 'Titanium Gray',
      color: 'Gray',
    },
    costBreakdown: {
      basePrice: 129999,
      deliveryFee: 0,
      platformFee: 0,
      discounts: 5000,
      effectivePrice: 124999,
    },
    productUrl: 'https://amazon.in/s24',
  };

  const mockResponse = {
    query: 's24',
    totalOffers: 3,
    cheapestPrice: 124999,
    cheapestMerchant: 'Amazon',
    failedProviders: ['Croma'],
    offers: [mockOffer],
  };

  beforeEach(() => {
    vi.restoreAllMocks();
  });

  describe('Feature 1: Product Results Page & 4 Ranking Tabs', () => {
    it('displays search query, result count, and renders 4 backend ranking tabs', async () => {
      const searchSpy = vi.spyOn(ProductServiceModule, 'searchProducts').mockResolvedValue(mockResponse);

      render(
        <MemoryRouter initialEntries={['/shopping?q=s24']}>
          <ShoppingPage />
        </MemoryRouter>
      );

      await waitFor(() => {
        expect(screen.getByText('Samsung Galaxy S24 Ultra')).toBeInTheDocument();
      });

      // Verify query display and result count
      expect(screen.getAllByText(/Showing/i).length).toBeGreaterThanOrEqual(1);
      expect(screen.getAllByText(/s24/i).length).toBeGreaterThanOrEqual(1);

      // Verify the 4 backend ranking tabs exist
      expect(screen.getByRole('tab', { name: /Cheapest/i })).toBeInTheDocument();
      expect(screen.getByRole('tab', { name: /Best Value/i })).toBeInTheDocument();
      expect(screen.getByRole('tab', { name: /Highest Rated/i })).toBeInTheDocument();
      expect(screen.getByRole('tab', { name: /Fastest Delivery/i })).toBeInTheDocument();

      // Click Cheapest tab -> calls backend with sortBy='price_asc'
      fireEvent.click(screen.getByRole('tab', { name: /Cheapest/i }));
      await waitFor(() => {
        expect(searchSpy).toHaveBeenCalledWith(
          expect.objectContaining({ sortBy: 'price_asc' })
        );
      });

      // Click Fastest Delivery tab -> calls backend with sortBy='fastest_delivery'
      fireEvent.click(screen.getByRole('tab', { name: /Fastest Delivery/i }));
      await waitFor(() => {
        expect(searchSpy).toHaveBeenCalledWith(
          expect.objectContaining({ sortBy: 'fastest_delivery' })
        );
      });

      // Click Highest Rated tab -> calls backend with sortBy='rating'
      fireEvent.click(screen.getByRole('tab', { name: /Highest Rated/i }));
      await waitFor(() => {
        expect(searchSpy).toHaveBeenCalledWith(
          expect.objectContaining({ sortBy: 'rating' })
        );
      });
    });
  });

  describe('Feature 2: Filter System & Mobile Drawer', () => {
    it('renders all filter dimensions and allows clearing and applying', async () => {
      vi.spyOn(ProductServiceModule, 'searchProducts').mockResolvedValue(mockResponse);

      render(
        <MemoryRouter>
          <ShoppingPage />
        </MemoryRouter>
      );

      await waitFor(() => {
        expect(screen.getByText('Samsung Galaxy S24 Ultra')).toBeInTheDocument();
      });

      // Check filter options are present
      expect(screen.getAllByText('Price Range').length).toBeGreaterThanOrEqual(1);
      expect(screen.getAllByText('Brand').length).toBeGreaterThanOrEqual(1);
      expect(screen.getAllByText('Customer Rating').length).toBeGreaterThanOrEqual(1);
      expect(screen.getAllByText('In Stock Only').length).toBeGreaterThanOrEqual(1);
      expect(screen.getAllByText('RAM').length).toBeGreaterThanOrEqual(1);
      expect(screen.getAllByText('Internal Storage').length).toBeGreaterThanOrEqual(1);
      expect(screen.getAllByText('Delivery Speed').length).toBeGreaterThanOrEqual(1);

      // Open mobile filter drawer
      const filterBtn = screen.getByLabelText(/Open filters drawer/i);
      fireEvent.click(filterBtn);

      expect(screen.getByRole('dialog')).toBeInTheDocument();
      expect(screen.getAllByRole('button', { name: /Clear Filters/i }).length).toBeGreaterThanOrEqual(1);
      expect(screen.getByRole('button', { name: /Apply Filters/i })).toBeInTheDocument();

      // Click Apply Filters closes the drawer
      fireEvent.click(screen.getByRole('button', { name: /Apply Filters/i }));
      await waitFor(() => {
        expect(screen.queryByRole('dialog')).not.toBeInTheDocument();
      });
    });
  });

  describe('Feature 3: Upgraded Product Card Design & Badges', () => {
    it('renders RAM, Storage, Variant, Badges, and Action buttons', () => {
      const onSaveMock = vi.fn();
      const onCompareMock = vi.fn();

      render(
        <ProductOfferCard
          offer={mockOffer}
          onSave={onSaveMock}
          onCompare={onCompareMock}
        />
      );

      // Verify product name & merchant
      expect(screen.getByText('Samsung Galaxy S24 Ultra')).toBeInTheDocument();
      expect(screen.getByText('Amazon')).toBeInTheDocument();

      // Verify specs: RAM and Storage
      expect(screen.getByText(/RAM: 12GB/i)).toBeInTheDocument();
      expect(screen.getByText(/Storage: 256GB/i)).toBeInTheDocument();
      expect(screen.getByText('Titanium Gray')).toBeInTheDocument();

      // Verify Badges: Cheapest, Best Value, Fastest Delivery, Top Rated
      expect(screen.getAllByText(/Cheapest/i).length).toBeGreaterThanOrEqual(1);
      expect(screen.getByText('Best Value')).toBeInTheDocument();
      expect(screen.getByText('Fastest Delivery')).toBeInTheDocument();

      // Verify Action buttons: View Deal, Compare, Save
      expect(screen.getByText('View Deal')).toBeInTheDocument();
      expect(screen.getByText('Compare')).toBeInTheDocument();
      expect(screen.getByTitle('Add to Wishlist')).toBeInTheDocument();

      // Click Compare
      fireEvent.click(screen.getByRole('button', { name: /Compare Samsung Galaxy S24 Ultra/i }));
      expect(onCompareMock).toHaveBeenCalledWith(mockOffer);

      // Click Save
      fireEvent.click(screen.getByTitle('Add to Wishlist'));
      expect(onSaveMock).toHaveBeenCalledWith(mockOffer);
    });

    it('shows partial provider failure alert without breaking the page', async () => {
      vi.spyOn(ProductServiceModule, 'searchProducts').mockResolvedValue(mockResponse);

      render(
        <MemoryRouter>
          <ShoppingPage />
        </MemoryRouter>
      );

      await waitFor(() => {
        expect(screen.getByText(/Some stores couldn't be reached/i)).toBeInTheDocument();
        expect(screen.getByText(/Unable to reach Croma in time/i)).toBeInTheDocument();
        expect(screen.getByText('Samsung Galaxy S24 Ultra')).toBeInTheDocument();
      });
    });
  });
});
