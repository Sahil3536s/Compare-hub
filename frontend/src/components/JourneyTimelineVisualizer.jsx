import React from 'react';

export const JourneyTimelineVisualizer = ({ journey, travelers = 1 }) => {
  if (!journey || !journey.segments) {
    return null;
  }

  const {
    title,
    segments,
    totalCost,
    formattedTotalDuration,
    totalWaitingTimeMinutes,
    transferCount,
    costPerTraveler,
    classification,
    recommendationReason,
    insights = [],
  } = journey;

  const isDirectCab = segments.some((s) => s.type === 'DIRECT_INTERCITY_CAB');

  return (
    <div className="bg-white rounded-3xl border border-slate-200 p-6 sm:p-8 shadow-xs space-y-6" data-testid="journey-timeline-visualizer">
      
      {/* Top Header & Badges */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4 border-b border-slate-100 pb-5">
        <div>
          <div className="flex items-center gap-2 flex-wrap mb-1">
            <span className="px-3 py-1 rounded-full text-xs font-black bg-indigo-50 border border-indigo-200 text-indigo-700">
              {classification ? classification.replace('_', ' ') : 'SMART JOURNEY'}
            </span>
            {isDirectCab && (
              <span className="px-2.5 py-0.5 rounded-full text-[10px] font-bold bg-amber-50 text-amber-800 border border-amber-200">
                0 Transfers (Direct Cab)
              </span>
            )}
            <span className="text-xs text-slate-400 font-medium">
              {transferCount} {transferCount === 1 ? 'transfer' : 'transfers'} • {travelers} {travelers === 1 ? 'traveler' : 'travelers'}
            </span>
          </div>
          <h3 className="text-xl sm:text-2xl font-black text-slate-900 tracking-tight">
            {title}
          </h3>
        </div>

        <div className="text-left sm:text-right bg-slate-50 px-5 py-3 rounded-2xl border border-slate-100 shrink-0">
          <span className="text-[10px] font-bold text-slate-400 uppercase tracking-wider block">Total Door-to-Door</span>
          <div className="text-2xl sm:text-3xl font-black text-slate-900 font-mono">
            ₹{Number(totalCost).toLocaleString('en-IN')}
          </div>
          <span className="text-[11px] text-emerald-700 font-bold block">
            ₹{Number(costPerTraveler).toLocaleString('en-IN')}/person • {formattedTotalDuration}
          </span>
        </div>
      </div>

      {recommendationReason && (
        <div className="bg-indigo-50/60 rounded-2xl p-4 border border-indigo-100 flex items-start gap-3">
          <span className="text-lg">💡</span>
          <p className="text-xs text-indigo-950 font-medium leading-relaxed">
            {recommendationReason}
          </p>
        </div>
      )}

      {/* Visual Step-by-Step Vertical Timeline */}
      <div className="space-y-4 pt-2">
        <h4 className="text-xs font-extrabold text-slate-400 uppercase tracking-wider">
          Door-to-Door Journey Timeline
        </h4>

        <div className="relative pl-6 sm:pl-8 border-l-2 border-indigo-200 space-y-6">
          {segments.map((segment, idx) => {
            const isBuffer = segment.type === 'AIRPORT_BUFFER' || segment.type === 'ARRIVAL_BUFFER';
            const isFlight = segment.type === 'FLIGHT';

            return (
              <div key={segment.id || idx} className="relative group">
                {/* Node Bullet Icon */}
                <div
                  className={`absolute -left-[35px] sm:-left-[43px] top-1.5 w-8 h-8 rounded-xl flex items-center justify-center text-sm font-bold shadow-xs border ${
                    isFlight
                      ? 'bg-indigo-600 text-white border-indigo-700'
                      : isBuffer
                      ? 'bg-amber-100 text-amber-800 border-amber-300'
                      : 'bg-emerald-100 text-emerald-800 border-emerald-300'
                  }`}
                >
                  {segment.icon || '📍'}
                </div>

                {/* Card Body */}
                <div
                  className={`p-4 rounded-2xl border transition-all duration-200 ${
                    isBuffer
                      ? 'bg-amber-50/40 border-amber-200/80'
                      : 'bg-slate-50/80 hover:bg-white border-slate-200 hover:shadow-sm'
                  }`}
                >
                  <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-2 mb-1.5">
                    <div className="flex items-center gap-2">
                      <span className="text-xs font-extrabold text-slate-900">
                        {segment.typeName}
                      </span>
                      <span className="px-2 py-0.5 rounded-md text-[10px] font-bold bg-white text-slate-700 border border-slate-200">
                        {segment.provider}
                      </span>
                    </div>

                    <div className="flex items-center gap-3 text-xs font-mono font-bold">
                      <span className="text-slate-600">{segment.formattedDuration}</span>
                      {segment.price > 0 ? (
                        <span className="text-emerald-700 font-extrabold">
                          ₹{Number(segment.price).toLocaleString('en-IN')}
                        </span>
                      ) : (
                        <span className="text-slate-400 font-normal">Included Buffer</span>
                      )}
                    </div>
                  </div>

                  {/* Route & Times */}
                  <div className="text-xs text-slate-600 flex flex-wrap items-center gap-x-3 gap-y-1">
                    <div className="flex items-center gap-1.5">
                      <span className="font-semibold text-slate-900">{segment.origin}</span>
                      <span className="text-slate-400">({segment.departureTime})</span>
                    </div>
                    <span className="text-slate-300">→</span>
                    <div className="flex items-center gap-1.5">
                      <span className="font-semibold text-slate-900">{segment.destination}</span>
                      <span className="text-slate-400">({segment.arrivalTime})</span>
                    </div>
                  </div>

                  {/* Metadata Chips */}
                  {segment.metadata && Object.keys(segment.metadata).length > 0 && (
                    <div className="mt-2.5 pt-2 border-t border-slate-200/50 flex flex-wrap gap-2 text-[10px]">
                      {Object.entries(segment.metadata).map(([key, val]) => (
                        <span key={key} className="px-2 py-0.5 rounded-md bg-white border border-slate-200 text-slate-600 font-medium">
                          {key}: <strong className="text-slate-800">{val}</strong>
                        </span>
                      ))}
                    </div>
                  )}
                </div>

              </div>
            );
          })}
        </div>
      </div>

      {/* Insights */}
      {insights.length > 0 && (
        <div className="border-t border-slate-100 pt-4 space-y-2">
          <h5 className="text-[11px] font-bold text-slate-400 uppercase tracking-wider">
            Journey Diagnostics & Safety Buffer
          </h5>
          <ul className="space-y-1.5 text-xs text-slate-600">
            {insights.map((insight, i) => (
              <li key={i} className="flex items-start gap-2">
                <span className="text-emerald-600 font-bold">✓</span>
                <span>{insight}</span>
              </li>
            ))}
          </ul>
        </div>
      )}

    </div>
  );
};

export default JourneyTimelineVisualizer;
