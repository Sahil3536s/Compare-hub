import React, { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { getSmartDeals } from '../services/dealDiscoveryService';
import LoadingSkeleton from './LoadingSkeleton';

const CATEGORY_TABS = [
  { id: 'ALL', label: '🔥 All Smart Deals', key: 'ALL' },
  { id: 'EXCEPTIONAL_DEALS', label: '🏆 Exceptional Deals', key: 'EXCEPTIONAL_DEALS' },
  { id: 'PRICE_DROPS', label: '📉 Price Drops', key: 'PRICE_DROPS' },
  { id: 'WATCHLIST_DEALS', label: '⭐ Watchlist Deals', key: 'WATCHLIST_DEALS' },
  { id: 'TRAVEL_DEALS', label: '✈️ Travel Deals', key: 'TRAVEL_DEALS' },
];

export const SmartDealsSection = () => {
  const navigate = useNavigate();
  const [activeCategory, setActiveCategory] = useState('ALL');
  const [page, setPage] = useState(0);
  const [pageSize] = useState(8);
  const [dealsData, setDealsData] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  const fetchDeals = async (cat, p) => {
    setLoading(true);
    setError(null);
    try {
      const data = await getSmartDeals({ category: cat, page: p, size: pageSize });
      setDealsData(data);
    } catch (err) {
      console.error('Failed to load smart deals:', err);
      setError(err.message || 'Could not fetch smart deals.');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchDeals(activeCategory, page);
  }, [activeCategory, page]);

  const handleTabChange = (catId) => {
    setActiveCategory(catId);
    setPage(0);
  };

  const handleDealAction = (deal) => {
    if (deal.dealType === 'TRAVEL') {
      navigate(deal.linkUrl || '/flights');
    } else if (deal.productId) {
      navigate(`/product/${deal.productId}`);
    } else if (deal.linkUrl && deal.linkUrl.startsWith('/')) {
      navigate(deal.linkUrl);
    } else if (deal.linkUrl) {
      window.open(deal.linkUrl, '_blank', 'noopener,noreferrer');
    }
  };

  const categoryCounts = dealsData?.categoryCounts || {};
  const deals = dealsData?.deals || [];
  const totalPages = dealsData?.totalPages || 1;
  const totalElements = dealsData?.totalElements || 0;

  return (
    <section className="py-12 space-y-8" data-testid="smart-deals-section">
      {/* Header */}
      <div className="flex flex-col md:flex-row md:items-end justify-between gap-4">
        <div>
          <div className="inline-flex items-center gap-1.5 px-3 py-1 rounded-full bg-emerald-50 border border-emerald-200 text-emerald-800 text-xs font-bold mb-2">
            <span>🛡️ Verified Price History Engine</span>
          </div>
          <h2 className="text-2xl sm:text-3xl font-black text-slate-900 tracking-tight">
            Today's <span className="text-indigo-600">Smart Deals</span>
          </h2>
          <p className="text-sm text-slate-500 mt-1 max-w-2xl">
            High-conviction deals evaluated against 30/90-day historical averages, verified price drops, and your watched targets—no inflated MRP discounts.
          </p>
        </div>

        {totalElements > 0 && (
          <div className="text-xs font-semibold text-slate-500">
            Showing <strong className="text-slate-900">{deals.length}</strong> of <strong className="text-slate-900">{totalElements}</strong> curated opportunities
          </div>
        )}
      </div>

      {/* Category Tabs */}
      <div className="flex items-center gap-2 overflow-x-auto pb-2 border-b border-slate-200 scrollbar-none">
        {CATEGORY_TABS.map((tab) => {
          const isActive = activeCategory === tab.id;
          const count = categoryCounts[tab.key] ?? 0;

          return (
            <button
              key={tab.id}
              type="button"
              onClick={() => handleTabChange(tab.id)}
              className={`px-4 py-2 rounded-2xl text-xs font-bold whitespace-nowrap transition-all duration-200 flex items-center gap-2 cursor-pointer ${
                isActive
                  ? 'bg-slate-900 text-white shadow-sm'
                  : 'bg-slate-100 hover:bg-slate-200 text-slate-700'
              }`}
            >
              <span>{tab.label}</span>
              <span
                className={`px-1.5 py-0.5 rounded-full text-[10px] font-black ${
                  isActive ? 'bg-indigo-500 text-white' : 'bg-slate-200 text-slate-600'
                }`}
              >
                {count}
              </span>
            </button>
          );
        })}
      </div>

      {/* Loading State */}
      {loading && (
        <div className="py-4">
          <LoadingSkeleton type="product-card" count={4} />
        </div>
      )}

      {/* Error State */}
      {error && !loading && (
        <div className="bg-rose-50 border border-rose-200 rounded-3xl p-6 text-center space-y-3">
          <p className="text-xs text-rose-700 font-medium">{error}</p>
          <button
            type="button"
            onClick={() => fetchDeals(activeCategory, page)}
            className="px-4 py-2 bg-rose-600 hover:bg-rose-700 text-white text-xs font-bold rounded-xl cursor-pointer"
          >
            Retry Deals
          </button>
        </div>
      )}

      {/* Empty State */}
      {!loading && !error && deals.length === 0 && (
        <div className="bg-slate-50 border border-slate-200 rounded-3xl p-10 text-center space-y-3">
          <div className="text-3xl">🔍</div>
          <h4 className="text-base font-bold text-slate-800">No deals found in this category</h4>
          <p className="text-xs text-slate-500 max-w-md mx-auto">
            {activeCategory === 'WATCHLIST_DEALS'
              ? 'Save products or configure price alerts to see personalized watchlist deals here.'
              : 'Our price engines are constantly monitoring prices. Check back soon for newly verified drops.'}
          </p>
          {activeCategory !== 'ALL' && (
            <button
              type="button"
              onClick={() => handleTabChange('ALL')}
              className="mt-2 px-4 py-2 bg-indigo-600 hover:bg-indigo-700 text-white text-xs font-bold rounded-xl cursor-pointer"
            >
              View All Smart Deals
            </button>
          )}
        </div>
      )}

      {/* Deals Grid */}
      {!loading && !error && deals.length > 0 && (
        <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-6">
          {deals.map((deal) => {
            const isTravel = deal.dealType === 'TRAVEL';
            const isExceptional = deal.dealScore >= 85;

            return (
              <div
                key={deal.id}
                className="bg-white rounded-3xl border border-slate-200 shadow-xs hover:shadow-lg transition-all duration-300 flex flex-col justify-between overflow-hidden group hover:border-indigo-300 relative"
              >
                {/* Top Badges Strip */}
                <div className="p-5 pb-0 flex items-center justify-between gap-2">
                  <div className="flex items-center gap-1.5 flex-wrap">
                    <span className="px-2.5 py-0.5 rounded-lg bg-slate-100 text-slate-700 text-[10px] font-bold uppercase tracking-wider">
                      {deal.category}
                    </span>
                    {deal.alertTriggered && (
                      <span className="px-2 py-0.5 rounded-lg bg-rose-50 border border-rose-200 text-rose-700 text-[10px] font-extrabold animate-pulse">
                        🎯 Target Alert
                      </span>
                    )}
                    {deal.watchlistMatch && !deal.alertTriggered && (
                      <span className="px-2 py-0.5 rounded-lg bg-amber-50 border border-amber-200 text-amber-800 text-[10px] font-extrabold">
                        ⭐ Watched
                      </span>
                    )}
                  </div>

                  {/* Deal Score Badge */}
                  <div
                    className={`shrink-0 px-2 py-0.5 rounded-lg text-[10px] font-black border ${
                      isExceptional
                        ? 'bg-emerald-50 text-emerald-800 border-emerald-300'
                        : 'bg-indigo-50 text-indigo-700 border-indigo-200'
                    }`}
                  >
                    {deal.dealScore} Deal Score
                  </div>
                </div>

                {/* Main Content */}
                <div className="p-5 space-y-3 flex-1 flex flex-col">
                  {/* Title & Provider */}
                  <div>
                    <h3 className="text-sm font-extrabold text-slate-900 group-hover:text-indigo-600 transition-colors line-clamp-2">
                      {deal.title}
                    </h3>
                    <p className="text-[11px] text-slate-500 font-medium mt-0.5">
                      via <strong className="text-slate-700">{deal.merchantOrProvider}</strong>
                    </p>
                  </div>

                  {/* Price Block */}
                  <div className="bg-slate-50 rounded-2xl p-3 space-y-1 border border-slate-100 mt-auto">
                    <div className="flex items-baseline justify-between gap-2">
                      <div className="text-xl font-black text-slate-900 font-mono">
                        ₹{Number(deal.currentPrice).toLocaleString('en-IN')}
                      </div>
                      {deal.realDiscountPercent > 0 && (
                        <div className="px-2 py-0.5 rounded-md bg-emerald-100 text-emerald-800 font-black text-[11px] font-mono">
                          {deal.realDiscountPercent}% OFF
                        </div>
                      )}
                    </div>

                    <div className="flex items-center justify-between text-[11px] text-slate-500">
                      <span>Historical Typical:</span>
                      <span className="line-through font-mono">
                        ₹{Number(deal.historicalTypicalPrice).toLocaleString('en-IN')}
                      </span>
                    </div>

                    {deal.realSavingsAmount > 0 && (
                      <div className="text-[11px] font-bold text-emerald-700 pt-0.5 border-t border-slate-200/60 flex justify-between">
                        <span>Real Saving:</span>
                        <span className="font-mono">₹{Number(deal.realSavingsAmount).toLocaleString('en-IN')}</span>
                      </div>
                    )}
                  </div>

                  {/* Objective Reason Callout */}
                  <div className="bg-indigo-50/50 rounded-xl p-2.5 border border-indigo-100/80">
                    <div className="text-[11px] text-indigo-950 font-medium leading-relaxed">
                      💡 {deal.reason}
                    </div>
                  </div>
                </div>

                {/* Card Action Footer */}
                <div className="p-5 pt-0">
                  <button
                    type="button"
                    onClick={() => handleDealAction(deal)}
                    className="w-full py-2.5 bg-slate-900 hover:bg-indigo-600 text-white font-extrabold text-xs rounded-xl shadow-xs transition duration-200 cursor-pointer flex items-center justify-center gap-1.5"
                  >
                    <span>{isTravel ? 'Explore Journey' : 'Compare Offers'}</span>
                    <span>→</span>
                  </button>
                </div>

              </div>
            );
          })}
        </div>
      )}

      {/* Pagination Controls */}
      {!loading && !error && totalPages > 1 && (
        <div className="flex items-center justify-between border-t border-slate-200 pt-6">
          <button
            type="button"
            disabled={page <= 0}
            onClick={() => setPage((prev) => Math.max(0, prev - 1))}
            className="px-4 py-2 rounded-xl text-xs font-bold border border-slate-300 text-slate-700 hover:bg-slate-100 disabled:opacity-40 disabled:cursor-not-allowed transition cursor-pointer"
          >
            ← Previous
          </button>

          <div className="flex items-center gap-1.5">
            {Array.from({ length: totalPages }, (_, i) => (
              <button
                key={i}
                type="button"
                onClick={() => setPage(i)}
                className={`w-8 h-8 rounded-xl text-xs font-bold transition cursor-pointer ${
                  page === i
                    ? 'bg-indigo-600 text-white shadow-xs'
                    : 'bg-white hover:bg-slate-100 text-slate-700 border border-slate-200'
                }`}
              >
                {i + 1}
              </button>
            ))}
          </div>

          <button
            type="button"
            disabled={page >= totalPages - 1}
            onClick={() => setPage((prev) => Math.min(totalPages - 1, prev + 1))}
            className="px-4 py-2 rounded-xl text-xs font-bold border border-slate-300 text-slate-700 hover:bg-slate-100 disabled:opacity-40 disabled:cursor-not-allowed transition cursor-pointer"
          >
            Next →
          </button>
        </div>
      )}

    </section>
  );
};

export default SmartDealsSection;
