import { render, screen, fireEvent } from '@testing-library/react';
import React from 'react';
import { describe, it, expect } from 'vitest';
import BestPaymentOptionCard from '../components/BestPaymentOptionCard';

describe('BestPaymentOptionCard Component', () => {
  const mockPaymentOffers = {
    standardPrice: 62999,
    bestEligiblePrice: 59999,
    maxSavings: 3000,
    bestOffer: {
      id: 'AMZ_HDFC_CC_50K',
      bank: 'HDFC',
      paymentType: 'CREDIT_CARD',
      title: 'HDFC Credit Card Instant Discount',
      description: 'Flat ?3,000 instant discount on orders above ?50,000',
      discountAmount: 3000,
      effectivePrice: 59999,
      eligibilityStatus: 'ELIGIBLE',
      eligibilityReason: 'Matches your HDFC Credit Card preference',
    },
    offers: [
      {
        id: 'AMZ_HDFC_CC_50K',
        bank: 'HDFC',
        paymentType: 'CREDIT_CARD',
        title: 'HDFC Credit Card Instant Discount',
        discountAmount: 3000,
        effectivePrice: 59999,
        eligibilityStatus: 'ELIGIBLE',
        eligibilityReason: 'Matches your HDFC Credit Card preference',
      },
      {
        id: 'AMZ_ICICI_CC_40K',
        bank: 'ICICI',
        paymentType: 'CREDIT_CARD',
        title: 'ICICI Credit Card Instant Discount',
        discountAmount: 2500,
        effectivePrice: 60499,
        eligibilityStatus: 'NOT_ELIGIBLE',
        eligibilityReason: 'Requires ICICI card (your preferred bank is HDFC)',
      },
      {
        id: 'AMZ_UPI_GEN',
        bank: 'ALL',
        paymentType: 'UPI',
        title: 'Amazon Pay UPI / Any UPI Discount',
        discountAmount: 250,
        effectivePrice: 62749,
        eligibilityStatus: 'ELIGIBLE',
        eligibilityReason: 'Matches your UPI payment preference',
      },
    ],
    userPreference: {
      preferredBank: 'HDFC',
      preferredCardType: 'CREDIT_CARD',
      hasUpi: true,
    },
    securityNote: 'CompareHub never collects or stores card numbers, CVVs, PINs, or OTPs.',
  };

  it('renders best payment option with savings highlight and best price', () => {
    render(<BestPaymentOptionCard paymentOffers={mockPaymentOffers} />);

    expect(screen.getByTestId('best-payment-option-card')).toBeInTheDocument();
    expect(screen.getByText(/Best Payment Option/i)).toBeInTheDocument();
    expect(screen.getByText(/Save/i)).toBeInTheDocument();
    expect(screen.getByText(/59,999/)).toBeInTheDocument();
    expect(screen.getByText(/HDFC Credit Card Instant Discount/i)).toBeInTheDocument();
  });

  it('toggles payment offers drawer when button is clicked', () => {
    render(<BestPaymentOptionCard paymentOffers={mockPaymentOffers} />);

    expect(screen.queryByTestId('payment-offers-drawer')).not.toBeInTheDocument();

    const toggleBtn = screen.getByTestId('toggle-payment-offers');
    fireEvent.click(toggleBtn);

    expect(screen.getByTestId('payment-offers-drawer')).toBeInTheDocument();
    expect(screen.getByText(/Filter by Bank:/i)).toBeInTheDocument();
    expect(screen.getAllByTestId('payment-offer-item')).toHaveLength(3);
    expect(screen.getByText(/CompareHub never collects or stores card numbers/i)).toBeInTheDocument();
  });

  it('filters offers list when bank filter is changed', () => {
    render(<BestPaymentOptionCard paymentOffers={mockPaymentOffers} />);

    fireEvent.click(screen.getByTestId('toggle-payment-offers'));

    const select = screen.getByTestId('bank-filter-select');
    fireEvent.change(select, { target: { value: 'ICICI' } });

    const items = screen.getAllByTestId('payment-offer-item');
    // Should show ICICI + Universal UPI offers
    expect(items.length).toBe(2);
    expect(screen.getByText(/ICICI Credit Card Instant Discount/i)).toBeInTheDocument();
  });

  it('returns null when paymentOffers is empty or missing', () => {
    const { container } = render(<BestPaymentOptionCard paymentOffers={null} />);
    expect(container.firstChild).toBeNull();
  });
});
