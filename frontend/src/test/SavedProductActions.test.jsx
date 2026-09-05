import React from 'react';
import { render, screen, fireEvent } from '@testing-library/react';
import { describe, it, expect, vi } from 'vitest';
import { ProductCard } from '../components/ProductCard';

describe('ProductCard Saved Actions', () => {
  const mockProduct = {
    id: 1,
    title: 'Apple MacBook Air M2',
    category: 'Laptops',
    image: 'https://example.com/macbook.jpg',
    cheapestPrice: 94990,
    originalPrice: 114900,
    rating: 4.8,
    reviewsCount: 1420,
    isCheapest: true,
    isBestValue: true,
    offers: [
      { id: '1', merchant: 'Amazon', price: 94990, inStock: true, productUrl: '#' }
    ]
  };

  it('renders product information accurately', () => {
    render(<ProductCard product={mockProduct} />);

    expect(screen.getByText('Apple MacBook Air M2')).toBeInTheDocument();
    expect(screen.getByText('Laptops')).toBeInTheDocument();
    expect(screen.getByText('₹94,990')).toBeInTheDocument();
  });

  it('toggles save status and triggers onSave callback', () => {
    const handleSave = vi.fn();
    render(<ProductCard product={mockProduct} onSave={handleSave} isSaved={false} />);

    const saveButton = screen.getByTitle('Save Product');
    expect(saveButton).toBeInTheDocument();

    fireEvent.click(saveButton);
    expect(handleSave).toHaveBeenCalledWith(mockProduct);

    expect(screen.getByTitle('Remove from Saved')).toBeInTheDocument();
  });
});
