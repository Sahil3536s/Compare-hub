import React, { useState } from 'react';
import PriceBadge from './PriceBadge';

export const FlightCard = ({ flight, onSelect }) => {
  const [showDetails, setShowDetails] = useState(false);

  return (
    <div className="bg-white rounded-2xl border border-slate-200 shadow-xs hover:shadow-md transition-all duration-200 overflow-hidden">
      <div className="p-5 sm:p-6 flex flex-col lg:flex-row items-stretch lg:items-center justify-between gap-6">
        
        {/* Airline Brand */}
        <div className="flex items-center gap-3.5 lg:w-48 shrink-0">
          <div className="w-11 h-11 rounded-xl bg-slate-100 flex items-center justify-center p-2 border border-slate-200 shrink-0 font-bold text-slate-800 text-sm">
            {flight.airline.slice(0, 2).toUpperCase()}
          </div>
          <div>
            <h4 className="font-bold text-sm text-slate-900 leading-snug">{flight.airline}</h4>
            <span className="text-xs text-slate-500 font-mono">{flight.airlineCode}</span>
            <div className="text-[11px] text-slate-400">{flight.aircraft}</div>
          </div>
        </div>

        {/* Flight Timing & Route Segment */}
        <div className="flex-1 flex items-center justify-between gap-4 max-w-xl mx-auto w-full">
          {/* Departure */}
          <div className="text-left">
            <div className="text-xl sm:text-2xl font-extrabold text-slate-900">{flight.departureTime}</div>
            <div className="text-xs font-semibold text-slate-700">{flight.from}</div>
            <div className="text-[11px] text-slate-400 hidden sm:block truncate max-w-[110px]">{flight.fromCity}</div>
          </div>

          {/* Timeline Visual & Stops */}
          <div className="flex-1 flex flex-col items-center px-2">
            <span className="text-xs font-medium text-slate-500 mb-1">{flight.duration}</span>
            <div className="w-full flex items-center gap-1.5">
              <span className="w-2 h-2 rounded-full border-2 border-indigo-600 bg-white"></span>
              <div className="flex-1 border-t-2 border-slate-200 relative">
                {flight.stops > 0 && (
                  <span className="absolute left-1/2 -top-1.5 -translate-x-1/2 w-2.5 h-2.5 rounded-full bg-amber-400 border-2 border-white" title={flight.stopDetails}></span>
                )}
              </div>
              <svg className="w-4 h-4 text-indigo-600 rotate-90" fill="currentColor" viewBox="0 0 20 20">
                <path d="M10.894 2.553a1 1 0 00-1.788 0l-7 14a1 1 0 001.169 1.409l5-1.429A1 1 0 009 15.571V11a1 1 0 112 0v4.571a1 1 0 00.725.962l5 1.428a1 1 0 001.17-1.408l-7-14z" />
              </svg>
            </div>
            <span className={`text-[11px] font-semibold mt-1 ${flight.stops === 0 ? 'text-emerald-600' : 'text-amber-600'}`}>
              {flight.stopDetails}
            </span>
          </div>

          {/* Arrival */}
          <div className="text-right">
            <div className="text-xl sm:text-2xl font-extrabold text-slate-900">{flight.arrivalTime}</div>
            <div className="text-xs font-semibold text-slate-700">{flight.to}</div>
            <div className="text-[11px] text-slate-400 hidden sm:block truncate max-w-[110px]">{flight.toCity}</div>
          </div>
        </div>

        {/* Price and Action Section */}
        <div className="flex sm:flex-row lg:flex-col items-center lg:items-end justify-between lg:justify-center gap-3 border-t lg:border-t-0 lg:border-l lg:border-slate-100 pt-4 lg:pt-0 lg:pl-6 shrink-0">
          <div className="text-left lg:text-right">
            <div className="flex items-center gap-1.5 justify-start lg:justify-end mb-1">
              {flight.isCheapest && <PriceBadge type="cheapest" text="Cheapest" />}
              {flight.isFastest && <PriceBadge type="fastest" text="Fastest" />}
            </div>
            <div className="text-2xl font-extrabold text-slate-900 tracking-tight">
              ₹{flight.price.toLocaleString('en-IN')}
            </div>
            <div className="text-[11px] text-slate-400">
              via <span className="font-semibold text-slate-600">{flight.dealProvider}</span>
            </div>
          </div>

          <div className="flex items-center gap-2">
            <button
              onClick={() => setShowDetails(!showDetails)}
              className="p-2 text-slate-400 hover:text-slate-600 rounded-lg hover:bg-slate-100 transition lg:hidden"
              title="Toggle details"
            >
              <svg className={`w-5 h-5 transition-transform ${showDetails ? 'rotate-180' : ''}`} fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M19 9l-7 7-7-7" />
              </svg>
            </button>
            <button
              onClick={() => onSelect && onSelect(flight)}
              className="px-5 py-2.5 bg-indigo-600 hover:bg-indigo-700 text-white text-xs sm:text-sm font-semibold rounded-xl transition shadow-xs flex items-center gap-1.5 cursor-pointer whitespace-nowrap"
            >
              <span>View Deal</span>
              <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M14 5l7 7m0 0l-7 7m7-7H3" />
              </svg>
            </button>
          </div>
        </div>

      </div>

      {/* Expanded Flight Details Strip */}
      <div className="bg-slate-50 px-5 py-2.5 border-t border-slate-100 flex flex-wrap items-center justify-between text-xs text-slate-600 gap-3">
        <div className="flex items-center gap-4 flex-wrap">
          <span className="flex items-center gap-1 text-slate-500">
            🧳 {flight.baggage}
          </span>
          <span>•</span>
          <span className="font-medium text-indigo-700 bg-indigo-50/70 px-2 py-0.5 rounded">
            {flight.cabinClass}
          </span>
        </div>

        <div className="flex items-center gap-3">
          {flight.seatsLeft && flight.seatsLeft <= 5 && (
            <span className="text-rose-600 font-semibold text-[11px]">
              Only {flight.seatsLeft} seats left at this fare
            </span>
          )}
          <button
            onClick={() => setShowDetails(!showDetails)}
            className="text-indigo-600 hover:text-indigo-800 font-semibold hidden lg:inline-flex items-center gap-1 cursor-pointer"
          >
            {showDetails ? 'Hide Details' : 'Flight Details'}
          </button>
        </div>
      </div>

      {showDetails && (
        <div className="p-5 bg-white border-t border-slate-200 text-xs text-slate-700 space-y-3">
          <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
            <div className="p-3 bg-slate-50 rounded-xl border border-slate-100 space-y-1.5">
              <span className="font-semibold text-slate-900 block">Fare Inclusions & Cancellation</span>
              <p className="text-slate-500">Free cancellation within 24 hours of booking. Standard airline change fees apply thereafter.</p>
              <p className="text-emerald-700 font-medium">✓ Hand baggage + check-in baggage included</p>
            </div>
            <div className="p-3 bg-slate-50 rounded-xl border border-slate-100 space-y-1.5">
              <span className="font-semibold text-slate-900 block">Aircraft & Amenities</span>
              <p className="text-slate-500">Operated by {flight.airline} ({flight.aircraft}). In-seat USB power and overhead entertainment stream.</p>
            </div>
          </div>
        </div>
      )}
    </div>
  );
};

export default FlightCard;
