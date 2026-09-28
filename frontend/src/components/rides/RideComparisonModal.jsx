import React, { useState } from 'react';

export const RideComparisonModal = ({
  isOpen,
  onClose,
  rides = [],
  pickupAddress = '',
  destinationAddress = '',
}) => {
  const [selectedCategory, setSelectedCategory] = useState('Cab'); // 'Cab' | 'Auto' | 'Bike'

  if (!isOpen) return null;

  // Filter rides matching category or all
  const categoryRides = rides.filter(
    (r) => (r.vehicleCategory || '').toLowerCase() === selectedCategory.toLowerCase()
  );

  // Group by distinct provider (Uber, Ola, Rapido)
  const providers = ['Uber', 'Ola', 'Rapido'];
  const providerOffers = providers.map((prov) => {
    return categoryRides.find(
      (r) => (r.provider || '').toLowerCase() === prov.toLowerCase()
    ) || null;
  });

  // Highlight metrics
  const validOffers = providerOffers.filter(Boolean);
  const minFare = validOffers.length > 0
    ? Math.min(...validOffers.map((o) => Number(o.estimatedPriceMin || o.estimatedPriceMax || 999999)))
    : null;
  const minEta = validOffers.length > 0
    ? Math.min(...validOffers.map((o) => Number(o.etaMinutes || 999)))
    : null;

  const categories = ['Cab', 'Auto', 'Bike'];

  return (
    <div
      role="dialog"
      aria-modal="true"
      aria-labelledby="ride-comparison-title"
      className="fixed inset-0 z-50 overflow-y-auto bg-slate-900/60 backdrop-blur-xs flex items-center justify-center p-3 sm:p-6 animate-fadeIn"
      onClick={onClose}
    >
      <div
        className="bg-white rounded-3xl shadow-2xl border border-slate-200 max-w-4xl w-full p-6 sm:p-8 space-y-6 max-h-[90vh] overflow-y-auto"
        onClick={(e) => e.stopPropagation()}
      >
        {/* Modal Header */}
        <div className="flex items-start justify-between gap-4 border-b border-slate-100 pb-5">
          <div>
            <div className="flex items-center gap-2 text-xs text-indigo-600 font-bold uppercase tracking-wider mb-1">
              <span>🚗</span> Side-by-Side Fare Matrix
            </div>
            <h2 id="ride-comparison-title" className="text-xl sm:text-2xl font-black text-slate-900">
              Provider Ride Comparison
            </h2>
            <p className="text-xs text-slate-500 mt-1">
              {pickupAddress ? pickupAddress.split(',')[0] : 'Pickup'} → {destinationAddress ? destinationAddress.split(',')[0] : 'Destination'}
            </p>
          </div>
          <button
            type="button"
            onClick={onClose}
            aria-label="Close comparison"
            className="w-9 h-9 rounded-full bg-slate-100 text-slate-500 hover:bg-slate-200 hover:text-slate-900 flex items-center justify-center font-bold text-sm transition cursor-pointer"
          >
            ✕
          </button>
        </div>

        {/* Vehicle Category Selector */}
        <div className="flex items-center gap-2">
          <span className="text-xs font-bold text-slate-500 uppercase tracking-wider mr-1">
            Category:
          </span>
          {categories.map((cat) => (
            <button
              key={cat}
              type="button"
              onClick={() => setSelectedCategory(cat)}
              className={`px-4 py-2 rounded-xl text-xs font-bold transition cursor-pointer flex items-center gap-1.5 ${
                selectedCategory === cat
                  ? 'bg-indigo-600 text-white shadow-xs'
                  : 'bg-slate-100 text-slate-600 hover:bg-slate-200'
              }`}
            >
              <span>{cat === 'Bike' ? '🛵' : cat === 'Auto' ? '🛺' : '🚗'}</span>
              <span>{cat}s</span>
            </button>
          ))}
        </div>

        {/* Comparison Table */}
        <div className="overflow-x-auto rounded-2xl border border-slate-200">
          <table className="w-full text-left text-xs border-collapse">
            <thead>
              <tr className="bg-slate-50 border-b border-slate-200">
                <th className="p-3.5 sm:p-4 text-slate-500 font-bold uppercase tracking-wider w-36">
                  Attribute
                </th>
                {providers.map((prov) => (
                  <th key={prov} className="p-3.5 sm:p-4 font-black text-slate-900 text-sm">
                    <span className="flex items-center gap-1.5">
                      <span className="w-2.5 h-2.5 rounded-full bg-indigo-600"></span>
                      <span>{prov}</span>
                    </span>
                  </th>
                ))}
              </tr>
            </thead>
            <tbody className="divide-y divide-slate-100 font-medium text-slate-700">
              {/* Fare Row */}
              <tr className="hover:bg-slate-50/50">
                <td className="p-3.5 sm:p-4 font-bold text-slate-900 bg-slate-50/30">
                  Estimated Fare
                </td>
                {providerOffers.map((offer, idx) => {
                  if (!offer) {
                    return <td key={idx} className="p-3.5 sm:p-4 text-slate-400 italic">Not available</td>;
                  }
                  const isLowest = minFare !== null && Number(offer.estimatedPriceMin) === minFare;
                  return (
                    <td key={idx} className="p-3.5 sm:p-4">
                      <div className="text-base font-black text-slate-900 font-mono">
                        ₹{Number(offer.estimatedPriceMin).toLocaleString('en-IN')}
                        {offer.estimatedPriceMax && (
                          <span className="text-xs text-slate-400 font-normal ml-1">
                            – ₹{Number(offer.estimatedPriceMax).toLocaleString('en-IN')}
                          </span>
                        )}
                      </div>
                      {isLowest && (
                        <span className="inline-block mt-1 px-2 py-0.5 rounded-full bg-emerald-100 text-emerald-800 font-bold text-[10px]">
                          ✓ Lowest Fare
                        </span>
                      )}
                    </td>
                  );
                })}
              </tr>

              {/* ETA Row */}
              <tr className="hover:bg-slate-50/50">
                <td className="p-3.5 sm:p-4 font-bold text-slate-900 bg-slate-50/30">
                  Pickup ETA
                </td>
                {providerOffers.map((offer, idx) => {
                  if (!offer) {
                    return <td key={idx} className="p-3.5 sm:p-4 text-slate-400 italic">—</td>;
                  }
                  const isFastest = minEta !== null && Number(offer.etaMinutes) === minEta;
                  return (
                    <td key={idx} className="p-3.5 sm:p-4">
                      <div className="font-extrabold text-slate-900">
                        {offer.etaMinutes} mins
                      </div>
                      {isFastest && (
                        <span className="inline-block mt-1 px-2 py-0.5 rounded-full bg-indigo-100 text-indigo-800 font-bold text-[10px]">
                          ⚡ Fastest Pickup
                        </span>
                      )}
                    </td>
                  );
                })}
              </tr>

              {/* Vehicle Row */}
              <tr className="hover:bg-slate-50/50">
                <td className="p-3.5 sm:p-4 font-bold text-slate-900 bg-slate-50/30">
                  Vehicle Type
                </td>
                {providerOffers.map((offer, idx) => (
                  <td key={idx} className="p-3.5 sm:p-4 font-bold text-slate-900">
                    {offer ? offer.rideType : '—'}
                  </td>
                ))}
              </tr>

              {/* Duration Row */}
              <tr className="hover:bg-slate-50/50">
                <td className="p-3.5 sm:p-4 font-bold text-slate-900 bg-slate-50/30">
                  Est. Duration
                </td>
                {providerOffers.map((offer, idx) => (
                  <td key={idx} className="p-3.5 sm:p-4">
                    {offer && offer.durationMinutes ? `${offer.durationMinutes} mins` : '28 mins'}
                  </td>
                ))}
              </tr>

              {/* Distance Row */}
              <tr className="hover:bg-slate-50/50">
                <td className="p-3.5 sm:p-4 font-bold text-slate-900 bg-slate-50/30">
                  Distance
                </td>
                {providerOffers.map((offer, idx) => (
                  <td key={idx} className="p-3.5 sm:p-4 font-mono">
                    {offer && offer.distanceKm ? `${offer.distanceKm} km` : '—'}
                  </td>
                ))}
              </tr>

              {/* Value Assessment Row */}
              <tr className="hover:bg-slate-50/50">
                <td className="p-3.5 sm:p-4 font-bold text-slate-900 bg-slate-50/30">
                  Best Value Rating
                </td>
                {providerOffers.map((offer, idx) => (
                  <td key={idx} className="p-3.5 sm:p-4">
                    {offer?.isBest ? (
                      <span className="px-2.5 py-1 rounded-lg bg-amber-100 text-amber-900 border border-amber-300 font-extrabold text-[11px] inline-flex items-center gap-1">
                        <span>⭐</span> Best Value
                      </span>
                    ) : offer?.score ? (
                      <span className="text-slate-500 font-bold">{offer.score}/100</span>
                    ) : (
                      '—'
                    )}
                  </td>
                ))}
              </tr>

              {/* Action Link Row */}
              <tr>
                <td className="p-3.5 sm:p-4 bg-slate-50/30 font-bold text-slate-900">
                  Book Ride
                </td>
                {providerOffers.map((offer, idx) => (
                  <td key={idx} className="p-3.5 sm:p-4">
                    {offer ? (
                      <a
                        href={offer.deepLink || '#'}
                        target="_blank"
                        rel="noopener noreferrer"
                        className="inline-flex items-center gap-1.5 px-3 py-1.5 rounded-xl bg-slate-900 hover:bg-slate-800 text-white font-bold text-xs shadow-xs transition"
                      >
                        <span>Open in {offer.provider}</span>
                        <span>↗</span>
                      </a>
                    ) : (
                      <span className="text-slate-400 text-xs italic">Unavailable</span>
                    )}
                  </td>
                ))}
              </tr>
            </tbody>
          </table>
        </div>

        {/* Disclaimer */}
        <div className="bg-amber-50 border border-amber-200 rounded-xl p-3 text-[11px] text-amber-800">
          <strong>Notice:</strong> Ride fares and ETAs displayed above are generated via standard urban simulation and distance telemetry models. Final fares depend on driver acceptance and real-time provider surcharge conditions.
        </div>
      </div>
    </div>
  );
};

export default RideComparisonModal;
