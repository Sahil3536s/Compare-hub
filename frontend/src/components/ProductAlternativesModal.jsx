import React, { useState, useEffect } from 'react';
import axios from 'axios';

export const ProductAlternativesModal = ({ isOpen, onClose, offer }) => {
  const [alternatives, setAlternatives] = useState([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);

  useEffect(() => {
    if (isOpen && offer) {
      fetchAlternatives();
    }
  }, [isOpen, offer]);

  const fetchAlternatives = async () => {
    setLoading(true);
    setError(null);
    try {
      const response = await axios.get('/api/products/alternatives', {
        params: {
          productName: offer.productName,
          price: offer.price,
          category: offer.category || 'Smartphones',
          brand: offer.brand || 'Generic',
          limit: 4,
        },
      });
      if (response.data && response.data.alternatives) {
        setAlternatives(response.data.alternatives);
      }
    } catch (err) {
      console.error('Failed to load product alternatives:', err);
      setError('Could not load alternatives at this time.');
    } finally {
      setLoading(false);
    }
  };

  if (!isOpen) return null;

  const getCategoryBadge = (categoryType) => {
    switch (categoryType) {
      case 'CHEAPER_ALTERNATIVE':
        return (
          <span className="px-2.5 py-1 rounded-lg text-xs font-black bg-emerald-100 text-emerald-800 border border-emerald-300 flex items-center gap-1 shadow-2xs">
            <span>??</span> Cheaper Option
          </span>
        );
      case 'BETTER_VALUE':
        return (
          <span className="px-2.5 py-1 rounded-lg text-xs font-black bg-amber-100 text-amber-900 border border-amber-300 flex items-center gap-1 shadow-2xs">
            <span>??</span> Best Value Pick
          </span>
        );
      case 'SIMILAR_PRICE_BETTER_FEATURE':
        return (
          <span className="px-2.5 py-1 rounded-lg text-xs font-black bg-indigo-100 text-indigo-900 border border-indigo-300 flex items-center gap-1 shadow-2xs">
            <span>?</span> Feature Upgrade
          </span>
        );
      case 'PREMIUM_ALTERNATIVE':
        return (
          <span className="px-2.5 py-1 rounded-lg text-xs font-black bg-purple-100 text-purple-900 border border-purple-300 flex items-center gap-1 shadow-2xs">
            <span>?</span> Premium Option
          </span>
        );
      default:
        return (
          <span className="px-2.5 py-1 rounded-lg text-xs font-bold bg-slate-100 text-slate-800 border border-slate-300">
            Alternative Pick
          </span>
        );
    }
  };

  return (
    <div
      className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-slate-900/60 backdrop-blur-xs animate-fadeIn"
      data-testid="product-alternatives-modal"
    >
      <div className="bg-white rounded-3xl border border-slate-200 shadow-2xl max-w-3xl w-full max-h-[90vh] flex flex-col overflow-hidden">
        {/* Modal Header */}
        <div className="p-5 sm:p-6 border-b border-slate-100 flex items-start justify-between bg-slate-50/80">
          <div>
            <div className="flex items-center gap-2">
              <span className="text-xs font-black text-indigo-600 uppercase tracking-wider">
                Smart Recommendations
              </span>
              <span className="px-2 py-0.5 rounded-full text-[10px] font-bold bg-indigo-100 text-indigo-800">
                Verified Specs
              </span>
            </div>
            <h3 className="text-lg sm:text-xl font-black text-slate-900 mt-1">
              Consider These Alternatives
            </h3>
            <p className="text-xs text-slate-500 mt-0.5">
              Comparing alternatives against{' '}
              <span className="font-bold text-slate-700">{offer?.productName}</span> (?{Number(offer?.price || 0).toLocaleString('en-IN')})
            </p>
          </div>

          <button
            onClick={onClose}
            className="p-2 text-slate-400 hover:text-slate-600 rounded-full hover:bg-slate-200 transition cursor-pointer"
            aria-label="Close modal"
            data-testid="close-alternatives-modal"
          >
            <svg className="w-5 h-5" fill="none" viewBox="0 0 24 24" stroke="currentColor">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M6 18L18 6M6 6l12 12" />
            </svg>
          </button>
        </div>

        {/* Modal Body */}
        <div className="p-5 sm:p-6 overflow-y-auto flex-1 space-y-4">
          {loading && (
            <div className="py-12 flex flex-col items-center justify-center gap-3 text-slate-500" data-testid="alternatives-loading">
              <div className="w-8 h-8 border-4 border-indigo-600 border-t-transparent rounded-full animate-spin"></div>
              <span className="text-xs font-medium">Finding and comparing top alternatives...</span>
            </div>
          )}

          {error && (
            <div className="p-4 rounded-2xl bg-rose-50 border border-rose-200 text-rose-700 text-xs font-medium">
              {error}
            </div>
          )}

          {!loading && !error && alternatives.length === 0 && (
            <div className="py-12 text-center text-slate-500 text-xs" data-testid="no-alternatives-message">
              No direct alternatives found for this specific specification range.
            </div>
          )}

          {!loading && !error && alternatives.length > 0 && (
            <div className="space-y-4" data-testid="alternatives-list">
              {alternatives.map((alt) => {
                const isCheaper = alt.priceDifference && Number(alt.priceDifference) < 0;
                const absDiff = Math.abs(Number(alt.priceDifference || 0));

                return (
                  <div
                    key={alt.id}
                    className="p-4 sm:p-5 rounded-2xl border border-slate-200 bg-white hover:border-indigo-300 hover:shadow-md transition-all space-y-3"
                    data-testid="alternative-item"
                  >
                    {/* Top Tier & Price Diff Header */}
                    <div className="flex flex-wrap items-center justify-between gap-2">
                      <div className="flex items-center gap-2">
                        {getCategoryBadge(alt.categoryType)}
                        {alt.similarityScore && (
                          <span className="text-[10px] font-bold text-slate-500 bg-slate-100 px-2 py-0.5 rounded-md">
                            {alt.similarityScore}% Match
                          </span>
                        )}
                      </div>

                      <div className="flex items-center gap-2">
                        {isCheaper ? (
                          <span className="px-2.5 py-1 rounded-lg text-xs font-black bg-emerald-50 text-emerald-700 border border-emerald-200 font-mono">
                            -?{absDiff.toLocaleString('en-IN')} cheaper ({Math.abs(alt.priceDifferencePercent || 0).toFixed(1)}%)
                          </span>
                        ) : Number(alt.priceDifference) > 0 ? (
                          <span className="px-2.5 py-1 rounded-lg text-xs font-bold bg-slate-100 text-slate-700 border border-slate-200 font-mono">
                            +?{absDiff.toLocaleString('en-IN')} ({Math.abs(alt.priceDifferencePercent || 0).toFixed(1)}% higher)
                          </span>
                        ) : (
                          <span className="px-2.5 py-1 rounded-lg text-xs font-bold bg-slate-100 text-slate-700 border border-slate-200">
                            Same Price
                          </span>
                        )}
                      </div>
                    </div>

                    {/* Product Name, Image & Specs */}
                    <div className="flex flex-col sm:flex-row items-start sm:items-center justify-between gap-4">
                      <div className="flex items-center gap-3">
                        {alt.imageUrl && (
                          <img
                            src={alt.imageUrl}
                            alt={alt.productName}
                            className="w-14 h-14 object-contain rounded-xl bg-slate-50 p-1 border border-slate-200 shrink-0"
                          />
                        )}
                        <div>
                          <span className="text-[10px] font-bold text-indigo-600 uppercase tracking-wider block">
                            {alt.brand} • {alt.merchant}
                          </span>
                          <h4 className="font-bold text-sm text-slate-900">
                            {alt.productName}
                          </h4>
                          <div className="flex items-center gap-2 mt-1 text-xs text-slate-500">
                            <span className="font-bold text-amber-500">? {alt.rating || 4.5}</span>
                            <span>•</span>
                            <span className="font-mono font-extrabold text-slate-900">
                              ?{Number(alt.price).toLocaleString('en-IN')}
                            </span>
                          </div>
                        </div>
                      </div>

                      <a
                        href={alt.productUrl || '#'}
                        target="_blank"
                        rel="noopener noreferrer"
                        className="px-4 py-2 rounded-xl text-xs font-bold bg-slate-900 hover:bg-slate-800 text-white transition shrink-0 self-end sm:self-auto cursor-pointer"
                      >
                        View Deal ?
                      </a>
                    </div>

                    {/* Verified Highlights */}
                    {alt.highlights && alt.highlights.length > 0 && (
                      <div className="pt-2 border-t border-slate-100 space-y-1">
                        <span className="text-[10px] font-bold text-slate-400 uppercase tracking-wider block">
                          Why Consider This?
                        </span>
                        <div className="grid grid-cols-1 sm:grid-cols-2 gap-1.5 text-xs text-slate-700">
                          {alt.highlights.map((h, i) => (
                            <div key={i} className="flex items-start gap-1.5">
                              <span className="text-emerald-500 font-bold shrink-0">?</span>
                              <span>{h}</span>
                            </div>
                          ))}
                        </div>
                      </div>
                    )}
                  </div>
                );
              })}
            </div>
          )}
        </div>

        {/* Modal Footer */}
        <div className="p-4 border-t border-slate-100 bg-slate-50 text-[11px] text-slate-400 flex items-center justify-between">
          <span>?? Comparisons are based strictly on verified hardware specifications and prices.</span>
          <button
            onClick={onClose}
            className="px-4 py-1.5 rounded-xl font-bold text-slate-600 hover:bg-slate-200 transition cursor-pointer"
          >
            Close
          </button>
        </div>
      </div>
    </div>
  );
};

export default ProductAlternativesModal;
