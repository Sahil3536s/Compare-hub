import React from 'react';
import { render, screen, fireEvent } from '@testing-library/react';
import { describe, it, expect, vi } from 'vitest';
import CostTimeSlider from '../components/CostTimeSlider';
import { rankCostTimeClientSide } from '../services/costTimeOptimizationService';

describe('Phase 34: Cost vs Time Optimization Engine', () => {
  it('renders CostTimeSlider with dual-pole labels and preset buttons', () => {
    const handleChange = vi.fn();
    render(<CostTimeSlider costWeight={50} onChange={handleChange} />);

    expect(screen.getByTestId('cost-time-slider-container')).toBeInTheDocument();
    expect(screen.getByText(/50% Money/i)).toBeInTheDocument();
    expect(screen.getByText(/50% Time/i)).toBeInTheDocument();
    expect(screen.getByText(/Save Money/i)).toBeInTheDocument();
    expect(screen.getByText(/Save Time/i)).toBeInTheDocument();
    expect(screen.getByRole('button', { name: /💰 Max Money/i })).toBeInTheDocument();
    expect(screen.getByRole('button', { name: /⚡ Max Time/i })).toBeInTheDocument();
  });

  it('clicking a preset button updates cost weight', () => {
    const handleChange = vi.fn();
    render(<CostTimeSlider costWeight={50} onChange={handleChange} />);

    const maxMoneyBtn = screen.getByRole('button', { name: /💰 Max Money/i });
    fireEvent.click(maxMoneyBtn);
    expect(handleChange).toHaveBeenCalledWith(100);

    const maxTimeBtn = screen.getByRole('button', { name: /⚡ Max Time/i });
    fireEvent.click(maxTimeBtn);
    expect(handleChange).toHaveBeenCalledWith(0);
  });

  it('rankCostTimeClientSide ranks cheapest option #1 when 100% money', () => {
    const options = [
      { id: 'opt_a', name: 'Option A', price: 4200, durationMinutes: 480 }, // 8h
      { id: 'opt_b', name: 'Option B', price: 5000, durationMinutes: 300 }, // 5h
      { id: 'opt_c', name: 'Option C', price: 6100, durationMinutes: 180 }, // 3h
    ];

    const ranked100Money = rankCostTimeClientSide(options, 100, (o) => o.price, (o) => o.durationMinutes);
    expect(ranked100Money[0].id).toBe('opt_a');
    expect(ranked100Money[0]._costScore).toBe(100);

    const ranked100Time = rankCostTimeClientSide(options, 0, (o) => o.price, (o) => o.durationMinutes);
    expect(ranked100Time[0].id).toBe('opt_c');
    expect(ranked100Time[0]._timeScore).toBe(100);

    const rankedBalanced = rankCostTimeClientSide(options, 50, (o) => o.price, (o) => o.durationMinutes);
    expect(rankedBalanced[0].id).toBe('opt_b');
  });
});
