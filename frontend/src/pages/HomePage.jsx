import React, { useState, useEffect } from 'react';
import { useNavigate, useSearchParams } from 'react-router-dom';
import { ROUTES } from '../utils/constants';
import { useHealthCheck } from '../hooks/useHealthCheck';
import StatusBadge from '../components/StatusBadge';
import { universalSearch } from '../services/searchService';
import UniversalSearchResults from '../components/UniversalSearchResults';
import LoadingSkeleton from '../components/LoadingSkeleton';
import SmartDealsSection from '../components/SmartDealsSection';

export const HomePage = () => {
  const navigate = useNavigate();
  const [searchParams] = useSearchParams();
  const { isConnected, loading: healthLoading, error: healthError, refetch: refetchHealth } = useHealthCheck();

  const [searchQuery, setSearchQuery] = useState('');
  const [isSearching, setIsSearching] = useState(false);
  const [searchResult, setSearchResult] = useState(null);
  const [searchError, setSearchError] = useState(null);
  const [hasSearched, setHasSearched] = useState(false);

  // Focus search input if directed from navbar search icon
  useEffect(() => {
    if (searchParams.get('focusSearch') === 'true') {
      const input = document.getElementById('universal-search-input');
      if (input) {
        input.focus();
        input.scrollIntoView({ behavior: 'smooth', block: 'center' });
      }
    }
  }, [searchParams]);

  const sampleQueries = [
    { label: '📱 iPhone 15 128GB', query: 'iPhone 15 128GB' },
    { label: '💻 MacBook Air M3', query: 'MacBook Air M3' },
    { label: '✈️ Delhi to Mumbai flight', query: 'Delhi to Mumbai flight tomorrow' },
    { label: '🚗 Ride to Airport', query: 'ride to Bhopal Airport' },
    { label: '🎧 Sony WH-1000XM5', query: 'Sony WH-1000XM5' },
  ];

  const handleUniversalSearch = async (queryToSearch) => {
    const q = (queryToSearch !== undefined ? queryToSearch : searchQuery).trim();
    if (!q) return;

    setIsSearching(true);
    setSearchError(null);
    setHasSearched(true);

    try {
      const res = await universalSearch(q);
      setSearchResult(res);
    } catch (err) {
      console.error('Universal Search failed:', err);
      setSearchError(err.message || 'Unable to fetch comparison data. Please try again.');
      setSearchResult(null);
    } finally {
      setIsSearching(false);
    }
  };

  const handleFormSubmit = (e) => {
    e.preventDefault();
    handleUniversalSearch(searchQuery);
  };

  const handleClear = () => {
    setSearchQuery('');
    setSearchResult(null);
    setSearchError(null);
    setHasSearched(false);
    const input = document.getElementById('universal-search-input');
    if (input) input.focus();
  };

  const handleSelectSample = (sample) => {
    setSearchQuery(sample.query);
    handleUniversalSearch(sample.query);
  };

  // Determine if result is truly empty (0 offers across all domains)
  const isResultEmpty = searchResult && !isSearching && (
    (searchResult.intent === 'PRODUCT_SEARCH' && (!searchResult.productResults?.offers || searchResult.productResults.offers.length === 0)) ||
    (searchResult.intent === 'FLIGHT_SEARCH' && (!searchResult.flightResults?.offers || searchResult.flightResults.offers.length === 0)) ||
    (searchResult.intent === 'RIDE_SEARCH' && (!searchResult.rideResults?.offers || searchResult.rideResults.offers.length === 0))
  );

  return (
    <div className="space-y-12 sm:space-y-16 pb-16 min-w-0 bg-slate-50/40">

      {/* Hero Section */}
      <section className="relative pt-10 sm:pt-16 pb-12 sm:pb-16 border-b border-slate-200/70 bg-gradient-to-b from-white via-indigo-50/20 to-slate-50/50">
        <div className="max-w-5xl mx-auto px-4 sm:px-6 lg:px-8">

          {/* Platform Status Badge */}
          <div className="flex items-center justify-center gap-2 mb-6">
            <span className="inline-flex items-center gap-1.5 px-3 py-1 rounded-full text-xs font-semibold bg-indigo-50 text-indigo-700 border border-indigo-100">
              ⚡ Multi-Platform Price Intelligence
            </span>
            <StatusBadge
              isConnected={isConnected}
              loading={healthLoading}
              error={healthError}
              onRetry={refetchHealth}
            />
          </div>

          {/* Main Headings (Verbatim from specification) */}
          <div className="text-center space-y-3 sm:space-y-4 mb-8 sm:mb-10">
            <h1 className="text-3xl sm:text-5xl lg:text-6xl font-black tracking-tight text-slate-900 leading-tight">
              Compare prices. <span className="text-indigo-600">Decide smarter.</span>
            </h1>
            <p className="text-base sm:text-lg lg:text-xl text-slate-600 font-normal max-w-2xl mx-auto leading-relaxed">
              Compare products, flights and rides in one place.
            </p>
          </div>

          {/* Large Central Search Area */}
          <div className="max-w-3xl mx-auto">
            <div className="bg-white rounded-3xl border border-slate-200 shadow-lg hover:shadow-xl transition-shadow p-2.5 sm:p-3.5">
              <form onSubmit={handleFormSubmit} className="flex flex-col sm:flex-row items-center gap-2" role="search">
                
                {/* Search Input Container */}
                <div className="relative flex-1 w-full min-w-0 flex items-center">
                  {/* Search Icon */}
                  <div className="absolute left-4 text-slate-400 pointer-events-none" aria-hidden="true">
                    <svg className="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                      <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M21 21l-6-6m2-5a7 7 0 11-14 0 7 7 0 0114 0z" />
                    </svg>
                  </div>

                  <input
                    id="universal-search-input"
                    type="text"
                    value={searchQuery}
                    onChange={(e) => setSearchQuery(e.target.value)}
                    placeholder="Search a product or paste a product link"
                    aria-label="Universal search query"
                    autoComplete="off"
                    className="w-full pl-12 pr-10 py-3.5 sm:py-4 rounded-2xl bg-transparent text-slate-900 text-sm sm:text-base placeholder:text-slate-400 focus:outline-hidden min-h-[48px]"
                  />

                  {/* Clear Button */}
                  {searchQuery && (
                    <button
                      type="button"
                      onClick={handleClear}
                      aria-label="Clear search input"
                      className="absolute right-3 p-1.5 text-slate-400 hover:text-slate-600 rounded-full hover:bg-slate-100 transition cursor-pointer min-h-[36px] min-w-[36px] flex items-center justify-center"
                    >
                      <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                        <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M6 18L18 6M6 6l12 12" />
                      </svg>
                    </button>
                  )}
                </div>

                {/* Submit Search Button */}
                <button
                  type="submit"
                  disabled={isSearching || !searchQuery.trim()}
                  aria-label="Search"
                  className="w-full sm:w-auto px-6 sm:px-8 py-3.5 sm:py-4 bg-indigo-600 hover:bg-indigo-700 disabled:bg-slate-200 disabled:text-slate-400 text-white font-bold text-sm sm:text-base rounded-2xl transition shadow-xs hover:shadow-md flex items-center justify-center gap-2 shrink-0 cursor-pointer min-h-[48px]"
                >
                  {isSearching ? (
                    <span className="flex items-center gap-2">
                      <span className="w-4 h-4 border-2 border-white/40 border-t-white rounded-full animate-spin"></span>
                      <span>Searching...</span>
                    </span>
                  ) : (
                    <>
                      <span>Search</span>
                      <svg className="w-4 h-4 shrink-0" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                        <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M14 5l7 7m0 0l-7 7m7-7H3" />
                      </svg>
                    </>
                  )}
                </button>
              </form>

              {/* Sample Queries Chips */}
              <div className="pt-2.5 mt-2 border-t border-slate-100 flex flex-wrap items-center gap-1.5 sm:gap-2 px-2 text-xs">
                <span className="text-slate-400 font-semibold mr-1">Popular:</span>
                {sampleQueries.map((sample) => (
                  <button
                    key={sample.query}
                    type="button"
                    onClick={() => handleSelectSample(sample)}
                    className="px-2.5 py-1 rounded-lg bg-slate-100 hover:bg-indigo-50 hover:text-indigo-700 text-slate-600 font-medium transition cursor-pointer"
                  >
                    {sample.label}
                  </button>
                ))}
              </div>
            </div>
          </div>

          {/* Loading State Feedback */}
          {isSearching && (
            <div className="max-w-4xl mx-auto mt-8 space-y-4" data-testid="search-loading-state">
              <div className="text-center text-xs font-semibold text-indigo-600 animate-pulse">
                Gathering real-time comparisons from verified providers...
              </div>
              <LoadingSkeleton type="product-card" count={3} />
            </div>
          )}

          {/* Error State */}
          {searchError && !isSearching && (
            <div className="max-w-2xl mx-auto mt-6 p-4 rounded-2xl bg-rose-50 border border-rose-200 text-rose-800 text-xs font-semibold flex items-center justify-between gap-3 shadow-xs" role="alert">
              <div className="flex items-center gap-2">
                <span className="text-base" aria-hidden="true">⚠️</span>
                <span>{searchError}</span>
              </div>
              <button
                type="button"
                onClick={() => handleUniversalSearch(searchQuery)}
                className="px-3 py-1 bg-white text-rose-700 hover:bg-rose-100 rounded-lg border border-rose-200 transition font-bold cursor-pointer shrink-0"
              >
                Try Again
              </button>
            </div>
          )}

          {/* Empty Results State */}
          {isResultEmpty && (
            <div className="max-w-md mx-auto mt-8 p-8 bg-white rounded-3xl border border-slate-200 text-center shadow-xs space-y-3" data-testid="empty-search-state">
              <div className="w-12 h-12 rounded-2xl bg-slate-100 text-slate-500 mx-auto flex items-center justify-center text-xl">
                🔍
              </div>
              <h3 className="font-bold text-base text-slate-900">No comparisons found</h3>
              <p className="text-xs text-slate-500 leading-relaxed">
                We couldn't find matches for <span className="font-semibold text-slate-700">"{searchQuery}"</span>.
                Try searching for a different brand, generic model, or paste a direct product link.
              </p>
              <button
                type="button"
                onClick={handleClear}
                className="px-4 py-2 bg-indigo-50 hover:bg-indigo-100 text-indigo-700 text-xs font-bold rounded-xl transition cursor-pointer"
              >
                Clear Search
              </button>
            </div>
          )}

          {/* Results State */}
          {searchResult && !isSearching && !isResultEmpty && (
            <div className="max-w-5xl mx-auto mt-8">
              <UniversalSearchResults
                result={searchResult}
                onResolveClarification={(refinedQuery) => {
                  setSearchQuery(refinedQuery);
                  handleUniversalSearch(refinedQuery);
                }}
                onClear={handleClear}
              />
            </div>
          )}

        </div>
      </section>

      {/* 3 Category Cards: Shopping, Flights, Rides */}
      <section className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8" aria-label="Comparison Categories">
        <div className="text-center mb-6 sm:mb-8">
          <h2 className="text-xs font-bold text-slate-400 uppercase tracking-widest">
            Explore Comparison Categories
          </h2>
        </div>

        <div className="grid grid-cols-1 md:grid-cols-3 gap-5 sm:gap-6">
          
          {/* Card 1: Shopping */}
          <div
            onClick={() => navigate(ROUTES.SHOPPING)}
            role="button"
            tabIndex={0}
            onKeyDown={(e) => e.key === 'Enter' && navigate(ROUTES.SHOPPING)}
            className="group bg-white rounded-3xl border border-slate-200/90 p-6 sm:p-8 shadow-xs hover:shadow-xl hover:border-indigo-300 transition-all duration-200 cursor-pointer flex flex-col justify-between"
          >
            <div>
              <div className="w-12 h-12 rounded-2xl bg-indigo-50 text-indigo-600 flex items-center justify-center text-2xl font-bold mb-5 group-hover:scale-105 transition-transform">
                🛍️
              </div>
              <h3 className="text-xl font-bold text-slate-900 group-hover:text-indigo-600 transition mb-2">
                Shopping
              </h3>
              <p className="text-slate-500 text-xs sm:text-sm leading-relaxed mb-6">
                Compare electronics, gadgets, and retail prices across Amazon, Flipkart, Croma and more.
              </p>
            </div>
            <div className="inline-flex items-center text-xs sm:text-sm font-bold text-indigo-600 group-hover:gap-2 gap-1.5 transition-all">
              <span>Compare Products</span>
              <svg className="w-4 h-4 shrink-0 transition-transform group-hover:translate-x-1" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M9 5l7 7-7 7" />
              </svg>
            </div>
          </div>

          {/* Card 2: Flights */}
          <div
            onClick={() => navigate(ROUTES.FLIGHTS)}
            role="button"
            tabIndex={0}
            onKeyDown={(e) => e.key === 'Enter' && navigate(ROUTES.FLIGHTS)}
            className="group bg-white rounded-3xl border border-slate-200/90 p-6 sm:p-8 shadow-xs hover:shadow-xl hover:border-indigo-300 transition-all duration-200 cursor-pointer flex flex-col justify-between"
          >
            <div>
              <div className="w-12 h-12 rounded-2xl bg-sky-50 text-sky-600 flex items-center justify-center text-2xl font-bold mb-5 group-hover:scale-105 transition-transform">
                ✈️
              </div>
              <h3 className="text-xl font-bold text-slate-900 group-hover:text-indigo-600 transition mb-2">
                Flights
              </h3>
              <p className="text-slate-500 text-xs sm:text-sm leading-relaxed mb-6">
                Search real-time airline airfares, direct routes, and cheaper dates across major carriers.
              </p>
            </div>
            <div className="inline-flex items-center text-xs sm:text-sm font-bold text-indigo-600 group-hover:gap-2 gap-1.5 transition-all">
              <span>Compare Flights</span>
              <svg className="w-4 h-4 shrink-0 transition-transform group-hover:translate-x-1" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M9 5l7 7-7 7" />
              </svg>
            </div>
          </div>

          {/* Card 3: Rides */}
          <div
            onClick={() => navigate(ROUTES.RIDES)}
            role="button"
            tabIndex={0}
            onKeyDown={(e) => e.key === 'Enter' && navigate(ROUTES.RIDES)}
            className="group bg-white rounded-3xl border border-slate-200/90 p-6 sm:p-8 shadow-xs hover:shadow-xl hover:border-indigo-300 transition-all duration-200 cursor-pointer flex flex-col justify-between"
          >
            <div>
              <div className="w-12 h-12 rounded-2xl bg-emerald-50 text-emerald-600 flex items-center justify-center text-2xl font-bold mb-5 group-hover:scale-105 transition-transform">
                🚗
              </div>
              <h3 className="text-xl font-bold text-slate-900 group-hover:text-indigo-600 transition mb-2">
                Rides
              </h3>
              <p className="text-slate-500 text-xs sm:text-sm leading-relaxed mb-6">
                Compare live cab, auto, and bike fares and instant ETAs between Uber, Ola, and Rapido.
              </p>
            </div>
            <div className="inline-flex items-center text-xs sm:text-sm font-bold text-indigo-600 group-hover:gap-2 gap-1.5 transition-all">
              <span>Compare Rides</span>
              <svg className="w-4 h-4 shrink-0 transition-transform group-hover:translate-x-1" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M9 5l7 7-7 7" />
              </svg>
            </div>
          </div>

        </div>
      </section>

      {/* Today's Smart Deals Section (Preserved) */}
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
        <SmartDealsSection />
      </div>

      {/* Value Proposition Strip (Preserved) */}
      <section className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
        <div className="bg-slate-900 rounded-3xl p-6 sm:p-10 text-white grid grid-cols-1 md:grid-cols-3 gap-6 sm:gap-8 text-center sm:text-left">
          <div className="space-y-1.5">
            <div className="text-2xl sm:text-3xl font-black text-indigo-400">100%</div>
            <div className="font-bold text-sm sm:text-base">Transparent Comparison</div>
            <p className="text-xs text-slate-400">Directly aggregated with no hidden markups or biased rankings.</p>
          </div>
          <div className="space-y-1.5">
            <div className="text-2xl sm:text-3xl font-black text-emerald-400">₹14,500+</div>
            <div className="font-bold text-sm sm:text-base">Avg. Yearly Savings</div>
            <p className="text-xs text-slate-400">Saved per household by checking multiple merchants before buying.</p>
          </div>
          <div className="space-y-1.5">
            <div className="text-2xl sm:text-3xl font-black text-amber-400">Instant</div>
            <div className="font-bold text-sm sm:text-base">Real-Time Aggregation</div>
            <p className="text-xs text-slate-400">Concurrent multi-provider queries returning true effective rates.</p>
          </div>
        </div>
      </section>

    </div>
  );
};

export default HomePage;
