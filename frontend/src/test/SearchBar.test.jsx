import React from 'react';
import { render, screen, fireEvent } from '@testing-library/react';
import { describe, it, expect, vi } from 'vitest';
import SearchBar from '../components/SearchBar';

describe('SearchBar Component', () => {
  it('renders search input with placeholder', () => {
    render(<SearchBar onSearch={() => {}} placeholder="Search for products..." />);
    const input = screen.getByPlaceholderText('Search for products...');
    expect(input).toBeInTheDocument();
  });

  it('updates input value on change', () => {
    render(<SearchBar onSearch={() => {}} placeholder="Search..." />);
    const input = screen.getByPlaceholderText('Search...');
    fireEvent.change(input, { target: { value: 'iPhone 15' } });
    expect(input.value).toBe('iPhone 15');
  });

  it('triggers onSearch callback on form submit', () => {
    const handleSearch = vi.fn();
    render(<SearchBar onSearch={handleSearch} placeholder="Search..." />);
    const input = screen.getByPlaceholderText('Search...');

    fireEvent.change(input, { target: { value: 'Sony Headphones' } });
    fireEvent.submit(input.closest('form'));

    expect(handleSearch).toHaveBeenCalledWith('Sony Headphones');
  });
});
