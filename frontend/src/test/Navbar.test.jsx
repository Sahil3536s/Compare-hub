import React from 'react';
import { render, screen, fireEvent } from '@testing-library/react';
import { describe, it, expect, vi } from 'vitest';
import { BrowserRouter } from 'react-router-dom';
import Navbar from '../components/Navbar';
import { AuthProvider } from '../context/AuthContext';

describe('Premium Navbar Component', () => {
  const renderNavbar = () => {
    return render(
      <BrowserRouter>
        <AuthProvider>
          <Navbar />
        </AuthProvider>
      </BrowserRouter>
    );
  };

  it('renders CompareHub brand logo and title on the left', () => {
    renderNavbar();
    const brand = screen.getByLabelText(/CompareHub Home/i);
    expect(brand).toBeInTheDocument();
    expect(screen.getByText('CompareHub')).toBeInTheDocument();
  });

  it('renders 5 center navigation links: Deals, Compare, Price Tracker, Flights, Rides', () => {
    renderNavbar();
    expect(screen.getByRole('link', { name: 'Deals' })).toBeInTheDocument();
    expect(screen.getByRole('link', { name: 'Compare' })).toBeInTheDocument();
    expect(screen.getByRole('link', { name: 'Price Tracker' })).toBeInTheDocument();
    expect(screen.getByRole('link', { name: 'Flights' })).toBeInTheDocument();
    expect(screen.getByRole('link', { name: 'Rides' })).toBeInTheDocument();
  });

  it('renders right action elements: search trigger, saved/wishlist, and auth buttons', () => {
    renderNavbar();
    const searchBtns = screen.getAllByLabelText(/Search/i);
    expect(searchBtns.length).toBeGreaterThan(0);

    const savedLinks = screen.getAllByLabelText(/Saved/i);
    expect(savedLinks.length).toBeGreaterThan(0);

    expect(screen.getByRole('button', { name: /Sign In/i })).toBeInTheDocument();
    expect(screen.getByRole('button', { name: /Get Started/i })).toBeInTheDocument();
  });

  it('toggles mobile menu drawer on mobile hamburger button click', () => {
    renderNavbar();
    const hamburger = screen.getByLabelText('Toggle navigation menu');
    expect(hamburger).toBeInTheDocument();
    expect(hamburger).toHaveAttribute('aria-expanded', 'false');

    // Click to open
    fireEvent.click(hamburger);
    expect(hamburger).toHaveAttribute('aria-expanded', 'true');
    expect(screen.getByLabelText('Mobile navigation drawer')).toBeInTheDocument();

    // Click to close
    fireEvent.click(hamburger);
    expect(hamburger).toHaveAttribute('aria-expanded', 'false');
    expect(screen.queryByLabelText('Mobile navigation drawer')).not.toBeInTheDocument();
  });
});
