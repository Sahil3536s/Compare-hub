import React from 'react';
import PriceBadge from './PriceBadge';

export const FlightOfferCard = ({ flight }) => {
  const formatDuration = (minutes) => {
    if (!minutes) return '2h 15m';
    const hrs = Math.floor(minutes / 60);
    const mins = minutes % 60;
    return `${hrs}h ${mins > 0 ? `${mins}m` : ''}`;
  };

  const getAirlineBadge = (airlineName) => {
    const name = (airlineName || '').toLowerCase();
    if (name.includes('indigo')) {
      return {
        code: '6E',
        bg: 'bg-blue-600 text-white shadow-blue-500/20',
        name: 'IndiGo',
      };
    }
    if (name.includes('air india express')) {
      return {
        code: 'IX',
        bg: 'bg-rose-600 text-white shadow-rose-500/20',
        name: 'Air India Express',
      };
    }
    if (name.includes('air india')) {
      return {
        code: 'AI',
        bg: 'bg-red-600 text-white shadow-red-500/20',
        name: 'Air India',
      };
    }
    if (name.includes('vistara')) {
      return {
        code: 'UK',
        bg: 'bg-purple-900 text-amber-200 border border-purple-700 shadow-purple-900/20',
        name: 'Vistara',
      };
    }
    if (name.includes('spicejet')) {
      return {
        code: 'SG',
        bg: 'bg-amber-600 text-white shadow-amber-500/20',
        name: 'SpiceJet',
      };
    }
    if (name.includes('akasa')) {
      return {
        code: 'QP',
        bg: 'bg-orange-500 text-white shadow-orange-500/20',
        name: 'Akasa Air',
      };
    }
    return {
      code: '✈',
      bg: 'bg-slate-800 text-white shadow-slate-800/20',
      name: airlineName || 'Airline',
    };
  };

  const badgeInfo = getAirlineBadge(flight.airline);

  const getStopsLabel = (stops) => {
    if (stops === 0 || stops === '0') return 'Non-stop';
    if (stops === 1 || stops === '1') return '1 Stop';
    return `${stops} Stops`;
  };

  return (
    <div
      data-testid="flight-card"
      className={`bg-white rounded-2xl border p-5 sm:p-6 transition-all duration-200 shadow-xs hover:shadow-md flex flex-col justify-between ${
        flight.isBest
          ? 'border-amber-300 ring-2 ring-amber-500/20 bg-linear-to-r from-amber-50/20 to-white'
          : flight.isCheapest
          ? 'border-emerald-300 ring-1 ring-emerald-500/20'
          : 'border-slate-200 hover:border-slate-300'
      }`}
    >
      {/* Top Header: Airline, Flight Number, Provider, Badges */}
      <div className="flex flex-wrap items-center justify-between gap-3 border-b border-slate-100 pb-4">
        <div className="flex items-center gap-3">
          <div
            className={`w-11 h-11 rounded-xl flex items-center justify-center font-black text-sm tracking-wider shadow-xs ${badgeInfo.bg}`}
            title={flight.airline}
          >
            {badgeInfo.code}
          </div>
          <div>
            <div className="flex items-center gap-2">
              <h3 className="font-extrabold text-base text-slate-900">{flight.airline}</h3>
              {flight.flightNumber && (
                <span className="text-xs text-slate-400 font-mono font-medium">({flight.flightNumber})</span>
              )}
            </div>
            <div className="flex items-center gap-2 text-[11px] text-slate-500 mt-0.5">
              <span>via <strong className="text-slate-700 font-semibold">{flight.provider || 'Direct Airline'}</strong></span>
            </div>
          </div>
        </div>

        {/* Badges */}
        <div className="flex items-center gap-1.5 flex-wrap">
          {flight.isBest && (
            <span className="px-2.5 py-1 rounded-lg text-xs font-bold bg-amber-100 text-amber-900 border border-amber-300 shadow-xs flex items-center gap-1">
              <span>⭐</span> Best Value
            </span>
          )}
          {flight.isCheapest && <PriceBadge type="cheapest" text="Cheapest Fare" />}
          {flight.isFastest && (
            <span className="px-2.5 py-1 rounded-lg text-xs font-bold bg-indigo-50 text-indigo-700 border border-indigo-200 shadow-xs flex items-center gap-1">
              <span>⚡</span> Fastest Flight
            </span>
          )}
        </div>
      </div>

      {/* Middle Section: Route Timeline */}
      <div className="py-6 grid grid-cols-1 sm:grid-cols-3 items-center gap-4 text-center sm:text-left">
        {/* Departure */}
        <div>
          <div className="text-2xl font-black text-slate-900 font-mono tracking-tight">
            {flight.departure}
          </div>
          <div className="text-xs font-bold text-slate-600 mt-0.5 flex items-center justify-center sm:justify-start gap-1">
            <span className="text-indigo-600">🛫</span>
            <span>{flight.origin}</span>
          </div>
        </div>

        {/* Flight Path & Duration */}
        <div className="flex flex-col items-center">
          <span className="text-xs font-semibold text-slate-500 mb-1">
            {formatDuration(flight.durationMinutes)}
          </span>
          <div className="w-full max-w-[140px] flex items-center relative">
            <div className="w-2.5 h-2.5 rounded-full border-2 border-indigo-600 bg-white"></div>
            <div className="flex-1 h-[2px] bg-slate-200 relative">
              {flight.stops > 0 && (
                <div
                  className="w-2.5 h-2.5 rounded-full bg-amber-500 border border-white absolute top-1/2 left-1/2 -translate-x-1/2 -translate-y-1/2"
                  title={`${flight.stops} stop`}
                ></div>
              )}
            </div>
            <div className="w-2.5 h-2.5 rounded-full bg-indigo-600"></div>
          </div>
          <span
            className={`text-[11px] font-bold mt-1.5 px-2 py-0.5 rounded-full ${
              flight.stops === 0
                ? 'bg-emerald-50 text-emerald-700'
                : 'bg-slate-100 text-slate-600'
            }`}
          >
            {getStopsLabel(flight.stops)}
          </span>
        </div>

        {/* Arrival */}
        <div className="sm:text-right">
          <div className="text-2xl font-black text-slate-900 font-mono tracking-tight">
            {flight.arrival}
          </div>
          <div className="text-xs font-bold text-slate-600 mt-0.5 flex items-center justify-center sm:justify-end gap-1">
            <span className="text-indigo-600">🛬</span>
            <span>{flight.destination}</span>
          </div>
        </div>
      </div>

      {/* Baggage & Inclusions Bar (if available) */}
      {(flight.cabinBaggage || flight.checkInBaggage) && (
        <div className="bg-slate-50 rounded-xl px-3.5 py-2 mb-4 flex items-center gap-4 text-xs text-slate-600 flex-wrap">
          {flight.cabinBaggage && (
            <span className="flex items-center gap-1.5 font-medium">
              <span className="text-slate-400">💼</span>
              <span>{flight.cabinBaggage}</span>
            </span>
          )}
          {flight.cabinBaggage && flight.checkInBaggage && (
            <span className="text-slate-300">•</span>
          )}
          {flight.checkInBaggage && (
            <span className="flex items-center gap-1.5 font-medium">
              <span className="text-slate-400">🧳</span>
              <span>{flight.checkInBaggage}</span>
            </span>
          )}
        </div>
      )}

      {/* Bottom Footer: Price & Booking Action */}
      <div className="pt-4 border-t border-slate-100 flex items-center justify-between gap-4">
        <div>
          <span className="text-[11px] font-bold text-slate-400 uppercase tracking-wider block">
            Total Fare
          </span>
          <div className="text-2xl font-black text-slate-900 font-mono tracking-tight">
            ₹{flight.price ? Number(flight.price).toLocaleString('en-IN') : '0'}
          </div>
        </div>

        <a
          href={flight.bookingUrl || '#'}
          target="_blank"
          rel="noopener noreferrer"
          onClick={(e) => {
            if (!flight.bookingUrl || flight.bookingUrl === '#') {
              e.preventDefault();
              alert(`Redirecting to ${flight.airline} official booking portal!`);
            }
          }}
          className={`px-5 py-2.5 rounded-xl text-xs font-bold transition shadow-xs flex items-center gap-2 cursor-pointer ${
            flight.isBest
              ? 'bg-amber-500 hover:bg-amber-600 text-slate-950 font-extrabold'
              : flight.isCheapest
              ? 'bg-emerald-600 hover:bg-emerald-700 text-white'
              : 'bg-indigo-600 hover:bg-indigo-700 text-white'
          }`}
        >
          <span>Book Flight</span>
          <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M14 5l7 7m0 0l-7 7m7-7H3" />
          </svg>
        </a>
      </div>
    </div>
  );
};

export default FlightOfferCard;
