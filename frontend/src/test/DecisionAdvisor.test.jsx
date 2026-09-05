import { describe, it, expect } from 'vitest';
import { render, screen } from '@testing-library/react';
import React from 'react';
import DecisionAdvisorCard from '../components/DecisionAdvisorCard';
import AiRecommendationCard from '../components/AiRecommendationCard';

describe('DecisionAdvisorCard Component', () => {
  it('renders decision advisor with summary, pick, reasons, tradeoffs, and high confidence', () => {
    const mockRec = {
      recommendedOption: 'Amazon (₹64,999)',
      summary: 'Store A costs ₹700 more than Store B, but has faster delivery and a higher seller rating. If price is your priority choose B; otherwise A offers better overall value.',
      reasons: [
        'Amazon offers higher seller satisfaction score (4.6★ vs 4.1★).',
        'Delivery speed: Tomorrow with Amazon vs 3-5 days with Croma.'
      ],
      tradeoffs: [
        'Paying ₹700 extra on Amazon for faster delivery and superior merchant rating.',
        'Opting for Croma saves ₹700 but may require longer delivery transit.'
      ],
      confidence: 'HIGH',
      contextType: 'PRODUCT'
    };

    render(<DecisionAdvisorCard recommendation={mockRec} />);

    expect(screen.getByText(/AI Decision Advisor/i)).toBeInTheDocument();
    expect(screen.getByText(/High Confidence/i)).toBeInTheDocument();
    expect(screen.getByText(/Amazon \(₹64,999\)/i)).toBeInTheDocument();
    expect(screen.getByText(/Store A costs ₹700 more than Store B/i)).toBeInTheDocument();
    expect(screen.getByText(/Key Deciding Factors/i)).toBeInTheDocument();
    expect(screen.getByText(/Trade-offs & Considerations/i)).toBeInTheDocument();
    expect(screen.getByText(/Paying ₹700 extra on Amazon/i)).toBeInTheDocument();
    expect(screen.getByText(/Zero hallucination guarantee/i)).toBeInTheDocument();
  });

  it('renders travel decision advice with stops and duration tradeoffs', () => {
    const travelRec = {
      recommendedOption: 'IndiGo 6E-204 (₹5,450)',
      summary: 'IndiGo (6E-204) costs ₹600 more but saves 2 hours and requires one fewer transfer.',
      reasons: [
        'Fastest & Best: IndiGo (6E-204) takes 2h 15m at ₹5,450.',
        'Non-stop direct flight eliminates layover baggage transfer risks.'
      ],
      tradeoffs: [
        'Paying ₹600 fare premium on IndiGo saves 2h of journey duration.'
      ],
      confidence: 'HIGH',
      contextType: 'FLIGHT'
    };

    render(<DecisionAdvisorCard recommendation={travelRec} title="Travel Decision Advisor" />);

    expect(screen.getByText(/Travel Decision Advisor/i)).toBeInTheDocument();
    expect(screen.getByText('IndiGo 6E-204 (₹5,450)')).toBeInTheDocument();
    expect(screen.getByText(/saves 2 hours and requires one fewer transfer/i)).toBeInTheDocument();
    expect(screen.getByText('FLIGHT')).toBeInTheDocument();
  });

  it('handles backward-compatible legacy recommendation format in AiRecommendationCard', () => {
    const legacyRec = {
      bestOverall: 'Amazon Store Pick',
      cheapest: 'Croma',
      recommendation: 'Amazon offers the highest value with confirmed stock.',
      reasoningPoints: ['Lowest price across verified stores', 'Next-day delivery']
    };

    render(<AiRecommendationCard recommendation={legacyRec} />);

    expect(screen.getByText(/Amazon offers the highest value with confirmed stock/i)).toBeInTheDocument();
    expect(screen.getByText('Amazon Store Pick')).toBeInTheDocument();
    expect(screen.getByText(/Lowest price across verified stores/i)).toBeInTheDocument();
  });

  it('returns null when recommendation is empty', () => {
    const { container } = render(<DecisionAdvisorCard recommendation={null} />);
    expect(container.firstChild).toBeNull();
  });
});
