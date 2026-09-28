import React, { useState, useEffect, useCallback } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import { getDashboardData } from '../services/dashboardService';
import { removeSavedProduct } from '../services/savedService';
import { deletePriceAlert, togglePriceAlertStatus } from '../services/alertService';
import SavedProductCard from '../components/saved/SavedProductCard';
import PriceAlertModal from '../components/PriceAlertModal';
import LoadingSkeleton from '../components/LoadingSkeleton';
import EmptyState from '../components/EmptyState';
import ErrorState from '../components/ErrorState';
import AuthModal from '../components/AuthModal';
import { ROUTES } from '../utils/constants';

export const DashboardPage = () => {
  const { isAuthenticated, user } = useAuth();
  const navigate = useNavigate();

  const [dashboard, setDashboard] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  // Active section tab for navigation
  const [activeTab, setActiveTab] = useState('overview'); // 'overview', 'drops', 'saved', 'alerts', 'activity'

  // Modal states
  const [authModalOpen, setAuthModalOpen] = useState(false);
  const [alertModalProduct, setAlertModalProduct] = useState(null);
  const [alertModalOpen, setAlertModalOpen] = useState(false);

  const fetchDashboard = useCallback(async () => {
    if (!isAuthenticated) {
      setLoading(false);
      return;
    }
    setLoading(true);
    setError(null);
    try {
      const data = await getDashboardData();
      setDashboard(data);
    } catch (err) {
      console.error('Failed to load user dashboard:', err);
      setError(err?.response?.data?.message || err.message || 'Unable to load your dashboard.');
    } finally {
      setLoading(false);
    }
  }, [isAuthenticated]);

  useEffect(() => {
    fetchDashboard();
  }, [fetchDashboard]);

  // Remove saved product handler
  const handleRemoveSaved = async (savedProductId) => {
    try {
      await removeSavedProduct(savedProductId);
      setDashboard((prev) => {
        if (!prev) return prev;
        const newSaved = prev.savedProducts.filter((p) => p.id !== savedProductId);
        const newDrops = prev.recentPriceDrops.filter((p) => p.id !== savedProductId);
        return {
          ...prev,
          summary: {
            ...prev.summary,
            savedProductsCount: Math.max(0, (prev.summary?.savedProductsCount || 1) - 1),
            priceDropsCount: newDrops.length,
          },
          savedProducts: newSaved,
          recentPriceDrops: newDrops,
        };
      });
    } catch (err) {
      console.error('Failed to remove saved product:', err);
      alert('Failed to remove product from saved list.');
    }
  };

  // Toggle alert active status
  const handleToggleAlert = async (alertId, currentActive) => {
    try {
      await togglePriceAlertStatus(alertId, !currentActive);
      setDashboard((prev) => {
        if (!prev) return prev;
        const updatedAlerts = prev.activeAlerts.map((a) =>
          a.id === alertId ? { ...a, active: !currentActive } : a
        );
        return {
          ...prev,
          activeAlerts: updatedAlerts,
        };
      });
    } catch (err) {
      console.error('Failed to toggle alert status:', err);
      alert('Unable to update alert status.');
    }
  };

  // Delete alert
  const handleDeleteAlert = async (alertId) => {
    try {
      await deletePriceAlert(alertId);
      setDashboard((prev) => {
        if (!prev) return prev;
        const remaining = prev.activeAlerts.filter((a) => a.id !== alertId);
        return {
          ...prev,
          summary: {
            ...prev.summary,
            activeAlertsCount: Math.max(0, (prev.summary?.activeAlertsCount || 1) - 1),
          },
          activeAlerts: remaining,
        };
      });
    } catch (err) {
      console.error('Failed to delete alert:', err);
      alert('Unable to delete alert.');
    }
  };

  // Open price alert modal for product
  const handleOpenAlertModal = (product) => {
    setAlertModalProduct(product);
    setAlertModalOpen(true);
  };

  // If user is not logged in, prompt sign in cleanly
  if (!isAuthenticated) {
    return (
      <div className="max-w-4xl mx-auto px-4 py-16 text-center space-y-6 animate-fadeIn" data-testid="dashboard-unauthenticated">
        <div className="w-16 h-16 bg-indigo-50 text-indigo-600 rounded-3xl flex items-center justify-center mx-auto text-2xl shadow-xs">
          🔐
        </div>
        <div className="space-y-2 max-w-md mx-auto">
          <h2 className="text-2xl sm:text-3xl font-extrabold text-slate-900 tracking-tight">
            Sign In to Access My CompareHub
          </h2>
          <p className="text-sm text-slate-500 leading-relaxed">
            Track real price drops, manage your personal saved watchlist, monitor live fare alerts, and revisit comparison history.
          </p>
        </div>
        <div className="pt-2">
          <button
            onClick={() => setAuthModalOpen(true)}
            className="inline-flex items-center gap-2 px-6 py-3 bg-indigo-600 hover:bg-indigo-700 text-white font-bold text-sm rounded-xl shadow-md transition cursor-pointer"
          >
            <span>Sign In / Create Account</span>
            <span>→</span>
          </button>
        </div>
        <AuthModal isOpen={authModalOpen} onClose={() => setAuthModalOpen(false)} />
      </div>
    );
  }

  if (loading) {
    return (
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
        <LoadingSkeleton type="dashboard" />
      </div>
    );
  }

  if (error) {
    return (
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-12">
        <ErrorState
          title="Dashboard Unavailable"
          message={error}
          onRetry={fetchDashboard}
        />
      </div>
    );
  }

  const {
    summary = {},
    recentPriceDrops = [],
    savedProducts = [],
    activeAlerts = [],
    recentActivity = [],
    recentComparisons = [],
  } = dashboard || {};

  return (
    <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8 space-y-8 animate-fadeIn" data-testid="user-dashboard">
      
      {/* 1. WELCOME HEADER */}
      <div className="bg-linear-to-r from-slate-900 via-indigo-950 to-slate-900 rounded-3xl p-6 sm:p-8 text-white shadow-xl relative overflow-hidden">
        <div className="absolute right-0 top-0 translate-x-8 -translate-y-8 w-64 h-64 bg-indigo-500/10 rounded-full blur-3xl pointer-events-none" />
        <div className="relative z-10 flex flex-col md:flex-row md:items-center justify-between gap-6">
          <div className="space-y-1.5">
            <div className="flex items-center gap-2 text-xs font-bold uppercase tracking-wider text-indigo-300">
              <span>MY COMPAREHUB</span>
              <span>•</span>
              <span className="text-emerald-400">Personal Hub</span>
            </div>
            <h1 className="text-2xl sm:text-4xl font-black tracking-tight">
              Welcome back, {summary.userName || user?.name || 'Shopper'} 👋
            </h1>
            <p className="text-xs sm:text-sm text-slate-300 max-w-2xl">
              Track live price variances, verified drops across Amazon, Flipkart & Croma, and revisit your multi-provider comparison telemetry.
            </p>
          </div>

          <div className="flex items-center gap-2.5 flex-wrap self-start md:self-auto">
            <Link
              to={ROUTES.SHOPPING}
              className="px-4 py-2.5 bg-indigo-600 hover:bg-indigo-500 text-white font-bold text-xs rounded-xl shadow-xs transition flex items-center gap-1.5"
            >
              <span>Explore Deals</span>
              <span>→</span>
            </Link>
            <Link
              to={ROUTES.SAVED}
              className="px-4 py-2.5 bg-white/10 hover:bg-white/20 text-white font-bold text-xs rounded-xl border border-white/15 transition flex items-center gap-1.5"
            >
              <span>Saved Items</span>
            </Link>
          </div>
        </div>
      </div>

      {/* 2. SUMMARY METRIC CARDS (REAL DATA ONLY) */}
      <div className="grid grid-cols-2 sm:grid-cols-3 lg:grid-cols-5 gap-3.5 sm:gap-4" data-testid="dashboard-summary-cards">
        
        {/* Saved Products */}
        <div className="bg-white rounded-2xl border border-slate-200 p-4 sm:p-5 shadow-2xs hover:shadow-xs transition">
          <div className="flex items-center justify-between">
            <span className="text-[11px] font-bold uppercase tracking-wider text-slate-500">Saved</span>
            <span className="text-base">❤️</span>
          </div>
          <div className="mt-2 flex items-baseline gap-1.5">
            <span className="text-2xl sm:text-3xl font-black text-slate-900 font-mono">
              {summary.savedProductsCount ?? 0}
            </span>
            <span className="text-xs text-slate-500">items</span>
          </div>
          <p className="text-[11px] text-slate-400 mt-1">In your watchlist</p>
        </div>

        {/* Active Alerts */}
        <div className="bg-white rounded-2xl border border-slate-200 p-4 sm:p-5 shadow-2xs hover:shadow-xs transition">
          <div className="flex items-center justify-between">
            <span className="text-[11px] font-bold uppercase tracking-wider text-slate-500">Active Alerts</span>
            <span className="text-base">🎯</span>
          </div>
          <div className="mt-2 flex items-baseline gap-1.5">
            <span className="text-2xl sm:text-3xl font-black text-slate-900 font-mono">
              {summary.activeAlertsCount ?? 0}
            </span>
            <span className="text-xs text-slate-500">monitors</span>
          </div>
          <p className="text-[11px] text-slate-400 mt-1">Price drop trackers</p>
        </div>

        {/* Recent Comparisons */}
        <div className="bg-white rounded-2xl border border-slate-200 p-4 sm:p-5 shadow-2xs hover:shadow-xs transition">
          <div className="flex items-center justify-between">
            <span className="text-[11px] font-bold uppercase tracking-wider text-slate-500">Comparisons</span>
            <span className="text-base">⇄</span>
          </div>
          <div className="mt-2 flex items-baseline gap-1.5">
            <span className="text-2xl sm:text-3xl font-black text-slate-900 font-mono">
              {summary.recentComparisonsCount ?? 0}
            </span>
            <span className="text-xs text-slate-500">searches</span>
          </div>
          <p className="text-[11px] text-slate-400 mt-1">Total search runs</p>
        </div>

        {/* Price Drops */}
        <div className="bg-white rounded-2xl border border-emerald-200 bg-emerald-50/20 p-4 sm:p-5 shadow-2xs hover:shadow-xs transition">
          <div className="flex items-center justify-between">
            <span className="text-[11px] font-bold uppercase tracking-wider text-emerald-800">Price Drops</span>
            <span className="text-base">🔥</span>
          </div>
          <div className="mt-2 flex items-baseline gap-1.5">
            <span className="text-2xl sm:text-3xl font-black text-emerald-700 font-mono">
              {summary.priceDropsCount ?? 0}
            </span>
            <span className="text-xs text-emerald-600 font-medium">deals</span>
          </div>
          <p className="text-[11px] text-emerald-700/80 mt-1">Verified lower prices</p>
        </div>

        {/* POTENTIAL SAVINGS (Strictly labeled 'Potential Savings') */}
        <div className="col-span-2 sm:col-span-1 bg-white rounded-2xl border border-indigo-200 bg-indigo-50/20 p-4 sm:p-5 shadow-2xs hover:shadow-xs transition">
          <div className="flex items-center justify-between">
            <span className="text-[11px] font-bold uppercase tracking-wider text-indigo-800">Potential Savings</span>
            <span className="text-base">💰</span>
          </div>
          <div className="mt-2 flex items-baseline gap-1">
            <span className="text-2xl sm:text-3xl font-black text-indigo-700 font-mono">
              ₹{Number(summary.potentialSavings || 0).toLocaleString('en-IN')}
            </span>
          </div>
          <p className="text-[10px] text-slate-500 mt-1 leading-tight">
            Variance on saved items
          </p>
        </div>
      </div>

      {/* 3. NAVIGATION PILLS FOR SUBSECTIONS */}
      <div className="flex items-center gap-2 overflow-x-auto pb-1 border-b border-slate-200/80">
        {[
          { id: 'overview', label: 'All Overview', icon: '📊' },
          { id: 'drops', label: `Price Drops (${summary.priceDropsCount ?? 0})`, icon: '🔥' },
          { id: 'saved', label: `Saved Products (${summary.savedProductsCount ?? 0})`, icon: '❤️' },
          { id: 'alerts', label: `Active Alerts (${summary.activeAlertsCount ?? 0})`, icon: '🎯' },
          { id: 'activity', label: 'Recent Activity', icon: '📜' },
        ].map((tab) => (
          <button
            key={tab.id}
            onClick={() => setActiveTab(tab.id)}
            className={`px-4 py-2 rounded-xl text-xs font-bold whitespace-nowrap transition cursor-pointer flex items-center gap-1.5 ${
              activeTab === tab.id
                ? 'bg-indigo-600 text-white shadow-2xs'
                : 'text-slate-600 hover:text-slate-900 hover:bg-slate-100'
            }`}
          >
            <span>{tab.icon}</span>
            <span>{tab.label}</span>
          </button>
        ))}
      </div>

      {/* 4. MAIN DASHBOARD CONTENT */}
      <div className="space-y-10">

        {/* SECTION A: RECENT PRICE DROPS */}
        {(activeTab === 'overview' || activeTab === 'drops') && (
          <section className="space-y-4" data-testid="section-price-drops">
            <div className="flex items-center justify-between gap-4">
              <div>
                <h2 className="text-xl sm:text-2xl font-black text-slate-900 tracking-tight flex items-center gap-2">
                  <span>🔥</span>
                  <span>Recent Price Drops</span>
                </h2>
                <p className="text-xs text-slate-500 mt-0.5">
                  Verified price drops on items you bookmarked where current store prices are lower than when you saved them.
                </p>
              </div>

              {recentPriceDrops.length > 0 && (
                <Link
                  to={ROUTES.SAVED}
                  className="text-xs font-bold text-indigo-600 hover:text-indigo-800 flex items-center gap-1 shrink-0"
                >
                  <span>View All Saved</span>
                  <span>→</span>
                </Link>
              )}
            </div>

            {recentPriceDrops.length > 0 ? (
              <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
                {recentPriceDrops.map((product) => (
                  <SavedProductCard
                    key={product.id}
                    product={product}
                    onRemove={handleRemoveSaved}
                    onSetAlert={handleOpenAlertModal}
                  />
                ))}
              </div>
            ) : (
              <div className="bg-white rounded-3xl border border-slate-200 p-8 text-center space-y-3 shadow-2xs">
                <span className="text-3xl">✨</span>
                <h3 className="font-bold text-sm text-slate-900">No price drops on your saved products yet</h3>
                <p className="text-xs text-slate-500 max-w-md mx-auto">
                  We verify prices across Amazon, Flipkart, and Croma continuously. As soon as a seller lowers their price on an item in your watchlist, it will appear here.
                </p>
                <div className="pt-1">
                  <Link
                    to={ROUTES.SHOPPING}
                    className="inline-flex items-center gap-1.5 px-4 py-2 bg-indigo-50 hover:bg-indigo-100 text-indigo-700 font-bold text-xs rounded-xl transition"
                  >
                    <span>Explore Products</span>
                    <span>→</span>
                  </Link>
                </div>
              </div>
            )}
          </section>
        )}

        {/* SECTION B: QUICK REOPEN RECENT COMPARISONS */}
        {(activeTab === 'overview' || activeTab === 'activity') && recentComparisons.length > 0 && (
          <section className="space-y-4" data-testid="section-recent-comparisons">
            <div className="flex items-center justify-between">
              <div>
                <h2 className="text-xl sm:text-2xl font-black text-slate-900 tracking-tight flex items-center gap-2">
                  <span>⇄</span>
                  <span>Recent Comparisons</span>
                </h2>
                <p className="text-xs text-slate-500 mt-0.5">
                  Quickly reopen comparisons with live updated store prices.
                </p>
              </div>
            </div>

            <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-4">
              {recentComparisons.map((comp) => (
                <div
                  key={comp.id}
                  className="bg-white rounded-2xl border border-slate-200 p-5 shadow-2xs hover:shadow-xs transition flex flex-col justify-between space-y-4"
                >
                  <div className="space-y-1.5">
                    <span className="text-[10px] font-bold uppercase tracking-wider text-indigo-600 bg-indigo-50 px-2 py-0.5 rounded-md">
                      Product Comparison
                    </span>
                    <h4 className="font-extrabold text-slate-900 text-sm truncate" title={comp.title}>
                      {comp.title}
                    </h4>
                    <p className="text-xs text-slate-500">
                      {comp.merchants}
                    </p>
                    <span className="text-[10px] text-slate-400 block">
                      Last compared {comp.lastCompared ? new Date(comp.lastCompared).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' }) : 'recently'}
                    </span>
                  </div>

                  <Link
                    to={comp.compareUrl || `/shopping?q=${encodeURIComponent(comp.title)}`}
                    className="w-full py-2 bg-slate-900 hover:bg-slate-800 text-white font-bold text-xs rounded-xl transition text-center flex items-center justify-center gap-1.5 shadow-2xs"
                  >
                    <span>Compare Again</span>
                    <span>⇄</span>
                  </Link>
                </div>
              ))}
            </div>
          </section>
        )}

        {/* SECTION C: SAVED PRODUCTS WATCHLIST */}
        {(activeTab === 'overview' || activeTab === 'saved') && (
          <section className="space-y-4" data-testid="section-saved-products">
            <div className="flex items-center justify-between gap-4">
              <div>
                <h2 className="text-xl sm:text-2xl font-black text-slate-900 tracking-tight flex items-center gap-2">
                  <span>❤️</span>
                  <span>My Saved Products</span>
                </h2>
                <p className="text-xs text-slate-500 mt-0.5">
                  Your bookmarked items with live store pricing and alert tracking.
                </p>
              </div>

              {savedProducts.length > 0 && (
                <Link
                  to={ROUTES.SAVED}
                  className="text-xs font-bold text-indigo-600 hover:text-indigo-800 flex items-center gap-1 shrink-0"
                >
                  <span>View All ({summary.savedProductsCount ?? savedProducts.length})</span>
                  <span>→</span>
                </Link>
              )}
            </div>

            {savedProducts.length > 0 ? (
              <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
                {savedProducts.map((product) => (
                  <SavedProductCard
                    key={product.id}
                    product={product}
                    onRemove={handleRemoveSaved}
                    onSetAlert={handleOpenAlertModal}
                  />
                ))}
              </div>
            ) : (
              <EmptyState
                title="No saved products yet."
                description="When exploring products, click the heart icon to save products to your personal watchlist for price tracking."
                actionLabel="Explore Products"
                actionUrl={ROUTES.SHOPPING}
              />
            )}
          </section>
        )}

        {/* SECTION D: ACTIVE ALERTS & RECENT ACTIVITY (TWO COLUMNS ON DESKTOP) */}
        <div className="grid grid-cols-1 lg:grid-cols-12 gap-8">
          
          {/* Active Alerts (6 Cols) */}
          {(activeTab === 'overview' || activeTab === 'alerts') && (
            <div className={activeTab === 'overview' ? 'lg:col-span-6 space-y-4' : 'lg:col-span-12 space-y-4'} data-testid="section-active-alerts">
              <div className="flex items-center justify-between">
                <div>
                  <h3 className="text-lg sm:text-xl font-extrabold text-slate-900 flex items-center gap-2">
                    <span>🎯</span>
                    <span>Active Price Alerts</span>
                  </h3>
                  <p className="text-xs text-slate-500 mt-0.5">Automated price drop monitors</p>
                </div>
                {activeAlerts.length > 0 && (
                  <Link
                    to={ROUTES.ALERTS}
                    className="text-xs font-bold text-indigo-600 hover:text-indigo-800 flex items-center gap-1"
                  >
                    <span>View All</span>
                    <span>→</span>
                  </Link>
                )}
              </div>

              {activeAlerts.length > 0 ? (
                <div className="bg-white rounded-3xl border border-slate-200 shadow-xs divide-y divide-slate-100 overflow-hidden">
                  {activeAlerts.map((alert) => (
                    <div
                      key={alert.id}
                      className="p-4 sm:p-5 flex items-center justify-between gap-4 hover:bg-slate-50/50 transition"
                    >
                      <div className="space-y-1 min-w-0">
                        <div className="flex items-center gap-2">
                          <span className="text-[10px] font-bold uppercase tracking-wider text-emerald-700 bg-emerald-50 px-2 py-0.5 rounded-md">
                            Active Monitor
                          </span>
                        </div>
                        <h4 className="font-bold text-slate-900 text-sm truncate" title={alert.productName}>
                          {alert.productName || 'Tracked Product'}
                        </h4>
                        <div className="text-xs text-slate-500 flex items-center gap-3">
                          <span>Target: <strong className="text-slate-900 font-mono">₹{Number(alert.targetPrice || 0).toLocaleString('en-IN')}</strong></span>
                          <span>•</span>
                          <span>{alert.createdAt ? new Date(alert.createdAt).toLocaleDateString() : 'Active'}</span>
                        </div>
                      </div>

                      <div className="flex items-center gap-2 shrink-0">
                        <button
                          type="button"
                          onClick={() => handleToggleAlert(alert.id, alert.active)}
                          className="px-2.5 py-1 text-xs font-semibold text-slate-600 hover:bg-slate-100 rounded-lg transition cursor-pointer"
                        >
                          {alert.active ? 'Pause' : 'Resume'}
                        </button>
                        <button
                          type="button"
                          onClick={() => handleDeleteAlert(alert.id)}
                          className="px-2.5 py-1 text-xs font-semibold text-rose-600 hover:bg-rose-50 rounded-lg transition cursor-pointer"
                        >
                          Delete
                        </button>
                      </div>
                    </div>
                  ))}
                </div>
              ) : (
                <EmptyState
                  title="You don't have any active price alerts."
                  description="Set target price alerts on products or flights to receive alerts when prices fall into your budget."
                  actionLabel="Explore Products to Track"
                  actionUrl={ROUTES.SHOPPING}
                />
              )}
            </div>
          )}

          {/* Recent Activity (6 Cols) */}
          {(activeTab === 'overview' || activeTab === 'activity') && (
            <div className={activeTab === 'overview' ? 'lg:col-span-6 space-y-4' : 'lg:col-span-12 space-y-4'} data-testid="section-recent-activity">
              <div className="flex items-center justify-between">
                <div>
                  <h3 className="text-lg sm:text-xl font-extrabold text-slate-900 flex items-center gap-2">
                    <span>📜</span>
                    <span>Recent Activity</span>
                  </h3>
                  <p className="text-xs text-slate-500 mt-0.5">Your recent comparison and search timeline</p>
                </div>
                {recentActivity.length > 0 && (
                  <Link
                    to={ROUTES.HISTORY}
                    className="text-xs font-bold text-indigo-600 hover:text-indigo-800 flex items-center gap-1"
                  >
                    <span>View All</span>
                    <span>→</span>
                  </Link>
                )}
              </div>

              {recentActivity.length > 0 ? (
                <div className="bg-white rounded-3xl border border-slate-200 shadow-xs divide-y divide-slate-100 overflow-hidden">
                  {recentActivity.map((act) => {
                    const icon =
                      act.activityType === 'FLIGHT_SEARCH'
                        ? '✈️'
                        : act.activityType === 'RIDE_SEARCH'
                        ? '🚗'
                        : '🛍️';

                    return (
                      <div
                        key={act.id}
                        className="p-4 sm:p-5 flex items-center justify-between gap-4 hover:bg-slate-50/50 transition"
                      >
                        <div className="flex items-center gap-3.5 min-w-0">
                          <div className="w-10 h-10 rounded-2xl bg-slate-100 flex items-center justify-center text-lg shrink-0">
                            {icon}
                          </div>
                          <div className="min-w-0">
                            <p className="font-bold text-slate-900 text-sm truncate">
                              {act.title}
                            </p>
                            <p className="text-xs text-slate-500 truncate">
                              {act.description}
                            </p>
                            <span className="text-[10px] text-slate-400 mt-0.5 block">
                              {act.createdAt ? new Date(act.createdAt).toLocaleString([], { dateStyle: 'short', timeStyle: 'short' }) : 'Recently'}
                            </span>
                          </div>
                        </div>

                        <Link
                          to={act.actionUrl || ROUTES.SHOPPING}
                          className="px-3 py-1.5 bg-indigo-50 hover:bg-indigo-100 text-indigo-700 font-bold text-xs rounded-xl transition shrink-0 flex items-center gap-1"
                        >
                          <span>{act.actionLabel || 'Compare Again'}</span>
                          <span>→</span>
                        </Link>
                      </div>
                    );
                  })}
                </div>
              ) : (
                <EmptyState
                  title="Your recent comparisons will appear here."
                  description="When you search and compare products, airfares, or ride options, your activity timeline will be recorded here for instant re-comparison."
                  actionLabel="Compare Products"
                  actionUrl={ROUTES.SHOPPING}
                />
              )}
            </div>
          )}

        </div>

      </div>

      {/* Price Alert Modal */}
      {alertModalProduct && (
        <PriceAlertModal
          isOpen={alertModalOpen}
          onClose={() => {
            setAlertModalOpen(false);
            setAlertModalProduct(null);
          }}
          product={alertModalProduct}
          onAlertCreated={fetchDashboard}
        />
      )}
    </div>
  );
};

export default DashboardPage;
