import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import React from 'react';
import { DecisionEnginePage } from '../pages/DecisionEnginePage';
import decisionEngineService from '../services/decisionEngineService';

vi.mock('../services/decisionEngineService', () => ({
  default: {
    evaluateDecision: vi.fn(),
    getSampleDecision: vi.fn()
  }
}));

describe('DecisionEnginePage Component', () => {
  const mockProductResponse = {
    decisionType: 'PRODUCT',
    query: 'iPhone 15',
    estimatedSavings: 1500,
    pipelineAudit: [
      'NLU_INTENT_PARSING',
      'MULTI_MERCHANT_PROVIDER_SEARCH',
      'OFFER_NORMALIZATION',
      'AI_DECISION_ADVISOR_2_0'
    ],
    decisionAdvisor: {
      recommendedOption: 'Amazon (₹64,999)',
      summary: 'Amazon costs ₹700 more than Croma, but has faster delivery and higher rating.',
      reasons: ['Amazon has 4.6★ customer rating.', 'Next-day delivery.'],
      tradeoffs: ['Paying ₹700 extra on Amazon for faster fulfillment.'],
      confidence: 'HIGH',
      contextType: 'PRODUCT'
    },
    insights: ['Lowest market price: ₹64,299 via Croma.', 'Purchase Timing: GOOD_TIME_TO_BUY.']
  };

  beforeEach(() => {
    vi.clearAllMocks();
    decisionEngineService.evaluateDecision.mockResolvedValue(mockProductResponse);
    decisionEngineService.getSampleDecision.mockResolvedValue(mockProductResponse);
  });

  it('renders Decision Engine Command Center header and controls', async () => {
    render(<DecisionEnginePage />);

    expect(screen.getByText(/CompareHub Decision Engine/i)).toBeInTheDocument();
    expect(screen.getByText(/Universal Orchestration Engine/i)).toBeInTheDocument();
    expect(screen.getByPlaceholderText(/Ask anything across products, flights, rides/i)).toBeInTheDocument();
    
    await waitFor(() => {
      expect(screen.getByRole('button', { name: /Run Engine/i })).toBeInTheDocument();
    });
  });

  it('renders quick scenario presets and changes query', async () => {
    render(<DecisionEnginePage />);

    const budgetPreset = screen.getByRole('button', { name: /🎯 Delhi to Goa Budget/i });
    expect(budgetPreset).toBeInTheDocument();

    fireEvent.click(budgetPreset);

    await waitFor(() => {
      expect(decisionEngineService.evaluateDecision).toHaveBeenCalledWith(
        expect.objectContaining({
          decisionType: 'BUDGET',
          query: expect.stringContaining('Delhi to Goa')
        })
      );
    });
  });

  it('renders active pipeline execution audit stages', async () => {
    render(<DecisionEnginePage />);

    await waitFor(() => {
      expect(screen.getByText(/Active Pipeline Execution Stages/i)).toBeInTheDocument();
      expect(screen.getByText(/NLU INTENT PARSING/i)).toBeInTheDocument();
      expect(screen.getByText(/AI DECISION ADVISOR 2 0/i)).toBeInTheDocument();
    });
  });

  it('renders winning decision metrics and AI Decision Advisor', async () => {
    render(<DecisionEnginePage />);

    await waitFor(() => {
      expect(screen.getByText(/Estimated Savings/i)).toBeInTheDocument();
      expect(screen.getByText(/₹1,500/i)).toBeInTheDocument();
      expect(screen.getByText(/Amazon costs ₹700 more than Croma/i)).toBeInTheDocument();
      expect(screen.getByText(/Zero hallucination guarantee/i)).toBeInTheDocument();
    });
  });
});
