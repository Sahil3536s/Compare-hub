import React, { useState, useEffect, useCallback } from 'react';
import { useSearchParams } from 'react-router-dom';
import { searchProducts } from '../services/productService';
import { PRODUCT_CATEGORIES } from '../utils/constants';
import SearchBar from '../components/SearchBar';
import ProductOfferCard from '../components/ProductOfferCard';
import PriceHistoryModal from '../components/PriceHistoryModal';
import AiRecommendationCard from '../components/AiRecommendationCard';
import RankingExplanationBanner from '../components/RankingExplanationBanner';
import LoadingSkeleton from '../components/LoadingSkeleton';
import EmptyState from '../components/EmptyState';
import ErrorState from '../components/ErrorState';
import PersonalizedRankingToolbar from '../components/PersonalizedRankingToolbar';

export const ShoppingPage = () => {
  const [searchParams, setSearchParams] = useSearchParams();
  const initialQuery = searchParams.get('q') || '';

  const [query, setQuery] = useState(initialQuery);
  const [selectedCategory, setSelectedCategory] = useState('All Categories');
  const [selectedMerchant, setSelectedMerchant] = useState('all'); // 'all' | 'Amazon' | 'Flipkart' | 'Croma'
  const [selectedBrand, setSelectedBrand] = useState('all'); // 'all' | 'Apple' | 'Sony' | 'Samsung'
  const [maxPrice, setMaxPrice] = useState(200000);
  const [inStockOnly, setInStockOnly] = useState(false);
  const [sortBy, setSortBy] = useState('best'); // 'best' | 'effective_price_asc' | 'price_asc' | 'price_desc' | 'rating' | 'discount'

  const [rankingWeights, setRankingWeights] = useState({
    price: 40,
    rating: 20,
    discount: 15,
    delivery: 15,
    reliability: 10,
  });
  const [rankingPreset, setRankingPreset] = useState('BALANCED');

  const [mobileFiltersOpen, setMobileFiltersOpen] = useState(false);

  const [offers, setOffers] = useState([]);
  const [totalOffers, setTotalOffers] = useState(0);
  const [cheapestPrice, setCheapestPrice] = useState(null);
  const [cheapestMerchant, setCheapestMerchant] = useState(null);
  const [aiRecommendation, setAiRecommendation] = useState(null);
  const [rankingSummary, setRankingSummary] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  // Price History Modal state
  const [historyModalOpen, setHistoryModalOpen] = useState(false);
  const [selectedProductForHistory, setSelectedProductForHistory] = useState(null);

  const handleOpenPriceHistory = (offer) => {
    setSelectedProductForHistory(offer);
    setHistoryModalOpen(true);
  };

  const fetchOffers = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      const data = await searchProducts({
        query: query.trim(),
        category: selectedCategory,
        merchant: selectedMerchant,
        brand: selectedBrand,
        maxPrice: maxPrice,
        inStock: inStockOnly ? true : '',
        sortBy: sortBy,
      });

      setOffers(data.offers || []);
      setTotalOffers(data.totalOffers || 0);
      setCheapestPrice(data.cheapestPrice);
      setCheapestMerchant(data.cheapestMerchant);
      setAiRecommendation(data.aiRecommendation || null);
      setRankingSummary(data.rankingSummary || null);
    } catch (err) {
      console.error('Failed to load shopping offers:', err);
      setError(err.message || 'Unable to connect to shopping comparison engine');
    } finally {
      setLoading(false);
    }
  }, [query, selectedCategory, selectedMerchant, selectedBrand, maxPrice, inStockOnly, sortBy]);

  useEffect(() => {
    fetchOffers();
  }, [fetchOffers]);

  const handleSearch = (newQuery) => {
    setQuery(newQuery);
    if (newQuery) {
      setSearchParams({ q: newQuery });
    } else {
      setSearchParams({});
    }
  };

  const handlePreferenceChange = ({ preset, weights }) => {
    setRankingPreset(preset);
    setRankingWeights(weights);

    // Re-score offers dynamically on frontend for instant feedback
    if (offers && offers.length > 0) {
      const wPrice = (weights.price || 40) / 100;
      const wRating = (weights.rating || 20) / 100;
      const wDiscount = (weights.discount || 15) / 100;
      const wDelivery = (weights.delivery || 15) / 100;
      const wTrust = (weights.reliability || 10) / 100;

      const minP = Math.min(...offers.map((o) => Number(o.effectivePrice || o.price)));
      const maxP = Math.max(...offers.map((o) => Number(o.effectivePrice || o.price)));
      const pRange = maxP - minP;

      const scored = offers.map((offer) => {
        const p = Number(offer.effectivePrice || offer.price);
        const normP = pRange > 0 ? (maxP - p) / pRange : 1.0;
        const normR = (offer.rating || 3.0) / 5.0;
        const normD = (offer.discountPercent || 0) / 100;
        const normDel = offer.delivery?.toLowerCase().includes('same day') || offer.delivery?.toLowerCase().includes('today')
          ? 1.0
          : offer.delivery?.toLowerCase().includes('tomorrow')
          ? 0.85
          : 0.6;
        const normT = offer.inStock ? 0.8 : 0.4;

        const totalScore = Math.round((normP * wPrice + normR * wRating + normD * wDiscount + normDel * wDelivery + normT * wTrust) * 100);

        return {
          ...offer,
          rankingScore: totalScore,
        };
      });

      if (sortBy === 'best') {
        scored.sort((a, b) => (b.rankingScore || 0) - (a.rankingScore || 0));
        if (scored.length > 0) {
          scored[0].isBestValue = true;
          for (let i = 1; i < scored.length; i++) {
            scored[i].isBestValue = false;
          }
        }
      }

      setOffers(scored);
    }
  };

  const resetFilters = () => {
    setQuery('');
    setSelectedCategory('All Categories');
    setSelectedMerchant('all');
    setSelectedBrand('all');
    setMaxPrice(200000);
    setInStockOnly(false);
    setSortBy('best');
    setSearchParams({});
  };

  return (
    <div className="space-y-6 sm:space-y-8 pb-16 min-w-0">
      
      {/* Search Header Banner */}
      <div className="bg-gradient-to-r from-indigo-900 via-indigo-800 to-slate-900 rounded-3xl p-6 sm:p-10 text-white shadow-xl">
        <div className="max-w-3xl space-y-4">
          <div className="inline-flex items-center gap-1.5 px-3 py-1 rounded-full text-xs font-semibold bg-indigo-500/30 text-indigo-200 border border-indigo-400/30">
            <span>🛍️</span>
            <span>Universal Product Comparison</span>
          </div>
          <h1 className="text-2xl sm:text-4xl lg:text-5xl font-black tracking-tight leading-tight">
            Compare Live Prices Across <span className="text-indigo-400">Trusted Stores</span>
          </h1>
          <p className="text-xs sm:text-base text-indigo-100/80 font-normal leading-relaxed">
            Uncover real-time price disparities across Amazon, Flipkart, Croma and more.
          </p>
          <div className="pt-2">
            <SearchBar
              initialQuery={query}
              onSearch={handleSearch}
              placeholder="Search 'iPhone 15', 'Sony WH-1000XM5' or 'MacBook Air'..."
            />
          </div>
        </div>
      </div>

      {/* Personalized Priority Toolbar */}
      <PersonalizedRankingToolbar
        onPreferenceChange={handlePreferenceChange}
        currentPreset={rankingPreset}
        currentWeights={rankingWeights}
      />

      {/* AI Recommendation Banner */}
      {aiRecommendation && (
        <AiRecommendationCard recommendation={aiRecommendation} />
      )}

      {/* Deterministic Ranking Explanation Banner */}
      {rankingSummary && (
        <RankingExplanationBanner rankingSummary={rankingSummary} type="product" />
      )}

      {/* Mobile Filter Drawer Button */}
      <div className="lg:hidden flex items-center justify-between bg-white p-3.5 rounded-2xl border border-slate-200 shadow-xs">
        <button
          type="button"
          onClick={() => setMobileFiltersOpen(!mobileFiltersOpen)}
          className="flex items-center gap-2 text-xs font-bold text-slate-800 bg-slate-100 hover:bg-slate-200 px-4 py-2 rounded-xl transition cursor-pointer min-h-[44px]"
          aria-expanded={mobileFiltersOpen}
          aria-controls="mobile-filter-drawer"
        >
          <svg className="w-4 h-4 text-indigo-600" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M3 4a1 1 0 011-1h16a1 1 0 011 1v2.586a1 1 0 01-.293.707l-6.414 6.414a1 1 0 00-.293.707V17l-4 4v-6.586a1 1 0 00-.293-.707L3.293 7.293A1 1 0 013 6.586V4z" />
          </svg>
          <span>{mobileFiltersOpen ? 'Hide Filters' : 'Filter Products'}</span>
        </button>
        <span className="text-xs font-semibold text-slate-500 font-mono">
          {offers.length} offers found
        </span>
      </div>

      {/* Main Grid Layout: Sidebar Filters + Products Feed */}
      <div className="grid grid-cols-1 lg:grid-cols-4 gap-6 sm:gap-8 items-start">
        
        {/* Left Filter Sidebar */}
        <div
          id="mobile-filter-drawer"
          className={`${
            mobileFiltersOpen ? 'block' : 'hidden'
          } lg:block bg-white p-5 sm:p-6 rounded-3xl border border-slate-200 shadow-xs space-y-6 lg:sticky lg:top-24`}
        >
          <div className="flex items-center justify-between pb-4 border-b border-slate-100">
            <h3 className="font-extrabold text-sm sm:text-base text-slate-900 flex items-center gap-2">
              <span>🎛️</span>
              <span>Filters</span>
            </h3>
            <button
              onClick={resetFilters}
              className="text-xs font-bold text-indigo-600 hover:text-indigo-800 cursor-pointer min-h-[36px] flex items-center"
            >
              Reset All
            </button>
          </div>

          {/* Category Filter */}
          <div className="space-y-3">
            <label className="text-xs font-bold uppercase tracking-wider text-slate-500 block">
              Category
            </label>
            <div className="flex flex-wrap lg:flex-col gap-1.5">
              {PRODUCT_CATEGORIES.map((cat) => (
                <button
                  key={cat}
                  onClick={() => setSelectedCategory(cat)}
                  className={`text-left text-xs font-semibold px-3 py-2 rounded-xl transition cursor-pointer flex items-center justify-between min-h-[36px] ${
                    selectedCategory === cat
                      ? 'bg-indigo-600 text-white shadow-xs font-bold'
                      : 'bg-slate-50 hover:bg-slate-100 text-slate-700'
                  }`}
                >
                  <span>{cat}</span>
                  {selectedCategory === cat && <span className="text-xs">✓</span>}
                </button>
              ))}
            </div>
          </div>

          {/* Merchant Filter */}
          <div className="space-y-3 pt-4 border-t border-slate-100">
            <label className="text-xs font-bold uppercase tracking-wider text-slate-500 block">
              Merchant
            </label>
            <div className="grid grid-cols-2 lg:grid-cols-1 gap-1.5">
              {[
                { id: 'all', label: 'All Stores' },
                { id: 'Amazon', label: 'Amazon' },
                { id: 'Flipkart', label: 'Flipkart' },
                { id: 'Croma', label: 'Croma' },
              ].map((m) => (
                <button
                  key={m.id}
                  onClick={() => setSelectedMerchant(m.id)}
                  className={`text-left text-xs font-semibold px-3 py-2 rounded-xl transition cursor-pointer flex items-center justify-between min-h-[36px] ${
                    selectedMerchant === m.id
                      ? 'bg-slate-900 text-white font-bold shadow-xs'
                      : 'bg-slate-50 hover:bg-slate-100 text-slate-700'
                  }`}
                >
                  <span>{m.label}</span>
                  {selectedMerchant === m.id && <span className="text-xs">✓</span>}
                </button>
              ))}
            </div>
          </div>

          {/* Brand Filter */}
          <div className="space-y-3 pt-4 border-t border-slate-100">
            <label className="text-xs font-bold uppercase tracking-wider text-slate-500 block">
              Brand
            </label>
            <div className="grid grid-cols-2 lg:grid-cols-1 gap-1.5">
              {[
                { id: 'all', label: 'All Brands' },
                { id: 'Apple', label: 'Apple' },
                { id: 'Samsung', label: 'Samsung' },
                { id: 'Sony', label: 'Sony' },
              ].map((b) => (
                <button
                  key={b.id}
                  onClick={() => setSelectedBrand(b.id)}
                  className={`text-left text-xs font-semibold px-3 py-2 rounded-xl transition cursor-pointer flex items-center justify-between min-h-[36px] ${
                    selectedBrand === b.id
                      ? 'bg-indigo-600 text-white font-bold shadow-xs'
                      : 'bg-slate-50 hover:bg-slate-100 text-slate-700'
                  }`}
                >
                  <span>{b.label}</span>
                  {selectedBrand === b.id && <span className="text-xs">✓</span>}
                </button>
              ))}
            </div>
          </div>

          {/* Max Price Range */}
          <div className="space-y-3 pt-4 border-t border-slate-100">
            <div className="flex items-center justify-between">
              <label className="text-xs font-bold uppercase tracking-wider text-slate-500">
                Max Price
              </label>
              <span className="text-xs font-bold text-slate-900 font-mono">
                ₹{maxPrice.toLocaleString('en-IN')}
              </span>
            </div>
            <input
              type="range"
              min="20000"
              max="200000"
              step="5000"
              value={maxPrice}
              onChange={(e) => setMaxPrice(Number(e.target.value))}
              aria-label="Maximum price filter"
              className="w-full accent-indigo-600 cursor-pointer"
            />
          </div>

          {/* In Stock Only */}
          <div className="pt-4 border-t border-slate-100">
            <label className="flex items-center gap-2 text-xs font-semibold text-slate-700 cursor-pointer select-none min-h-[36px]">
              <input
                type="checkbox"
                checked={inStockOnly}
                onChange={(e) => setInStockOnly(e.target.checked)}
                className="rounded text-indigo-600 focus:ring-indigo-500 w-4 h-4 cursor-pointer"
              />
              <span>In Stock Only</span>
            </label>
          </div>
        </div>

        {/* Right Main Product Feed */}
        <div className="lg:col-span-3 space-y-6 min-w-0">
          
          {/* Controls Bar: Count & Sort */}
          <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-3 bg-white p-3.5 sm:p-4 rounded-2xl border border-slate-200 shadow-xs">
            <div className="text-xs sm:text-sm text-slate-600">
              Showing <strong className="text-slate-900 font-bold">{offers.length}</strong> offers
              {query && <span> for &ldquo;<strong>{query}</strong>&rdquo;</span>}
              {selectedMerchant !== 'all' && <span> on <strong className="text-indigo-600">{selectedMerchant}</strong></span>}
            </div>

            <div className="flex items-center gap-2 self-start sm:self-auto">
              <label className="text-xs font-bold uppercase tracking-wider text-slate-500 shrink-0">
                Sort By:
              </label>
              <select
                value={sortBy}
                onChange={(e) => setSortBy(e.target.value)}
                aria-label="Sort products by"
                className="bg-slate-50 border border-slate-200 text-xs sm:text-sm font-semibold text-slate-800 rounded-xl px-3 py-1.5 focus:outline-hidden focus:border-indigo-500 cursor-pointer min-h-[36px]"
              >
                <option value="best">🏆 Best Value (Personalized)</option>
                <option value="effective_price_asc">🏷️ Effective Price: Low to High</option>
                <option value="price_asc">Price: Low to High</option>
                <option value="price_desc">Price: High to Low</option>
                <option value="rating">Rating</option>
                <option value="discount">Discount Percentage</option>
              </select>
            </div>
          </div>

          {/* Product Feed / Error / Skeleton / Empty */}
          {loading ? (
            <LoadingSkeleton type="product-card" count={6} />
          ) : error ? (
            <ErrorState
              title="Shopping Engine Error"
              message={error}
              onRetry={fetchOffers}
            />
          ) : offers.length === 0 ? (
            <EmptyState
              title="No Products Found"
              description="We couldn't find any products matching your filters. Try adjusting your query or price range."
              actionText="Clear All Filters"
              onAction={resetFilters}
            />
          ) : (
            <div className="grid grid-cols-1 sm:grid-cols-2 xl:grid-cols-3 gap-5 sm:gap-6">
              {offers.map((offer, idx) => (
                <ProductOfferCard
                  key={`${offer.merchant}-${offer.productName}-${idx}`}
                  offer={offer}
                  onViewPriceHistory={handleOpenPriceHistory}
                />
              ))}
            </div>
          )}

        </div>

      </div>

      {/* Price History Modal */}
      <PriceHistoryModal
        isOpen={historyModalOpen}
        onClose={() => setHistoryModalOpen(false)}
        product={selectedProductForHistory}
      />

    </div>
  );
};

export default ShoppingPage;
