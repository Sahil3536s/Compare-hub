import React from 'react';
import { render, screen, waitFor } from '@testing-library/react';
import { describe, it, expect, vi, beforeEach } from 'vitest';

// ─── Mock predictionService ──────────────────────────────────────────────────
vi.mock('../services/predictionService', () => ({
  getProductPrediction: vi.fn(),
}));

// ─── Mock productService ─────────────────────────────────────────────────────
vi.mock('../services/productService', () => ({
  getProductPriceHistory: vi.fn(),
}));

// ─── Import after mocking ────────────────────────────────────────────────────
import { getProductPrediction } from '../services/predictionService';
import { getProductPriceHistory } from '../services/productService';
import PriceHistoryModal from '../components/PriceHistoryModal';

// ─── Shared helpers ──────────────────────────────────────────────────────────

const mockHistoryData = {
  productId: 1,
  productName: 'iPhone 15 Pro',
  period: '30D',
  currentPrice: 58999,
  lowestPrice: 54999,
  highestPrice: 62999,
  averagePrice: 58000,
  currency: 'INR',
  analysisText: 'Current price is stable.',
  pricePoints: [
    { date: '2024-01-01', price: 60000, merchant: 'Amazon', recordedAt: '2024-01-01T00:00:00Z' },
    { date: '2024-01-15', price: 58999, merchant: 'Amazon', recordedAt: '2024-01-15T00:00:00Z' },
  ],
  dealQuality: null,
  purchaseTiming: null,
};

const mockProduct = { id: 1, productName: 'iPhone 15 Pro', name: 'iPhone 15 Pro' };

const renderModal = (overrides = {}) =>
  render(
    <PriceHistoryModal
      isOpen={true}
      onClose={vi.fn()}
      product={mockProduct}
      {...overrides}
    />
  );

// ─── Tests ───────────────────────────────────────────────────────────────────

