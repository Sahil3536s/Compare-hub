import React from 'react';
import { render, screen } from '@testing-library/react';
import { describe, it, expect } from 'vitest';
import ProductOfferCard from '../components/ProductOfferCard';

describe('ProductOfferCard Component', () => {
  const mockOffer = {
    productName: 'iPhone 15 Pro 128GB',
    merchant: 'Amazon',
    price: 127990,
    originalPrice: 134900,
    currency: 'INR',
    rating: 4.8,
    delivery: 'Free Delivery by Tomorrow',
    inStock: true,
    discountPercent: 5,
    isCheapest: true,
    isBestValue: true,
    rankingScore: 88.5,
    productUrl: 'https://amazon.in/test',
  };

  it('renders product name, merchant and formatted prices', () => {
    render(<ProductOfferCard offer={mockOffer} />);

    expect(screen.getByText('iPhone 15 Pro 128GB')).toBeInTheDocument();
    expect(screen.getByText('Amazon')).toBeInTheDocument();
    expect(screen.getByText(/1,27,990/)).toBeInTheDocument();
  });

  it('renders Best Value and Cheapest badges', () => {
    render(<ProductOfferCard offer={mockOffer} />);

    expect(screen.getByText('Best Value')).toBeInTheDocument();
    expect(screen.getByText('Cheapest Option')).toBeInTheDocument();
  });

  it('displays delivery time and in-stock indicator', () => {
    render(<ProductOfferCard offer={mockOffer} />);

    expect(screen.getByText(/Free Delivery by Tomorrow/i)).toBeInTheDocument();
    expect(screen.getByText('In Stock')).toBeInTheDocument();
  });
});
