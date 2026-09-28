import React from 'react';

export const RideCardSkeleton = ({ count = 3 }) => {
  const items = Array.from({ length: count }, (_, i) => i);

  return (
    <div className="space-y-4" data-testid="ride-skeleton-list">
      {items.map((i) => (
        <div
          key={i}
          className="bg-white rounded-2xl border border-slate-200 p-5 sm:p-6 shadow-xs animate-pulse flex flex-col justify-between space-y-4"
        >
          {/* Header */}
          <div className="flex items-center justify-between border-b border-slate-100 pb-3">
            <div className="flex items-center gap-3">
              <div className="w-16 h-7 bg-slate-200 rounded-lg"></div>
              <div className="space-y-1.5">
                <div className="h-4 bg-slate-200 rounded w-24"></div>
                <div className="h-3 bg-slate-200 rounded w-16"></div>
              </div>
            </div>
            <div className="h-6 bg-slate-200 rounded-lg w-20"></div>
          </div>

          {/* Middle: ETA & Distance */}
          <div className="py-2 flex items-center justify-between">
            <div className="h-4 bg-slate-200 rounded w-28"></div>
            <div className="h-4 bg-slate-200 rounded w-24"></div>
            <div className="h-4 bg-slate-200 rounded w-20"></div>
          </div>

          {/* Footer: Price & Button */}
          <div className="pt-3 border-t border-slate-100 flex items-center justify-between">
            <div className="space-y-1">
              <div className="h-3 bg-slate-200 rounded w-16"></div>
              <div className="h-7 bg-slate-200 rounded w-28"></div>
            </div>
            <div className="h-9 bg-slate-200 rounded-xl w-32"></div>
          </div>
        </div>
      ))}
    </div>
  );
};

export default RideCardSkeleton;
