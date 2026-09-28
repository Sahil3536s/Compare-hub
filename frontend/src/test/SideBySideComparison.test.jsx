import React from 'react';
import { render, screen, fireEvent } from '@testing-library/react';
import { describe, it, expect } from 'vitest';
import ProductCard from '../components/ProductCard';

describe('Side-By-Side Store Comparison UI', () => {
  const mockProduct = {
    id: 1,
    title: 'Samsung Galaxy S24 Ultra 256GB',
    category: 'Smartphones',
    image: 'https://example.com/s24.jpg',
    rating: 4.8,
    reviewsCount: 3420,
    cheapestPrice: 129999,
    originalPrice: 139999,
    isCheapest: true,
    isBestValue: true,
    merchantOffers: [
      {
        id: 101,
        merchant: 'Amazon',
        price: 129999,
        originalPrice: 139999,
        inStock: true,
        deliveryText: 'Free Delivery Tomorrow',
        rating: 4.8,
        productUrl: 'https://amazon.in/s24',
        tag: 'Cheapest',
      },
      {
        id: 102,
        merchant: 'Flipkart',
        price: 131999,
        originalPrice: 139999,
        inStock: true,
        deliveryText: 'Standard Delivery 2 Days',
        rating: 4.7,
        productUrl: 'https://flipkart.com/s24',
        tag: 'Best Value',
      },
      {
        id: 103,
        merchant: 'Croma',
        price: 134999,
        originalPrice: 139999,
        inStock: false,
        deliveryText: 'Store Pickup Available',
        rating: 4.6,
        productUrl: 'https://croma.com/s24',
        tag: 'Store Pickup',
      },
    ],
  };

  it('renders product summary with lowest price and badges', () => {
    render(<ProductCard product={mockProduct} />);

    expect(screen.getByText('Samsung Galaxy S24 Ultra 256GB')).toBeInTheDocument();
    expect(screen.getByText(/1,29,999/)).toBeInTheDocument();
    expect(screen.getByText('Cheapest Price')).toBeInTheDocument();
    expect(screen.getByText('Best Value')).toBeInTheDocument();
  });

  it('expands side-by-side merchant offers when Compare Stores is clicked', () => {
    render(<ProductCard product={mockProduct} />);

    const compareButton = screen.getByRole('button', { name: /Compare 3 Stores/i });
    expect(compareButton).toBeInTheDocument();
    expect(compareButton).toHaveAttribute('aria-expanded', 'false');

    // Click to expand
    fireEvent.click(compareButton);

    expect(compareButton).toHaveAttribute('aria-expanded', 'true');
    expect(screen.getByText('Available Store Offers (3)')).toBeInTheDocument();
    expect(screen.getByText('Amazon')).toBeInTheDocument();
    expect(screen.getByText('Flipkart')).toBeInTheDocument();
    expect(screen.getByText('Croma')).toBeInTheDocument();

    // Click to collapse
    fireEvent.click(compareButton);
    expect(compareButton).toHaveAttribute('aria-expanded', 'false');
    expect(screen.queryByText('Available Store Offers (3)')).not.toBeInTheDocument();
  });
});
