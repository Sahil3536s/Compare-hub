import React, { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import DemoBadge from './DemoBadge';

export const CanonicalProductCard = ({ product, onSave, isSaved = false }) => {
  const navigate = useNavigate();
  const [saved, setSaved] = useState(isSaved);

  if (!product) return null;

  const {
    productId,
    canonicalTitle = 'Unnamed Product',
    brand,
    category,
    imageUrl,
    rating,
    reviewCount,
    lowestPrice,
    highestPrice,
    cheapestMerchant,
    priceSpread,
    attributes,
    offers = [],
  } = product;

  const handleCardClick = () => {
    if (productId) {
      navigate(`/products/${productId}`);
    }
  };

  const handleSaveToggle = (e) => {
    e.stopPropagation();
    setSaved(!saved);
    if (onSave) onSave(product);
  };

  const rawOffers = Array.isArray(offers) ? offers : [];
  const hasMultipleOffers = rawOffers.length > 1;
  const storeNames = Array.from(new Set(rawOffers.map((o) => o?.merchant || o?.provider).filter(Boolean)));
  const hasDemoOnly = rawOffers.length > 0 && rawOffers.every((o) => !o?.live || o?.dataSource === 'DEMO');

  return (
    <div
      onClick={handleCardClick}
      role="button"
      tabIndex={0}
      onKeyDown={(e) => {
        if (e.key === 'Enter' || e.key === ' ') {
          e.preventDefault();
          handleCardClick();
        }
      }}
      className="bg-white rounded-2xl border border-slate-200 overflow-hidden shadow-xs hover:shadow-lg hover:border-indigo-200 transition-all duration-300 flex flex-col cursor-pointer group focus:outline-hidden focus:ring-2 focus:ring-indigo-500 focus:ring-offset-2"
    >
      {/* Top Media Area */}
      <div className="relative bg-gradient-to-b from-slate-50 to-slate-100/60 p-5 flex items-center justify-center min-h-[200px] overflow-hidden">
        {imageUrl ? (
          <img
            src={imageUrl}
            alt={canonicalTitle}
            className="max-h-44 w-auto object-contain group-hover:scale-105 transition-transform duration-300"
            loading="lazy"
            onError={(e) => {
              e.currentTarget.style.display = 'none';
            }}
          />
        ) : (
          <div className="w-24 h-24 rounded-2xl bg-indigo-50 flex items-center justify-center text-3xl text-indigo-400">
            📦
          </div>
        )}

        {/* Floating Badges */}
        <div className="absolute top-3 left-3 flex flex-col gap-1.5 z-10">
          {storeNames.length > 1 && (
            <span className="bg-indigo-600 text-white text-[11px] font-bold px-2.5 py-1 rounded-full shadow-xs">
              {storeNames.length} Stores
            </span>
          )}
          {hasDemoOnly && <DemoBadge />}
        </div>

        {/* Save to Wishlist Button */}
        <button
          onClick={handleSaveToggle}
          title={saved ? 'Remove from Saved' : 'Save Product'}
          aria-label={saved ? 'Remove from Saved' : 'Save Product'}
          className={`absolute top-3 right-3 p-2 rounded-full backdrop-blur-md transition shadow-xs z-10 ${
            saved
              ? 'bg-rose-50 text-rose-600 border border-rose-200'
              : 'bg-white/80 text-slate-400 hover:text-rose-500 hover:bg-white border border-slate-200'
          }`}
        >
          <svg className="w-4 h-4 fill-current" viewBox="0 0 20 20">
            <path
              fillRule="evenodd"
              d="M3.172 5.172a4 4 0 015.656 0L10 6.343l1.172-1.171a4 4 0 115.656 5.656L10 17.657l-6.828-6.829a4 4 0 010-5.656z"
              clipRule="evenodd"
            />
          </svg>
        </button>
      </div>

      {/* Main Info */}
      <div className="p-5 flex-1 flex flex-col justify-between space-y-4">
        <div>
          {/* Category & Rating */}
          <div className="flex items-center justify-between text-xs text-slate-500 mb-2">
            <div className="flex items-center gap-1.5 flex-wrap">
              {brand && (
                <span className="font-semibold text-slate-700 bg-slate-100 px-2 py-0.5 rounded-md">
                  {brand}
                </span>
              )}
              {category && (
                <span className="text-indigo-600 bg-indigo-50 px-2 py-0.5 rounded-md font-medium">
                  {category}
                </span>
              )}
            </div>

            {rating != null && !isNaN(Number(rating)) && Number(rating) > 0 && (
              <div className="flex items-center gap-1 font-semibold text-slate-700">
                <span className="text-amber-500">★</span>
                <span>{Number(rating).toFixed(1)}</span>
                {reviewCount != null && !isNaN(Number(reviewCount)) && (
                  <span className="text-slate-400 font-normal">
                    ({Number(reviewCount).toLocaleString()})
                  </span>
                )}
              </div>
            )}
          </div>

          {/* Canonical Product Title */}
          <h3 className="font-bold text-base text-slate-900 group-hover:text-indigo-600 transition-colors line-clamp-2 leading-snug">
            {canonicalTitle}
          </h3>

          {/* Quick Spec Pills */}
          {attributes && typeof attributes === 'object' && (
            <div className="flex flex-wrap gap-1.5 mt-2.5">
              {attributes.storage && (
                <span className="text-[11px] font-medium bg-slate-100 text-slate-700 px-2 py-0.5 rounded">
                  {attributes.storage}
                </span>
              )}
              {attributes.ram && (
                <span className="text-[11px] font-medium bg-slate-100 text-slate-700 px-2 py-0.5 rounded">
                  {attributes.ram} RAM
                </span>
              )}
              {attributes.variant && attributes.variant !== 'Standard' && (
                <span className="text-[11px] font-medium bg-slate-100 text-slate-700 px-2 py-0.5 rounded">
                  {attributes.variant}
                </span>
              )}
            </div>
          )}
        </div>

        {/* Pricing Summary */}
        <div className="pt-3 border-t border-slate-100">
          <div className="flex items-baseline justify-between gap-2">
            <div>
              <span className="text-xs text-slate-400 font-medium block">
                {hasMultipleOffers ? 'Lowest from' : 'Price'}
              </span>
              <div className="text-2xl font-black text-slate-900 tracking-tight">
                {lowestPrice != null && !isNaN(Number(lowestPrice))
                  ? `₹${Number(lowestPrice).toLocaleString('en-IN')}`
                  : '—'}
              </div>
            </div>

            {priceSpread != null && !isNaN(Number(priceSpread)) && Number(priceSpread) > 0 && (
              <div className="text-right">
                <span className="text-[11px] font-bold text-emerald-700 bg-emerald-50 border border-emerald-200 px-2 py-0.5 rounded-full inline-block">
                  Save up to ₹{Number(priceSpread).toLocaleString('en-IN')}
                </span>
              </div>
            )}
          </div>

          {/* Merchant presence badges */}
          {storeNames.length > 0 && (
            <div className="mt-3 flex items-center justify-between text-xs text-slate-500">
              <span className="truncate max-w-[200px]">
                {cheapestMerchant ? (
                  <>
                    Best on <strong className="text-slate-800">{cheapestMerchant}</strong>
                  </>
                ) : (
                  <span>Available stores</span>
                )}
              </span>
              <span className="text-indigo-600 font-medium group-hover:underline">
                Compare {storeNames.length} {storeNames.length === 1 ? 'store' : 'stores'} →
              </span>
            </div>
          )}
        </div>

        {/* CTA Button */}
        <div className="pt-1">
          <button
            type="button"
            className="w-full py-2.5 px-4 bg-indigo-50 group-hover:bg-indigo-600 text-indigo-700 group-hover:text-white text-xs sm:text-sm font-bold rounded-xl transition duration-200 flex items-center justify-center gap-2 cursor-pointer shadow-2xs"
          >
            <span>Compare Prices & Trends</span>
            <svg
              className="w-4 h-4 transform group-hover:translate-x-1 transition-transform"
              fill="none"
              stroke="currentColor"
              viewBox="0 0 24 24"
            >
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M14 5l7 7m0 0l-7 7m7-7H3" />
            </svg>
          </button>
        </div>
      </div>
    </div>
  );
};

export default CanonicalProductCard;
