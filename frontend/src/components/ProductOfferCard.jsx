import React, { useState } from 'react';
import PriceBadge from './PriceBadge';
import BestPaymentOptionCard from './BestPaymentOptionCard';
import ProductAlternativesModal from './ProductAlternativesModal';

export const ProductOfferCard = ({ offer, onSave, isSaved = false, onViewPriceHistory }) => {
  const [saved, setSaved] = useState(isSaved);
  const [showBreakdown, setShowBreakdown] = useState(false);
  const [showAlternatives, setShowAlternatives] = useState(false);

  const merchantColors = {
    Amazon: 'bg-amber-50 text-amber-800 border-amber-200',
    Flipkart: 'bg-blue-50 text-blue-800 border-blue-200',
    Croma: 'bg-teal-50 text-teal-800 border-teal-200',
  };

  const handleSaveToggle = () => {
    setSaved(!saved);
    if (onSave) onSave(offer);
  };

  const costBreakdown = offer.costBreakdown;
  const dealQuality = offer.dealQuality;
  const paymentOffers = offer.paymentOffers || costBreakdown?.paymentOffers;
  const effectivePrice = offer.effectivePrice || offer.price;
  const hasDiscounts = costBreakdown && costBreakdown.discounts && Number(costBreakdown.discounts) > 0;

  return (
    <div className={`bg-white rounded-2xl border transition-all duration-200 overflow-hidden flex flex-col justify-between shadow-xs hover:shadow-md ${
      offer.isCheapest ? 'border-emerald-300 ring-1 ring-emerald-500/20' : 'border-slate-200 hover:border-slate-300'
    }`} data-testid="product-offer-card">
      {/* Product Image and Badges */}
      <div className="relative bg-slate-50 p-6 flex items-center justify-center min-h-[200px] overflow-hidden group">
        <img
          src={offer.imageUrl || 'https://images.unsplash.com/photo-1546868871-7041f2a55e12?w=600'}
          alt={offer.productName}
          className="max-h-44 w-auto object-contain group-hover:scale-105 transition-transform duration-300"
          loading="lazy"
        />

        {/* Badges on Top Left */}
        <div className="absolute top-3 left-3 flex flex-col gap-1.5 z-10">
          {dealQuality && dealQuality.dealScore >= 75 && (
            <span className={`px-2.5 py-1 rounded-lg text-[10px] font-black uppercase tracking-wider shadow-xs flex items-center gap-1 ${
              dealQuality.dealScore >= 90
                ? 'bg-linear-to-r from-emerald-600 to-teal-600 text-white'
                : 'bg-linear-to-r from-indigo-600 to-blue-600 text-white'
            }`}>
              <span>🎯</span>
              <span>{dealQuality.dealScore} Deal Score</span>
            </span>
          )}
          {offer.isBestValue && (
            <span className="px-2.5 py-1 rounded-lg text-[10px] font-black uppercase tracking-wider bg-linear-to-r from-amber-500 to-amber-600 text-white shadow-xs flex items-center gap-1">
              <span>🏆</span>
              <span>Best Value</span>
            </span>
          )}
          {offer.isCheapest && <PriceBadge type="cheapest" text="Cheapest Option" />}
          {offer.isHighestRated && !offer.isBestValue && (
            <span className="px-2.5 py-1 rounded-lg text-[10px] font-bold bg-indigo-600 text-white shadow-xs flex items-center gap-1">
              <span>⭐</span>
              <span>Top Rated</span>
            </span>
          )}
          {offer.discountPercent > 10 && (
            <PriceBadge type="discount" text={`${offer.discountPercent}% OFF`} />
          )}
        </div>

        {/* Merchant Tag Top Right */}
        <div className="absolute top-3 right-3 z-10 flex items-center gap-1.5">
          <span className={`px-2.5 py-1 rounded-lg text-xs font-bold border shadow-xs ${
            merchantColors[offer.merchant] || 'bg-slate-100 text-slate-800 border-slate-200'
          }`}>
            {offer.merchant}
          </span>
          <button
            onClick={handleSaveToggle}
            className={`p-1.5 rounded-lg border transition shadow-xs ${
              saved
                ? 'bg-rose-50 text-rose-600 border-rose-200'
                : 'bg-white/90 text-slate-400 hover:text-rose-500 border-slate-200'
            }`}
            title="Save deal"
          >
            <svg className="w-3.5 h-3.5 fill-current" viewBox="0 0 20 20">
              <path fillRule="evenodd" d="M3.172 5.172a4 4 0 015.656 0L10 6.343l1.172-1.171a4 4 0 115.656 5.656L10 17.657l-6.828-6.829a4 4 0 010-5.656z" clipRule="evenodd" />
            </svg>
          </button>
        </div>
      </div>

      {/* Main Details Body */}
      <div className="p-5 flex-1 flex flex-col justify-between space-y-4">
        <div>
          <div className="flex items-center justify-between text-xs text-slate-500 mb-1.5">
            <span className="font-semibold text-indigo-600 uppercase tracking-wider text-[11px]">
              {offer.brand || offer.category}
            </span>
            <div className="flex items-center gap-1 font-semibold text-slate-700">
              <span className="text-amber-500">★</span>
              <span>{offer.rating}</span>
            </div>
          </div>

          <h3 className="font-bold text-sm text-slate-900 line-clamp-2 leading-snug title-font">
            {offer.productName}
          </h3>

          {/* Deal Quality & Historical Comparison Pill */}
          {dealQuality && (
            <div className="mt-2 space-y-1.5" data-testid="deal-quality-summary">
              <div className="flex items-center justify-between text-[11px] bg-slate-50 p-2 rounded-xl border border-slate-200/80">
                <span className="font-bold text-slate-700 flex items-center gap-1">
                  <span>📊</span>
                  <span>{dealQuality.classificationLabel || 'Deal Quality'}</span>
                </span>
                <span className="font-mono font-bold text-indigo-600">
                  {dealQuality.dealScore}/100 Score
                </span>
              </div>

              {/* Safe Nuanced Disclaimer if MRP is Inflated */}
              {dealQuality.disclaimer && (
                <div className="p-2 rounded-xl bg-amber-50/80 border border-amber-200/80 text-[10px] text-amber-900 font-medium leading-tight">
                  💡 {dealQuality.disclaimer}
                </div>
              )}
            </div>
          )}

          <div className="mt-2.5 flex items-center justify-between text-xs text-slate-500">
            <div className="flex items-center gap-2">
              <span>🚚 {offer.delivery}</span>
              <span>•</span>
              <span className={offer.inStock ? 'text-emerald-600 font-medium' : 'text-rose-600'}>
                {offer.inStock ? 'In Stock' : 'Out of Stock'}
              </span>
            </div>
            <div className="flex items-center gap-1.5">
              <button
                type="button"
                onClick={() => setShowAlternatives(true)}
                className="text-[11px] font-bold text-indigo-600 hover:text-indigo-800 flex items-center gap-1 cursor-pointer bg-indigo-50/80 px-2 py-0.5 rounded-md hover:bg-indigo-100 transition"
                data-testid="view-alternatives-btn"
              >
                <span>🔄</span>
                <span>Alternatives</span>
              </button>
              {onViewPriceHistory && (
                <button
                  type="button"
                  onClick={() => onViewPriceHistory(offer)}
                  className="text-[11px] font-bold text-indigo-600 hover:text-indigo-800 flex items-center gap-1 cursor-pointer bg-indigo-50/80 px-2 py-0.5 rounded-md hover:bg-indigo-100 transition"
                >
                  <span>📊</span>
                  <span>History</span>
                </button>
              )}
            </div>
          </div>
        </div>

        {/* Phase 30: Best Payment Option Card */}
        {paymentOffers && <BestPaymentOptionCard paymentOffers={paymentOffers} />}

        {/* Expandable True Cost Breakdown Toggle */}
        <div className="pt-2">
          <button
            type="button"
            onClick={() => setShowBreakdown(!showBreakdown)}
            className="w-full flex items-center justify-between text-xs font-bold text-indigo-600 hover:text-indigo-800 bg-indigo-50/60 hover:bg-indigo-50 px-3 py-1.5 rounded-xl transition cursor-pointer border border-indigo-100/80"
          >
            <span className="flex items-center gap-1.5">
              <span>🏷️</span>
              <span>Price Breakdown</span>
              {hasDiscounts && (
                <span className="px-1.5 py-0.2 rounded-md bg-emerald-100 text-emerald-700 text-[10px]">
                  -₹{Number(costBreakdown.discounts).toLocaleString('en-IN')}
                </span>
              )}
            </span>
            <span>{showBreakdown ? '▲' : '▼'}</span>
          </button>

          {showBreakdown && costBreakdown && (
            <div className="mt-2.5 p-3 rounded-xl bg-slate-50 border border-slate-200 text-xs space-y-2 animate-fadeIn" data-testid="price-breakdown-drawer">
              <div className="flex justify-between text-slate-600">
                <span>Listed Base Price</span>
                <span className="font-mono font-medium">₹{Number(costBreakdown.basePrice).toLocaleString('en-IN')}</span>
              </div>

              {Number(costBreakdown.deliveryFee) > 0 ? (
                <div className="flex justify-between text-slate-600">
                  <span>+ Delivery Fee</span>
                  <span className="font-mono text-amber-700">+₹{Number(costBreakdown.deliveryFee).toLocaleString('en-IN')}</span>
                </div>
              ) : (
                <div className="flex justify-between text-slate-500 text-[11px]">
                  <span>Delivery Fee</span>
                  <span className="text-emerald-600 font-bold">FREE</span>
                </div>
              )}

              {Number(costBreakdown.platformFee) > 0 && (
                <div className="flex justify-between text-slate-600">
                  <span>+ Platform Fee</span>
                  <span className="font-mono text-amber-700">+₹{Number(costBreakdown.platformFee).toLocaleString('en-IN')}</span>
                </div>
              )}

              {costBreakdown.appliedOffers && costBreakdown.appliedOffers.length > 0 && (
                <div className="pt-1 border-t border-slate-200/80 space-y-1">
                  <span className="font-bold text-[10px] uppercase text-slate-500 block">Applicable Discounts</span>
                  {costBreakdown.appliedOffers.map((off, idx) => (
                    <div key={idx} className="flex flex-col gap-0.5 text-emerald-700 bg-emerald-50/80 p-1.5 rounded-lg border border-emerald-100">
                      <div className="flex justify-between font-semibold">
                        <span className="line-clamp-1">{off.description}</span>
                        <span className="font-mono">-₹{Number(off.discountAmount).toLocaleString('en-IN')}</span>
                      </div>
                      {off.isConditional && (
                        <span className="text-[10px] text-amber-700 font-medium">
                          ⚠️ Conditional: {off.terms || 'Bank/Card offer'}
                        </span>
                      )}
                    </div>
                  ))}
                </div>
              )}

              <div className="pt-2 border-t border-slate-200 flex justify-between font-extrabold text-slate-900">
                <span>Effective Final Cost</span>
                <span className="font-mono text-indigo-700">₹{Number(effectivePrice).toLocaleString('en-IN')}</span>
              </div>
            </div>
          )}
        </div>

        {/* Price and View Deal Button */}
        <div className="pt-2 border-t border-slate-100 flex items-center justify-between gap-3">
          <div>
            <div className="flex items-baseline gap-1.5">
              <div className="text-2xl font-black text-slate-900 tracking-tight font-mono">
                ₹{Number(effectivePrice).toLocaleString('en-IN')}
              </div>
              {hasDiscounts && (
                <span className="text-[10px] font-bold text-emerald-600 uppercase bg-emerald-50 px-1.5 py-0.5 rounded-md border border-emerald-100">
                  Effective
                </span>
              )}
            </div>
            
            {hasDiscounts ? (
              <div className="text-xs text-slate-400">
                <span>Listed: </span>
                <span className="line-through font-mono">₹{Number(offer.price).toLocaleString('en-IN')}</span>
              </div>
            ) : offer.originalPrice && Number(offer.originalPrice) > Number(offer.price) ? (
              <div className="text-xs text-slate-400">
                <span className="line-through">
                  ₹{Number(offer.originalPrice).toLocaleString('en-IN')}
                </span>
                <span className="text-emerald-600 font-bold ml-1.5">
                  Save {offer.discountPercent}%
                </span>
              </div>
            ) : null}
          </div>

          <a
            href={offer.productUrl || '#'}
            target="_blank"
            rel="noopener noreferrer"
            onClick={(e) => {
              if (offer.productUrl === '#') {
                e.preventDefault();
                alert(`Redirecting to ${offer.merchant} store for ${offer.productName}!`);
              }
            }}
            className={`px-4 py-2.5 rounded-xl text-xs font-bold transition shadow-xs flex items-center gap-1.5 whitespace-nowrap cursor-pointer ${
              offer.isCheapest
                ? 'bg-emerald-600 hover:bg-emerald-700 text-white'
                : 'bg-slate-900 hover:bg-slate-800 text-white'
            }`}
          >
            <span>View Deal</span>
            <svg className="w-3.5 h-3.5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M10 6H6a2 2 0 00-2 2v10a2 2 0 002 2h10a2 2 0 002-2v-4M14 4h6m0 0v6m0-6L10 14" />
            </svg>
          </a>
        </div>
      </div>

      {/* Phase 31: Product Alternatives Modal */}
      {showAlternatives && (
        <ProductAlternativesModal
          isOpen={showAlternatives}
          onClose={() => setShowAlternatives(false)}
          offer={offer}
        />
      )}
    </div>
  );
};

export default ProductOfferCard;
