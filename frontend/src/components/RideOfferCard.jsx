import React from 'react';
import PriceBadge from './PriceBadge';

export const RideOfferCard = ({ ride }) => {
  const getProviderTheme = (provider) => {
    const p = (provider || '').toLowerCase();
    if (p.includes('uber')) {
      return {
        badge: 'bg-black text-white border-black shadow-xs',
        btn: 'bg-black hover:bg-slate-800 text-white',
        logoText: 'UBER',
        icon: '🚗',
      };
    }
    if (p.includes('ola')) {
      return {
        badge: 'bg-amber-400 text-slate-950 border-amber-500 shadow-xs font-black',
        btn: 'bg-amber-400 hover:bg-amber-500 text-slate-950 font-black',
        logoText: 'OLA',
        icon: '🚕',
      };
    }
    if (p.includes('rapido')) {
      return {
        badge: 'bg-yellow-400 text-slate-950 border-yellow-500 shadow-xs font-black',
        btn: 'bg-yellow-400 hover:bg-yellow-500 text-slate-950 font-black',
        logoText: 'RAPIDO',
        icon: '🛵',
      };
    }
    return {
      badge: 'bg-indigo-600 text-white border-indigo-600 shadow-xs',
      btn: 'bg-indigo-600 hover:bg-indigo-700 text-white',
      logoText: provider || 'RIDE',
      icon: '🚗',
    };
  };

  const theme = getProviderTheme(ride.provider);

  const getVehicleIcon = (category) => {
    const c = (category || '').toLowerCase();
    if (c.includes('bike') || c.includes('moto')) return '🛵';
    if (c.includes('auto')) return '🛺';
    if (c.includes('premier') || c.includes('prime') || c.includes('sedan')) return '🚘';
    return '🚗';
  };

  return (
    <div
      data-testid="ride-offer-card"
      className={`bg-white rounded-2xl border p-5 sm:p-6 transition-all duration-200 shadow-xs hover:shadow-md flex flex-col justify-between ${
        ride.isBest
          ? 'border-amber-300 ring-2 ring-amber-500/20 bg-linear-to-r from-amber-50/20 to-white'
          : ride.isCheapest
          ? 'border-emerald-300 ring-1 ring-emerald-500/20'
          : 'border-slate-200 hover:border-slate-300'
      }`}
    >
      {/* Top Header: Provider Logo Badge, Ride Type, Vehicle Category, Badges */}
      <div className="flex flex-wrap items-center justify-between gap-3 border-b border-slate-100 pb-4">
        <div className="flex items-center gap-3">
          <div
            className={`px-3 py-1.5 rounded-xl text-xs font-black tracking-wider flex items-center gap-1.5 ${theme.badge}`}
            title={ride.provider}
          >
            <span>{theme.icon}</span>
            <span>{theme.logoText}</span>
          </div>
          <div>
            <div className="flex items-center gap-2">
              <h3 className="font-extrabold text-base text-slate-900 leading-tight">
                {ride.rideType}
              </h3>
              <span className="text-xs text-slate-400 font-medium">
                {getVehicleIcon(ride.vehicleCategory)} {ride.vehicleCategory}
              </span>
            </div>
            <div className="flex items-center gap-2 mt-0.5">
              <span className="text-[10px] font-semibold text-amber-700 bg-amber-50 border border-amber-200 px-1.5 py-0.5 rounded">
                Simulated / Demo Fare
              </span>
            </div>
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

      {/* Middle Details: Pickup ETA, Journey Duration, Distance */}
      <div className="py-4 grid grid-cols-3 gap-2 text-center sm:text-left border-b border-slate-50">
        {/* Pickup ETA */}
        <div className="p-2 sm:p-2.5 bg-slate-50 rounded-xl">
          <span className="text-[10px] font-bold text-slate-400 uppercase tracking-wider block">
            Pickup ETA
          </span>
          <div className="text-sm sm:text-base font-extrabold text-slate-900 flex items-center justify-center sm:justify-start gap-1 mt-0.5">
            <span className="w-2 h-2 rounded-full bg-emerald-500 animate-pulse"></span>
            <span>{ride.etaMinutes} mins</span>
          </div>
        </div>

        {/* Journey Duration */}
        <div className="p-2 sm:p-2.5 bg-slate-50 rounded-xl">
          <span className="text-[10px] font-bold text-slate-400 uppercase tracking-wider block">
            Est. Journey
          </span>
          <div className="text-sm sm:text-base font-extrabold text-slate-900 flex items-center justify-center sm:justify-start gap-1 mt-0.5">
            <span>⏱️</span>
            <span>{ride.durationMinutes ? `${ride.durationMinutes} min` : '28 min'}</span>
          </div>
        </div>

        {/* Distance */}
        <div className="p-2 sm:p-2.5 bg-slate-50 rounded-xl">
          <span className="text-[10px] font-bold text-slate-400 uppercase tracking-wider block">
            Distance
          </span>
          <div className="text-sm sm:text-base font-extrabold text-slate-900 flex items-center justify-center sm:justify-start gap-1 mt-0.5">
            <span>🛣️</span>
            <span>{ride.distanceKm ? `${ride.distanceKm} km` : '12.4 km'}</span>
          </div>
        </div>
      </div>

      {/* Bottom Footer: Estimated Fare Range & Action Button */}
      <div className="pt-4 flex items-center justify-between gap-4">
        <div>
          <span className="text-[10px] font-bold text-slate-400 uppercase tracking-wider block">
            Estimated Fare
          </span>
          <div className="text-2xl font-black text-slate-900 font-mono tracking-tight">
            ₹{ride.estimatedPriceMin ? Number(ride.estimatedPriceMin).toLocaleString('en-IN') : '0'}
            {ride.estimatedPriceMax && Number(ride.estimatedPriceMax) > Number(ride.estimatedPriceMin) && (
              <span className="text-sm font-semibold text-slate-400 ml-1">
                – ₹{Number(ride.estimatedPriceMax).toLocaleString('en-IN')}
              </span>
            )}
          </div>
        </div>

        <a
          href={ride.deepLink || '#'}
          target="_blank"
          rel="noopener noreferrer"
          onClick={(e) => {
            if (!ride.deepLink || ride.deepLink === '#') {
              e.preventDefault();
              alert(`Opening ${ride.provider} app for ${ride.rideType}...`);
            }
          }}
          className={`px-5 py-2.5 rounded-xl text-xs font-bold transition shadow-xs flex items-center gap-2 whitespace-nowrap cursor-pointer ${theme.btn}`}
        >
          <span>Open in {ride.provider}</span>
          <svg className="w-3.5 h-3.5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2.5" d="M10 6H6a2 2 0 00-2 2v10a2 2 0 002 2h10a2 2 0 002-2v-4M14 4h6m0 0v6m0-6L10 14" />
          </svg>
        </a>
      </div>
    </div>
  );
};

export default RideOfferCard;
