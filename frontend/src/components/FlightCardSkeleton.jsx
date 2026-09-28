import React from 'react';

export const FlightCardSkeleton = ({ count = 3 }) => {
  const items = Array.from({ length: count }, (_, i) => i);

  return (
    <div className="space-y-4" data-testid="flight-skeleton-list">
      {items.map((i) => (
        <div
          key={i}
          className="bg-white rounded-2xl border border-slate-200 p-5 sm:p-6 shadow-xs animate-pulse flex flex-col justify-between space-y-4"
        >
          {/* Header */}
          <div className="flex items-center justify-between border-b border-slate-100 pb-4">
            <div className="flex items-center gap-3">
              <div className="w-11 h-11 bg-slate-200 rounded-xl"></div>
              <div className="space-y-1.5">
                <div className="h-4 bg-slate-200 rounded w-28"></div>
                <div className="h-3 bg-slate-200 rounded w-20"></div>
              </div>
            </div>
            <div className="h-6 bg-slate-200 rounded-lg w-24"></div>
          </div>

          {/* Timeline & Route */}
          <div className="py-4 grid grid-cols-1 sm:grid-cols-3 items-center gap-4 text-center sm:text-left">
            <div className="space-y-1.5">
              <div className="h-7 bg-slate-200 rounded w-20 mx-auto sm:mx-0"></div>
              <div className="h-3 bg-slate-200 rounded w-16 mx-auto sm:mx-0"></div>
            </div>

            <div className="flex flex-col items-center space-y-2">
              <div className="h-3 bg-slate-200 rounded w-16"></div>
              <div className="w-28 h-1 bg-slate-200 rounded"></div>
              <div className="h-4 bg-slate-200 rounded-full w-20"></div>
            </div>

            <div className="space-y-1.5 sm:text-right flex flex-col sm:items-end">
              <div className="h-7 bg-slate-200 rounded w-20"></div>
              <div className="h-3 bg-slate-200 rounded w-16"></div>
            </div>
          </div>

          {/* Baggage strip */}
          <div className="h-7 bg-slate-100 rounded-xl w-3/4"></div>

          {/* Footer */}
          <div className="pt-4 border-t border-slate-100 flex items-center justify-between">
            <div className="space-y-1">
              <div className="h-3 bg-slate-200 rounded w-14"></div>
              <div className="h-7 bg-slate-200 rounded w-24"></div>
            </div>
            <div className="h-9 bg-slate-200 rounded-xl w-28"></div>
          </div>
        </div>
      ))}
    </div>
  );
};

export default FlightCardSkeleton;
