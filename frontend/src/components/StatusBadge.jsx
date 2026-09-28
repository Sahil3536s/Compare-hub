import React from 'react';

export const StatusBadge = ({ isConnected, loading, error, onRetry }) => {
  if (loading) {
    return (
      <div className="inline-flex items-center gap-2 px-4 py-2 rounded-full text-sm font-medium bg-amber-50 text-amber-800 border border-amber-200 shadow-sm animate-pulse">
        <span className="w-2.5 h-2.5 rounded-full bg-amber-400"></span>
        Connecting to Backend...
      </div>
    );
  }

  if (isConnected) {
    return (
      <div className="inline-flex items-center gap-2 px-4 py-2 rounded-full text-sm font-medium bg-emerald-50 text-emerald-800 border border-emerald-200 shadow-sm">
        <span className="w-2.5 h-2.5 rounded-full bg-emerald-500 animate-ping"></span>
        <span className="w-2.5 h-2.5 rounded-full bg-emerald-500 -ml-5"></span>
        Backend Connected
      </div>
    );
  }

  return (
    <div className="flex flex-col sm:flex-row items-center gap-3">
      <div className="inline-flex items-center gap-2 px-4 py-2 rounded-full text-sm font-medium bg-rose-50 text-rose-800 border border-rose-200 shadow-sm">
        <span className="w-2.5 h-2.5 rounded-full bg-rose-500"></span>
        Backend Disconnected
      </div>
      {onRetry && (
        <button
          onClick={onRetry}
          className="text-xs px-3 py-1.5 rounded-md bg-white hover:bg-slate-50 border border-slate-300 text-slate-700 font-medium transition shadow-xs"
        >
          Retry Connection
        </button>
      )}
    </div>
  );
};

export default StatusBadge;
