import React, { useState, useMemo, useEffect } from 'react';

// Storage extraction helper for title parsing if attributes are missing
const extractStorageFromTitle = (title = '') => {
  const match = title.match(/\b(64|128|256|512)\s*(?:gb|gigabytes?)\b|\b(1|2)\s*(?:tb|terabytes?)\b/i);
  return match ? match[0].toUpperCase().replace(/\s+/g, '') : null;
};

// RAM extraction helper
const extractRamFromTitle = (title = '') => {
  const match = title.match(/\b(4|6|8|12|16|24|32)\s*(?:gb)?\s*ram\b|\b(4|6|8|12|16|24|32)\s*gb\b/i);
  return match ? match[0].toUpperCase().replace(/\s+/g, '') : null;
};

/**
 * Strict equivalence checker.
 * CRITICAL RULE: Samsung S24 128GB must NOT be displayed as the same variant as Samsung S24 256GB.
 */
export const areOffersEquivalent = (offerA, offerB) => {
  if (!offerA || !offerB) return false;

  // 1. Strict storage comparison
  const storageA = (offerA.attributes?.storage || extractStorageFromTitle(offerA.productName) || '').toUpperCase().replace(/\s+/g, '');
  const storageB = (offerB.attributes?.storage || extractStorageFromTitle(offerB.productName) || '').toUpperCase().replace(/\s+/g, '');
  if (storageA && storageB && storageA !== storageB) {
    return false; // Hardware variant mismatch: 128GB vs 256GB
  }

  // 2. Strict RAM comparison
  const ramA = (offerA.attributes?.ram || extractRamFromTitle(offerA.productName) || '').toUpperCase().replace(/\s+/g, '');
  const ramB = (offerB.attributes?.ram || extractRamFromTitle(offerB.productName) || '').toUpperCase().replace(/\s+/g, '');
  if (ramA && ramB && ramA !== ramB) {
    return false;
  }

  // 3. Strict variant comparison (e.g. Pro vs Pro Max, Plus vs Ultra)
  const varA = (offerA.attributes?.variant || '').toLowerCase().trim();
  const varB = (offerB.attributes?.variant || '').toLowerCase().trim();
  if (varA && varB && varA !== varB) {
    return false;
  }

  // 4. Canonical key check if present from backend
  if (offerA.canonicalKey && offerB.canonicalKey) {
    return offerA.canonicalKey === offerB.canonicalKey;
  }

  // 5. Brand check
  const brandA = (offerA.brand || offerA.attributes?.brand || '').toLowerCase().trim();
  const brandB = (offerB.brand || offerB.attributes?.brand || '').toLowerCase().trim();
  if (brandA && brandB && brandA !== brandB) {
    return false;
  }

  return true;
};

