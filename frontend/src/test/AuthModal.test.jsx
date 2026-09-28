import React from 'react';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import { describe, it, expect, vi, beforeEach } from 'vitest';
import { AuthModal } from '../components/AuthModal';
import * as AuthContextModule from '../context/AuthContext';

describe('AuthModal Component', () => {
  beforeEach(() => {
    vi.spyOn(AuthContextModule, 'useAuth').mockReturnValue({
      login: vi.fn(),
      register: vi.fn(),
    });
  });

  it('does not render when isOpen is false', () => {
    const { container } = render(<AuthModal isOpen={false} onClose={() => {}} />);
    expect(container.firstChild).toBeNull();
  });

  it('renders login form with email and password inputs', () => {
    render(<AuthModal isOpen={true} onClose={() => {}} />);

    expect(screen.getByPlaceholderText('alex@example.com')).toBeInTheDocument();
    expect(screen.getByPlaceholderText('••••••••')).toBeInTheDocument();
    expect(screen.getByRole('button', { name: /Sign In to CompareHub/i })).toBeInTheDocument();
  });

  it('submits credentials and calls login function', async () => {
    const mockLogin = vi.fn().mockResolvedValue({ token: 'mock-jwt-token' });
    const mockClose = vi.fn();

    vi.spyOn(AuthContextModule, 'useAuth').mockReturnValue({
      login: mockLogin,
      register: vi.fn(),
    });

    render(<AuthModal isOpen={true} onClose={mockClose} />);

    const emailInput = screen.getByPlaceholderText('alex@example.com');
    const passwordInput = screen.getByPlaceholderText('••••••••');
    const submitBtn = screen.getByRole('button', { name: /Sign In to CompareHub/i });

    fireEvent.change(emailInput, { target: { value: 'user@example.com' } });
    fireEvent.change(passwordInput, { target: { value: 'Secret123!' } });
    fireEvent.click(submitBtn);

    await waitFor(() => {
      expect(mockLogin).toHaveBeenCalledWith('user@example.com', 'Secret123!');
      expect(mockClose).toHaveBeenCalledTimes(1);
    });
  });

  it('displays error message when authentication fails', async () => {
    const mockLogin = vi.fn().mockRejectedValue(new Error('Invalid email or password'));

    vi.spyOn(AuthContextModule, 'useAuth').mockReturnValue({
      login: mockLogin,
      register: vi.fn(),
    });

    render(<AuthModal isOpen={true} onClose={() => {}} />);

    const emailInput = screen.getByPlaceholderText('alex@example.com');
    const passwordInput = screen.getByPlaceholderText('••••••••');
    const submitBtn = screen.getByRole('button', { name: /Sign In to CompareHub/i });

    fireEvent.change(emailInput, { target: { value: 'user@example.com' } });
    fireEvent.change(passwordInput, { target: { value: 'wrongpassword' } });
    fireEvent.click(submitBtn);

    await waitFor(() => {
      expect(screen.getByText('Invalid email or password')).toBeInTheDocument();
    });
  });
});
