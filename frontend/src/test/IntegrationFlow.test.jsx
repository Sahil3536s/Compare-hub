import React from 'react';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import { describe, it, expect, vi } from 'vitest';
import { MemoryRouter } from 'react-router-dom';
import { ShoppingPage } from '../pages/ShoppingPage';
import * as ProductServiceModule from '../services/productService';

describe('ShoppingPage Integration Flow', () => {
  const mockSearchResponse = {
    query: 'iphone',
    totalOffers: 2,
    cheapestPrice: 78900,
    cheapestMerchant: 'Flipkart',
    rankingSummary: {
      cheapest: 'Flipkart',
      bestValue: 'Amazon',
      highestRated: 'Amazon',
      weights: { price: 0.4, rating: 0.2, discount: 0.15, delivery: 0.15, trust: 0.1 },
      explanation: 'Amazon selected as Best Value due to faster delivery.',
    },
    offers: [
      {
        productName: 'iPhone 15 128GB',
        merchant: 'Flipkart',
        price: 78900,
        originalPrice: 79900,
        currency: 'INR',
        rating: 4.7,
        delivery: '2 Days Delivery',
        inStock: true,
        discountPercent: 1,
        isCheapest: true,
        isBestValue: false,
        productUrl: 'https://flipkart.com/test',
      },
      {
        productName: 'iPhone 15 Pro 256GB',
        merchant: 'Amazon',
        price: 79900,
        originalPrice: 79900,
        currency: 'INR',
        rating: 4.8,
        delivery: 'Tomorrow',
        inStock: true,
        discountPercent: 0,
        isCheapest: false,
        isBestValue: true,
        productUrl: 'https://amazon.in/test',
      },
    ],
  };

  it('loads products, displays total offers, and updates on new query', async () => {
    vi.spyOn(ProductServiceModule, 'searchProducts').mockResolvedValue(mockSearchResponse);

    render(
      <MemoryRouter>
        <ShoppingPage />
      </MemoryRouter>
    );

    // Verify loading transitions to results
    await waitFor(() => {
      expect(screen.getByText('iPhone 15 128GB')).toBeInTheDocument();
      expect(screen.getByText('iPhone 15 Pro 256GB')).toBeInTheDocument();
    });

    expect(screen.getByText(/Showing/i)).toBeInTheDocument();
    expect(screen.getAllByText('Flipkart').length).toBeGreaterThanOrEqual(1);
    expect(screen.getAllByText('Amazon').length).toBeGreaterThanOrEqual(1);

    // Trigger a new search
    const searchInput = screen.getByPlaceholderText(/Search 'iPhone 15'/i);
    fireEvent.change(searchInput, { target: { value: 'MacBook' } });
    fireEvent.submit(searchInput.closest('form'));

    await waitFor(() => {
      expect(ProductServiceModule.searchProducts).toHaveBeenCalledWith(
        expect.objectContaining({ query: 'MacBook' })
      );
    });
  });
});
