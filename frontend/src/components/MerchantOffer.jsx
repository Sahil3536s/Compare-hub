import React from 'react';
import PriceBadge from './PriceBadge';

export const MerchantOffer = ({ offer }) => {
  const discountPercent = offer.originalPrice && offer.originalPrice > offer.price
    ? Math.round(((offer.originalPrice - offer.price) / offer.originalPrice) * 100)
    : 0;

  return (
    <div className={`p-4 rounded-xl border transition-all ${
      offer.isCheapest
        ? 'bg-emerald-50/40 border-emerald-200'
        : 'bg-white border-slate-200 hover:border-slate-300'
    }`}>
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-3">
        {/* Merchant Info */}
        <div className="flex items-center gap-3">
          <div className="w-10 h-10 rounded-lg bg-slate-50 border border-slate-100 flex items-center justify-center p-1.5 shrink-0">
            {offer.merchantLogo ? (
              <img
                src={offer.merchantLogo}
                alt={offer.merchantName}
                className="max-h-full max-w-full object-contain"
                onError={(e) => {
                  e.target.style.display = 'none';
                  e.target.nextElementSibling.style.display = 'block';
                }}
              />
            ) : null}
            <span className="text-xs font-bold text-slate-700 hidden">
              {offer.merchantName.slice(0, 2).toUpperCase()}
            </span>
          </div>

          <div>
            <div className="flex items-center gap-2 flex-wrap">
              <h5 className="font-semibold text-sm text-slate-900">{offer.merchantName}</h5>
              {offer.isCheapest && <PriceBadge type="cheapest" text="Lowest Price" />}
              {offer.rating && (
                <span className="text-xs text-slate-500 flex items-center gap-1">
                  ★ {offer.rating}
                </span>
              )}
            </div>
            <div className="flex items-center gap-2 text-xs text-slate-500 mt-0.5">
              <span>{offer.deliveryTime}</span>
              <span>•</span>
              <span className={offer.inStock ? 'text-emerald-600 font-medium' : 'text-rose-600'}>
                {offer.inStock ? 'In Stock' : 'Out of Stock'}
              </span>
            </div>
          </div>
        </div>

        {/* Offer Tag */}
        {offer.offerTag && (
          <div className="sm:text-center">
            <span className="inline-block px-2.5 py-1 rounded bg-amber-50 text-amber-800 text-xs font-medium border border-amber-200/60">
              🏷️ {offer.offerTag}
            </span>
          </div>
        )}

        {/* Price & Action */}
        <div className="flex items-center justify-between sm:justify-end gap-4 border-t sm:border-t-0 pt-3 sm:pt-0 border-slate-100">
          <div className="text-left sm:text-right">
            <div className="text-lg font-bold text-slate-900">
              ₹{offer.price.toLocaleString('en-IN')}
            </div>
            {discountPercent > 0 && (
              <div className="text-xs text-slate-400">
                <span className="line-through">₹{offer.originalPrice.toLocaleString('en-IN')}</span>
                <span className="text-emerald-600 font-semibold ml-1.5">{discountPercent}% OFF</span>
              </div>
            )}
          </div>

          <a
            href={offer.productUrl || '#'}
            onClick={(e) => e.preventDefault()}
            className={`inline-flex items-center gap-1.5 px-4 py-2 text-xs font-semibold rounded-lg transition shadow-xs ${
              offer.isCheapest
                ? 'bg-emerald-600 hover:bg-emerald-700 text-white'
                : 'bg-slate-900 hover:bg-slate-800 text-white'
            }`}
          >
            <span>Go to Store</span>
            <svg className="w-3.5 h-3.5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M10 6H6a2 2 0 00-2 2v10a2 2 0 002 2h10a2 2 0 002-2v-4M14 4h6m0 0v6m0-6L10 14" />
            </svg>
          </a>
        </div>
      </div>
    </div>
  );
};

export default MerchantOffer;
