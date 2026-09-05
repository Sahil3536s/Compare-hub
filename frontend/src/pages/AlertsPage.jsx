import React, { useState } from 'react';
import { useAuth } from '../context/AuthContext';
import { MOCK_ALERTS } from '../utils/mockData';
import PriceBadge from '../components/PriceBadge';
import AuthModal from '../components/AuthModal';

export const AlertsPage = () => {
  const { isAuthenticated, user } = useAuth();
  const [alerts, setAlerts] = useState(MOCK_ALERTS);
  const [authModalOpen, setAuthModalOpen] = useState(false);

  const handleDelete = (id) => {
    setAlerts(alerts.filter((a) => a.id !== id));
  };

  if (!isAuthenticated) {
    return (
      <div className="max-w-4xl mx-auto px-4 py-16 text-center space-y-6">
        <div className="w-16 h-16 bg-indigo-50 text-indigo-600 rounded-3xl flex items-center justify-center mx-auto text-2xl shadow-xs">
          🔔
        </div>
        <div className="space-y-2 max-w-md mx-auto">
          <h2 className="text-2xl font-bold text-slate-900">Sign In to Manage Price Alerts</h2>
          <p className="text-sm text-slate-500">
            Get instant email and push alerts the moment product prices or flight fares drop to your budget.
          </p>
        </div>
        <button
          onClick={() => setAuthModalOpen(true)}
          className="inline-flex items-center gap-2 px-6 py-3 bg-indigo-600 hover:bg-indigo-700 text-white font-semibold text-sm rounded-xl shadow-md transition cursor-pointer"
        >
          <span>Sign In / Create Account</span>
          <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M14 5l7 7m0 0l-7 7m7-7H3" />
          </svg>
        </button>

        <AuthModal isOpen={authModalOpen} onClose={() => setAuthModalOpen(false)} />
      </div>
    );
  }

  return (
    <div className="max-w-5xl mx-auto px-4 sm:px-6 lg:px-8 py-8 space-y-8">
      <div>
        <div className="flex items-center gap-2 text-xs text-slate-500 mb-2">
          <span>Home</span>
          <span>/</span>
          <span className="text-indigo-600 font-medium">Price Alerts</span>
        </div>
        <h1 className="text-3xl sm:text-4xl font-extrabold text-slate-900 tracking-tight">
          Price Drop Alerts
        </h1>
        <p className="text-sm text-slate-500 mt-1">
          Active price monitors for <strong className="text-slate-800">{user?.name}</strong> ({user?.email}).
        </p>
      </div>

      <div className="bg-white rounded-2xl border border-slate-200 shadow-xs divide-y divide-slate-100">
        {alerts.map((alert) => (
          <div key={alert.id} className="p-5 flex flex-col sm:flex-row sm:items-center justify-between gap-4">
            <div className="space-y-1">
              <div className="flex items-center gap-2 flex-wrap">
                <span className="text-xs font-bold text-indigo-600 uppercase">
                  {alert.type === 'product' ? '🛍️ Product Alert' : '✈️ Flight Alert'}
                </span>
                <PriceBadge
                  type={alert.status === 'TRIGGERED' ? 'cheapest' : 'neutral'}
                  text={alert.status === 'TRIGGERED' ? 'Price Goal Reached!' : 'Tracking Live'}
                />
              </div>
              <h3 className="font-bold text-base text-slate-900">{alert.title}</h3>
              <div className="flex items-center gap-4 text-xs text-slate-500">
                <span>Target: <strong className="text-slate-900 font-mono">₹{alert.targetPrice.toLocaleString('en-IN')}</strong></span>
                <span>•</span>
                <span>Current: <strong className="text-emerald-600 font-mono">₹{alert.currentPrice.toLocaleString('en-IN')}</strong></span>
                <span>•</span>
                <span>Channel: {alert.channel}</span>
              </div>
            </div>

            <div className="flex items-center gap-3">
              <button
                onClick={() => handleDelete(alert.id)}
                className="px-3 py-1.5 text-xs font-semibold text-rose-600 hover:bg-rose-50 rounded-lg transition cursor-pointer"
              >
                Delete Alert
              </button>
            </div>
          </div>
        ))}
      </div>
    </div>
  );
};

export default AlertsPage;
