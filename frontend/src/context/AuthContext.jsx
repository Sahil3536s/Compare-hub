import React, { createContext, useContext, useState, useEffect, useCallback } from 'react';
import authService from '../services/authService';

const AuthContext = createContext(null);

export const AuthProvider = ({ children }) => {
  const [token, setToken] = useState(() => localStorage.getItem('comparehub_auth_token') || null);
  const [user, setUser] = useState(() => {
    const cachedUser = localStorage.getItem('comparehub_auth_user');
    return cachedUser ? JSON.parse(cachedUser) : null;
  });
  const [loading, setLoading] = useState(true);

  // Initialize and verify user on mount
  useEffect(() => {
    const initializeAuth = async () => {
      const savedToken = localStorage.getItem('comparehub_auth_token');
      if (savedToken) {
        try {
          const userData = await authService.getMe();
          setUser(userData);
          localStorage.setItem('comparehub_auth_user', JSON.stringify(userData));
        } catch (error) {
          console.warn('Session expired or invalid token:', error.message);
          logout();
        }
      }
      setLoading(false);
    };

    initializeAuth();

    // Listen for custom logout events dispatched by Axios interceptor
    const handleGlobalLogout = () => {
      setToken(null);
      setUser(null);
    };
    window.addEventListener('comparehub:auth:logout', handleGlobalLogout);

    return () => {
      window.removeEventListener('comparehub:auth:logout', handleGlobalLogout);
    };
  }, []);

  const login = useCallback(async (email, password) => {
    const data = await authService.login(email, password);
    setToken(data.token);
    setUser(data.user);
    localStorage.setItem('comparehub_auth_token', data.token);
    localStorage.setItem('comparehub_auth_user', JSON.stringify(data.user));
    return data;
  }, []);

  const register = useCallback(async (name, email, password) => {
    const data = await authService.register(name, email, password);
    setToken(data.token);
    setUser(data.user);
    localStorage.setItem('comparehub_auth_token', data.token);
    localStorage.setItem('comparehub_auth_user', JSON.stringify(data.user));
    return data;
  }, []);

  const logout = useCallback(() => {
    setToken(null);
    setUser(null);
    localStorage.removeItem('comparehub_auth_token');
    localStorage.removeItem('comparehub_auth_user');
  }, []);

  const value = {
    token,
    user,
    isAuthenticated: !!token && !!user,
    loading,
    login,
    register,
    logout,
  };

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
};

export const useAuth = () => {
  const context = useContext(AuthContext);
  if (!context) {
    throw new Error('useAuth must be used within an AuthProvider');
  }
  return context;
};

export default AuthContext;
