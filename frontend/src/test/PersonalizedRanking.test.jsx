import React from 'react';
import { render, screen, fireEvent } from '@testing-library/react';
import { describe, it, expect, vi } from 'vitest';
import PersonalizedRankingToolbar from '../components/PersonalizedRankingToolbar';
import { AuthProvider } from '../context/AuthContext';

describe('Personalized Ranking Toolbar Frontend', () => {
  it('renders preset options: Balanced, Save Money, Fastest Delivery, Best Rated', () => {
    render(
      <AuthProvider>
        <PersonalizedRankingToolbar onPreferenceChange={vi.fn()} />
      </AuthProvider>
    );

    expect(screen.getByText(/What matters most to you\?/i)).toBeInTheDocument();
    expect(screen.getByText(/Balanced/i)).toBeInTheDocument();
    expect(screen.getByText(/Save Money/i)).toBeInTheDocument();
    expect(screen.getByText(/Fastest Delivery/i)).toBeInTheDocument();
    expect(screen.getByText(/Best Rated/i)).toBeInTheDocument();
  });

  it('clicking Save Money preset triggers callback with price 70%', () => {
    const handlePrefChange = vi.fn();
    render(
      <AuthProvider>
        <PersonalizedRankingToolbar onPreferenceChange={handlePrefChange} />
      </AuthProvider>
    );

    const saveMoneyBtn = screen.getByRole('button', { name: /Save Money/i });
    fireEvent.click(saveMoneyBtn);

    expect(handlePrefChange).toHaveBeenCalledWith({
      preset: 'CHEAPEST',
      weights: { price: 70, rating: 10, discount: 0, delivery: 10, reliability: 10 },
    });
  });

  it('toggling Tune Sliders opens custom sliders drawer', () => {
    render(
      <AuthProvider>
        <PersonalizedRankingToolbar onPreferenceChange={vi.fn()} />
      </AuthProvider>
    );

    const tuneBtn = screen.getByRole('button', { name: /Tune Sliders/i });
    fireEvent.click(tuneBtn);

    expect(screen.getByTestId('custom-sliders-drawer')).toBeInTheDocument();
    expect(screen.getByText(/Price Weight/i)).toBeInTheDocument();
    expect(screen.getByText(/Rating Weight/i)).toBeInTheDocument();
    expect(screen.getByText(/Delivery Speed/i)).toBeInTheDocument();
  });
});
