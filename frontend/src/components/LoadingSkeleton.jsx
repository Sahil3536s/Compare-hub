import React from 'react';

export const LoadingSkeleton = ({ type = 'product-card', count = 3 }) => {
  const items = Array.from({ length: count }, (_, i) => i);

  if (type === 'product-card') {
    return (
      <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-6">
        {items.map((i) => (
          <div key={i} className="bg-white rounded-2xl border border-slate-200 p-5 shadow-xs animate-pulse flex flex-col space-y-4">
            <div className="w-full h-48 bg-slate-200 rounded-xl"></div>
            <div className="h-4 bg-slate-200 rounded w-1/3"></div>
            <div className="h-5 bg-slate-200 rounded w-4/5"></div>
            <div className="h-4 bg-slate-200 rounded w-1/2"></div>
            <div className="pt-4 border-t border-slate-100 flex justify-between items-center">
              <div className="h-6 bg-slate-200 rounded w-1/3"></div>
              <div className="h-8 bg-slate-200 rounded w-24"></div>
            </div>
          </div>
        ))}
      </div>
    );
  }

  if (type === 'flight-card') {
    return (
      <div className="space-y-4">
        {items.map((i) => (
          <div key={i} className="bg-white rounded-2xl border border-slate-200 p-6 shadow-xs animate-pulse flex flex-col md:flex-row justify-between gap-6 items-center">
            <div className="flex items-center gap-4 w-full md:w-1/4">
              <div className="w-12 h-12 bg-slate-200 rounded-xl"></div>
              <div className="space-y-2 flex-1">
                <div className="h-4 bg-slate-200 rounded w-3/4"></div>
                <div className="h-3 bg-slate-200 rounded w-1/2"></div>
              </div>
            </div>
            <div className="flex items-center justify-center gap-6 w-full md:w-1/2">
              <div className="h-6 bg-slate-200 rounded w-16"></div>
              <div className="h-2 bg-slate-200 rounded w-24"></div>
              <div className="h-6 bg-slate-200 rounded w-16"></div>
            </div>
            <div className="flex md:flex-col items-center md:items-end justify-between w-full md:w-1/4 gap-3">
              <div className="h-6 bg-slate-200 rounded w-24"></div>
              <div className="h-9 bg-slate-200 rounded w-28"></div>
            </div>
          </div>
        ))}
      </div>
    );
  }

  if (type === 'ride-card') {
    return (
      <div className="space-y-3">
        {items.map((i) => (
          <div key={i} className="bg-white rounded-xl border border-slate-200 p-4 shadow-xs animate-pulse flex items-center justify-between gap-4">
            <div className="flex items-center gap-3">
              <div className="w-10 h-10 bg-slate-200 rounded-lg"></div>
              <div className="space-y-1.5">
                <div className="h-4 bg-slate-200 rounded w-24"></div>
                <div className="h-3 bg-slate-200 rounded w-32"></div>
              </div>
            </div>
            <div className="flex items-center gap-4">
              <div className="h-5 bg-slate-200 rounded w-16"></div>
              <div className="h-8 bg-slate-200 rounded w-20"></div>
            </div>
          </div>
        ))}
      </div>
    );
  }

  return (
    <div className="space-y-3 animate-pulse">
      {items.map((i) => (
        <div key={i} className="h-10 bg-slate-200 rounded-lg w-full"></div>
      ))}
    </div>
  );
};

export default LoadingSkeleton;
