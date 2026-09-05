import { render, screen } from '@testing-library/react';
import React from 'react';
import { describe, it, expect } from 'vitest';
import PurchaseTimingCard from '../components/PurchaseTimingCard';

describe('PurchaseTimingCard Component', () => {
  it('renders correctly with GOOD_TIME_TO_BUY status and score', () => {
    const timing = {
      score: 82,
      status: 'GOOD_TIME_TO_BUY',
      statusLabel: 'Good Time to Buy',
      confidence: 'MEDIUM',
      priceTrend: 'FALLING',
      sevenDayAvg: 49500,
      thirtyDayAvg: 51200,
      ninetyDayLow: 48999,
      reasons: [
        'Current price is 3.3% below 30-day average (?51,200).',
        'Close to 90-day minimum price (?48,999).',
        'Price decreased during the last 7 days.',
      ],
      disclaimer: 'Based strictly on historical price movements. Future prices are not guaranteed.',
    };

    render(<PurchaseTimingCard timing={timing} />);

    expect(screen.getByTestId('purchase-timing-card')).toBeInTheDocument();
    expect(screen.getByText('Good Time to Buy')).toBeInTheDocument();
    expect(screen.getByText('8.2')).toBeInTheDocument();
    expect(screen.getByText('MEDIUM Confidence')).toBeInTheDocument();
    expect(screen.getByText(/Current price is 3.3% below 30-day average/i)).toBeInTheDocument();
    expect(screen.getByText(/Close to 90-day minimum price/i)).toBeInTheDocument();
    expect(screen.getByText(/Based strictly on historical price movements/i)).toBeInTheDocument();
  });

  it('renders CONSIDER_WAITING with warning state', () => {
    const timing = {
      score: 30,
      status: 'CONSIDER_WAITING',
      statusLabel: 'Consider Waiting',
      confidence: 'HIGH',
      reasons: [
        'Current price is 12% above 30-day average.',
        'Price increased during the last week.',
      ],
      disclaimer: 'Historical trend analysis does not guarantee future prices.',
    };

    render(<PurchaseTimingCard timing={timing} />);

    expect(screen.getByText('Consider Waiting')).toBeInTheDocument();
    expect(screen.getByText('3.0')).toBeInTheDocument();
    expect(screen.getByText(/Current price is 12% above 30-day average/i)).toBeInTheDocument();
  });

  it('renders INSUFFICIENT_DATA gracefully when history is limited', () => {
    const timing = {
      score: 50,
      status: 'INSUFFICIENT_DATA',
      statusLabel: 'Insufficient Price History',
      confidence: 'LOW',
      reasons: [
        'At least 2 historical price points are required to evaluate timing patterns.',
      ],
      disclaimer: 'Historical trend analysis does not guarantee future prices.',
    };

    render(<PurchaseTimingCard timing={timing} />);

    expect(screen.getByText('Insufficient Price History')).toBeInTheDocument();
    expect(screen.getByText('5.0')).toBeInTheDocument();
    expect(screen.getByText('LOW Confidence')).toBeInTheDocument();
    expect(screen.getByText(/At least 2 historical price points are required/i)).toBeInTheDocument();
  });

  it('returns null when timing is not provided', () => {
    const { container } = render(<PurchaseTimingCard timing={null} />);
    expect(container.firstChild).toBeNull();
  });
});
