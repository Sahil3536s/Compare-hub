import React from 'react';

export const NluIntentBadge = ({ queryUnderstanding, intent }) => {
  if (!queryUnderstanding) return null;

  const {
    intent: detectedIntent,
    confidence,
    productEntities,
    flightEntities,
    rideEntities,
  } = queryUnderstanding;

  const currentIntent = detectedIntent || intent;

  return (
    <div className="bg-gradient-to-r from-indigo-50/80 via-purple-50/50 to-sky-50/80 border border-indigo-100/90 rounded-2xl p-4 space-y-3" data-testid="nlu-intent-badge">
      <div className="flex flex-wrap items-center justify-between gap-2">
        <div className="flex items-center gap-2">
          <span className="inline-flex items-center gap-1.5 px-2.5 py-1 rounded-lg text-xs font-bold bg-indigo-600 text-white shadow-2xs">
            <span>✨</span>
            <span>NLU Understood</span>
          </span>
          <span className="text-xs font-semibold text-slate-700">
            {currentIntent === 'PRODUCT_SEARCH' && 'Product Search Intent'}
            {currentIntent === 'FLIGHT_SEARCH' && 'Flight Routing Intent'}
            {currentIntent === 'RIDE_SEARCH' && 'Ride Fare Intent'}
            {currentIntent === 'UNKNOWN' && 'General Query'}
          </span>
        </div>

        {confidence > 0 && (
          <span className="text-[11px] font-bold px-2 py-0.5 rounded-full bg-emerald-100 text-emerald-800 border border-emerald-200">
            {Math.round(confidence * 100)}% Confidence
          </span>
        )}
      </div>

      {/* Extracted Structured Entity Chips */}
      <div className="flex flex-wrap items-center gap-2 text-xs">
        {/* Product Entities */}
        {productEntities && (
          <>
            {productEntities.brand && (
              <span className="inline-flex items-center gap-1 px-2.5 py-1 rounded-xl bg-white border border-slate-200 text-slate-700 font-semibold shadow-2xs">
                <span>🏷️</span>
                <span>Brand: <strong className="text-slate-900">{productEntities.brand}</strong></span>
              </span>
            )}
            {productEntities.category && (
              <span className="inline-flex items-center gap-1 px-2.5 py-1 rounded-xl bg-white border border-slate-200 text-slate-700 font-semibold shadow-2xs">
                <span>📦</span>
                <span>{productEntities.category}</span>
              </span>
            )}
            {productEntities.maxPrice && (
              <span className="inline-flex items-center gap-1 px-2.5 py-1 rounded-xl bg-emerald-50 border border-emerald-200 text-emerald-800 font-bold shadow-2xs font-mono">
                <span>💰</span>
                <span>Max ₹{Number(productEntities.maxPrice).toLocaleString('en-IN')}</span>
              </span>
            )}
            {productEntities.storage && (
              <span className="inline-flex items-center gap-1 px-2.5 py-1 rounded-xl bg-white border border-slate-200 text-slate-700 font-semibold shadow-2xs">
                <span>💾</span>
                <span>{productEntities.storage}</span>
              </span>
            )}
            {productEntities.ram && (
              <span className="inline-flex items-center gap-1 px-2.5 py-1 rounded-xl bg-white border border-slate-200 text-slate-700 font-semibold shadow-2xs">
                <span>🧠</span>
                <span>{productEntities.ram}</span>
              </span>
            )}
            {productEntities.priority && (
              <span className="inline-flex items-center gap-1 px-2.5 py-1 rounded-xl bg-purple-50 border border-purple-200 text-purple-700 font-bold shadow-2xs capitalize">
                <span>🎯</span>
                <span>{productEntities.priority} Priority</span>
              </span>
            )}
          </>
        )}

        {/* Flight Entities */}
        {flightEntities && (
          <>
            {flightEntities.origin && flightEntities.destination && (
              <span className="inline-flex items-center gap-1 px-2.5 py-1 rounded-xl bg-sky-50 border border-sky-200 text-sky-800 font-bold shadow-2xs font-mono">
                <span>✈️</span>
                <span>{flightEntities.origin} → {flightEntities.destination}</span>
              </span>
            )}
            {flightEntities.departureDate && (
              <span className="inline-flex items-center gap-1 px-2.5 py-1 rounded-xl bg-white border border-slate-200 text-slate-700 font-semibold shadow-2xs">
                <span>📅</span>
                <span>{flightEntities.departureDate}</span>
              </span>
            )}
            {flightEntities.stops === 0 && (
              <span className="inline-flex items-center gap-1 px-2.5 py-1 rounded-xl bg-emerald-50 border border-emerald-200 text-emerald-800 font-bold shadow-2xs">
                <span>⚡</span>
                <span>Non-Stop Only</span>
              </span>
            )}
            {flightEntities.timePreference && (
              <span className="inline-flex items-center gap-1 px-2.5 py-1 rounded-xl bg-amber-50 border border-amber-200 text-amber-800 font-bold shadow-2xs capitalize">
                <span>🌆</span>
                <span>{flightEntities.timePreference.toLowerCase()}</span>
              </span>
            )}
          </>
        )}

        {/* Ride Entities */}
        {rideEntities && (
          <>
            {rideEntities.pickup && (
              <span className="inline-flex items-center gap-1 px-2.5 py-1 rounded-xl bg-white border border-slate-200 text-slate-700 font-semibold shadow-2xs">
                <span>📍</span>
                <span>From: <strong className="text-slate-900">{rideEntities.pickup}</strong></span>
              </span>
            )}
            {rideEntities.destination && (
              <span className="inline-flex items-center gap-1 px-2.5 py-1 rounded-xl bg-emerald-50 border border-emerald-200 text-emerald-800 font-bold shadow-2xs">
                <span>🏁</span>
                <span>To: <strong>{rideEntities.destination}</strong></span>
              </span>
            )}
            {rideEntities.rideType && (
              <span className="inline-flex items-center gap-1 px-2.5 py-1 rounded-xl bg-white border border-slate-200 text-slate-700 font-semibold shadow-2xs capitalize">
                <span>🚗</span>
                <span>{rideEntities.rideType}</span>
              </span>
            )}
          </>
        )}
      </div>
    </div>
  );
};

export default NluIntentBadge;
