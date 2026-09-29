import React from 'react';
import { vi } from 'vitest';

// Mock the service
vi.mock('../services/locationService', () => ({
  suggestPlaces: vi.fn(),
}));

import { suggestPlaces } from '../services/locationService';

describe('LocationAutocomplete component', () => {
  beforeEach(() => {
    vi.useFakeTimers();
    suggestPlaces.mockReset();
  });

  afterEach(() => {
    vi.runOnlyPendingTimers();
    vi.useRealTimers();
  });

  test('debounces input and shows suggestions', async () => {
    const mockResults = [
      { placeId: '1', name: 'Delhi Airport', formattedAddress: 'Indira Gandhi Intl, Delhi', latitude: 28.56, longitude: 77.1, city: 'Delhi' },
      { placeId: '2', name: 'New Delhi Station', formattedAddress: 'New Delhi, India', latitude: 28.61, longitude: 77.23, city: 'Delhi' },
    ];
    suggestPlaces.mockResolvedValueOnce(mockResults);

    const handleSelect = jest.fn();
    render(<LocationAutocomplete id="test-autocomplete" onSelect={handleSelect} placeholder="Search" />);

    const input = screen.getByPlaceholderText('Search');
    fireEvent.change(input, { target: { value: 'del' } });

    // Fast-forward debounce timer (300ms)
    jest.advanceTimersByTime(300);

    // Wait for async suggestions to render
    await waitFor(() => expect(suggestPlaces).toHaveBeenCalledWith('del'));
    await waitFor(() => expect(screen.getByRole('listbox')).toBeInTheDocument());
    const firstOption = screen.getByText('Delhi Airport');
    expect(firstOption).toBeInTheDocument();

    // Click first suggestion
    fireEvent.click(firstOption);
    expect(handleSelect).toHaveBeenCalledWith(
      expect.objectContaining({
        name: 'Delhi Airport',
        formattedAddress: expect.any(String),
        latitude: 28.56,
        longitude: 77.1,
        city: 'Delhi',
        providerPlaceId: expect.any(String),
      })
    );
  });

  test('shows no‑results message when backend returns empty array', async () => {
    suggestPlaces.mockResolvedValueOnce([]);
    render(<LocationAutocomplete id="test-autocomplete" placeholder="Search" />);
    const input = screen.getByPlaceholderText('Search');
    fireEvent.change(input, { target: { value: 'xyz' } });
    jest.advanceTimersByTime(300);
    await waitFor(() => expect(suggestPlaces).toHaveBeenCalledWith('xyz'));
    await waitFor(() => expect(screen.getByTestId('no-matching-location')).toBeInTheDocument());
  });

  test('displays error UI when service throws', async () => {
    suggestPlaces.mockRejectedValueOnce(new Error('Network error'));
    render(<LocationAutocomplete id="test-autocomplete" placeholder="Search" />);
    const input = screen.getByPlaceholderText('Search');
    fireEvent.change(input, { target: { value: 'del' } });
    jest.advanceTimersByTime(300);
    await waitFor(() => expect(suggestPlaces).toHaveBeenCalledWith('del'));
    await waitFor(() => expect(screen.getByTestId('location-search-error')).toBeInTheDocument());
    expect(screen.getByTestId('location-search-error')).toHaveTextContent('Unable to search locations');
  });
});
