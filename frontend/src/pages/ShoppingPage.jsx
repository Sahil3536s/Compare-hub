import React, { useState, useEffect, useCallback } from 'react';
import { useSearchParams } from 'react-router-dom';
import { searchProducts } from '../services/productService';
import SearchBar from '../components/SearchBar';
import ProductOfferCard from '../components/ProductOfferCard';
import ProductFilterPanel from '../components/shopping/ProductFilterPanel';
import PriceHistoryModal from '../components/PriceHistoryModal';
import ProductComparisonModal from '../components/ProductComparisonModal';
import PriceAlertModal from '../components/PriceAlertModal';
import AuthModal from '../components/AuthModal';
import { useAuth } from '../context/AuthContext';
import AiRecommendationCard from '../components/AiRecommendationCard';
import RankingExplanationBanner from '../components/RankingExplanationBanner';
import LoadingSkeleton from '../components/LoadingSkeleton';
import EmptyState from '../components/EmptyState';
import ErrorState from '../components/ErrorState';
import PersonalizedRankingToolbar from '../components/PersonalizedRankingToolbar';
import { saveProduct } from '../services/savedService';
import { recordSearchHistory } from '../services/historyService';

export const ShoppingPage = () => {
  const [searchParams, setSearchParams] = useSearchParams();
  const initialQuery = searchParams.get('q') || '';

  const [query, setQuery] = useState(initialQuery);
  const [selectedCategory, setSelectedCategory] = useState('All Categories');
  const [selectedMerchant, setSelectedMerchant] = useState('all'); // 'all' | 'Amazon' | 'Flipkart' | 'Croma'
  const [selectedBrand, setSelectedBrand] = useState('all');
  const [minPrice, setMinPrice] = useState('');
  const [maxPrice, setMaxPrice] = useState(200000);
  const [selectedRating, setSelectedRating] = useState('');
  const [inStockOnly, setInStockOnly] = useState(false);
  const [selectedRam, setSelectedRam] = useState('all');
  const [selectedStorage, setSelectedStorage] = useState('all');
  const [selectedDelivery, setSelectedDelivery] = useState('all');

  // 4 Ranking Tabs: 'best' (Best Value) | 'price_asc' (Cheapest) | 'rating' (Highest Rated) | 'fastest_delivery' (Fastest Delivery)
  const [sortBy, setSortBy] = useState('best');

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
  const [failedProviders, setFailedProviders] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  const { isAuthenticated } = useAuth();
  const [authModalOpen, setAuthModalOpen] = useState(false);

  const [comparedOfferKeys, setComparedOfferKeys] = useState([]);

  // Price History Modal state
  const [historyModalOpen, setHistoryModalOpen] = useState(false);
  const [selectedProductForHistory, setSelectedProductForHistory] = useState(null);

  // Side-by-Side Product Comparison Modal state
  const [comparisonModalOpen, setComparisonModalOpen] = useState(false);
  const [selectedOfferForComparison, setSelectedOfferForComparison] = useState(null);

  // Price Alert Modal state
  const [priceAlertModalOpen, setPriceAlertModalOpen] = useState(false);
  const [selectedOfferForAlert, setSelectedOfferForAlert] = useState(null);

  const handleOpenPriceHistory = (offer) => {
    setSelectedProductForHistory(offer);
    setHistoryModalOpen(true);
  };

  const handleOpenComparison = (offer) => {
    setSelectedOfferForComparison(offer);
    setComparisonModalOpen(true);
    if (offer && offer.productName) {
      recordSearchHistory(
        offer.productName,
        'PRODUCT_COMPARE',
        `${offer.merchant || 'Multi-Store'} Comparison`,
        `/shopping?q=${encodeURIComponent(offer.productName)}`
      );
    }
  };

  const handleSaveOffer = async (offer) => {
    if (!isAuthenticated) {
      setAuthModalOpen(true);
      return;
    }
    try {
      await saveProduct({
        productId: offer.productId,
        savedPrice: offer.effectivePrice || offer.price,
        savedMerchant: offer.merchant,
      });
    } catch (err) {
      console.error('Failed to save product:', err);
    }
  };

  const handleOpenPriceAlert = (offer) => {
    if (!isAuthenticated) {
      setAuthModalOpen(true);
      return;
    }
    setSelectedOfferForAlert(offer);
    setPriceAlertModalOpen(true);
  };

  const handleCompareToggle = (offer) => {
    const key = `${offer.merchant}-${offer.productName}`;
    setComparedOfferKeys((prev) =>
      prev.includes(key) ? prev.filter((k) => k !== key) : [...prev, key]
    );
    handleOpenComparison(offer);
  };

  const activeFilterCount =
    (selectedBrand !== 'all' ? 1 : 0) +
    (selectedRating ? 1 : 0) +
    (inStockOnly ? 1 : 0) +
    (selectedRam !== 'all' ? 1 : 0) +
    (selectedStorage !== 'all' ? 1 : 0) +
    (selectedDelivery !== 'all' ? 1 : 0) +
    (selectedMerchant !== 'all' ? 1 : 0) +
    (selectedCategory !== 'All Categories' ? 1 : 0) +
    (minPrice !== '' && minPrice > 0 || maxPrice < 200000 ? 1 : 0);

  const fetchOffers = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      const data = await searchProducts({
        query: query.trim(),
        category: selectedCategory,
        merchant: selectedMerchant,
        brand: selectedBrand,
        minPrice: minPrice !== '' && minPrice > 0 ? minPrice : '',
        maxPrice: maxPrice,
        inStock: inStockOnly ? true : '',
        sortBy: sortBy,
        minRating: selectedRating,
        ram: selectedRam,
        storage: selectedStorage,
        delivery: selectedDelivery,
      });

      setOffers(data.offers || []);
      setTotalOffers(data.totalOffers || (data.offers ? data.offers.length : 0));
      setCheapestPrice(data.cheapestPrice);
      setCheapestMerchant(data.cheapestMerchant);
      setAiRecommendation(data.aiRecommendation || null);
      setRankingSummary(data.rankingSummary || null);
      setFailedProviders(data.failedProviders || []);
    } catch (err) {
      console.error('Failed to load shopping offers:', err);
      setError(err.message || 'Unable to connect to shopping comparison engine');
    } finally {
      setLoading(false);
    }
  }, [
    query,
    selectedCategory,
    selectedMerchant,
    selectedBrand,
    minPrice,
    maxPrice,
    inStockOnly,
    sortBy,
    selectedRating,
    selectedRam,
    selectedStorage,
    selectedDelivery,
  ]);

  useEffect(() => {
    fetchOffers();
  }, [fetchOffers]);

  const handleSearch = (newQuery) => {
    setQuery(newQuery);
    if (newQuery) {
      setSearchParams({ q: newQuery });
      recordSearchHistory(newQuery, 'SHOPPING', 'Product Search', `/shopping?q=${encodeURIComponent(newQuery)}`);
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
    setSelectedCategory('All Categories');
    setSelectedMerchant('all');
    setSelectedBrand('all');
    setMinPrice('');
    setMaxPrice(200000);
    setSelectedRating('');
    setInStockOnly(false);
    setSelectedRam('all');
    setSelectedStorage('all');
    setSelectedDelivery('all');
    setSortBy('best');
  };

  const rankingTabs = [
    { id: 'price_asc', label: 'Cheapest', icon: '🏷️' },
    { id: 'best', label: 'Best Value', icon: '🏆' },
    { id: 'rating', label: 'Highest Rated', icon: '⭐' },
    { id: 'fastest_delivery', label: 'Fastest Delivery', icon: '⚡' },
  ];

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

      {/* Partial Provider Outage Notice */}
      {failedProviders && failedProviders.length > 0 && (
        <div className="bg-amber-50 border border-amber-200/90 rounded-2xl p-4 flex items-start gap-3 shadow-xs">
          <span className="text-xl shrink-0">⚠️</span>
          <div className="text-xs sm:text-sm text-amber-900">
            <div className="font-bold">Some stores couldn&apos;t be reached. Showing available results.</div>
            <div className="text-amber-800/90 text-xs mt-0.5">
              Unable to reach {failedProviders.join(', ')} in time. Displaying live verified offers from active stores without interruption.
            </div>
          </div>
        </div>
      )}

      {/* Mobile Filter Trigger Bar */}
      <div className="lg:hidden flex items-center justify-between bg-white p-3.5 rounded-2xl border border-slate-200 shadow-xs">
        <button
          type="button"
          onClick={() => setMobileFiltersOpen(true)}
          className="flex items-center gap-2 text-xs font-bold text-slate-800 bg-slate-100 hover:bg-slate-200 px-4 py-2 rounded-xl transition cursor-pointer min-h-[44px]"
          aria-expanded={mobileFiltersOpen}
          aria-label="Open filters drawer"
        >
          <svg className="w-4 h-4 text-indigo-600" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M3 4a1 1 0 011-1h16a1 1 0 011 1v2.586a1 1 0 01-.293.707l-6.414 6.414a1 1 0 00-.293.707V17l-4 4v-6.586a1 1 0 00-.293-.707L3.293 7.293A1 1 0 013 6.586V4z" />
          </svg>
          <span>Filters</span>
          {activeFilterCount > 0 && (
            <span className="w-5 h-5 rounded-full bg-indigo-600 text-white text-[10px] font-bold flex items-center justify-center">
              {activeFilterCount}
            </span>
          )}
        </button>
        <span className="text-xs font-semibold text-slate-500 font-mono">
          {offers.length} offers found
        </span>
      </div>

      {/* Mobile Filter Drawer / Bottom Sheet */}
      {mobileFiltersOpen && (
        <div className="fixed inset-0 z-50 lg:hidden overflow-hidden" role="dialog" aria-modal="true">
          {/* Backdrop */}
          <div
            className="fixed inset-0 bg-slate-900/60 backdrop-blur-xs transition-opacity"
            onClick={() => setMobileFiltersOpen(false)}
          />

          {/* Drawer Sheet */}
          <div className="fixed inset-y-0 right-0 max-w-full flex pl-8">
            <div className="w-screen max-w-md bg-white shadow-2xl flex flex-col justify-between">
              {/* Drawer Header */}
              <div className="p-4 sm:p-5 border-b border-slate-100 flex items-center justify-between">
                <div className="flex items-center gap-2">
                  <span className="text-lg">🎛️</span>
                  <h2 className="font-extrabold text-base text-slate-900">Filters</h2>
                  {activeFilterCount > 0 && (
                    <span className="px-2 py-0.5 rounded-full text-[10px] font-bold bg-indigo-100 text-indigo-700">
                      {activeFilterCount} active
                    </span>
                  )}
                </div>
                <button
                  type="button"
                  onClick={() => setMobileFiltersOpen(false)}
                  className="p-2 rounded-xl text-slate-400 hover:text-slate-600 hover:bg-slate-100 cursor-pointer min-h-[44px] min-w-[44px] flex items-center justify-center"
                  aria-label="Close filters"
                >
                  ✕
                </button>
              </div>

              {/* Scrollable Filters Body */}
              <div className="p-5 overflow-y-auto space-y-6 flex-1">
                <ProductFilterPanel
                  selectedCategory={selectedCategory}
                  setSelectedCategory={setSelectedCategory}
                  selectedMerchant={selectedMerchant}
                  setSelectedMerchant={setSelectedMerchant}
                  selectedBrand={selectedBrand}
                  setSelectedBrand={setSelectedBrand}
                  minPrice={minPrice}
                  setMinPrice={setMinPrice}
                  maxPrice={maxPrice}
                  setMaxPrice={setMaxPrice}
                  selectedRating={selectedRating}
                  setSelectedRating={setSelectedRating}
                  inStockOnly={inStockOnly}
                  setInStockOnly={setInStockOnly}
                  selectedRam={selectedRam}
                  setSelectedRam={setSelectedRam}
                  selectedStorage={selectedStorage}
                  setSelectedStorage={setSelectedStorage}
                  selectedDelivery={selectedDelivery}
                  setSelectedDelivery={setSelectedDelivery}
                  onReset={resetFilters}
                  activeFilterCount={activeFilterCount}
                />
              </div>

              {/* Drawer Footer with Clear Filters and Apply Filters */}
              <div className="p-4 border-t border-slate-100 bg-slate-50 flex items-center gap-3">
                <button
                  type="button"
                  onClick={resetFilters}
                  className="flex-1 py-3 px-4 rounded-xl text-xs font-bold bg-white text-slate-700 border border-slate-200 hover:bg-slate-100 transition cursor-pointer min-h-[44px]"
                >
                  Clear Filters
                </button>
                <button
                  type="button"
                  onClick={() => {
                    setMobileFiltersOpen(false);
                    fetchOffers();
                  }}
                  className="flex-1 py-3 px-4 rounded-xl text-xs font-bold bg-indigo-600 text-white hover:bg-indigo-700 shadow-xs transition cursor-pointer min-h-[44px]"
                >
                  Apply Filters
                </button>
              </div>
            </div>
          </div>
        </div>
      )}

      {/* Main Grid Layout: LEFT Filters + RIGHT Product Results */}
      <div className="grid grid-cols-1 lg:grid-cols-4 gap-6 sm:gap-8 items-start">
        
        {/* LEFT: Desktop Sticky Filter Sidebar */}
        <aside className="hidden lg:block bg-white p-5 sm:p-6 rounded-3xl border border-slate-200 shadow-xs lg:sticky lg:top-24">
          <ProductFilterPanel
            selectedCategory={selectedCategory}
            setSelectedCategory={setSelectedCategory}
            selectedMerchant={selectedMerchant}
            setSelectedMerchant={setSelectedMerchant}
            selectedBrand={selectedBrand}
            setSelectedBrand={setSelectedBrand}
            minPrice={minPrice}
            setMinPrice={setMinPrice}
            maxPrice={maxPrice}
            setMaxPrice={setMaxPrice}
            selectedRating={selectedRating}
            setSelectedRating={setSelectedRating}
            inStockOnly={inStockOnly}
            setInStockOnly={setInStockOnly}
            selectedRam={selectedRam}
            setSelectedRam={setSelectedRam}
            selectedStorage={selectedStorage}
            setSelectedStorage={setSelectedStorage}
            selectedDelivery={selectedDelivery}
            setSelectedDelivery={setSelectedDelivery}
            onReset={resetFilters}
            activeFilterCount={activeFilterCount}
          />
        </aside>

        {/* RIGHT: Product Results Feed */}
        <main className="lg:col-span-3 space-y-6 min-w-0">
          
          {/* Top Results Header with Query, Count & Backend Ranking Tabs */}
          <div className="bg-white p-4 sm:p-5 rounded-2xl border border-slate-200 shadow-xs space-y-4">
            
            {/* Query & Result Count Display */}
            <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-2">
              <div>
                <div className="text-xs sm:text-sm text-slate-600">
                  Showing <strong className="text-slate-900 font-bold">{offers.length}</strong> offers
                  {query && <span> for &ldquo;<strong className="text-indigo-600">{query}</strong>&rdquo;</span>}
                  {selectedMerchant !== 'all' && <span> on <strong className="text-indigo-600">{selectedMerchant}</strong></span>}
                </div>
                <div className="text-[11px] text-slate-400 mt-0.5">
                  {totalOffers > 0 && `Total matched: ${totalOffers} across active providers`}
                </div>
              </div>

              {/* Fallback Dropdown for Extra Sort Options */}
              <div className="flex items-center gap-1.5 self-start sm:self-auto">
                <span className="text-[11px] font-bold uppercase tracking-wider text-slate-400">Sort:</span>
                <select
                  value={sortBy}
                  onChange={(e) => setSortBy(e.target.value)}
                  aria-label="Sort products by"
                  className="bg-slate-50 border border-slate-200 text-xs font-semibold text-slate-800 rounded-xl px-2.5 py-1.5 focus:outline-hidden focus:border-indigo-500 cursor-pointer min-h-[36px]"
                >
                  <option value="best">🏆 Best Value</option>
                  <option value="price_asc">🏷️ Cheapest</option>
                  <option value="rating">⭐ Highest Rated</option>
                  <option value="fastest_delivery">⚡ Fastest Delivery</option>
                  <option value="price_desc">Price: High to Low</option>
                  <option value="discount">Discount Percentage</option>
                  <option value="effective_price_asc">Effective Net Price</option>
                </select>
              </div>
            </div>

            {/* 4 Ranking Tabs: Cheapest, Best Value, Highest Rated, Fastest Delivery */}
            <div className="pt-3 border-t border-slate-100">
              <div className="flex items-center gap-2 overflow-x-auto pb-1 no-scrollbar" role="tablist" aria-label="Ranking tabs">
                {rankingTabs.map((tab) => {
                  const isActive = sortBy === tab.id;
                  return (
                    <button
                      key={tab.id}
                      role="tab"
                      aria-selected={isActive}
                      onClick={() => setSortBy(tab.id)}
                      className={`px-3.5 py-2 rounded-xl text-xs sm:text-sm font-bold flex items-center gap-1.5 transition cursor-pointer whitespace-nowrap border shrink-0 ${
                        isActive
                          ? 'bg-indigo-600 text-white border-indigo-600 shadow-sm ring-2 ring-indigo-500/20'
                          : 'bg-slate-50 text-slate-700 border-slate-200 hover:bg-slate-100 hover:text-slate-900'
                      }`}
                    >
                      <span>{tab.icon}</span>
                      <span>{tab.label}</span>
                    </button>
                  );
                })}
              </div>
            </div>

          </div>

          {/* Product Feed / Error / Skeleton / Empty State */}
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
              {offers.map((offer, idx) => {
                const cardKey = `${offer.merchant}-${offer.productName}-${idx}`;
                const isCompared = comparedOfferKeys.includes(`${offer.merchant}-${offer.productName}`);
                return (
                  <ProductOfferCard
                    key={cardKey}
                    offer={offer}
                    onSave={handleSaveOffer}
                    onViewPriceHistory={handleOpenPriceHistory}
                    onCompare={handleCompareToggle}
                    onSetPriceAlert={handleOpenPriceAlert}
                    isCompared={isCompared}
                  />
                );
              })}
            </div>
          )}

        </main>

      </div>

      {/* Price History Modal */}
      <PriceHistoryModal
        isOpen={historyModalOpen}
        onClose={() => setHistoryModalOpen(false)}
        product={selectedProductForHistory}
      />

      {/* Side-by-Side Product Comparison Modal */}
      <ProductComparisonModal
        isOpen={comparisonModalOpen}
        onClose={() => setComparisonModalOpen(false)}
        selectedOffer={selectedOfferForComparison}
        allOffers={offers}
        onSetPriceAlert={handleOpenPriceAlert}
      />

      {/* Price Alert Modal */}
      <PriceAlertModal
        isOpen={priceAlertModalOpen}
        onClose={() => setPriceAlertModalOpen(false)}
        product={selectedOfferForAlert}
        onOpenAuthModal={() => setAuthModalOpen(true)}
      />

      {/* Auth Modal for Unauthenticated Users */}
      <AuthModal
        isOpen={authModalOpen}
        onClose={() => setAuthModalOpen(false)}
      />

    </div>
  );
};

export default ShoppingPage;
