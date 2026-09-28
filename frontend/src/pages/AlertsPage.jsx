import React, { useState, useEffect } from 'react';
import { Link } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import { getPriceAlerts, deletePriceAlert, togglePriceAlertStatus } from '../services/alertService';
import PriceBadge from '../components/PriceBadge';
import AuthModal from '../components/AuthModal';

export const AlertsPage = () => {
  const { isAuthenticated, user } = useAuth();
  const [alerts, setAlerts] = useState([]);
  const [loading, setLoading] = useState(true);
  const [authModalOpen, setAuthModalOpen] = useState(false);
  const [actionError, setActionError] = useState(null);

  useEffect(() => {
    if (!isAuthenticated) {
      setLoading(false);
      return;
    }

    const fetchAlerts = async () => {
      setLoading(true);
      try {
        const data = await getPriceAlerts();
        setAlerts(Array.isArray(data) ? data : []);
      } catch (err) {
        console.error('Failed to load user price alerts:', err);
      } finally {
        setLoading(false);
      }
    };

    fetchAlerts();
  }, [isAuthenticated]);

  const handleDelete = async (id) => {
    setActionError(null);
    try {
      await deletePriceAlert(id);
      setAlerts((prev) => prev.filter((a) => a.id !== id));
    } catch (err) {
      console.error('Failed to delete alert:', err);
      setActionError('Unable to delete alert. Please try again.');
    }
  };

  const handleToggleActive = async (id, currentActive) => {
    setActionError(null);
    try {
      await togglePriceAlertStatus(id, !currentActive);
      setAlerts((prev) =>
        prev.map((a) => (a.id === id ? { ...a, active: !currentActive } : a))
      );
    } catch (err) {
      console.error('Failed to toggle alert status:', err);
      setActionError('Unable to update alert status.');
    }
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

      {actionError && (
        <div className="p-4 bg-rose-50 border border-rose-200 text-rose-700 text-xs rounded-xl">
          {actionError}
        </div>
      )}

      {loading ? (
        <div className="bg-white rounded-2xl border border-slate-200 p-6 space-y-4 animate-pulse">
          {[1, 2, 3].map((i) => (
            <div key={i} className="h-16 bg-slate-100 rounded-xl"></div>
          ))}
        </div>
      ) : alerts.length === 0 ? (
        <div className="bg-white rounded-3xl border border-slate-200 p-12 text-center space-y-4 shadow-xs">
          <div className="w-16 h-16 bg-indigo-50 text-indigo-600 rounded-3xl flex items-center justify-center mx-auto text-2xl">
            🔔
          </div>
          <div className="space-y-1.5 max-w-sm mx-auto">
            <h3 className="text-lg font-bold text-slate-900">No Price Alerts Active</h3>
            <p className="text-xs text-slate-500 leading-relaxed">
              You haven&apos;t set any price monitors yet. Search for products and click &ldquo;Set Price Alert&rdquo; to track deals automatically.
            </p>
          </div>
          <div className="pt-2">
            <Link
              to="/shopping"
              className="inline-flex items-center gap-2 px-5 py-2.5 bg-indigo-600 hover:bg-indigo-700 text-white font-bold text-xs rounded-xl shadow-xs transition"
            >
              <span>Explore Products to Track</span>
              <span>→</span>
            </Link>
          </div>
        </div>
      ) : (
        <div className="bg-white rounded-2xl border border-slate-200 shadow-xs divide-y divide-slate-100">
          {alerts.map((alert) => (
            <div key={alert.id} className="p-5 flex flex-col sm:flex-row sm:items-center justify-between gap-4">
              <div className="space-y-1">
                <div className="flex items-center gap-2 flex-wrap">
                  <span className="text-xs font-bold text-indigo-600 uppercase">
                    🛍️ Product Alert
                  </span>
                  <PriceBadge
                    type={alert.active ? 'cheapest' : 'neutral'}
                    text={alert.active ? 'Tracking Active' : 'Paused'}
                  />
                </div>
                <h3 className="font-bold text-base text-slate-900">
                  {alert.productName || alert.title || `Product #${alert.productId}`}
                </h3>
                <div className="flex items-center gap-4 text-xs text-slate-500">
                  <span>Target: <strong className="text-slate-900 font-mono">₹{Number(alert.targetPrice || 0).toLocaleString('en-IN')}</strong></span>
                  {alert.currentPrice && (
                    <>
                      <span>•</span>
                      <span>Current: <strong className="text-emerald-600 font-mono">₹{Number(alert.currentPrice).toLocaleString('en-IN')}</strong></span>
                    </>
                  )}
                  {alert.createdAt && (
                    <>
                      <span>•</span>
                      <span>Created: {new Date(alert.createdAt).toLocaleDateString()}</span>
                    </>
                  )}
                </div>
              </div>

              <div className="flex items-center gap-2 self-start sm:self-auto">
                <button
                  type="button"
                  onClick={() => handleToggleActive(alert.id, alert.active)}
                  className="px-3 py-1.5 text-xs font-semibold text-slate-600 hover:bg-slate-100 rounded-lg transition cursor-pointer"
                >
                  {alert.active ? 'Pause' : 'Resume'}
                </button>
                <button
                  type="button"
                  onClick={() => handleDelete(alert.id)}
                  className="px-3 py-1.5 text-xs font-semibold text-rose-600 hover:bg-rose-50 rounded-lg transition cursor-pointer"
                >
                  Delete Alert
                </button>
              </div>
            </div>
          ))}
        </div>
      )}
    </div>
  );
};

export default AlertsPage;
