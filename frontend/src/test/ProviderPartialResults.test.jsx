import React from 'react';
import { render, screen } from '@testing-library/react';
import { describe, it, expect } from 'vitest';
import { BrowserRouter } from 'react-router-dom';
import UniversalSearchResults from '../components/UniversalSearchResults';
import ProductOfferCard from '../components/ProductOfferCard';

describe('Provider Partial Results & Resilience UI', () => {
  it('renders partial search results when only one provider responds successfully', () => {
    const partialResult = {
      intent: 'PRODUCT_SEARCH',
      query: 'MacBook Air M3',
      executionTimeMs: 142,
      redirectRoute: '/shopping',
      productResults: {
        totalOffers: 1,
        cheapestPrice: 104900,
        cheapestMerchant: 'Flipkart',
        offers: [
          {
            productName: 'Apple MacBook Air M3 256GB',
            merchant: 'Flipkart',
            price: 104900,
            originalPrice: 114900,
            currency: 'INR',
            rating: 4.9,
            delivery: 'Free Delivery by Friday',
            inStock: true,
            discountPercent: 9,
            isCheapest: true,
            isBestValue: true,
            rankingScore: 92.0,
            costBreakdown: {
              basePrice: 104900,
              deliveryFee: 0,
              platformFee: 0,
              discounts: 2000,
              effectivePrice: 102900,
            },
          },
        ],
      },
    };

    render(
      <BrowserRouter>
        <UniversalSearchResults result={partialResult} />
      </BrowserRouter>
    );

    expect(screen.getByText('Product Search')).toBeInTheDocument();
    expect(screen.getByText('Apple MacBook Air M3 256GB')).toBeInTheDocument();
    expect(screen.getByText('Flipkart')).toBeInTheDocument();
    expect(screen.getAllByText(/1,04,900/)[0]).toBeInTheDocument();
    expect(screen.getByText('Cheapest Option')).toBeInTheDocument();
  });

  it('renders ProductOfferCard correctly when cost breakdown has delivery and platform fees', () => {
    const offerWithFees = {
      productName: 'Mechanical Keyboard',
      merchant: 'Amazon',
      price: 2999,
      originalPrice: 3999,
      currency: 'INR',
      rating: 4.5,
      delivery: 'Standard Delivery',
      inStock: true,
      discountPercent: 25,
      isCheapest: false,
      costBreakdown: {
        basePrice: 2999,
        deliveryFee: 40,
        platformFee: 5,
        discounts: 0,
        effectivePrice: 3044,
      },
    };

    render(<ProductOfferCard offer={offerWithFees} />);

    expect(screen.getByText('Mechanical Keyboard')).toBeInTheDocument();
    expect(screen.getByText('Amazon')).toBeInTheDocument();
    expect(screen.getByText(/2,999/)).toBeInTheDocument();
    expect(screen.getByText('Price Breakdown')).toBeInTheDocument();
  });
});
