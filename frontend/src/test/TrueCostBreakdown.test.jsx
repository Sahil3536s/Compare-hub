import React from 'react';
import { render, screen, fireEvent } from '@testing-library/react';
import { describe, it, expect, vi } from 'vitest';
import ProductOfferCard from '../components/ProductOfferCard';

describe('True Cost Engine Frontend - Price Breakdown', () => {
  const mockOfferWithTrueCost = {
    productName: 'Samsung Galaxy S24 Ultra 256GB',
    merchant: 'Amazon',
    price: 120000,
    effectivePrice: 118500,
    rating: 4.8,
    delivery: 'Free Delivery',
    inStock: true,
    discountPercent: 10,
    isCheapest: true,
    costBreakdown: {
      basePrice: 120000,
      deliveryFee: 0,
      platformFee: 0,
      discounts: 1500,
      effectivePrice: 118500,
      appliedOffers: [
        {
          type: 'PAYMENT_OFFER',
          description: '₹1,500 Instant Discount on HDFC & ICICI Credit Cards',
          discountAmount: 1500,
          isConditional: true,
          terms: 'Requires eligible HDFC or ICICI Bank Credit Card transaction',
        },
      ],
      hasConditionalDiscounts: true,
    },
  };

  it('renders effective price badge and price breakdown trigger button', () => {
    render(<ProductOfferCard offer={mockOfferWithTrueCost} />);

    expect(screen.getByText('₹1,18,500')).toBeInTheDocument();
    expect(screen.getByText(/Price Breakdown/i)).toBeInTheDocument();
    expect(screen.getByText('-₹1,500')).toBeInTheDocument();
  });

  it('expands price breakdown drawer showing base price, discounts, and conditional terms', () => {
    render(<ProductOfferCard offer={mockOfferWithTrueCost} />);

    // Click Price Breakdown toggle
    const toggleBtn = screen.getByRole('button', { name: /Price Breakdown/i });
    fireEvent.click(toggleBtn);

    expect(screen.getByTestId('price-breakdown-drawer')).toBeInTheDocument();
    expect(screen.getByText(/Listed Base Price/i)).toBeInTheDocument();
    expect(screen.getByText(/₹1,500 Instant Discount on HDFC & ICICI Credit Cards/i)).toBeInTheDocument();
    expect(screen.getByText(/⚠️ Conditional: Requires eligible HDFC or ICICI Bank Credit Card transaction/i)).toBeInTheDocument();
  });
});
