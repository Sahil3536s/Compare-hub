import React from 'react';
import PriceBadge from './PriceBadge';

export const RideOfferCard = ({ ride }) => {
  const providerTheme = {
    Uber: {
      badge: 'bg-black text-white border-black',
      btn: 'bg-black hover:bg-slate-800 text-white',
      icon: '🚗',
    },
    Ola: {
      badge: 'bg-amber-400 text-slate-950 border-amber-500',
      btn: 'bg-amber-400 hover:bg-amber-500 text-slate-950 font-black',
      icon: '🚕',
    },
    Rapido: {
      badge: 'bg-yellow-400 text-slate-950 border-yellow-500',
      btn: 'bg-yellow-400 hover:bg-yellow-500 text-slate-950 font-black',
      icon: '🛵',
    },
  };

  const theme = providerTheme[ride.provider] || {
    badge: 'bg-indigo-50 text-indigo-700 border-indigo-200',
    btn: 'bg-indigo-600 hover:bg-indigo-700 text-white',
    icon: '🚗',
  };

  return (
    <div className={`bg-white rounded-2xl border p-5 sm:p-6 transition-all duration-200 shadow-xs hover:shadow-md flex flex-col justify-between ${
      ride.isBest
        ? 'border-amber-300 ring-2 ring-amber-500/20 bg-linear-to-r from-amber-50/20 to-white'
        : ride.isCheapest
        ? 'border-emerald-300 ring-1 ring-emerald-500/20'
        : 'border-slate-200 hover:border-slate-300'
    }`}>
      {/* Top Header: Provider & Badges */}
      <div className="flex items-center justify-between gap-3 border-b border-slate-100 pb-3">
        <div className="flex items-center gap-3">
          <span className={`px-2.5 py-1 rounded-lg text-xs font-black border shadow-xs ${theme.badge}`}>
            {ride.provider}
          </span>
          <div>
            <h3 className="font-extrabold text-base text-slate-900 leading-none">{ride.rideType}</h3>
            <span className="text-[11px] font-semibold text-slate-400 uppercase tracking-wider">{ride.vehicleCategory}</span>
          </div>
        </div>

        {/* Badges */}
        <div className="flex items-center gap-1.5 flex-wrap">
          {ride.isBest && (
            <span className="px-2.5 py-1 rounded-lg text-xs font-bold bg-amber-100 text-amber-900 border border-amber-300 shadow-xs flex items-center gap-1">
              <span>⭐</span> Best Value
            </span>
          )}
          {ride.isCheapest && <PriceBadge type="cheapest" text="Cheapest Fare" />}
          {ride.isFastest && (
            <span className="px-2.5 py-1 rounded-lg text-xs font-bold bg-indigo-50 text-indigo-700 border border-indigo-200 shadow-xs flex items-center gap-1">
              <span>⚡</span> Fastest Pickup
            </span>
          )}
        </div>
      </div>

      {/* Middle: ETA & Distance Details */}
      <div className="py-4 flex items-center justify-between text-xs text-slate-600">
        <div className="flex items-center gap-1.5 font-bold text-slate-900">
          <span className="w-2 h-2 rounded-full bg-emerald-500 animate-pulse"></span>
          <span>Pickup in <strong className="text-emerald-600 font-extrabold">{ride.etaMinutes} mins</strong></span>
        </div>
        <div className="text-slate-400 font-medium">
          Distance: {ride.distanceKm} km
        </div>
      </div>

      {/* Bottom: Fare Range & Booking Action */}
      <div className="pt-3 border-t border-slate-100 flex items-center justify-between gap-4">
        <div>
          <span className="text-[10px] font-bold text-slate-400 uppercase tracking-wider block">Estimated Fare</span>
          <div className="text-2xl font-black text-slate-900 font-mono tracking-tight">
            ₹{ride.estimatedPriceMin ? Number(ride.estimatedPriceMin).toLocaleString('en-IN') : '0'}
            {ride.estimatedPriceMax && Number(ride.estimatedPriceMax) > Number(ride.estimatedPriceMin) && (
              <span className="text-sm font-normal text-slate-400 ml-1">
                - ₹{Number(ride.estimatedPriceMax).toLocaleString('en-IN')}
              </span>
            )}
          </div>
        </div>

        <a
          href={ride.deepLink || '#'}
          target="_blank"
          rel="noopener noreferrer"
          onClick={(e) => {
            if (ride.deepLink === '#') {
              e.preventDefault();
              alert(`Opening ${ride.provider} app for ${ride.rideType}...`);
            }
          }}
          className={`px-4 py-2.5 rounded-xl text-xs font-bold transition shadow-xs flex items-center gap-1.5 whitespace-nowrap cursor-pointer ${theme.btn}`}
        >
          <span>Open in {ride.provider}</span>
          <svg className="w-3.5 h-3.5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M10 6H6a2 2 0 00-2 2v10a2 2 0 002 2h10a2 2 0 002-2v-4M14 4h6m0 0v6m0-6L10 14" />
          </svg>
        </a>
      </div>
    </div>
  );
};

export default RideOfferCard;
