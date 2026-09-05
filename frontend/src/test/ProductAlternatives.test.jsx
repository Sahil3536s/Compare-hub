import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import React from 'react';
import { describe, it, expect, vi, beforeEach } from 'vitest';
import axios from 'axios';
import ProductAlternativesModal from '../components/ProductAlternativesModal';
import ProductAlternativesSection from '../components/ProductAlternativesSection';

vi.mock('axios');

describe('Product Alternatives Components', () => {
  const mockOffer = {
    productName: 'Apple iPhone 17 (128GB)',
    price: 70000,
    category: 'Smartphones',
    brand: 'Apple',
  };

  const mockAlternativesResponse = {
    data: {
      baseProductName: 'Apple iPhone 17 (128GB)',
      basePrice: 70000,
      totalAlternatives: 2,
      alternatives: [
        {
          id: 'alt-1',
          productName: 'OnePlus 13 (256GB)',
          brand: 'OnePlus',
          merchant: 'Amazon',
          price: 62000,
          priceDifference: -8000,
          priceDifferencePercent: -11.4,
          rating: 4.8,
          categoryType: 'CHEAPER_ALTERNATIVE',
          categoryLabel: 'Cheaper Option',
          similarityScore: 88,
          highlights: ['?8,000 cheaper (11.4% lower price)', 'More memory: 12GB vs 8GB (+4GB RAM)'],
        },
        {
          id: 'alt-2',
          productName: 'Samsung Galaxy S24 Plus (256GB)',
          brand: 'Samsung',
          merchant: 'Flipkart',
          price: 72000,
          priceDifference: 2000,
          priceDifferencePercent: 2.9,
          rating: 4.9,
          categoryType: 'SIMILAR_PRICE_BETTER_FEATURE',
          categoryLabel: 'Feature Upgrade',
          similarityScore: 85,
          highlights: ['Larger display: 6.7 inch vs 6.1 inch', 'Higher user rating: 4.9? vs 4.6?'],
        },
      ],
    },
  };

  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('renders modal with alternatives and category badges', async () => {
    axios.get.mockResolvedValueOnce(mockAlternativesResponse);
    const handleClose = vi.fn();

    render(
      <ProductAlternativesModal
        isOpen={true}
        onClose={handleClose}
        offer={mockOffer}
      />
    );

    expect(screen.getByTestId('product-alternatives-modal')).toBeInTheDocument();
    expect(screen.getByText(/Consider These Alternatives/i)).toBeInTheDocument();

    await waitFor(() => {
      expect(screen.getByText('OnePlus 13 (256GB)')).toBeInTheDocument();
      expect(screen.getByText('Cheaper Option')).toBeInTheDocument();
      expect(screen.getByText('Feature Upgrade')).toBeInTheDocument();
      expect(screen.getByText(/More memory: 12GB vs 8GB/i)).toBeInTheDocument();
    });

    const closeBtn = screen.getByTestId('close-alternatives-modal');
    fireEvent.click(closeBtn);
    expect(handleClose).toHaveBeenCalledTimes(1);
  });

  it('renders inline ProductAlternativesSection correctly', () => {
    render(
      <ProductAlternativesSection
        alternatives={mockAlternativesResponse.data.alternatives}
        baseProductTitle="Apple iPhone 17"
      />
    );

    expect(screen.getByTestId('product-alternatives-section')).toBeInTheDocument();
    expect(screen.getByText('OnePlus 13 (256GB)')).toBeInTheDocument();
    expect(screen.getByText('Samsung Galaxy S24 Plus (256GB)')).toBeInTheDocument();
    expect(screen.getByText(/2 suggestions/i)).toBeInTheDocument();
  });

  it('returns null when modal is closed', () => {
    const { container } = render(
      <ProductAlternativesModal isOpen={false} onClose={() => {}} offer={mockOffer} />
    );
    expect(container.firstChild).toBeNull();
  });
});
