import React, { useState, useEffect } from 'react';
import { useAuth } from '../context/AuthContext';
import { createPriceAlert, getPriceAlerts } from '../services/alertService';

export const PriceAlertModal = ({
  isOpen,
  onClose,
  product,
  onOpenAuthModal,
  onAlertCreated,
}) => {
  const { isAuthenticated, user } = useAuth();

  const [targetPrice, setTargetPrice] = useState('');
  const [loading, setLoading] = useState(false);
  const [checkingExisting, setCheckingExisting] = useState(false);
  const [existingAlert, setExistingAlert] = useState(null);
  const [error, setError] = useState(null);
  const [successMessage, setSuccessMessage] = useState(null);

  const currentPrice = Number(product?.effectivePrice || product?.price || product?.cheapestPrice || 0);
  const productId = product?.productId || product?.id || Math.abs((product?.productName || product?.name || 'prod').hashCode?.() % 500) || 1;
  const productName = product?.productName || product?.name || product?.title || 'Selected Product';

  // Check for existing alerts to prevent accidental duplicates
  useEffect(() => {
    if (!isOpen || !isAuthenticated) {
      setExistingAlert(null);
      setError(null);
      setSuccessMessage(null);
      return;
    }

    let isMounted = true;
    const checkAlerts = async () => {
      setCheckingExisting(true);
      try {
        const userAlerts = await getPriceAlerts();
        if (isMounted && Array.isArray(userAlerts)) {
          const match = userAlerts.find(
            (a) =>
              a.active &&
              (a.productId === productId ||
                (a.productName && productName && a.productName.toLowerCase().trim() === productName.toLowerCase().trim()))
          );
          setExistingAlert(match || null);
        }
      } catch (err) {
        // Non-blocking: will rely on backend duplicate check on submit
        console.warn('Unable to pre-fetch alerts for duplicate check:', err);
      } finally {
        if (isMounted) setCheckingExisting(false);
      }
    };

    checkAlerts();
    return () => {
      isMounted = false;
    };
  }, [isOpen, isAuthenticated, productId, productName]);

  // Set default suggested target price (10% drop)
  useEffect(() => {
    if (isOpen && currentPrice > 0) {
      const suggested = Math.round(currentPrice * 0.9);
      setTargetPrice(suggested.toString());
      setError(null);
      setSuccessMessage(null);
    }
  }, [isOpen, currentPrice]);

  useEffect(() => {
    const handleKeyDown = (e) => {
      if (e.key === 'Escape' && isOpen) {
        onClose();
      }
    };
    window.addEventListener('keydown', handleKeyDown);
    return () => window.removeEventListener('keydown', handleKeyDown);
  }, [isOpen, onClose]);

  if (!isOpen || !product) return null;

  // If user is not authenticated, prompt them to sign in
  if (!isAuthenticated) {
    return (
      <div
        role="dialog"
        aria-modal="true"
        aria-labelledby="auth-gate-title"
        className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-slate-900/60 backdrop-blur-xs animate-fadeIn"
        onClick={(e) => {
          if (e.target === e.currentTarget) onClose();
        }}
      >
        <div className="bg-white rounded-3xl max-w-md w-full p-6 sm:p-8 shadow-2xl border border-slate-100 relative space-y-6 text-center">
          <button
            onClick={onClose}
            aria-label="Close modal"
            className="absolute top-4 right-4 p-2 rounded-full text-slate-400 hover:text-slate-600 hover:bg-slate-100 cursor-pointer min-h-[44px] min-w-[44px] flex items-center justify-center"
          >
            ✕
          </button>
          <div className="w-14 h-14 bg-indigo-50 text-indigo-600 rounded-2xl flex items-center justify-center mx-auto text-2xl shadow-xs">
            🔔
          </div>
          <div className="space-y-2">
            <h2 id="auth-gate-title" className="text-xl font-bold text-slate-900">
              Sign In to Set Price Alerts
            </h2>
            <p className="text-xs text-slate-500">
              Never miss a price drop on <strong className="text-slate-700">{productName}</strong>. Sign in or create an account to get instant notifications.
            </p>
          </div>
          <div className="flex flex-col gap-2 pt-2">
            <button
              type="button"
              onClick={() => {
                onClose();
                if (onOpenAuthModal) onOpenAuthModal();
              }}
              className="w-full py-3 px-4 rounded-xl text-xs sm:text-sm font-bold bg-indigo-600 hover:bg-indigo-700 text-white shadow-md transition cursor-pointer min-h-[44px]"
            >
              Sign In / Create Account
            </button>
            <button
              type="button"
              onClick={onClose}
              className="w-full py-2.5 px-4 rounded-xl text-xs font-semibold text-slate-500 hover:text-slate-700 hover:bg-slate-50 transition cursor-pointer"
            >
              Cancel
            </button>
          </div>
        </div>
      </div>
    );
  }

  const handlePercentageClick = (pct) => {
    if (currentPrice > 0) {
      const calculated = Math.round(currentPrice * (1 - pct / 100));
      setTargetPrice(calculated.toString());
      setError(null);
    }
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    setError(null);
    setSuccessMessage(null);

    const numericTarget = Number(targetPrice);
    if (!numericTarget || isNaN(numericTarget) || numericTarget <= 0) {
      setError('Please enter a valid target price greater than 0.');
      return;
    }

    if (currentPrice > 0 && numericTarget >= currentPrice) {
      setError('Target price must be lower than the current price.');
      return;
    }

    if (existingAlert) {
      setError(`An active price alert already exists for this product at ₹${Number(existingAlert.targetPrice).toLocaleString('en-IN')}.`);
      return;
    }

    setLoading(true);
    try {
      const response = await createPriceAlert({
        productId,
        productName,
        targetPrice: numericTarget,
        merchant: product.merchant || undefined,
      });

      setSuccessMessage(
        `Price alert set! We will notify you at ${user?.email || 'your email'} when the price reaches ₹${numericTarget.toLocaleString('en-IN')}.`
      );
      setExistingAlert(response);

      if (onAlertCreated) {
        onAlertCreated(response);
      }

      // Auto-close after brief delay to allow user to see success
      setTimeout(() => {
        onClose();
      }, 1800);
    } catch (err) {
      console.error('Failed to create price alert:', err);
      const msg = err.response?.data?.message || err.message || 'Unable to create price alert. Please try again.';
      setError(msg);
    } finally {
      setLoading(false);
    }
  };

  return (
    <div
      role="dialog"
      aria-modal="true"
      aria-labelledby="price-alert-modal-title"
      className="fixed inset-0 z-50 flex items-center justify-center p-3 sm:p-4 bg-slate-900/60 backdrop-blur-xs animate-fadeIn"
      onClick={(e) => {
        if (e.target === e.currentTarget) onClose();
      }}
    >
      <div className="bg-white rounded-3xl max-w-lg w-full p-5 sm:p-8 shadow-2xl border border-slate-100 relative space-y-6 max-h-[92vh] overflow-y-auto min-w-0">
        
        {/* Close button */}
        <button
          onClick={onClose}
          aria-label="Close price alert modal"
          className="absolute top-4 right-4 sm:top-5 sm:right-5 w-8 h-8 rounded-full bg-slate-100 hover:bg-slate-200 text-slate-500 hover:text-slate-800 flex items-center justify-center transition cursor-pointer min-h-[44px] min-w-[44px]"
        >
          ✕
        </button>

        {/* Modal Header */}
        <div className="space-y-1.5 pr-8">
          <div className="flex items-center gap-2">
            <span className="text-[10px] font-bold text-amber-700 uppercase tracking-wider bg-amber-50 px-2.5 py-1 rounded-md border border-amber-200/60">
              🔔 Smart Price Drop Alert
            </span>
          </div>
          <h2 id="price-alert-modal-title" className="text-xl sm:text-2xl font-black text-slate-900 leading-tight">
            {productName}
          </h2>
          <p className="text-xs text-slate-500">
            Notify me when the price reaches your target.
          </p>
        </div>

        {/* Product Snapshot & Current Price */}
        <div className="bg-slate-50 p-4 rounded-2xl border border-slate-200/80 flex items-center justify-between gap-4">
          <div className="flex items-center gap-3 min-w-0">
            {product.imageUrl && (
              <img
                src={product.imageUrl}
                alt={productName}
                className="w-12 h-12 object-contain bg-white p-1 rounded-xl border border-slate-200 shrink-0"
              />
            )}
            <div className="min-w-0">
              <span className="text-[10px] font-bold text-slate-400 uppercase tracking-wider block">
                Current Price
              </span>
              <span className="text-lg sm:text-xl font-mono font-black text-slate-900">
                ₹{currentPrice.toLocaleString('en-IN')}
              </span>
            </div>
          </div>
          {product.merchant && (
            <span className="px-2.5 py-1 rounded-lg text-xs font-bold bg-white text-slate-700 border border-slate-200 shadow-2xs shrink-0">
              {product.merchant}
            </span>
          )}
        </div>

        {/* Success Feedback Banner */}
        {successMessage && (
          <div className="p-4 bg-emerald-50 border border-emerald-200 rounded-2xl flex items-start gap-3 animate-fadeIn" role="alert">
            <span className="text-lg text-emerald-600 shrink-0">✓</span>
            <div className="text-xs text-emerald-900 font-medium">
              {successMessage}
            </div>
          </div>
        )}

        {/* Error Feedback Banner */}
        {error && (
          <div className="p-4 bg-rose-50 border border-rose-200 rounded-2xl flex items-start gap-3 animate-fadeIn" role="alert">
            <span className="text-lg text-rose-600 shrink-0">⚠️</span>
            <div className="text-xs text-rose-800 font-medium">
              {error}
            </div>
          </div>
        )}

        {/* Existing Duplicate Alert Notice */}
        {existingAlert && !successMessage && (
          <div className="p-3.5 bg-amber-50 border border-amber-200 rounded-2xl flex items-start gap-2.5">
            <span className="text-base shrink-0">ℹ️</span>
            <div className="text-xs text-amber-900">
              <strong className="font-bold">Active alert already set:</strong> You already have an active price alert for this item at{' '}
              <strong className="font-mono font-bold">₹{Number(existingAlert.targetPrice).toLocaleString('en-IN')}</strong>. Duplicate alerts are blocked.
            </div>
          </div>
        )}

        {/* Alert Form */}
        <form onSubmit={handleSubmit} className="space-y-5">
          <div className="space-y-2">
            <label htmlFor="target-price-input" className="block text-xs font-bold text-slate-700 uppercase tracking-wider">
              Target Price (₹)
            </label>
            <div className="relative rounded-2xl shadow-2xs">
              <div className="pointer-events-none absolute inset-y-0 left-0 pl-3.5 flex items-center text-slate-400 font-mono font-bold text-sm">
                ₹
              </div>
              <input
                id="target-price-input"
                type="number"
                min="1"
                max={currentPrice > 0 ? currentPrice - 1 : undefined}
                step="1"
                value={targetPrice}
                onChange={(e) => {
                  setTargetPrice(e.target.value);
                  setError(null);
                }}
                disabled={loading || !!existingAlert || !!successMessage}
                placeholder={currentPrice > 0 ? `e.g. ${Math.round(currentPrice * 0.9)}` : 'e.g. 50000'}
                className="w-full pl-8 pr-4 py-3 rounded-2xl border border-slate-200 focus:outline-hidden focus:ring-2 focus:ring-indigo-500/20 focus:border-indigo-600 text-sm sm:text-base font-mono font-bold text-slate-900 disabled:bg-slate-50 disabled:text-slate-400 transition"
                required
              />
            </div>
          </div>

          {/* Quick Target Price Discount Presets */}
          {currentPrice > 0 && !existingAlert && !successMessage && (
            <div className="space-y-1.5">
              <span className="text-[11px] font-semibold text-slate-500">Quick Target Discounts:</span>
              <div className="flex flex-wrap gap-2">
                {[5, 10, 15, 20].map((pct) => {
                  const val = Math.round(currentPrice * (1 - pct / 100));
                  const isSelected = Number(targetPrice) === val;
                  return (
                    <button
                      key={pct}
                      type="button"
                      onClick={() => handlePercentageClick(pct)}
                      className={`px-3 py-1.5 rounded-xl text-xs font-bold border transition cursor-pointer ${
                        isSelected
                          ? 'bg-indigo-600 text-white border-indigo-600 shadow-2xs'
                          : 'bg-slate-50 text-slate-700 border-slate-200 hover:bg-slate-100 hover:border-slate-300'
                      }`}
                    >
                      -{pct}% (₹{val.toLocaleString('en-IN')})
                    </button>
                  );
                })}
              </div>
            </div>
          )}

          {/* Modal Action Buttons: Cancel and Create Alert */}
          <div className="pt-2 flex items-center gap-3">
            <button
              type="button"
              onClick={onClose}
              className="flex-1 py-3 px-4 rounded-xl text-xs sm:text-sm font-bold bg-slate-100 hover:bg-slate-200 text-slate-700 transition cursor-pointer min-h-[44px]"
            >
              Cancel
            </button>
            <button
              type="submit"
              disabled={loading || checkingExisting || !!existingAlert || !!successMessage}
              className="flex-1 py-3 px-4 rounded-xl text-xs sm:text-sm font-bold bg-indigo-600 hover:bg-indigo-700 text-white shadow-md transition cursor-pointer disabled:opacity-50 disabled:cursor-not-allowed min-h-[44px] flex items-center justify-center gap-2"
            >
              {loading ? (
                <>
                  <div className="animate-spin w-4 h-4 border-2 border-white border-t-transparent rounded-full" />
                  <span>Creating Alert...</span>
                </>
              ) : (
                <span>Create Alert</span>
              )}
            </button>
          </div>
        </form>

      </div>
    </div>
  );
};

// Polyfill hashCode for synthetic IDs
if (!String.prototype.hashCode) {
  String.prototype.hashCode = function () {
    let hash = 0;
    for (let i = 0; i < this.length; i++) {
      hash = (hash << 5) - hash + this.charCodeAt(i);
      hash |= 0;
    }
    return hash;
  };
}

export default PriceAlertModal;
