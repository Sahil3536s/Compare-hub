import React from 'react';
import { render, screen } from '@testing-library/react';
import { describe, it, expect } from 'vitest';
import ProductOfferCard from '../components/ProductOfferCard';

describe('Deal Quality Engine Frontend', () => {
  const mockOfferWithExceptionalDeal = {
    productName: 'Samsung Galaxy S24 Ultra 256GB',
    merchant: 'Amazon',
    price: 110000,
    effectivePrice: 110000,
    originalPrice: 134999,
    discountPercent: 18,
    dealQuality: {
      dealScore: 94,
      classification: 'EXCEPTIONAL_DEAL',
      classificationLabel: 'Exceptional Deal',
      currentPrice: 110000,
      historicalAverage: 124000,
      thirtyDayLow: 110000,
      ninetyDayLow: 110000,
      realDiscountPercentVsAverage: 11,
      realSavingsVsAverage: 14000,
      summary: 'Current price is at or near its 90-day lowest record.',
      isAtLowest: true,
    },
  };

  const mockOfferWithInflatedMrp = {
    productName: 'Headphones Pro',
    merchant: 'Flipkart',
    price: 49999,
    effectivePrice: 49999,
    originalPrice: 69999,
    discountPercent: 29,
    dealQuality: {
      dealScore: 52,
      classification: 'AVERAGE_PRICE',
      classificationLabel: 'Normal Market Price',
      currentPrice: 49999,
      historicalAverage: 51200,
      advertisedDiscountPercent: 29,
      realDiscountPercentVsAverage: 2,
      summary: 'Current price is within its normal historical trading range.',
      disclaimer: 'Advertised discount is high (29% vs MRP), but the current price is close to its recent average (real saving: 2%).',
    },
  };

  it('renders exceptional deal badge and 90-day lowest indicator', () => {
    render(<ProductOfferCard offer={mockOfferWithExceptionalDeal} />);

    expect(screen.getByText('94 Deal Score')).toBeInTheDocument();
    expect(screen.getByText(/Exceptional Deal/i)).toBeInTheDocument();
  });

  it('renders safe nuanced disclaimer for inflated MRP discounts without false accusations', () => {
    render(<ProductOfferCard offer={mockOfferWithInflatedMrp} />);

    expect(screen.getByText(/Normal Market Price/i)).toBeInTheDocument();
    expect(screen.getByText(/Advertised discount is high \(29% vs MRP\)/i)).toBeInTheDocument();
    expect(screen.getByText(/close to its recent average/i)).toBeInTheDocument();
  });
});
