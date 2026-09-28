import React from 'react';
import { render, screen, fireEvent } from '@testing-library/react';
import { describe, it, expect, vi } from 'vitest';
import NluIntentBadge from '../components/NluIntentBadge';
import NluClarificationModal from '../components/NluClarificationModal';
import UniversalSearchResults from '../components/UniversalSearchResults';
import { BrowserRouter } from 'react-router-dom';

describe('Phase 32: Advanced NLU Query Understanding Components', () => {
  it('renders NluIntentBadge with product entities correctly', () => {
    const mockQueryUnderstanding = {
      intent: 'PRODUCT_SEARCH',
      confidence: 0.95,
      productEntities: {
        category: 'smartphone',
        brand: 'Samsung',
        maxPrice: 30000,
        storage: '256GB',
        priority: 'camera',
      },
    };

    render(<NluIntentBadge queryUnderstanding={mockQueryUnderstanding} intent="PRODUCT_SEARCH" />);

    expect(screen.getByTestId('nlu-intent-badge')).toBeInTheDocument();
    expect(screen.getByText(/NLU Understood/i)).toBeInTheDocument();
    expect(screen.getByText(/95% Confidence/i)).toBeInTheDocument();
    expect(screen.getByText(/Brand:/i)).toBeInTheDocument();
    expect(screen.getByText(/Samsung/i)).toBeInTheDocument();
    expect(screen.getByText(/smartphone/i)).toBeInTheDocument();
    expect(screen.getByText(/Max ₹30,000/i)).toBeInTheDocument();
    expect(screen.getByText(/256GB/i)).toBeInTheDocument();
    expect(screen.getByText(/camera Priority/i)).toBeInTheDocument();
  });

  it('renders NluIntentBadge with flight entities correctly', () => {
    const mockQueryUnderstanding = {
      intent: 'FLIGHT_SEARCH',
      confidence: 0.96,
      flightEntities: {
        origin: 'DEL',
        destination: 'BLR',
        departureDate: 'Friday',
        stops: 0,
        timePreference: 'EVENING',
      },
    };

    render(<NluIntentBadge queryUnderstanding={mockQueryUnderstanding} intent="FLIGHT_SEARCH" />);

    expect(screen.getByText(/Flight Routing Intent/i)).toBeInTheDocument();
    expect(screen.getByText(/DEL → BLR/i)).toBeInTheDocument();
    expect(screen.getByText(/Friday/i)).toBeInTheDocument();
    expect(screen.getByText(/Non-Stop Only/i)).toBeInTheDocument();
    expect(screen.getByText(/evening/i)).toBeInTheDocument();
  });

  it('renders NluClarificationModal and resolves missing fields', () => {
    const onResolveClarification = vi.fn();
    const onCancel = vi.fn();

    render(
      <NluClarificationModal
        clarificationPrompt="Where are you flying to Goa from? (e.g. Delhi, Mumbai)"
        missingFields={['origin']}
        query="Flights to Goa"
        onResolveClarification={onResolveClarification}
        onCancel={onCancel}
      />
    );

    expect(screen.getByTestId('nlu-clarification-card')).toBeInTheDocument();
    expect(screen.getByText(/Where are you flying to Goa from\?/i)).toBeInTheDocument();

    const input = screen.getByPlaceholderText(/e\.g\., Delhi, Mumbai, BLR/i);
    fireEvent.change(input, { target: { value: 'Delhi' } });

    const submitBtn = screen.getByRole('button', { name: /Find Real Deals/i });
    fireEvent.click(submitBtn);

    expect(onResolveClarification).toHaveBeenCalledWith('Flights to Goa Delhi', { origin: 'Delhi' });
  });

  it('UniversalSearchResults displays clarification card when requiresClarification is true', () => {
    const result = {
      intent: 'FLIGHT_SEARCH',
      query: 'Flights to Goa',
      requiresClarification: true,
      missingFields: ['origin'],
      clarificationPrompt: 'Where are you flying to Goa from? (e.g. Delhi, Mumbai)',
      executionTimeMs: 12,
    };

    render(
      <BrowserRouter>
        <UniversalSearchResults result={result} />
      </BrowserRouter>
    );

    expect(screen.getByTestId('nlu-clarification-card')).toBeInTheDocument();
    expect(screen.getByText(/Where are you flying to Goa from\?/i)).toBeInTheDocument();
  });
});
