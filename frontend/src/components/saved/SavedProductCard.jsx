import React from 'react';
import { Link } from 'react-router-dom';

export const SavedProductCard = ({
  product,
  onRemove,
  onSetAlert,
  compact = false,
}) => {
  if (!product) return null;

  const {
    id,
    productId,
    productName,
    productCategory,
    productImageUrl,
    savedPrice,
    savedMerchant,
    currentPrice,
    currentMerchant,
    priceChange,
    priceDropAmount,
    priceDropPercentage,
    isPriceDropped,
    hasActiveAlert,
    alertTargetPrice,
    productUrl,
    createdAt,
  } = product;

  const merchantColors = {
    Amazon: 'bg-amber-50 text-amber-900 border-amber-200',
    Flipkart: 'bg-blue-50 text-blue-900 border-blue-200',
    Croma: 'bg-teal-50 text-teal-900 border-teal-200',
  };

  const merchant = currentMerchant || product.currentProvider || savedMerchant;
  const currentProviderBadge = merchant
    ? merchantColors[merchant] || 'bg-slate-100 text-slate-800 border-slate-200'
    : 'bg-slate-100 text-slate-800 border-slate-200';

  return (
    <div
      className={`bg-white rounded-3xl border transition-all duration-200 shadow-xs hover:shadow-md flex flex-col justify-between overflow-hidden ${
        isPriceDropped
          ? 'border-emerald-300 ring-2 ring-emerald-500/10'
          : 'border-slate-200 hover:border-slate-300'
      }`}
      data-testid="saved-product-card"
    >
      {/* Top Section */}
      <div className="p-5 sm:p-6 space-y-4">
        {/* Header Tags */}
        <div className="flex items-center justify-between gap-2 flex-wrap">
          <div className="flex items-center gap-1.5 flex-wrap">
            <span className="text-[10px] font-bold uppercase tracking-wider text-indigo-700 bg-indigo-50 px-2.5 py-1 rounded-lg">
              {productCategory || 'General'}
            </span>
            {merchant && (
              <span
                className={`text-[10px] font-bold px-2 py-0.5 rounded-lg border ${currentProviderBadge}`}
              >
                {merchant}
              </span>
            )}
          </div>

          {/* PRICE DROPPED Badge: Only show if actual stored/current data proves it */}
          {isPriceDropped && (
            <span
              className="px-2.5 py-0.5 rounded-full text-[10px] font-black tracking-wider uppercase bg-emerald-100 text-emerald-800 border border-emerald-300 flex items-center gap-1"
              data-testid="price-dropped-badge"
            >
              <span>🔥</span>
              <span>PRICE DROPPED</span>
            </span>
          )}
        </div>

        {/* Product Image & Title */}
        <div className="flex items-start gap-4">
          <div className="w-20 h-20 sm:w-24 sm:h-24 rounded-2xl bg-slate-50 border border-slate-100 p-2.5 flex items-center justify-center shrink-0 overflow-hidden">
            {productImageUrl ? (
              <img
                src={productImageUrl}
                alt={productName}
                className="w-full h-full object-contain hover:scale-105 transition"
                loading="lazy"
              />
            ) : (
              <span className="text-3xl">📦</span>
            )}
          </div>

          <div className="min-w-0 flex-1 space-y-1">
            <h3
              className="font-extrabold text-slate-900 text-base leading-snug line-clamp-2"
              title={productName}
            >
              {productName}
            </h3>
            <p className="text-[11px] text-slate-400">
              Saved {createdAt ? new Date(createdAt).toLocaleDateString() : 'recently'}
            </p>
          </div>
        </div>

        {/* Price Tracking Matrix */}
        <div className="bg-slate-50/80 rounded-2xl p-3.5 border border-slate-100 space-y-2">
          <div className="flex items-center justify-between text-xs">
            <span className="text-slate-500 font-medium">Saved at:</span>
            <span className="font-extrabold text-slate-700 font-mono">
              ₹{savedPrice != null ? Number(savedPrice).toLocaleString('en-IN') : '—'}
            </span>
          </div>

          <div className="flex items-center justify-between text-xs">
            <span className="text-slate-500 font-medium">Current:</span>
            <span className="font-black text-slate-900 text-sm font-mono">
              ₹{currentPrice != null ? Number(currentPrice).toLocaleString('en-IN') : '—'}
            </span>
          </div>

          {/* Price Change / Drop Telemetry */}
          {isPriceDropped && priceDropAmount > 0 ? (
            <div className="pt-2 border-t border-slate-200/60 flex items-center justify-between">
              <span className="text-xs font-bold text-emerald-800">Verified Drop:</span>
              <span className="text-xs font-extrabold text-emerald-700 font-mono flex items-center gap-1">
                <span>↓</span>
                <span>₹{Number(priceDropAmount).toLocaleString('en-IN')}</span>
                {priceDropPercentage > 0 && <span>({priceDropPercentage}%)</span>}
              </span>
            </div>
          ) : priceChange > 0 ? (
            <div className="pt-2 border-t border-slate-200/60 flex items-center justify-between text-xs text-slate-500">
              <span>Price change:</span>
              <span className="font-semibold text-slate-600 font-mono">
                +₹{Number(priceChange).toLocaleString('en-IN')}
              </span>
            </div>
          ) : null}
        </div>

        {/* Alert Status */}
        <div className="flex items-center justify-between text-xs pt-1">
          {hasActiveAlert ? (
            <span className="inline-flex items-center gap-1 text-[11px] font-bold text-indigo-700 bg-indigo-50 border border-indigo-200 px-2.5 py-1 rounded-xl">
              <span>🎯</span>
              <span>Alert: ₹{Number(alertTargetPrice || 0).toLocaleString('en-IN')}</span>
            </span>
          ) : onSetAlert ? (
            <button
              type="button"
              onClick={() =>
                onSetAlert({
                  id: productId,
                  productId,
                  productName,
                  price: currentPrice || savedPrice,
                })
              }
              className="text-[11px] font-bold text-slate-500 hover:text-indigo-600 flex items-center gap-1 cursor-pointer transition"
            >
              <span>🔔</span>
              <span>Set Price Alert</span>
            </button>
          ) : null}

          {onRemove && (
            <button
              type="button"
              onClick={() => onRemove(id)}
              className="text-[11px] font-semibold text-rose-600 hover:text-rose-800 cursor-pointer transition"
              title="Remove from saved products"
            >
              Remove
            </button>
          )}
        </div>
      </div>

      {/* Bottom Action Footer */}
      <div className="p-4 sm:p-5 bg-slate-50/50 border-t border-slate-100 flex items-center gap-2 justify-between">
        <Link
          to={`/shopping?q=${encodeURIComponent(productName)}`}
          className="flex-1 py-2 px-3 bg-indigo-600 hover:bg-indigo-700 text-white font-bold text-xs rounded-xl shadow-2xs transition text-center flex items-center justify-center gap-1.5"
        >
          <span>Compare Prices</span>
          <span>⇄</span>
        </Link>

        {productUrl && (
          <a
            href={productUrl}
            target="_blank"
            rel="noopener noreferrer"
            className="py-2 px-3.5 bg-white hover:bg-slate-100 text-slate-700 border border-slate-200 font-bold text-xs rounded-xl shadow-2xs transition flex items-center gap-1"
            title="Open merchant product page"
          >
            <span>Open</span>
            <span>↗</span>
          </a>
        )}
      </div>
    </div>
  );
};

export default SavedProductCard;
