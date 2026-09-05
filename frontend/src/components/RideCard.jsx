import React from 'react';
import PriceBadge from './PriceBadge';

export const RideCard = ({ ride, onBook }) => {
  const providerColors = {
    Uber: 'bg-black text-white',
    Ola: 'bg-emerald-600 text-white',
    Rapido: 'bg-amber-500 text-slate-900',
  };

  const vehicleIcons = {
    cab: (
      <svg className="w-6 h-6" fill="none" stroke="currentColor" viewBox="0 0 24 24">
        <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="1.5" d="M8 17a2 2 0 100-4 2 2 0 000 4zm8 0a2 2 0 100-4 2 2 0 000 4zM3 13h18l-1.5-6.5a2 2 0 00-1.9-1.5H6.4a2 2 0 00-1.9 1.5L3 13zm0 0v5a1 1 0 001 1h2m12 0h2a1 1 0 001-1v-5" />
      </svg>
    ),
    auto: (
      <svg className="w-6 h-6" fill="none" stroke="currentColor" viewBox="0 0 24 24">
        <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="1.5" d="M12 8c-1.657 0-3 .895-3 2s1.343 2 3 2 3 .895 3 2-1.343 2-3 2m0-8c1.11 0 2.08.402 2.599 1M12 8V7m0 1v8m0 0v1m0-1c-1.11 0-2.08-.402-2.599-1M21 12a9 9 0 11-18 0 9 9 0 0118 0z" />
      </svg>
    ),
    bike: (
      <svg className="w-6 h-6" fill="none" stroke="currentColor" viewBox="0 0 24 24">
        <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="1.5" d="M13 10V3L4 14h7v7l9-11h-7z" />
      </svg>
    ),
  };

  return (
    <div className={`p-4 sm:p-5 rounded-2xl border transition-all duration-200 ${
      ride.isCheapest
        ? 'bg-emerald-50/30 border-emerald-300 shadow-xs'
        : 'bg-white border-slate-200 hover:border-slate-300 shadow-xs'
    }`}>
      <div className="flex flex-col sm:flex-row items-stretch sm:items-center justify-between gap-4">
        
        {/* Left Provider & Service Info */}
        <div className="flex items-center gap-3.5">
          <div className="w-12 h-12 rounded-xl bg-slate-100 flex items-center justify-center text-slate-700 p-2.5 border border-slate-200/80 shrink-0">
            {vehicleIcons[ride.vehicleCategory] || vehicleIcons.cab}
          </div>

          <div>
            <div className="flex items-center gap-2 flex-wrap">
              <span className={`px-2 py-0.5 rounded text-[11px] font-bold ${providerColors[ride.provider] || 'bg-slate-800 text-white'}`}>
                {ride.provider}
              </span>
              <h4 className="font-bold text-slate-900 text-base">{ride.serviceName}</h4>
              {ride.isCheapest && <PriceBadge type="cheapest" text="Cheapest Fare" />}
              {ride.surgeMultiplier > 1.0 && <PriceBadge type="surge" text={`${ride.surgeMultiplier}x Surge`} />}
            </div>

            <p className="text-xs text-slate-500 mt-1">{ride.description}</p>
            
            <div className="flex items-center gap-3 text-xs text-slate-500 mt-1">
              <span className="flex items-center gap-1 text-slate-700 font-medium">
                ⏱️ {ride.etaMinutes} mins away
              </span>
              <span>•</span>
              <span>Trip: {ride.durationText}</span>
              <span>•</span>
              <span>{ride.capacity} Seats</span>
            </div>
          </div>
        </div>

        {/* Right Price & Booking Action */}
        <div className="flex items-center justify-between sm:justify-end gap-5 border-t sm:border-t-0 pt-3 sm:pt-0 border-slate-100">
          <div className="text-left sm:text-right">
            <div className="text-2xl font-extrabold text-slate-900 tracking-tight">
              ₹{ride.price}
            </div>
            {ride.originalPrice && ride.originalPrice > ride.price && (
              <span className="text-xs text-slate-400 line-through">
                ₹{ride.originalPrice}
              </span>
            )}
          </div>

          <button
            onClick={() => onBook && onBook(ride)}
            className={`px-5 py-2.5 rounded-xl text-xs sm:text-sm font-semibold transition shadow-xs flex items-center gap-1.5 cursor-pointer whitespace-nowrap ${
              ride.isCheapest
                ? 'bg-emerald-600 hover:bg-emerald-700 text-white'
                : 'bg-slate-900 hover:bg-slate-800 text-white'
            }`}
          >
            <span>Book with {ride.provider}</span>
            <svg className="w-3.5 h-3.5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M10 6H6a2 2 0 00-2 2v10a2 2 0 002 2h10a2 2 0 002-2v-4M14 4h6m0 0v6m0-6L10 14" />
            </svg>
          </button>
        </div>

      </div>
    </div>
  );
};

export default RideCard;
