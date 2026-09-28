import React, { useState } from 'react';

export const RankingExplanationBanner = ({ rankingSummary, type = 'product' }) => {
  const [expanded, setExpanded] = useState(false);

  if (!rankingSummary) return null;

  const { cheapest, bestValue, highestRated, fastest, weights = {}, explanation } = rankingSummary;

  // Weight display metadata based on comparison domain
  const weightLabels = {
    price: { label: 'Price / Fare', color: 'bg-emerald-500' },
    fare: { label: 'Estimated Fare', color: 'bg-emerald-500' },
    rating: { label: 'Customer Rating', color: 'bg-amber-500' },
    discount: { label: 'Discount %', color: 'bg-purple-500' },
    delivery: { label: 'Delivery Speed', color: 'bg-blue-500' },
    trust: { label: 'Stock & Trust', color: 'bg-teal-500' },
    duration: { label: 'Flight Duration', color: 'bg-indigo-500' },
    stops: { label: 'Stops & Layovers', color: 'bg-rose-500' },
    eta: { label: 'Driver Pickup ETA', color: 'bg-sky-500' },
  };

  return (
    <div className="bg-slate-50 border border-slate-200/80 rounded-2xl p-4 sm:p-5 mb-6 text-slate-800 transition-all shadow-xs">
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-3">
        <div className="flex items-center gap-2.5">
          <span className="w-8 h-8 rounded-xl bg-amber-100 text-amber-800 flex items-center justify-center font-bold text-sm shrink-0">
            ⚖️
          </span>
          <div>
            <div className="flex items-center gap-2">
              <h4 className="font-extrabold text-xs sm:text-sm text-slate-900 tracking-tight">
                Deterministic Best Value Ranking
              </h4>
              <span className="px-2 py-0.5 bg-indigo-50 text-indigo-700 text-[10px] font-bold rounded-md border border-indigo-200/50">
                Mathematical Model
              </span>
            </div>
            <p className="text-[11px] text-slate-500 mt-0.5">
              Every offer is mathematically normalized across all attributes before scoring.
            </p>
          </div>
        </div>

        <button
          onClick={() => setExpanded(!expanded)}
          className="text-xs font-bold text-indigo-600 hover:text-indigo-800 flex items-center gap-1 cursor-pointer self-start sm:self-auto py-1 px-2.5 rounded-lg hover:bg-indigo-50/60 transition"
        >
          <span>{expanded ? 'Hide Factor Breakdown' : 'View Scoring Formula'}</span>
          <span>{expanded ? '▲' : '▼'}</span>
        </button>
      </div>

      {/* Highlights Bar */}
      <div className="grid grid-cols-2 sm:grid-cols-4 gap-2.5 mt-3 pt-3 border-t border-slate-200/60">
        {bestValue && bestValue !== 'N/A' && (
          <div className="bg-white p-2.5 rounded-xl border border-slate-100 shadow-2xs">
            <span className="text-[10px] font-bold uppercase tracking-wider text-indigo-600 block">🏆 Best Value</span>
            <span className="text-xs font-black text-slate-900 truncate block mt-0.5">{bestValue}</span>
          </div>
        )}
        {cheapest && cheapest !== 'N/A' && (
          <div className="bg-white p-2.5 rounded-xl border border-slate-100 shadow-2xs">
            <span className="text-[10px] font-bold uppercase tracking-wider text-emerald-600 block">💰 Lowest Price</span>
            <span className="text-xs font-black text-slate-900 truncate block mt-0.5">{cheapest}</span>
          </div>
        )}
        {highestRated && highestRated !== 'N/A' && (
          <div className="bg-white p-2.5 rounded-xl border border-slate-100 shadow-2xs">
            <span className="text-[10px] font-bold uppercase tracking-wider text-amber-600 block">⭐ Top Rated</span>
            <span className="text-xs font-black text-slate-900 truncate block mt-0.5">{highestRated}</span>
          </div>
        )}
        {fastest && fastest !== 'N/A' && (
          <div className="bg-white p-2.5 rounded-xl border border-slate-100 shadow-2xs">
            <span className="text-[10px] font-bold uppercase tracking-wider text-sky-600 block">⚡ Fastest</span>
            <span className="text-xs font-black text-slate-900 truncate block mt-0.5">{fastest}</span>
          </div>
        )}
      </div>

      {/* Expanded Factor Breakdown */}
      {expanded && (
        <div className="mt-4 pt-4 border-t border-slate-200/60 space-y-3 animate-fadeIn">
          {explanation && (
            <p className="text-xs text-slate-600 font-medium">
              ℹ️ {explanation}
            </p>
          )}

          <div className="space-y-2">
            <span className="text-[10px] font-bold uppercase tracking-wider text-slate-400">
              Normalized Weights Distribution
            </span>
            <div className="flex h-3 w-full rounded-full overflow-hidden bg-slate-200 shadow-inner">
              {Object.entries(weights).map(([key, val]) => {
                const meta = weightLabels[key] || { label: key, color: 'bg-slate-400' };
                const pct = Math.round(val * 100);
                return (
                  <div
                    key={key}
                    className={`${meta.color} transition-all duration-500`}
                    style={{ width: `${pct}%` }}
                    title={`${meta.label}: ${pct}%`}
                  ></div>
                );
              })}
            </div>

            <div className="flex flex-wrap gap-3 pt-1 text-[11px]">
              {Object.entries(weights).map(([key, val]) => {
                const meta = weightLabels[key] || { label: key, color: 'bg-slate-400' };
                const pct = Math.round(val * 100);
                return (
                  <div key={key} className="flex items-center gap-1.5">
                    <span className={`w-2.5 h-2.5 rounded-full ${meta.color}`}></span>
                    <span className="text-slate-600 font-semibold">{meta.label}:</span>
                    <span className="font-bold text-slate-900">{pct}%</span>
                  </div>
                );
              })}
            </div>
          </div>
        </div>
      )}
    </div>
  );
};

export default RankingExplanationBanner;
