import React from 'react';

export const CostTimeSlider = ({
  costWeight = 50,
  onChange,
  className = '',
  compact = false,
}) => {
  const timeWeight = 100 - costWeight;

  const presets = [
    { label: '💰 Max Money', cost: 100, desc: '100% Cheapest' },
    { label: '🎯 Value', cost: 70, desc: '70% Money / 30% Time' },
    { label: '⚖️ Balanced', cost: 50, desc: '50% / 50%' },
    { label: '🚀 Speed', cost: 30, desc: '30% Money / 70% Time' },
    { label: '⚡ Max Time', cost: 0, desc: '100% Fastest' },
  ];

  const handleSliderChange = (e) => {
    const val = Number(e.target.value);
    if (onChange) onChange(val);
  };

  const handlePresetClick = (presetCost) => {
    if (onChange) onChange(presetCost);
  };

  return (
    <div
      className={`bg-white rounded-3xl border border-slate-200/90 shadow-sm p-4 sm:p-5 space-y-4 ${className}`}
      data-testid="cost-time-slider-container"
    >
      {/* Header & Status Indicator */}
      <div className="flex flex-wrap items-center justify-between gap-2">
        <div className="flex items-center gap-2">
          <span className="w-8 h-8 rounded-xl bg-indigo-50 text-indigo-600 flex items-center justify-center font-bold text-sm">
            ⚖️
          </span>
          <div>
            <h4 className="text-xs sm:text-sm font-extrabold text-slate-900">
              Cost vs Time Tradeoff
            </h4>
            <p className="text-[11px] text-slate-500 font-medium">
              Adjust your balance between lowest price and fastest duration
            </p>
          </div>
        </div>

        {/* Live Weight Pill */}
        <div className="flex items-center gap-1.5 px-3 py-1 rounded-xl bg-slate-100 border border-slate-200 text-xs font-bold font-mono">
          <span className="text-emerald-700">{costWeight}% Money</span>
          <span className="text-slate-400">•</span>
          <span className="text-indigo-700">{timeWeight}% Time</span>
        </div>
      </div>

      {/* Dual-Pole Range Track */}
      <div className="space-y-2 pt-1">
        <div className="flex items-center justify-between text-xs font-black uppercase tracking-wider">
          <span className="text-emerald-700 flex items-center gap-1">
            <span>💰</span>
            <span>Save Money</span>
          </span>
          <span className="text-indigo-700 flex items-center gap-1">
            <span>Save Time</span>
            <span>⚡</span>
          </span>
        </div>

        <div className="relative flex items-center">
          <input
            type="range"
            min="0"
            max="100"
            step="1"
            value={costWeight}
            onChange={handleSliderChange}
            aria-label="Cost vs Time Weight Slider"
            className="w-full h-3 bg-gradient-to-r from-emerald-400 via-amber-300 to-indigo-500 rounded-lg appearance-none cursor-pointer accent-indigo-700 focus:outline-hidden"
          />
        </div>

        <div className="flex justify-between text-[10px] text-slate-400 font-bold px-1">
          <span>100% Cheapest</span>
          <span>50/50 Compromise</span>
          <span>100% Fastest</span>
        </div>
      </div>

      {/* Quick Presets Strip */}
      {!compact && (
        <div className="pt-2 border-t border-slate-100 flex flex-wrap items-center gap-1.5">
          <span className="text-[11px] font-bold text-slate-400 mr-1">Presets:</span>
          {presets.map((p) => {
            const isSelected = Math.abs(costWeight - p.cost) < 5;
            return (
              <button
                key={p.label}
                type="button"
                onClick={() => handlePresetClick(p.cost)}
                className={`px-3 py-1 rounded-xl text-xs font-bold transition cursor-pointer ${
                  isSelected
                    ? 'bg-slate-900 text-white shadow-2xs scale-105'
                    : 'bg-slate-100 hover:bg-slate-200 text-slate-700'
                }`}
                title={p.desc}
              >
                {p.label}
              </button>
            );
          })}
        </div>
      )}
    </div>
  );
};

export default CostTimeSlider;
