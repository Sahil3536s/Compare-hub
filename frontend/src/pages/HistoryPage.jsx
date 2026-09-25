import React, { useState, useEffect, useCallback } from 'react';
import { Link } from 'react-router-dom';
import { getSearchHistory, clearSearchHistory, deleteHistoryItem, getFlightHistory, getRideHistory } from '../services/historyService';
import LoadingSkeleton from '../components/LoadingSkeleton';
import EmptyState from '../components/EmptyState';
import ErrorState from '../components/ErrorState';
import { ROUTES } from '../utils/constants';

export const HistoryPage = () => {
  const [historyItems, setHistoryItems] = useState([]);
  const [flightHistory, setFlightHistory] = useState([]);
  const [rideHistory, setRideHistory] = useState([]);
  const [activeCategory, setActiveCategory] = useState('all'); // 'all', 'comparisons', 'shopping', 'flights', 'rides'
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  const loadHistory = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      const [historyData, flightsData, ridesData] = await Promise.allSettled([
        getSearchHistory(),
        getFlightHistory(),
        getRideHistory(),
      ]);

      if (historyData.status === 'fulfilled') setHistoryItems(historyData.value || []);
      if (flightsData.status === 'fulfilled') setFlightHistory(flightsData.value || []);
      if (ridesData.status === 'fulfilled') setRideHistory(ridesData.value || []);
    } catch (err) {
      console.error('Failed to load history:', err);
      setError('Unable to load your search history. Please check your connection.');
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    loadHistory();
  }, [loadHistory]);

  const handleClearAll = async () => {
    if (!window.confirm('Are you sure you want to clear your entire search history?')) return;
    try {
      await clearSearchHistory();
      setHistoryItems([]);
    } catch (e) {
      alert('Failed to clear search history.');
    }
  };

  const handleDeleteItem = async (id) => {
    try {
      await deleteHistoryItem(id);
      setHistoryItems((prev) => prev.filter((item) => item.id !== id));
    } catch (e) {
      alert('Failed to delete history item.');
    }
  };

  const comparisonItems = historyItems.filter((item) => item.searchType === 'PRODUCT_COMPARE');

  const filteredHistory = historyItems.filter((item) => {
    if (activeCategory === 'comparisons') return item.searchType === 'PRODUCT_COMPARE';
    if (activeCategory === 'shopping') return item.searchType === 'SHOPPING';
    if (activeCategory === 'flights') return item.searchType === 'FLIGHTS';
    if (activeCategory === 'rides') return item.searchType === 'RIDES';
    return true;
  });

  return (
    <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8 space-y-8 animate-fadeIn" data-testid="history-page">
      
      {/* Header */}
      <div>
        <div className="flex items-center gap-2 text-xs text-slate-500 mb-2">
          <span>Home</span>
          <span>/</span>
          <span className="text-indigo-600 font-medium">Search & Activity</span>
        </div>
        <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
          <div>
            <h1 className="text-3xl sm:text-4xl font-extrabold text-slate-900 tracking-tight">
              Search & Activity History
            </h1>
            <p className="text-sm text-slate-500 mt-1">
              Review your recent searches across shopping, comparisons, flights, and rides with one-click re-search and reopen.
            </p>
          </div>
          {historyItems.length > 0 && (
            <button
              onClick={handleClearAll}
              className="px-4 py-2 bg-rose-50 hover:bg-rose-100 text-rose-700 font-bold text-xs rounded-xl transition cursor-pointer self-start sm:self-auto"
            >
              🗑️ Clear All History
            </button>
          )}
        </div>
      </div>

      {/* Filter Category Pills */}
      <div className="flex gap-2 overflow-x-auto pb-1" role="tablist" aria-label="History categories">
        {[
          { id: 'all', label: 'All Activity', count: historyItems.length },
          { id: 'comparisons', label: '⚖️ Comparisons', count: comparisonItems.length },
          { id: 'shopping', label: '🛍️ Shopping', count: historyItems.filter((i) => i.searchType === 'SHOPPING').length },
          { id: 'flights', label: '✈️ Flights', count: historyItems.filter((i) => i.searchType === 'FLIGHTS').length + flightHistory.length },
          { id: 'rides', label: '🚗 Rides', count: historyItems.filter((i) => i.searchType === 'RIDES').length + rideHistory.length },
        ].map((cat) => (
          <button
            key={cat.id}
            role="tab"
            aria-selected={activeCategory === cat.id}
            onClick={() => setActiveCategory(cat.id)}
            className={`px-4 py-2 rounded-xl text-xs font-bold transition whitespace-nowrap flex items-center gap-2 cursor-pointer ${
              activeCategory === cat.id
                ? 'bg-slate-900 text-white shadow-xs'
                : 'bg-white border border-slate-200 text-slate-600 hover:bg-slate-50'
            }`}
          >
            <span>{cat.label}</span>
            <span className={`px-1.5 py-0.5 rounded-full text-[10px] ${
              activeCategory === cat.id ? 'bg-slate-700 text-white' : 'bg-slate-100 text-slate-500'
            }`}>
              {cat.count}
            </span>
          </button>
        ))}
      </div>

      {/* Main History Feed */}
      {loading ? (
        <LoadingSkeleton type="product-card" count={3} />
      ) : error ? (
        <ErrorState title="History Error" message={error} onRetry={loadHistory} />
      ) : (
        <div className="space-y-8">

          {/* DEDICATED SECTION: RECENT COMPARISONS (When viewing All or Comparisons) */}
          {(activeCategory === 'all' || activeCategory === 'comparisons') && (
            <div className="space-y-4" data-testid="recent-comparisons-section">
              <div className="flex items-center justify-between">
                <div>
                  <h2 className="text-xl font-black text-slate-900 flex items-center gap-2">
                    <span>⇄</span>
                    <span>Recent Comparisons</span>
                  </h2>
                  <p className="text-xs text-slate-500 mt-0.5">
                    Multi-provider comparisons you ran recently with one-click reload.
                  </p>
                </div>
              </div>

              {comparisonItems.length > 0 ? (
                <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-4">
                  {comparisonItems.map((comp) => {
                    const target = comp.targetUrl || `/shopping?q=${encodeURIComponent(comp.query)}`;
                    return (
                      <div
                        key={`comp-${comp.id}`}
                        className="bg-white rounded-2xl border border-slate-200 p-5 shadow-2xs hover:shadow-xs transition flex flex-col justify-between space-y-4"
                      >
                        <div className="space-y-1.5">
                          <span className="text-[10px] font-bold uppercase tracking-wider text-indigo-600 bg-indigo-50 px-2 py-0.5 rounded-md">
                            Product Comparison
                          </span>
                          <h3 className="font-extrabold text-slate-900 text-sm truncate" title={comp.query}>
                            {comp.query}
                          </h3>
                          <p className="text-xs text-slate-500">
                            {comp.details || 'Amazon vs Flipkart vs Croma'}
                          </p>
                          <span className="text-[10px] text-slate-400 block">
                            Compared {comp.createdAt ? new Date(comp.createdAt).toLocaleString([], { dateStyle: 'short', timeStyle: 'short' }) : 'recently'}
                          </span>
                        </div>

                        <Link
                          to={target}
                          className="w-full py-2 bg-slate-900 hover:bg-slate-800 text-white font-bold text-xs rounded-xl transition text-center flex items-center justify-center gap-1.5 shadow-2xs"
                        >
                          <span>Reopen Comparison</span>
                          <span>⇄</span>
                        </Link>
                      </div>
                    );
                  })}
                </div>
              ) : activeCategory === 'comparisons' ? (
                <EmptyState
                  title="Your recent comparisons will appear here."
                  description="Compare products side-by-side across stores to save and reopen them anytime."
                  actionLabel="Compare Products"
                  actionUrl={ROUTES.SHOPPING}
                />
              ) : null}
            </div>
          )}

          {/* MAIN SEARCH & ACTIVITY LIST (Filtered by selected category) */}
          <div className="space-y-4">
            <h2 className="text-xl font-black text-slate-900 flex items-center gap-2">
              <span>📜</span>
              <span>All Activity Items</span>
            </h2>

            {filteredHistory.length > 0 ? (
              <div className="bg-white rounded-3xl border border-slate-200 shadow-xs divide-y divide-slate-100 overflow-hidden">
                {filteredHistory.map((item) => {
                  const getRedirectUrl = () => {
                    if (item.targetUrl) return item.targetUrl;
                    if (item.searchType === 'FLIGHTS') return `/flights`;
                    if (item.searchType === 'RIDES') return `/rides`;
                    return `/shopping?q=${encodeURIComponent(item.query)}`;
                  };

                  const getIcon = () => {
                    if (item.searchType === 'PRODUCT_COMPARE') return '⚖️';
                    if (item.searchType === 'FLIGHTS') return '✈️';
                    if (item.searchType === 'RIDES') return '🚗';
                    return '🛍️';
                  };

                  const getActionLabel = () => {
                    if (item.searchType === 'PRODUCT_COMPARE') return 'Compare Again';
                    if (item.searchType === 'FLIGHTS') return 'Re-search Flights';
                    if (item.searchType === 'RIDES') return 'Re-search Rides';
                    return 'Re-search';
                  };

                  return (
                    <div
                      key={item.id}
                      className="p-4 sm:p-5 flex items-center justify-between gap-4 hover:bg-slate-50 transition"
                    >
                      <div className="flex items-center gap-3.5 min-w-0">
                        <div className="w-10 h-10 rounded-2xl bg-slate-100 flex items-center justify-center text-lg shrink-0">
                          {getIcon()}
                        </div>
                        <div className="min-w-0">
                          <Link
                            to={getRedirectUrl()}
                            className="font-bold text-slate-900 text-sm hover:text-indigo-600 transition block truncate"
                          >
                            {item.query}
                          </Link>
                          {item.details && (
                            <p className="text-xs text-slate-500 truncate mt-0.5">
                              {item.details}
                            </p>
                          )}
                          <div className="flex items-center gap-2 mt-0.5 text-[11px] text-slate-400">
                            <span className="font-semibold uppercase tracking-wider text-indigo-600">
                              {item.searchType === 'PRODUCT_COMPARE' ? 'Comparison' : item.searchType}
                            </span>
                            <span>•</span>
                            <span>{item.createdAt ? new Date(item.createdAt).toLocaleString() : 'Recent'}</span>
                          </div>
                        </div>
                      </div>

                      <div className="flex items-center gap-2 shrink-0">
                        <Link
                          to={getRedirectUrl()}
                          className="px-3.5 py-1.5 bg-indigo-50 hover:bg-indigo-100 text-indigo-700 font-bold text-xs rounded-xl transition flex items-center gap-1"
                        >
                          <span>{getActionLabel()}</span>
                          <span>→</span>
                        </Link>
                        <button
                          onClick={() => handleDeleteItem(item.id)}
                          className="p-1.5 text-slate-400 hover:text-rose-600 rounded-lg transition cursor-pointer"
                          title="Delete from history"
                          aria-label="Delete history item"
                        >
                          ✕
                        </button>
                      </div>
                    </div>
                  );
                })}
              </div>
            ) : (
              <EmptyState
                title="Your recent comparisons will appear here."
                description="Your product, flight, and ride searches and comparisons will be listed here for quick review."
                actionLabel="Compare Products"
                actionUrl={ROUTES.SHOPPING}
              />
            )}
          </div>

        </div>
      )}

    </div>
  );
};

export default HistoryPage;
