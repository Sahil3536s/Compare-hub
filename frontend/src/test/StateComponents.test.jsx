import React from 'react';
import { render, screen, fireEvent } from '@testing-library/react';
import { describe, it, expect, vi } from 'vitest';
import LoadingSkeleton from '../components/LoadingSkeleton';
import EmptyState from '../components/EmptyState';
import ErrorState from '../components/ErrorState';

describe('UI State Components', () => {
  it('renders LoadingSkeleton without crashing', () => {
    const { container } = render(<LoadingSkeleton type="product-card" count={3} />);
    expect(container.querySelectorAll('.animate-pulse').length).toBeGreaterThan(0);
  });

  it('renders EmptyState with custom title and action button', () => {
    const handleAction = vi.fn();
    render(
      <EmptyState
        title="No Products Found"
        description="Try searching with a different keyword."
        actionLabel="Clear Search"
        onAction={handleAction}
      />
    );

    expect(screen.getByText('No Products Found')).toBeInTheDocument();
    expect(screen.getByText('Try searching with a different keyword.')).toBeInTheDocument();

    const button = screen.getByText('Clear Search');
    fireEvent.click(button);
    expect(handleAction).toHaveBeenCalledTimes(1);
  });

  it('renders ErrorState with user-friendly message and retry button', () => {
    const handleRetry = vi.fn();
    render(
      <ErrorState
        title="Connection Error"
        message="Unable to reach comparison providers. Please retry."
        onRetry={handleRetry}
      />
    );

    expect(screen.getByText('Connection Error')).toBeInTheDocument();
    expect(screen.getByText('Unable to reach comparison providers. Please retry.')).toBeInTheDocument();

    const retryBtn = screen.getByText(/Try Again/i);
    fireEvent.click(retryBtn);
    expect(handleRetry).toHaveBeenCalledTimes(1);
  });
});
