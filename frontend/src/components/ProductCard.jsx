import React, { useState } from 'react';
import PriceBadge from './PriceBadge';
import MerchantOffer from './MerchantOffer';

export const ProductCard = ({ product, onSave, isSaved = false }) => {
  const [showOffers, setShowOffers] = useState(false);
  const [saved, setSaved] = useState(isSaved);
  const [showPriceHistory, setShowPriceHistory] = useState(false);

  const discountPercent = product.originalPrice && product.originalPrice > product.cheapestPrice
    ? Math.round(((product.originalPrice - product.cheapestPrice) / product.originalPrice) * 100)
    : 0;

  const handleSaveToggle = () => {
    setSaved(!saved);
    if (onSave) onSave(product);
  };

  return (
    <div className="bg-white rounded-2xl border border-slate-200 overflow-hidden shadow-xs hover:shadow-md transition-all duration-200 flex flex-col">
      {/* Top Banner with Image and Floating Badges */}
      <div className="relative bg-slate-50 p-6 flex items-center justify-center min-h-[220px] overflow-hidden group">
        <img
          src={product.image}
          alt={product.title}
          className="max-h-48 w-auto object-contain group-hover:scale-105 transition-transform duration-300"
          loading="lazy"
        />

        {/* Badges on Top Left */}
        <div className="absolute top-3 left-3 flex flex-col gap-1.5 z-10">
          {product.isCheapest && <PriceBadge type="cheapest" text="Cheapest Price" />}
          {product.isBestValue && <PriceBadge type="best-value" text="Best Value" />}
        </div>

        {/* Favorite / Save Button Top Right */}
        <button
          onClick={handleSaveToggle}
          title={saved ? 'Remove from Saved' : 'Save Product'}
          className={`absolute top-3 right-3 p-2 rounded-full backdrop-blur-md transition shadow-xs z-10 ${
            saved
              ? 'bg-rose-50 text-rose-600 border border-rose-200'
              : 'bg-white/80 text-slate-400 hover:text-rose-500 hover:bg-white border border-slate-200'
          }`}
        >
          <svg className="w-4 h-4 fill-current" viewBox="0 0 20 20">
            <path fillRule="evenodd" d="M3.172 5.172a4 4 0 015.656 0L10 6.343l1.172-1.171a4 4 0 115.656 5.656L10 17.657l-6.828-6.829a4 4 0 010-5.656z" clipRule="evenodd" />
          </svg>
        </button>
      </div>

      {/* Main Content Area */}
      <div className="p-5 flex-1 flex flex-col justify-between space-y-4">
        <div>
          <div className="flex items-center justify-between text-xs text-slate-500 mb-1.5">
            <span className="font-medium text-indigo-600 bg-indigo-50 px-2 py-0.5 rounded">
              {product.category}
            </span>
            <div className="flex items-center gap-1 font-semibold text-slate-700">
              <span className="text-amber-500">★</span>
              <span>{product.rating}</span>
              <span className="text-slate-400 font-normal">({product.reviewsCount.toLocaleString()})</span>
            </div>
          </div>

          <h3 className="font-semibold text-base text-slate-900 line-clamp-2 title-font hover:text-indigo-600 transition">
            {product.title}
          </h3>
        </div>

        {/* Price & Lowest Tag */}
        <div className="pt-2 border-t border-slate-100">
          <div className="flex items-baseline justify-between gap-2">
            <div>
              <span className="text-xs text-slate-400 block">Lowest from:</span>
              <div className="text-2xl font-extrabold text-slate-900 tracking-tight">
                ₹{product.cheapestPrice.toLocaleString('en-IN')}
              </div>
            </div>

            {discountPercent > 0 && (
              <div className="text-right">
                <span className="text-xs text-slate-400 line-through block">
                  ₹{product.originalPrice.toLocaleString('en-IN')}
                </span>
                <span className="text-xs font-bold text-emerald-600 bg-emerald-50 px-2 py-0.5 rounded border border-emerald-100">
                  Save {discountPercent}%
                </span>
              </div>
            )}
          </div>

          {/* Lowest in 30 Days indicator */}
          {product.lowest30DaysPrice && (
            <div className="flex items-center justify-between mt-2.5 text-xs text-slate-500 bg-slate-50 px-3 py-1.5 rounded-lg border border-slate-100">
              <span className="flex items-center gap-1">
                <svg className="w-3.5 h-3.5 text-indigo-600" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M13 7h8m0 0v8m0-8l-8 8-4-4-6 6" />
                </svg>
                30-day low: <strong className="text-slate-700">₹{product.lowest30DaysPrice.toLocaleString('en-IN')}</strong>
              </span>

              <button
                onClick={() => setShowPriceHistory(!showPriceHistory)}
                className="text-indigo-600 hover:text-indigo-800 font-semibold cursor-pointer"
              >
                {showPriceHistory ? 'Hide Trend' : 'Price Trend'}
              </button>
            </div>
          )}

          {/* Price History Sparkline Placeholder */}
          {showPriceHistory && product.priceHistory && (
            <div className="mt-3 p-3 bg-slate-900 text-white rounded-xl text-xs space-y-2">
              <div className="flex justify-between items-center text-slate-300">
                <span className="font-semibold text-slate-200">5-Month Price Trend</span>
                <span className="text-emerald-400 font-medium">Currently at lowest</span>
              </div>
              <div className="grid grid-cols-5 gap-2 pt-2 text-center">
                {product.priceHistory.map((item, idx) => (
                  <div key={idx} className="flex flex-col items-center gap-1">
                    <div className="w-full bg-slate-800 rounded-t h-12 flex items-end justify-center pb-1">
                      <div
                        className={`w-3 rounded-xs ${
                          idx === product.priceHistory.length - 1
                            ? 'bg-emerald-400'
                            : 'bg-indigo-400/80'
                        }`}
                        style={{
                          height: `${Math.max(
                            25,
                            Math.round((item.price / product.originalPrice) * 45)
                          )}px`,
                        }}
                      ></div>
                    </div>
                    <span className="text-[10px] text-slate-400">{item.month}</span>
                    <span className="text-[10px] font-mono text-slate-300">₹{(item.price / 1000).toFixed(0)}k</span>
                  </div>
                ))}
              </div>
            </div>
          )}
        </div>

        {/* Merchant Offers Trigger Button */}
        <div className="pt-2">
          <button
            onClick={() => setShowOffers(!showOffers)}
            className="w-full py-2.5 px-4 bg-indigo-50 hover:bg-indigo-100 text-indigo-700 text-xs sm:text-sm font-semibold rounded-xl transition flex items-center justify-center gap-2 cursor-pointer"
          >
            <span>Compare {product.merchantOffers?.length || 0} Stores</span>
            <svg
              className={`w-4 h-4 transform transition-transform ${showOffers ? 'rotate-180' : ''}`}
              fill="none"
              stroke="currentColor"
              viewBox="0 0 24 24"
            >
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M19 9l-7 7-7-7" />
            </svg>
          </button>
        </div>
      </div>

      {/* Expanded Merchant Offers List */}
      {showOffers && product.merchantOffers && (
        <div className="p-4 bg-slate-50/80 border-t border-slate-200 space-y-2.5">
          <div className="text-xs font-semibold text-slate-600 px-1">
            Available Store Offers ({product.merchantOffers.length})
          </div>
          {product.merchantOffers.map((offer) => (
            <MerchantOffer key={offer.id} offer={offer} />
          ))}
        </div>
      )}
    </div>
  );
};

export default ProductCard;
