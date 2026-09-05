import React from 'react';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import { describe, it, expect, vi } from 'vitest';
import SmartCartPage from '../pages/SmartCartPage';
import * as CartServiceModule from '../services/cartService';

describe('Smart Cart Page Frontend', () => {
  const mockOptimizationResponse = {
    strategy: 'MINIMIZE_PRICE',
    totalItemsRequested: 2,
    totalItemsMatched: 2,
    estimatedSavings: 650,
    explanation: 'Splitting into 2 orders saves ₹650 vs single-store purchase.',
    recommendedPlan: {
      planType: 'OPTIMIZED_MIXED',
      title: 'Smart Split: Amazon + Flipkart',
      description: 'Multi-store optimized combination split across 2 separate orders.',
      totalProductsCost: 7100,
      totalDeliveryFees: 200,
      totalPlatformFees: 0,
      grandTotal: 7300,
      totalOrders: 2,
      isRecommended: true,
      merchantOrders: [
        {
          merchant: 'Amazon',
          itemsSubtotal: 1500,
          deliveryFee: 0,
          platformFee: 0,
          merchantTotal: 1500,
          items: [
            {
              itemName: 'Logitech Wireless Mouse',
              matchedProductName: 'Logitech MX Master 3S Mouse',
              merchant: 'Amazon',
              quantity: 1,
              unitPrice: 1500,
              totalPrice: 1500,
            },
          ],
        },
        {
          merchant: 'Flipkart',
          itemsSubtotal: 5600,
          deliveryFee: 200,
          platformFee: 0,
          merchantTotal: 5800,
          items: [
            {
              itemName: 'Mechanical Keyboard',
              matchedProductName: 'Keychron Mechanical Keyboard',
              merchant: 'Flipkart',
              quantity: 1,
              unitPrice: 5600,
              totalPrice: 5600,
            },
          ],
        },
      ],
    },
    singleStorePlans: [
      {
        planType: 'SINGLE_STORE',
        title: 'All-in-One: Amazon',
        grandTotal: 7950,
        totalDeliveryFees: 0,
        totalOrders: 1,
      },
      {
        planType: 'SINGLE_STORE',
        title: 'All-in-One: Flipkart',
        grandTotal: 8100,
        totalDeliveryFees: 0,
        totalOrders: 1,
      },
    ],
    cheapestSingleStore: {
      planType: 'SINGLE_STORE',
      title: 'All-in-One: Amazon',
      grandTotal: 7950,
    },
  };

  it('renders shopping list and adds new items', () => {
    render(<SmartCartPage />);

    expect(screen.getByText(/Your Shopping List/i)).toBeInTheDocument();
    expect(screen.getByText('Logitech Wireless Mouse')).toBeInTheDocument();

    const input = screen.getByPlaceholderText(/Add item/i);
    fireEvent.change(input, { target: { value: 'Webcam' } });
    const addBtn = screen.getByRole('button', { name: /\+ Add/i });
    fireEvent.click(addBtn);

    expect(screen.getByText('Webcam')).toBeInTheDocument();
  });

  it('optimizes cart and displays smart split savings breakdown', async () => {
    vi.spyOn(CartServiceModule, 'optimizeCart').mockResolvedValue(mockOptimizationResponse);

    render(<SmartCartPage />);

    const optimizeBtn = screen.getByRole('button', { name: /Compare & Optimize Cart/i });
    fireEvent.click(optimizeBtn);

    await waitFor(() => {
      expect(screen.getByText(/Smart Split: Amazon \+ Flipkart/i)).toBeInTheDocument();
      expect(screen.getByText(/Save ₹650 with Smart Split!/i)).toBeInTheDocument();
      expect(screen.getByText('All-in-One: Amazon')).toBeInTheDocument();
    });
  });
});
