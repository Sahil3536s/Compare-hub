import React from 'react';
import { render, screen, fireEvent } from '@testing-library/react';
import { describe, it, expect } from 'vitest';
import { RankingExplanationBanner } from '../components/RankingExplanationBanner';

describe('RankingExplanationBanner Component', () => {
  const mockRankingSummary = {
    cheapest: 'Flipkart',
    bestValue: 'Amazon',
    highestRated: 'Amazon',
    fastest: null,
    weights: {
      price: 0.40,
      rating: 0.20,
      discount: 0.15,
      delivery: 0.15,
      trust: 0.10,
    },
    explanation: 'Amazon is selected as Best Value due to superior rating and faster delivery.',
  };

  it('renders ranking badges and outcome highlights', () => {
    render(<RankingExplanationBanner rankingSummary={mockRankingSummary} type="product" />);

    expect(screen.getByText('Deterministic Best Value Ranking')).toBeInTheDocument();
    expect(screen.getByText('🏆 Best Value')).toBeInTheDocument();
    expect(screen.getAllByText('Amazon').length).toBeGreaterThanOrEqual(1);
    expect(screen.getAllByText('Flipkart').length).toBeGreaterThanOrEqual(1);
  });

  it('expands and reveals scoring formula and weights when clicked', () => {
    render(<RankingExplanationBanner rankingSummary={mockRankingSummary} type="product" />);

    const toggleButton = screen.getByText(/View Scoring Formula/i);
    fireEvent.click(toggleButton);

    expect(screen.getByText(/Price \/ Fare/i)).toBeInTheDocument();
    expect(screen.getByText(/Customer Rating/i)).toBeInTheDocument();
    expect(screen.getByText(new RegExp('Amazon is selected as Best Value', 'i'))).toBeInTheDocument();
  });
});