export const ProductComparisonModal = ({
  isOpen,
  onClose,
  selectedOffer,
  allOffers = [],
  onSetPriceAlert,
}) => {
  const [activeOffer, setActiveOffer] = useState(selectedOffer);
  const [mobileView, setMobileView] = useState('cards'); // 'cards' | 'table'

  useEffect(() => {
    if (selectedOffer) {
      setActiveOffer(selectedOffer);
    }
  }, [selectedOffer]);

  useEffect(() => {
    const handleKeyDown = (e) => {
      if (e.key === 'Escape' && isOpen) {
        onClose();
      }
    };
    window.addEventListener('keydown', handleKeyDown);
    return () => window.removeEventListener('keydown', handleKeyDown);
  }, [isOpen, onClose]);

  // Find all available variants in allOffers for the brand/model of activeOffer
  const availableVariants = useMemo(() => {
    if (!activeOffer || !allOffers.length) return [];

    const activeBrand = (activeOffer.brand || activeOffer.attributes?.brand || '').toLowerCase().trim();
    // Group all offers for this general model line
    const variantMap = new Map();

    allOffers.forEach((off) => {
      const offBrand = (off.brand || off.attributes?.brand || '').toLowerCase().trim();
      if (activeBrand && offBrand && activeBrand !== offBrand) return;

      const storage = (off.attributes?.storage || extractStorageFromTitle(off.productName) || 'Standard Storage').toUpperCase();
      const ram = (off.attributes?.ram || extractRamFromTitle(off.productName) || '').toUpperCase();
      const variantKey = `${storage}${ram ? ` • ${ram}` : ''}`;

      if (!variantMap.has(variantKey)) {
        variantMap.set(variantKey, {
          key: variantKey,
          sampleOffer: off,
          storage,
          ram,
          count: 0,
        });
      }
      variantMap.get(variantKey).count += 1;
    });

    return Array.from(variantMap.values());
  }, [activeOffer, allOffers]);

  // Strictly filter only equivalent offers for activeOffer
  const equivalentOffers = useMemo(() => {
    if (!activeOffer) return [];

    // Filter matching equivalent offers from allOffers
    const matched = allOffers.filter((off) => areOffersEquivalent(activeOffer, off));

    // Ensure activeOffer itself is always in the list
    const hasActive = matched.some(
      (m) => m.merchant === activeOffer.merchant && m.productName === activeOffer.productName
    );
    const combined = hasActive ? matched : [activeOffer, ...matched];

    // Deduplicate by merchant if multiple offers from same merchant exist
    const byMerchant = new Map();
    combined.forEach((off) => {
      if (!byMerchant.has(off.merchant)) {
        byMerchant.set(off.merchant, off);
      } else {
        // Keep the cheaper one if duplicate merchant offers exist
        const existing = byMerchant.get(off.merchant);
        if (Number(off.effectivePrice || off.price) < Number(existing.effectivePrice || existing.price)) {
          byMerchant.set(off.merchant, off);
        }
      }
    });

    return Array.from(byMerchant.values());
  }, [activeOffer, allOffers]);

  if (!isOpen || !activeOffer) return null;

  // Compute best value highlights among equivalent offers
  const prices = equivalentOffers.map((o) => Number(o.effectivePrice || o.price || 0)).filter((p) => p > 0);
  const minPrice = prices.length ? Math.min(...prices) : 0;

  const ratings = equivalentOffers.map((o) => Number(o.rating || 0)).filter((r) => r > 0);
  const maxRating = ratings.length ? Math.max(...ratings) : 0;

  const getDeliverySpeedScore = (del = '') => {
    const d = del.toLowerCase();
    if (d.includes('same day') || d.includes('today')) return 3;
    if (d.includes('tomorrow') || d.includes('next day') || d.includes('1 day')) return 2;
    if (d.includes('2 day') || d.includes('express')) return 1;
    return 0;
  };
  const deliveryScores = equivalentOffers.map((o) => getDeliverySpeedScore(o.delivery));
  const maxDeliveryScore = deliveryScores.length ? Math.max(...deliveryScores) : 0;

  const activeStorage = activeOffer.attributes?.storage || extractStorageFromTitle(activeOffer.productName) || '128GB';
  const activeRam = activeOffer.attributes?.ram || extractRamFromTitle(activeOffer.productName) || '8GB';

  const merchantLogos = {
    Amazon: '📦',
    Flipkart: '🛒',
    Croma: '⚡',
  };

  return (
    <div
      role="dialog"
      aria-modal="true"
      aria-labelledby="comparison-modal-title"
      className="fixed inset-0 z-50 flex items-center justify-center p-2 sm:p-4 bg-slate-900/60 backdrop-blur-xs animate-fadeIn"
      onClick={(e) => {
        if (e.target === e.currentTarget) onClose();
      }}
    >
      <div className="bg-white rounded-3xl max-w-5xl w-full p-4 sm:p-8 shadow-2xl border border-slate-100 relative space-y-6 max-h-[92vh] overflow-y-auto min-w-0">
        
        {/* Close Button */}
        <button
          onClick={onClose}
          aria-label="Close comparison modal"
          className="absolute top-4 right-4 sm:top-5 sm:right-5 w-8 h-8 rounded-full bg-slate-100 hover:bg-slate-200 text-slate-500 hover:text-slate-800 flex items-center justify-center transition cursor-pointer min-h-[44px] min-w-[44px]"
        >
          ✕
        </button>

        {/* Modal Header */}
        <div className="space-y-2 pr-8">
          <div className="flex items-center gap-2 flex-wrap">
            <span className="text-[10px] font-bold text-indigo-600 uppercase tracking-wider bg-indigo-50 px-2.5 py-1 rounded-md border border-indigo-200/60">
              ⚖️ Side-by-Side Store Comparison
            </span>
            <span className="text-[10px] font-bold text-emerald-700 uppercase tracking-wider bg-emerald-50 px-2 py-0.5 rounded-md border border-emerald-200/60 flex items-center gap-1">
              <span>🔒</span>
              <span>Strict Equivalent Matching</span>
            </span>
          </div>

          <h2 id="comparison-modal-title" className="text-xl sm:text-3xl font-black text-slate-900 leading-tight">
            {activeOffer.productName}
          </h2>

          {/* Strict Variant Isolation Notice */}
          <div className="p-2.5 bg-slate-50 border border-slate-200/80 rounded-xl text-xs text-slate-600 flex items-center gap-2">
            <span className="text-base shrink-0">🛡️</span>
            <span>
              Variant Isolation Active: Comparing strictly equivalent <strong className="text-slate-900 font-bold">{activeStorage}</strong> ({activeRam} RAM) models. Incompatible capacities (e.g. 128GB vs 256GB) are segregated.
            </span>
          </div>
        </div>

        {/* Available Variant Tabs (e.g. 128GB vs 256GB) */}
        {availableVariants.length > 1 && (
          <div className="space-y-1.5">
            <span className="text-[11px] font-bold uppercase tracking-wider text-slate-400 block">
              Switch Hardware Variant:
            </span>
            <div className="flex flex-wrap gap-2">
              {availableVariants.map((v) => {
                const isActive = areOffersEquivalent(activeOffer, v.sampleOffer);
                return (
                  <button
                    key={v.key}
                    type="button"
                    onClick={() => setActiveOffer(v.sampleOffer)}
                    className={`px-3 py-1.5 rounded-xl text-xs font-bold border transition cursor-pointer flex items-center gap-1.5 ${
                      isActive
                        ? 'bg-indigo-600 text-white border-indigo-600 shadow-xs ring-2 ring-indigo-500/20'
                        : 'bg-white text-slate-700 border-slate-200 hover:bg-slate-50 hover:border-slate-300'
                    }`}
                  >
                    <span>{isActive ? '✓' : '•'}</span>
                    <span>{v.key}</span>
                    <span className="text-[10px] opacity-80">({v.count} store{v.count > 1 ? 's' : ''})</span>
                  </button>
                );
              })}
            </div>
          </div>
        )}

        {/* Mobile View Toggle (Cards vs Table) */}
        <div className="md:hidden flex justify-end">
          <div className="inline-flex bg-slate-100 p-0.5 rounded-xl text-xs font-bold">
            <button
              type="button"
              onClick={() => setMobileView('cards')}
              className={`px-3 py-1 rounded-lg transition ${
                mobileView === 'cards' ? 'bg-white text-slate-900 shadow-2xs' : 'text-slate-500'
              }`}
            >
              Cards View
            </button>
            <button
              type="button"
              onClick={() => setMobileView('table')}
              className={`px-3 py-1 rounded-lg transition ${
                mobileView === 'table' ? 'bg-white text-slate-900 shadow-2xs' : 'text-slate-500'
              }`}
            >
              Table View
            </button>
          </div>
        </div>

        {/* DESKTOP & TABLE VIEW: Comparison Table */}
        <div className={`${mobileView === 'cards' ? 'hidden md:block' : 'block'} overflow-x-auto rounded-2xl border border-slate-200 shadow-xs`}>
          <table className="w-full text-left border-collapse text-xs sm:text-sm">
            <thead>
              <tr className="bg-slate-50 border-b border-slate-200 divide-x divide-slate-200">
                <th scope="col" className="p-4 font-bold text-slate-500 uppercase tracking-wider text-[11px] w-40 shrink-0">
                  Feature / Store
                </th>
                {equivalentOffers.map((offer, idx) => {
                  const isLowestPrice = minPrice > 0 && Number(offer.effectivePrice || offer.price) === minPrice;
                  const isBestRating = maxRating > 0 && Number(offer.rating) === maxRating;
                  const isFastestDel = maxDeliveryScore > 0 && getDeliverySpeedScore(offer.delivery) === maxDeliveryScore;

                  return (
                    <th key={idx} scope="col" className="p-4 font-black text-slate-900 min-w-[200px] text-center">
                      <div className="flex flex-col items-center gap-2">
                        <div className="flex items-center gap-1.5 text-base sm:text-lg font-black">
                          <span>{merchantLogos[offer.merchant] || '🏪'}</span>
                          <span>{offer.merchant}</span>
                        </div>

                        {/* Best Value Badges in Header */}
                        <div className="flex flex-wrap justify-center gap-1">
                          {isLowestPrice && (
                            <span className="px-2 py-0.5 rounded-md text-[10px] font-black uppercase tracking-wider bg-emerald-600 text-white shadow-2xs">
                              🏷️ Lowest Price
                            </span>
                          )}
                          {isBestRating && (
                            <span className="px-2 py-0.5 rounded-md text-[10px] font-black uppercase tracking-wider bg-amber-500 text-white shadow-2xs">
                              ⭐ Best Rating
                            </span>
                          )}
                          {isFastestDel && (
                            <span className="px-2 py-0.5 rounded-md text-[10px] font-black uppercase tracking-wider bg-cyan-600 text-white shadow-2xs">
                              ⚡ Fastest Delivery
                            </span>
                          )}
                        </div>
                      </div>
                    </th>
                  );
                })}
              </tr>
            </thead>

            <tbody className="divide-y divide-slate-100">
              {/* Row 1: Listed Price */}
              <tr className="divide-x divide-slate-100 hover:bg-slate-50/50 transition">
                <th scope="row" className="p-3.5 font-bold text-slate-700 bg-slate-50/60 text-xs">
                  Price
                </th>
                {equivalentOffers.map((offer, idx) => (
                  <td key={idx} className="p-3.5 text-center font-mono font-bold text-slate-800">
                    ₹{Number(offer.price).toLocaleString('en-IN')}
                  </td>
                ))}
              </tr>

              {/* Row 2: Effective Price */}
              <tr className="divide-x divide-slate-100 bg-indigo-50/20 hover:bg-indigo-50/40 transition">
                <th scope="row" className="p-3.5 font-bold text-indigo-950 bg-indigo-50/40 text-xs">
                  Effective Price
                </th>
                {equivalentOffers.map((offer, idx) => {
                  const effective = Number(offer.effectivePrice || offer.price);
                  const isLowest = minPrice > 0 && effective === minPrice;
                  return (
                    <td key={idx} className="p-3.5 text-center">
                      <div className="flex flex-col items-center">
                        <span className={`text-base font-black font-mono ${
                          isLowest ? 'text-emerald-600 font-extrabold' : 'text-slate-900'
                        }`}>
                          ₹{effective.toLocaleString('en-IN')}
                        </span>
                        {isLowest && (
                          <span className="text-[10px] font-bold text-emerald-700 uppercase bg-emerald-50 px-1.5 py-0.2 rounded border border-emerald-100 mt-0.5">
                            Best Value
                          </span>
                        )}
                      </div>
                    </td>
                  );
                })}
              </tr>

              {/* Row 3: Rating */}
              <tr className="divide-x divide-slate-100 hover:bg-slate-50/50 transition">
                <th scope="row" className="p-3.5 font-bold text-slate-700 bg-slate-50/60 text-xs">
                  Rating
                </th>
                {equivalentOffers.map((offer, idx) => {
                  const isBest = maxRating > 0 && Number(offer.rating) === maxRating;
                  return (
                    <td key={idx} className="p-3.5 text-center">
                      <span className={`font-semibold inline-flex items-center gap-1 ${
                        isBest ? 'text-amber-600 font-bold' : 'text-slate-700'
                      }`}>
                        <span>★</span>
                        <span>{offer.rating || '4.5'}</span>
                        {isBest && <span className="text-[10px] bg-amber-50 text-amber-700 px-1.5 py-0.2 rounded border border-amber-200">Top</span>}
                      </span>
                    </td>
                  );
                })}
              </tr>

              {/* Row 4: Discount */}
              <tr className="divide-x divide-slate-100 hover:bg-slate-50/50 transition">
                <th scope="row" className="p-3.5 font-bold text-slate-700 bg-slate-50/60 text-xs">
                  Discount
                </th>
                {equivalentOffers.map((offer, idx) => (
                  <td key={idx} className="p-3.5 text-center">
                    {offer.discountPercent && offer.discountPercent > 0 ? (
                      <span className="font-bold text-emerald-600 bg-emerald-50 px-2 py-0.5 rounded-md border border-emerald-100">
                        {offer.discountPercent}% OFF
                      </span>
                    ) : (
                      <span className="text-slate-400">—</span>
                    )}
                  </td>
                ))}
              </tr>

              {/* Row 5: Delivery */}
              <tr className="divide-x divide-slate-100 hover:bg-slate-50/50 transition">
                <th scope="row" className="p-3.5 font-bold text-slate-700 bg-slate-50/60 text-xs">
                  Delivery
                </th>
                {equivalentOffers.map((offer, idx) => {
                  const isFastest = maxDeliveryScore > 0 && getDeliverySpeedScore(offer.delivery) === maxDeliveryScore;
                  return (
                    <td key={idx} className="p-3.5 text-center">
                      <span className={`text-xs font-semibold ${isFastest ? 'text-cyan-700 font-bold' : 'text-slate-700'}`}>
                        🚚 {offer.delivery || 'Standard Delivery'}
                      </span>
                    </td>
                  );
                })}
              </tr>

              {/* Row 6: Availability */}
              <tr className="divide-x divide-slate-100 hover:bg-slate-50/50 transition">
                <th scope="row" className="p-3.5 font-bold text-slate-700 bg-slate-50/60 text-xs">
                  Availability
                </th>
                {equivalentOffers.map((offer, idx) => (
                  <td key={idx} className="p-3.5 text-center">
                    <span className={`px-2 py-0.5 rounded-md text-xs font-bold ${
                      offer.inStock
                        ? 'bg-emerald-50 text-emerald-700 border border-emerald-200'
                        : 'bg-rose-50 text-rose-700 border border-rose-200'
                    }`}>
                      {offer.inStock ? 'In Stock' : 'Out of Stock'}
                    </span>
                  </td>
                ))}
              </tr>

              {/* Row 7: RAM */}
              <tr className="divide-x divide-slate-100 hover:bg-slate-50/50 transition">
                <th scope="row" className="p-3.5 font-bold text-slate-700 bg-slate-50/60 text-xs">
                  RAM
                </th>
                {equivalentOffers.map((offer, idx) => (
                  <td key={idx} className="p-3.5 text-center font-mono font-medium text-slate-800">
                    {offer.attributes?.ram || activeRam || '8GB'}
                  </td>
                ))}
              </tr>

              {/* Row 8: Storage */}
              <tr className="divide-x divide-slate-100 hover:bg-slate-50/50 transition">
                <th scope="row" className="p-3.5 font-bold text-slate-700 bg-slate-50/60 text-xs">
                  Storage
                </th>
                {equivalentOffers.map((offer, idx) => (
                  <td key={idx} className="p-3.5 text-center font-mono font-bold text-slate-900 bg-slate-50/30">
                    {offer.attributes?.storage || activeStorage || '128GB'}
                  </td>
                ))}
              </tr>

              {/* Row 9: Color */}
              <tr className="divide-x divide-slate-100 hover:bg-slate-50/50 transition">
                <th scope="row" className="p-3.5 font-bold text-slate-700 bg-slate-50/60 text-xs">
                  Color
                </th>
                {equivalentOffers.map((offer, idx) => (
                  <td key={idx} className="p-3.5 text-center text-slate-600">
                    {offer.attributes?.color || 'Standard Color'}
                  </td>
                ))}
              </tr>

              {/* Action Buttons Row */}
              <tr className="divide-x divide-slate-100 bg-slate-50/80">
                <th scope="row" className="p-3.5 font-bold text-slate-700 text-xs">
                  Actions
                </th>
                {equivalentOffers.map((offer, idx) => (
                  <td key={idx} className="p-3.5 text-center">
                    <div className="flex flex-col gap-2 max-w-[160px] mx-auto">
                      <a
                        href={offer.productUrl || '#'}
                        target="_blank"
                        rel="noopener noreferrer"
                        className="py-2 px-3 rounded-xl text-xs font-bold bg-slate-900 hover:bg-slate-800 text-white shadow-xs transition text-center cursor-pointer min-h-[36px] flex items-center justify-center gap-1"
                      >
                        <span>Go to {offer.merchant}</span>
                        <span>↗</span>
                      </a>
                      {onSetPriceAlert && (
                        <button
                          type="button"
                          onClick={() => {
                            onClose();
                            onSetPriceAlert(offer);
                          }}
                          className="py-1.5 px-2.5 rounded-xl text-[11px] font-bold text-amber-700 bg-amber-50 hover:bg-amber-100 border border-amber-200 transition cursor-pointer flex items-center justify-center gap-1"
                        >
                          <span>🔔 Set Price Alert</span>
                        </button>
                      )}
                    </div>
                  </td>
                ))}
              </tr>
            </tbody>
          </table>
        </div>

        {/* MOBILE VIEW: Side-by-Side Comparison Cards */}
        <div className={`${mobileView === 'cards' ? 'block md:hidden' : 'hidden'} space-y-4`}>
          <div className="flex gap-4 overflow-x-auto pb-4 snap-x snap-mandatory no-scrollbar">
            {equivalentOffers.map((offer, idx) => {
              const isLowestPrice = minPrice > 0 && Number(offer.effectivePrice || offer.price) === minPrice;
              const isBestRating = maxRating > 0 && Number(offer.rating) === maxRating;
              const isFastestDel = maxDeliveryScore > 0 && getDeliverySpeedScore(offer.delivery) === maxDeliveryScore;

              return (
                <div
                  key={idx}
                  className={`w-72 shrink-0 bg-white rounded-2xl border p-4.5 space-y-4 shadow-sm snap-start ${
                    isLowestPrice ? 'border-emerald-400 ring-2 ring-emerald-500/20' : 'border-slate-200'
                  }`}
                >
                  {/* Card Header with Store Name & Best Badges */}
                  <div className="flex items-center justify-between border-b border-slate-100 pb-3">
                    <div className="flex items-center gap-2 font-black text-slate-900 text-base">
                      <span>{merchantLogos[offer.merchant] || '🏪'}</span>
                      <span>{offer.merchant}</span>
                    </div>
                    {isLowestPrice && (
                      <span className="px-2 py-0.5 rounded-md text-[10px] font-black uppercase bg-emerald-600 text-white">
                        Lowest Price
                      </span>
                    )}
                  </div>

                  {/* Highlights Bar */}
                  <div className="flex flex-wrap gap-1">
                    {isBestRating && (
                      <span className="px-2 py-0.5 rounded-md text-[10px] font-bold bg-amber-100 text-amber-800">
                        ⭐ Best Rating
                      </span>
                    )}
                    {isFastestDel && (
                      <span className="px-2 py-0.5 rounded-md text-[10px] font-bold bg-cyan-100 text-cyan-800">
                        ⚡ Fastest Delivery
                      </span>
                    )}
                  </div>

                  {/* 9 Spec Rows */}
                  <div className="space-y-2 text-xs divide-y divide-slate-100">
                    <div className="flex justify-between pt-1 text-slate-600">
                      <span>Price</span>
                      <span className="font-mono font-bold text-slate-800">₹{Number(offer.price).toLocaleString('en-IN')}</span>
                    </div>
                    <div className="flex justify-between pt-1">
                      <span className="font-bold text-indigo-950">Effective Price</span>
                      <span className="font-mono font-black text-indigo-600 text-sm">
                        ₹{Number(offer.effectivePrice || offer.price).toLocaleString('en-IN')}
                      </span>
                    </div>
                    <div className="flex justify-between pt-1 text-slate-600">
                      <span>Rating</span>
                      <span className="font-semibold text-amber-600">★ {offer.rating || '4.5'}</span>
                    </div>
                    <div className="flex justify-between pt-1 text-slate-600">
                      <span>Discount</span>
                      <span className="font-bold text-emerald-600">{offer.discountPercent ? `${offer.discountPercent}% OFF` : '—'}</span>
                    </div>
                    <div className="flex justify-between pt-1 text-slate-600">
                      <span>Delivery</span>
                      <span className="font-medium">🚚 {offer.delivery || 'Standard'}</span>
                    </div>
                    <div className="flex justify-between pt-1 text-slate-600">
                      <span>Availability</span>
                      <span className={`font-bold ${offer.inStock ? 'text-emerald-600' : 'text-rose-600'}`}>
                        {offer.inStock ? 'In Stock' : 'Out of Stock'}
                      </span>
                    </div>
                    <div className="flex justify-between pt-1 text-slate-600">
                      <span>RAM</span>
                      <span className="font-mono font-bold text-slate-800">{offer.attributes?.ram || activeRam || '8GB'}</span>
                    </div>
                    <div className="flex justify-between pt-1 text-slate-600">
                      <span>Storage</span>
                      <span className="font-mono font-bold text-slate-900">{offer.attributes?.storage || activeStorage || '128GB'}</span>
                    </div>
                    <div className="flex justify-between pt-1 text-slate-600">
                      <span>Color</span>
                      <span className="text-slate-800">{offer.attributes?.color || 'Standard Color'}</span>
                    </div>
                  </div>

                  {/* Actions */}
                  <div className="pt-2 flex flex-col gap-2">
                    <a
                      href={offer.productUrl || '#'}
                      target="_blank"
                      rel="noopener noreferrer"
                      className="w-full py-2.5 px-3 rounded-xl text-xs font-bold bg-slate-900 text-white text-center cursor-pointer min-h-[44px] flex items-center justify-center"
                    >
                      Visit {offer.merchant}
                    </a>
                    {onSetPriceAlert && (
                      <button
                        type="button"
                        onClick={() => {
                          onClose();
                          onSetPriceAlert(offer);
                        }}
                        className="w-full py-2 px-3 rounded-xl text-xs font-bold text-amber-700 bg-amber-50 border border-amber-200 transition cursor-pointer min-h-[44px] flex items-center justify-center gap-1"
                      >
                        🔔 Set Price Alert
                      </button>
                    )}
                  </div>
                </div>
              );
            })}
          </div>
        </div>

      </div>
    </div>
  );
};

export default ProductComparisonModal;