describe('ML Price Prediction — Frontend', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    getProductPriceHistory.mockResolvedValue(mockHistoryData);
  });

  // ── Loading state ──
  it('shows ML loading spinner while fetching prediction', async () => {
    // Keep prediction pending
    getProductPrediction.mockReturnValue(new Promise(() => {}));

    renderModal();

    await waitFor(() => {
      expect(screen.getByText(/ML Price Prediction/i)).toBeInTheDocument();
    });
    // Spinner should be visible (via animate-spin class or "Analysing" text)
    await waitFor(() => {
      expect(screen.getByText(/Analysing price patterns/i)).toBeInTheDocument();
    });
  });

  // ── INSUFFICIENT_DATA ──
  it('shows insufficient data message when status is INSUFFICIENT_DATA', async () => {
    getProductPrediction.mockResolvedValue({
      status: 'INSUFFICIENT_DATA',
      productId: 1,
      dataPointsUsed: 5,
      message: 'More price observations are needed before a reliable ML prediction can be generated.',
    });

    renderModal();

    await waitFor(() => {
      expect(screen.getByText(/Not enough price history yet/i)).toBeInTheDocument();
    });
    expect(screen.getByText(/More price observations/i)).toBeInTheDocument();
    // Must NOT show any predicted price
    expect(screen.queryByText(/Est\. Price in 7d/i)).not.toBeInTheDocument();
  });

  // ── ML_UNAVAILABLE ──
  it('shows unavailable message when status is ML_UNAVAILABLE', async () => {
    getProductPrediction.mockResolvedValue({
      status: 'ML_UNAVAILABLE',
      message: 'Price prediction service is temporarily unavailable.',
    });

    renderModal();

    await waitFor(() => {
      expect(screen.getByText('Prediction temporarily unavailable')).toBeInTheDocument();
    });
    expect(screen.getByText('Price prediction service is temporarily unavailable.')).toBeInTheDocument();
  });

  // ── Network error ──
  it('shows error message when prediction fetch fails', async () => {
    getProductPrediction.mockRejectedValue(new Error('Network Error'));

    renderModal();

    await waitFor(() => {
      expect(screen.getByText(/Could not reach the prediction service/i)).toBeInTheDocument();
    });
  });

  // ── SUCCESS — WAIT ──
  it('renders WAIT recommendation with correct styling', async () => {
    getProductPrediction.mockResolvedValue({
      status: 'SUCCESS',
      productId: 1,
      currentPrice: 58999,
      predictedPrice7d: 56500,
      predictedChange: -2499,
      predictedChangePercent: -4.24,
      recommendation: 'WAIT',
      recommendationReason: 'Model estimates the price may decrease by 4.2% over the next 7 days.',
      confidenceLabel: 'Medium',
      predictedPriceLow: 54900,
      predictedPriceHigh: 58300,
      dealQuality: 'GOOD_DEAL',
      modelName: 'RandomForestRegressor',
      modelVersion: '1.0',
      dataPointsUsed: 30,
    });

    renderModal();

    await waitFor(() => {
      expect(screen.getByText(/⏳ WAIT/i)).toBeInTheDocument();
    });
    expect(screen.getByText(/Est\. Price in 7d/i)).toBeInTheDocument();
    expect(screen.getByText(/🏷️ Good Deal/i)).toBeInTheDocument();
    expect(screen.getByText(/RandomForestRegressor/i)).toBeInTheDocument();
    expect(screen.getByText(/Medium Confidence/i)).toBeInTheDocument();
    expect(screen.getByText(/price may decrease/i)).toBeInTheDocument();
  });

  // ── SUCCESS — BUY_NOW ──
  it('renders BUY NOW recommendation', async () => {
    getProductPrediction.mockResolvedValue({
      status: 'SUCCESS',
      productId: 1,
      currentPrice: 55000,
      predictedPrice7d: 58000,
      predictedChange: 3000,
      predictedChangePercent: 5.45,
      recommendation: 'BUY_NOW',
      recommendationReason: 'Model estimates the price may increase by 5.5% over the next 7 days.',
      confidenceLabel: 'High',
      predictedPriceLow: 57000,
      predictedPriceHigh: 59500,
      dealQuality: 'NORMAL_PRICE',
      modelName: 'RandomForestRegressor',
      modelVersion: '1.0',
      dataPointsUsed: 28,
    });

    renderModal();

    await waitFor(() => {
      expect(screen.getByText(/✅ BUY NOW/i)).toBeInTheDocument();
    });
    expect(screen.getByText(/📊 Normal Price/i)).toBeInTheDocument();
    expect(screen.getByText(/High Confidence/i)).toBeInTheDocument();
    expect(screen.getByText(/price may increase/i)).toBeInTheDocument();
  });

  // ── SUCCESS — HOLD ──
  it('renders HOLD recommendation', async () => {
    getProductPrediction.mockResolvedValue({
      status: 'SUCCESS',
      productId: 1,
      currentPrice: 58000,
      predictedPrice7d: 58100,
      predictedChange: 100,
      predictedChangePercent: 0.17,
      recommendation: 'HOLD',
      recommendationReason: 'The estimated price is expected to remain relatively stable.',
      confidenceLabel: 'High',
      predictedPriceLow: 57500,
      predictedPriceHigh: 58700,
      dealQuality: 'EXPENSIVE',
      modelName: 'LinearRegression',
      modelVersion: '1.0',
      dataPointsUsed: 22,
    });

    renderModal();

    await waitFor(() => {
      expect(screen.getByText(/🔄 HOLD/i)).toBeInTheDocument();
    });
    expect(screen.getByText(/⚠️ Expensive/i)).toBeInTheDocument();
    expect(screen.getByText(/remain relatively stable/i)).toBeInTheDocument();
  });

  // ── Disclaimer ──
  it('renders disclaimer about estimated prices', async () => {
    getProductPrediction.mockResolvedValue({
      status: 'SUCCESS',
      productId: 1,
      currentPrice: 58999,
      predictedPrice7d: 56500,
      predictedChange: -2499,
      predictedChangePercent: -4.24,
      recommendation: 'WAIT',
      confidenceLabel: 'Low',
      predictedPriceLow: 53000,
      predictedPriceHigh: 60000,
      dealQuality: 'GOOD_DEAL',
      modelName: 'RandomForestRegressor',
    });

    renderModal();

    await waitFor(() => {
      expect(screen.getByText(/estimates, not guaranteed future prices/i)).toBeInTheDocument();
    });
  });

  // ── Modal closed ──
  it('does not render when isOpen is false', () => {
    renderModal({ isOpen: false });
    expect(screen.queryByText(/Price Trend Analysis/i)).not.toBeInTheDocument();
  });

  // ── MODEL_NOT_LOADED ──
  it('shows unavailable message when model is not trained', async () => {
    getProductPrediction.mockResolvedValue({
      status: 'MODEL_NOT_LOADED',
      message: 'ML model not yet trained. Run training/train.py to train the model.',
    });

    renderModal();

    await waitFor(() => {
      expect(screen.getByText(/Prediction temporarily unavailable/i)).toBeInTheDocument();
    });
  });
});
