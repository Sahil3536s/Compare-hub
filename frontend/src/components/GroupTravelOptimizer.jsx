import React, { useState } from 'react';
import { optimizeGroupTravel } from '../services/groupTravelService';
import { rankCostTimeClientSide } from '../services/costTimeOptimizationService';
import LoadingSkeleton from './LoadingSkeleton';
import CostTimeSlider from './CostTimeSlider';

export const GroupTravelOptimizer = ({ initialOrigin = 'Delhi', initialDestination = 'Jaipur' }) => {
  const [origin, setOrigin] = useState(initialOrigin);
  const [destination, setDestination] = useState(initialDestination);
  const [travelDate, setTravelDate] = useState('2026-09-10');
  const [travelers, setTravelers] = useState(4);
  const [budget, setBudget] = useState('');
  const [priority, setPriority] = useState('CHEAPEST'); // 'CHEAPEST' | 'FASTEST' | 'BALANCED'
  const [costWeight, setCostWeight] = useState(80); // 0 (Fastest) to 100 (Cheapest)

  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);
  const [result, setResult] = useState(null);
  const [expandedOptionId, setExpandedOptionId] = useState(null);

  const handleOptimize = async (e) => {
    if (e) e.preventDefault();
    if (!origin || !destination) return;

    setLoading(true);
    setError(null);
    try {
      const data = await optimizeGroupTravel({
        origin,
        destination,
        travelDate,
        numberOfTravelers: travelers,
        budget: budget ? Number(budget) : null,
        priority,
      });
      setResult(data);
    } catch (err) {
      console.error(err);
      setError(err.response?.data?.message || err.message || 'Failed to optimize group travel.');
    } finally {
      setLoading(false);
    }
  };

  const toggleExpand = (id) => {
    setExpandedOptionId((prev) => (prev === id ? null : id));
  };

  return (
    <div className="space-y-8 min-w-0" data-testid="group-travel-optimizer">
      
      {/* Header Banner */}
      <div className="bg-gradient-to-r from-indigo-900 via-indigo-800 to-slate-900 rounded-3xl p-6 sm:p-10 text-white shadow-xl relative overflow-hidden">
        <div className="absolute right-0 top-0 translate-x-8 -translate-y-8 w-64 h-64 bg-indigo-500/10 rounded-full blur-3xl pointer-events-none"></div>
        <div className="max-w-2xl space-y-3 relative z-10">
          <div className="inline-flex items-center gap-2 px-3 py-1 rounded-full bg-indigo-500/30 text-indigo-200 border border-indigo-400/30 text-xs font-bold">
            <span>👥 Multi-Passenger Travel Intelligence</span>
          </div>
          <h2 className="text-2xl sm:text-4xl font-black tracking-tight">
            Group Travel <span className="text-indigo-400">Optimization</span>
          </h2>
          <p className="text-xs sm:text-sm text-slate-300 leading-relaxed">
            What is cheapest for 1 person may not be cheapest for 4. Compare pooled cabs, multi-modal flights + transfers, and shared vehicle economics.
          </p>
        </div>
      </div>

      {/* Control Panel Form */}
      <div className="bg-white rounded-3xl border border-slate-200 shadow-md p-5 sm:p-8">
        <form onSubmit={handleOptimize} className="space-y-6">
          <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4">
            
            {/* Origin */}
            <div className="space-y-1.5">
              <label className="text-xs font-bold text-slate-700">Origin City / Airport</label>
              <input
                type="text"
                required
                placeholder="e.g. Delhi or DEL"
                value={origin}
                onChange={(e) => setOrigin(e.target.value)}
                className="w-full px-3.5 py-3 rounded-2xl bg-slate-50 border border-slate-200 text-slate-900 text-xs sm:text-sm focus:bg-white focus:outline-hidden focus:border-indigo-500 font-semibold"
              />
            </div>

            {/* Destination */}
            <div className="space-y-1.5">
              <label className="text-xs font-bold text-slate-700">Destination</label>
              <input
                type="text"
                required
                placeholder="e.g. Jaipur or JAI"
                value={destination}
                onChange={(e) => setDestination(e.target.value)}
                className="w-full px-3.5 py-3 rounded-2xl bg-slate-50 border border-slate-200 text-slate-900 text-xs sm:text-sm focus:bg-white focus:outline-hidden focus:border-indigo-500 font-semibold"
              />
            </div>

            {/* Travel Date */}
            <div className="space-y-1.5">
              <label className="text-xs font-bold text-slate-700">Travel Date</label>
              <input
                type="date"
                value={travelDate}
                onChange={(e) => setTravelDate(e.target.value)}
                className="w-full px-3.5 py-3 rounded-2xl bg-slate-50 border border-slate-200 text-slate-900 text-xs sm:text-sm focus:bg-white focus:outline-hidden focus:border-indigo-500 font-semibold"
              />
            </div>

            {/* Number of Travelers Stepper */}
            <div className="space-y-1.5">
              <label className="text-xs font-bold text-slate-700">Number of Travelers</label>
              <div className="flex items-center bg-slate-50 border border-slate-200 rounded-2xl p-1.5">
                <button
                  type="button"
                  onClick={() => setTravelers((t) => Math.max(1, t - 1))}
                  className="w-9 h-9 rounded-xl bg-white border border-slate-200 text-slate-700 font-bold hover:bg-slate-100 flex items-center justify-center transition cursor-pointer"
                  aria-label="Decrease travelers"
                >
                  −
                </button>
                <div className="flex-1 text-center font-extrabold text-slate-900 text-sm">
                  {travelers} {travelers === 1 ? 'Person' : 'People'}
                </div>
                <button
                  type="button"
                  onClick={() => setTravelers((t) => Math.min(12, t + 1))}
                  className="w-9 h-9 rounded-xl bg-white border border-slate-200 text-slate-700 font-bold hover:bg-slate-100 flex items-center justify-center transition cursor-pointer"
                  aria-label="Increase travelers"
                >
                  +
                </button>
              </div>
            </div>

          </div>

          {/* Priority Presets & Optional Budget */}
          <div className="flex flex-wrap items-center justify-between gap-4 pt-2 border-t border-slate-100">
            <div className="space-y-1.5">
              <label className="text-xs font-bold text-slate-600 block">Group Priority:</label>
              <div className="inline-flex p-1 bg-slate-100 rounded-2xl gap-1">
                {[
                  { id: 'CHEAPEST', label: '💰 Cheapest', desc: 'Lowest group cost' },
                  { id: 'FASTEST', label: '⚡ Fastest', desc: 'Shortest travel time' },
                  { id: 'BALANCED', label: '⚖️ Balanced', desc: 'Best value balance' },
                ].map((p) => (
                  <button
                    key={p.id}
                    type="button"
                    onClick={() => setPriority(p.id)}
                    className={`px-3.5 py-2 rounded-xl text-xs font-bold transition cursor-pointer ${
                      priority === p.id
                        ? 'bg-indigo-600 text-white shadow-xs'
                        : 'text-slate-600 hover:text-slate-900'
                    }`}
                  >
                    {p.label}
                  </button>
                ))}
              </div>
            </div>

            <div className="flex items-center gap-3">
              <div className="w-36 space-y-1">
                <label className="text-[11px] font-bold text-slate-500">Max Budget (₹)</label>
                <input
                  type="number"
                  placeholder="Optional"
                  value={budget}
                  onChange={(e) => setBudget(e.target.value)}
                  className="w-full px-3 py-2 bg-slate-50 rounded-xl border border-slate-200 text-xs text-slate-900 font-mono"
                />
              </div>

              <button
                type="submit"
                disabled={loading}
                className="mt-4 px-6 py-3 bg-indigo-600 hover:bg-indigo-700 text-white font-bold text-xs sm:text-sm rounded-2xl transition shadow-md flex items-center gap-2 cursor-pointer disabled:opacity-75"
              >
                {loading ? (
                  <>
                    <span className="w-4 h-4 border-2 border-white/40 border-t-white rounded-full animate-spin"></span>
                    <span>Comparing...</span>
                  </>
                ) : (
                  <>
                    <span>Compare Group Modes</span>
                    <span>→</span>
                  </>
                )}
              </button>
            </div>
          </div>
        </form>
      </div>

      {/* Loading Skeleton */}
      {loading && <LoadingSkeleton type="product-card" count={2} />}

      {/* Error Banner */}
      {error && (
        <div className="p-4 rounded-2xl bg-rose-50 border border-rose-200 text-rose-700 text-xs font-semibold">
          {error}
        </div>
      )}

      {/* Group Travel Results */}
      {result && !loading && (() => {
        const displayOptions = rankCostTimeClientSide(
          result.options,
          costWeight,
          (o) => o.totalCost,
          (o) => o.estimatedTravelTimeMinutes
        );

        return (
        <div className="space-y-6 animate-fadeIn" data-testid="group-travel-results">
          
          {/* Group Insights Pill */}
          {result.groupInsights && (
            <div className="bg-emerald-50 border border-emerald-200/90 rounded-2xl p-4 sm:p-5 flex items-start gap-3.5 shadow-xs">
              <span className="text-2xl shrink-0">💡</span>
              <div className="space-y-1">
                <h4 className="text-xs sm:text-sm font-extrabold text-emerald-900">
                  Group Economy Analysis ({result.numberOfTravelers} Travelers)
                </h4>
                <p className="text-xs sm:text-sm text-emerald-800 leading-relaxed font-medium">
                  {result.groupInsights}
                </p>
              </div>
            </div>
          )}

          {/* Interactive Cost vs Time Tradeoff Slider */}
          <CostTimeSlider
            costWeight={costWeight}
            onChange={(w) => setCostWeight(w)}
          />

          {/* Options Grid */}
          <div className="space-y-4">
            <div className="flex items-center justify-between text-xs text-slate-500 font-semibold px-1">
              <span>Comparing <strong>{displayOptions?.length || 0}</strong> supported transport combinations</span>
              <span className="font-mono font-bold text-indigo-600">
                {costWeight}% Money / {100 - costWeight}% Time
              </span>
            </div>

            <div className="grid grid-cols-1 gap-4">
              {displayOptions?.map((option, idx) => {
                const isExpanded = expandedOptionId === option.id;
                const isDirectCab = option.mode === 'DIRECT_RIDE';
                const isTopRanked = idx === 0;

                return (
                  <div
                    key={option.id}
                    className={`bg-white rounded-3xl border transition-all duration-200 shadow-xs hover:shadow-lg overflow-hidden ${
                      option.isRecommended
                        ? 'border-indigo-500/80 ring-2 ring-indigo-500/20'
                        : 'border-slate-200'
                    }`}
                  >
                    <div className="p-5 sm:p-6 space-y-4">
                      
                      {/* Top Row: Badges & Provider Info */}
                      <div className="flex flex-wrap items-center justify-between gap-2 border-b border-slate-100 pb-3.5">
                        <div className="flex flex-wrap items-center gap-2">
                          <span
                            className={`px-3 py-1 rounded-xl text-xs font-bold border flex items-center gap-1.5 shadow-2xs ${
                              isDirectCab
                                ? 'bg-emerald-50 text-emerald-700 border-emerald-200'
                                : 'bg-sky-50 text-sky-700 border-sky-200'
                            }`}
                          >
                            <span>{isDirectCab ? '🚗' : '✈️+🚗'}</span>
                            <span>{isDirectCab ? 'Direct Road Cab' : 'Flight + Airport Transfers'}</span>
                          </span>

                          {option.isRecommended && (
                            <span className="px-3 py-1 rounded-xl text-xs font-black bg-indigo-600 text-white shadow-2xs">
                              🏆 Top Recommendation ({priority})
                            </span>
                          )}

                          {option.classification === 'BEST_GROUP_VALUE' && !option.isRecommended && (
                            <span className="px-2.5 py-0.5 rounded-lg text-[11px] font-bold bg-emerald-100 text-emerald-800 border border-emerald-200">
                              💰 Best Group Value
                            </span>
                          )}
                        </div>

                        <div className="text-xs font-mono font-bold text-slate-400">
                          Score: {option.score}/100
                        </div>
                      </div>

                      {/* Main Summary Info */}
                      <div className="grid grid-cols-1 sm:grid-cols-3 gap-4 items-center">
                        
                        {/* Title & Vehicle */}
                        <div className="space-y-1">
                          <h3 className="text-base sm:text-lg font-black text-slate-900">
                            {option.title}
                          </h3>
                          <div className="text-xs text-slate-500 font-medium">
                            {option.vehicleCapacityNote}
                          </div>
                        </div>

                        {/* Travel Time & Connections */}
                        <div className="space-y-1 sm:text-center">
                          <div className="text-xs text-slate-400 font-bold uppercase tracking-wider">
                            Travel Duration
                          </div>
                          <div className="text-sm sm:text-base font-extrabold text-slate-800 flex items-center sm:justify-center gap-1.5">
                            <span>⏱️ {option.formattedDuration}</span>
                          </div>
                          <div className="text-[11px] text-slate-500">
                            {option.requiredConnections === 0 ? 'Direct (0 transfers)' : `${option.requiredConnections} connections / legs`}
                          </div>
                        </div>

                        {/* Cost Metrics */}
                        <div className="space-y-1 sm:text-right">
                          <div className="text-xs text-slate-400 font-bold uppercase tracking-wider">
                            Total Group Cost
                          </div>
                          <div className="text-xl sm:text-2xl font-black text-slate-900 font-mono">
                            ₹{Number(option.totalCost).toLocaleString('en-IN')}
                          </div>
                          <div className="inline-block px-2 py-0.5 rounded-lg bg-emerald-50 text-emerald-700 font-bold text-xs border border-emerald-200 font-mono">
                            ₹{Number(option.costPerPerson).toLocaleString('en-IN')} / person
                          </div>
                        </div>

                      </div>

                      {/* Recommendation Reason */}
                      {option.recommendationReason && (
                        <div className="p-3 bg-slate-50 rounded-2xl text-xs text-slate-700 font-medium border border-slate-200/60">
                          <strong>Why this option:</strong> {option.recommendationReason}
                        </div>
                      )}

                      {/* Expand / Details Toggle Button */}
                      <div className="flex items-center justify-between pt-2 border-t border-slate-100 text-xs">
                        <button
                          type="button"
                          onClick={() => toggleExpand(option.id)}
                          className="font-bold text-indigo-600 hover:text-indigo-800 flex items-center gap-1.5 cursor-pointer py-1"
                        >
                          <span>{isExpanded ? 'Hide Cost Breakdown & Legs' : 'View Itemized Breakdown & Legs'}</span>
                          <span>{isExpanded ? '▲' : '▼'}</span>
                        </button>

                        <button
                          type="button"
                          className="px-4 py-2 bg-slate-900 hover:bg-black text-white font-bold rounded-xl transition cursor-pointer shadow-xs"
                        >
                          Select Option →
                        </button>
                      </div>

                      {/* Expandable Breakdown and Leg Details */}
                      {isExpanded && (
                        <div className="mt-4 pt-4 border-t border-slate-200 space-y-4 bg-slate-50/70 p-4 rounded-2xl animate-fadeIn">
                          
                          {/* Itemized Cost Breakdown */}
                          <div className="space-y-2">
                            <h5 className="text-xs font-extrabold text-slate-800 uppercase tracking-wider">
                              Cost Breakdown ({result.numberOfTravelers} Travelers)
                            </h5>
                            <div className="bg-white rounded-xl border border-slate-200 divide-y divide-slate-100 overflow-hidden text-xs">
                              {option.breakdown?.map((item, idx) => (
                                <div key={idx} className="p-2.5 flex items-center justify-between">
                                  <div>
                                    <div className="font-bold text-slate-800">{item.label}</div>
                                    <div className="text-[11px] text-slate-500">{item.description}</div>
                                  </div>
                                  <div className="font-mono font-bold text-slate-900">
                                    ₹{Number(item.amount).toLocaleString('en-IN')}
                                  </div>
                                </div>
                              ))}
                              <div className="p-2.5 bg-slate-50 flex items-center justify-between font-black text-slate-900">
                                <span>Total Effective Group Cost</span>
                                <span className="font-mono text-sm">
                                  ₹{Number(option.totalCost).toLocaleString('en-IN')}
                                </span>
                              </div>
                            </div>
                          </div>

                          {/* Journey Legs */}
                          <div className="space-y-2">
                            <h5 className="text-xs font-extrabold text-slate-800 uppercase tracking-wider">
                              Journey Steps & Multi-Modal Transfers
                            </h5>
                            <div className="space-y-2">
                              {option.legs?.map((leg, lIdx) => (
                                <div key={lIdx} className="bg-white p-3 rounded-xl border border-slate-200 flex items-center justify-between gap-3 text-xs">
                                  <div className="flex items-center gap-2.5">
                                    <span className="w-6 h-6 rounded-full bg-indigo-100 text-indigo-700 font-bold flex items-center justify-center text-[10px]">
                                      {lIdx + 1}
                                    </span>
                                    <div>
                                      <div className="font-bold text-slate-900">{leg.title}</div>
                                      <div className="text-[11px] text-slate-500 font-medium">
                                        {leg.origin} → {leg.destination} ({leg.durationMinutes} mins)
                                      </div>
                                    </div>
                                  </div>
                                  <div className="text-right">
                                    <div className="font-mono font-bold text-slate-900">
                                      ₹{Number(leg.cost).toLocaleString('en-IN')}
                                    </div>
                                    <div className="text-[10px] text-slate-400 font-medium">{leg.provider}</div>
                                  </div>
                                </div>
                              ))}
                            </div>
                          </div>

                        </div>
                      )}

                    </div>
                  </div>
                );
              })}
            </div>
          </div>

        </div>
        );
      })()}

    </div>
  );
};

export default GroupTravelOptimizer;
