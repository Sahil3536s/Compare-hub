import React, { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { ROUTES } from '../utils/constants';
import { MOCK_PRODUCTS } from '../utils/mockData';
import ProductCard from '../components/ProductCard';
import { useHealthCheck } from '../hooks/useHealthCheck';
import StatusBadge from '../components/StatusBadge';
import { universalSearch } from '../services/searchService';
import UniversalSearchResults from '../components/UniversalSearchResults';
import LoadingSkeleton from '../components/LoadingSkeleton';
import SmartDealsSection from '../components/SmartDealsSection';

export const HomePage = () => {
  const navigate = useNavigate();
  const { isConnected, loading: healthLoading, error: healthError, refetch: refetchHealth } = useHealthCheck();

  const [searchQuery, setSearchQuery] = useState('');
  const [isSearching, setIsSearching] = useState(false);
  const [searchResult, setSearchResult] = useState(null);
  const [searchError, setSearchError] = useState(null);

  const sampleQueries = [
    { label: '📱 iPhone 17 256GB', query: 'iPhone 17 256GB' },
    { label: '✈️ Delhi to Mumbai flight tomorrow', query: 'Delhi to Mumbai flight tomorrow' },
    { label: '🚗 Ride from VIT Bhopal to Bhopal Airport', query: 'ride from VIT Bhopal to Bhopal Airport' },
    { label: '🔥 Samsung phone under 30000', query: 'Samsung phone under 30000' },
  ];

  const handleUniversalSearch = async (queryToSearch) => {
    const q = queryToSearch || searchQuery;
    if (!q || !q.trim()) return;

    setIsSearching(true);
    setSearchError(null);
    try {
      const res = await universalSearch(q.trim());
      setSearchResult(res);
    } catch (err) {
      console.error('Universal Search failed:', err);
      setSearchError(err.message || 'Failed to analyze search query');
    } finally {
      setIsSearching(false);
    }
  };

  const handleFormSubmit = (e) => {
    e.preventDefault();
    handleUniversalSearch(searchQuery);
  };

  const handleSelectSample = (sample) => {
    setSearchQuery(sample.query);
    handleUniversalSearch(sample.query);
  };

  return (
    <div className="space-y-12 sm:space-y-16 pb-16 min-w-0">
      
      {/* Top Hero Section with Universal Search */}
      <section className="relative bg-gradient-to-b from-indigo-50/50 via-slate-50 to-white pt-10 sm:pt-16 lg:pt-20 pb-10 sm:pb-12 border-b border-slate-100">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
          
          {/* Status & Intro Pill */}
          <div className="flex flex-col sm:flex-row items-center justify-center gap-2.5 sm:gap-3 mb-6">
            <span className="inline-flex items-center gap-1.5 px-3 py-1 rounded-full text-xs font-semibold bg-indigo-100/70 text-indigo-700 border border-indigo-200">
              ⚡ Multi-Platform Price Intelligence
            </span>
            <StatusBadge
              isConnected={isConnected}
              loading={healthLoading}
              error={healthError}
              onRetry={refetchHealth}
            />
          </div>

          {/* Main Hero Headings */}
          <div className="text-center max-w-4xl mx-auto space-y-3 sm:space-y-4 mb-8 sm:mb-10">
            <h1 className="text-3xl sm:text-5xl lg:text-6xl font-black tracking-tight text-slate-900 leading-tight">
              Compare Everything. <span className="text-indigo-600">Pay Less.</span>
            </h1>
            <p className="text-sm sm:text-lg lg:text-xl text-slate-600 font-normal max-w-2xl mx-auto leading-relaxed">
              One universal search bar for shopping products, flight tickets, and cab rides.
            </p>
          </div>

          {/* Universal Search Bar */}
          <div className="max-w-4xl mx-auto bg-white rounded-3xl border border-slate-200/90 shadow-xl p-3.5 sm:p-5 space-y-4">
            <form onSubmit={handleFormSubmit} className="flex flex-col sm:flex-row items-center gap-3">
              <div className="relative flex-1 w-full min-w-0">
                <div className="absolute left-4 top-1/2 -translate-y-1/2 text-slate-400 pointer-events-none" aria-hidden="true">
                  <svg className="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M21 21l-6-6m2-5a7 7 0 11-14 0 7 7 0 0114 0z" />
                  </svg>
                </div>
                <input
                  type="text"
                  value={searchQuery}
                  onChange={(e) => setSearchQuery(e.target.value)}
                  placeholder="Search products, flights ('Delhi to Mumbai') or rides ('VIT to Airport')..."
                  aria-label="Universal search query"
                  className="w-full pl-12 pr-4 py-3.5 sm:py-4 rounded-2xl bg-slate-50 border border-slate-200 text-slate-900 text-sm sm:text-base focus:bg-white focus:outline-hidden focus:border-indigo-500 focus:ring-2 focus:ring-indigo-500/20 min-h-[44px]"
                />
              </div>

              <button
                type="submit"
                disabled={isSearching}
                className="w-full sm:w-auto px-6 sm:px-8 py-3.5 sm:py-4 bg-indigo-600 hover:bg-indigo-700 text-white font-bold text-sm sm:text-base rounded-2xl transition shadow-md flex items-center justify-center gap-2 shrink-0 cursor-pointer min-h-[44px] disabled:opacity-75"
              >
                {isSearching ? (
                  <span className="flex items-center gap-2">
                    <span className="w-4 h-4 border-2 border-white/40 border-t-white rounded-full animate-spin"></span>
                    <span>Analyzing...</span>
                  </span>
                ) : (
                  <>
                    <span>Universal Search</span>
                    <svg className="w-5 h-5 shrink-0" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                      <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M14 5l7 7m0 0l-7 7m7-7H3" />
                    </svg>
                  </>
                )}
              </button>
            </form>

            {/* Quick Example Suggestions Chips */}
            <div className="pt-2 border-t border-slate-100 flex flex-wrap items-center gap-2 text-xs">
              <span className="text-slate-400 font-semibold">Try queries:</span>
              {sampleQueries.map((sample) => (
                <button
                  key={sample.query}
                  type="button"
                  onClick={() => handleSelectSample(sample)}
                  className="px-3 py-1.5 rounded-xl bg-slate-100 hover:bg-indigo-50 hover:text-indigo-700 text-slate-700 font-medium transition cursor-pointer"
                >
                  {sample.label}
                </button>
              ))}
            </div>
          </div>

          {/* Search Results / Loading / Error Container */}
          {isSearching && (
            <div className="max-w-4xl mx-auto mt-8">
              <LoadingSkeleton type="product-card" count={3} />
            </div>
          )}

          {searchError && (
            <div className="max-w-4xl mx-auto mt-6 p-4 rounded-2xl bg-rose-50 border border-rose-200 text-rose-700 text-xs font-semibold">
              {searchError}
            </div>
          )}

          {searchResult && !isSearching && (
            <div className="max-w-5xl mx-auto mt-8">
              <UniversalSearchResults
                result={searchResult}
                onResolveClarification={(refinedQuery) => {
                  setSearchQuery(refinedQuery);
                  handleUniversalSearch(refinedQuery);
                }}
                onClear={() => {
                  setSearchResult(null);
                  setSearchQuery('');
                }}
              />
            </div>
          )}

        </div>
      </section>

      {/* 3 Dedicated Feature Engine Cards */}
      <section className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
        <div className="grid grid-cols-1 md:grid-cols-3 gap-5 sm:gap-6">
          
          {/* Shopping Card */}
          <div
            onClick={() => navigate(ROUTES.SHOPPING)}
            className="group bg-white rounded-3xl border border-slate-200 p-6 sm:p-8 shadow-xs hover:shadow-xl hover:border-indigo-200 transition-all duration-300 cursor-pointer flex flex-col justify-between"
          >
            <div>
              <div className="w-12 h-12 sm:w-14 sm:h-14 rounded-2xl bg-indigo-50 text-indigo-600 flex items-center justify-center text-xl sm:text-2xl font-bold mb-4 sm:mb-6 group-hover:scale-110 transition-transform">
                🛍️
              </div>
              <h3 className="text-xl sm:text-2xl font-extrabold text-slate-900 group-hover:text-indigo-600 transition mb-2">
                Shopping Deals
              </h3>
              <p className="text-slate-500 text-xs sm:text-sm leading-relaxed mb-6">
                Compare product prices across Amazon, Flipkart, Croma and major retail outlets with verified best value models.
              </p>
            </div>
            <div className="inline-flex items-center text-xs sm:text-sm font-bold text-indigo-600 group-hover:gap-2 gap-1.5 transition-all">
              <span>Open shopping comparison</span>
              <svg className="w-4 h-4 shrink-0" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M9 5l7 7-7 7" />
              </svg>
            </div>
          </div>

          {/* Flights Card */}
          <div
            onClick={() => navigate(ROUTES.FLIGHTS)}
            className="group bg-white rounded-3xl border border-slate-200 p-6 sm:p-8 shadow-xs hover:shadow-xl hover:border-indigo-200 transition-all duration-300 cursor-pointer flex flex-col justify-between"
          >
            <div>
              <div className="w-12 h-12 sm:w-14 sm:h-14 rounded-2xl bg-sky-50 text-sky-600 flex items-center justify-center text-xl sm:text-2xl font-bold mb-4 sm:mb-6 group-hover:scale-110 transition-transform">
                ✈️
              </div>
              <h3 className="text-xl sm:text-2xl font-extrabold text-slate-900 group-hover:text-indigo-600 transition mb-2">
                Flight Fares
              </h3>
              <p className="text-slate-500 text-xs sm:text-sm leading-relaxed mb-6">
                Search hundreds of airline routes in seconds. Uncover airfare disparities, layover options and cheapest dates.
              </p>
            </div>
            <div className="inline-flex items-center text-xs sm:text-sm font-bold text-indigo-600 group-hover:gap-2 gap-1.5 transition-all">
              <span>Find cheaper flights</span>
              <svg className="w-4 h-4 shrink-0" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M9 5l7 7-7 7" />
              </svg>
            </div>
          </div>

          {/* Rides Card */}
          <div
            onClick={() => navigate(ROUTES.RIDES)}
            className="group bg-white rounded-3xl border border-slate-200 p-6 sm:p-8 shadow-xs hover:shadow-xl hover:border-indigo-200 transition-all duration-300 cursor-pointer flex flex-col justify-between"
          >
            <div>
              <div className="w-12 h-12 sm:w-14 sm:h-14 rounded-2xl bg-emerald-50 text-emerald-600 flex items-center justify-center text-xl sm:text-2xl font-bold mb-4 sm:mb-6 group-hover:scale-110 transition-transform">
                🚗
              </div>
              <h3 className="text-xl sm:text-2xl font-extrabold text-slate-900 group-hover:text-indigo-600 transition mb-2">
                Cab & Ride Fares
              </h3>
              <p className="text-slate-500 text-xs sm:text-sm leading-relaxed mb-6">
                Compare on-demand cab fares and ETAs in real-time across Uber, Ola, and Rapido without jumping between apps.
              </p>
            </div>
            <div className="inline-flex items-center text-xs sm:text-sm font-bold text-indigo-600 group-hover:gap-2 gap-1.5 transition-all">
              <span>Compare ride fares</span>
              <svg className="w-4 h-4 shrink-0" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M9 5l7 7-7 7" />
              </svg>
            </div>
          </div>

        </div>
      </section>

      {/* Today's Smart Deals Section */}
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
        <SmartDealsSection />
      </div>

      {/* Value Proposition Strip */}
      <section className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
        <div className="bg-slate-900 rounded-3xl p-6 sm:p-10 lg:p-12 text-white grid grid-cols-1 md:grid-cols-3 gap-6 sm:gap-8 text-center sm:text-left">
          <div className="space-y-1.5 sm:space-y-2">
            <div className="text-2xl sm:text-3xl font-black text-indigo-400">100%</div>
            <div className="font-bold text-sm sm:text-base">Transparent Comparison</div>
            <p className="text-xs text-slate-400">Unbiased rates directly aggregated with no hidden markups.</p>
          </div>
          <div className="space-y-1.5 sm:space-y-2">
            <div className="text-2xl sm:text-3xl font-black text-emerald-400">₹14,500+</div>
            <div className="font-bold text-sm sm:text-base">Avg. Yearly Savings</div>
            <p className="text-xs text-slate-400">Saved per member by verifying prices before booking or buying.</p>
          </div>
          <div className="space-y-1.5 sm:space-y-2">
            <div className="text-2xl sm:text-3xl font-black text-amber-400">Instant</div>
            <div className="font-bold text-sm sm:text-base">Real-Time Aggregation</div>
            <p className="text-xs text-slate-400">Multi-engine queries delivering results in under 500ms.</p>
          </div>
        </div>
      </section>

    </div>
  );
};

export default HomePage;
