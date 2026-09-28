import React from 'react';

export const FlightFilterPanel = ({
  stops,
  setStops,
  departureTime,
  setDepartureTime,
  maxPrice,
  setMaxPrice,
  selectedAirline,
  setSelectedAirline,
  maxDuration,
  setMaxDuration,
  onResetFilters,
  onApplyFilters,
  isMobileDrawer = false,
  onCloseDrawer,
  availableAirlines = ['IndiGo', 'Air India', 'Vistara', 'SpiceJet', 'Akasa Air', 'Air India Express'],
}) => {
  const timeSlots = [
    { id: 'all', label: 'Any Time', icon: '⏰', sub: 'All hours' },
    { id: 'before_6am', label: 'Before 6 AM', icon: '🌙', sub: 'Early morning / Night' },
    { id: '6am_12pm', label: '6 AM – 12 PM', icon: '🌅', sub: 'Morning departure' },
    { id: '12pm_6pm', label: '12 PM – 6 PM', icon: '☀️', sub: 'Afternoon departure' },
    { id: 'after_6pm', label: 'After 6 PM', icon: '🌆', sub: 'Evening departure' },
  ];

  const stopOptions = [
    { id: '', label: 'All Stops' },
    { id: '0', label: 'Non-stop' },
    { id: '1', label: '1 Stop' },
    { id: '2', label: '2+ Stops' },
  ];

  const formatDurationDisplay = (mins) => {
    if (!mins || mins >= 720) return 'Any Duration';
    const h = Math.floor(mins / 60);
    const m = mins % 60;
    return `Up to ${h}h${m > 0 ? ` ${m}m` : ''}`;
  };

  const activeFiltersCount = [
    stops !== '' && stops !== null,
    departureTime !== 'all' && departureTime !== '',
    maxPrice < 35000,
    selectedAirline !== 'all' && selectedAirline !== '',
    maxDuration < 720,
  ].filter(Boolean).length;

  return (
    <div
      data-testid="flight-filter-panel"
      className={`bg-white rounded-3xl border border-slate-200 shadow-xs flex flex-col ${
        isMobileDrawer ? 'p-6 h-full overflow-y-auto' : 'p-6'
      }`}
    >
      {/* Header */}
      <div className="flex items-center justify-between border-b border-slate-100 pb-4 mb-6">
        <div className="flex items-center gap-2">
          <span className="text-base">🎛️</span>
          <h3 className="font-extrabold text-slate-900 text-sm">Filters</h3>
          {activeFiltersCount > 0 && (
            <span className="px-2 py-0.5 rounded-full bg-indigo-100 text-indigo-700 text-xs font-bold">
              {activeFiltersCount} applied
            </span>
          )}
        </div>
        <div className="flex items-center gap-2">
          {activeFiltersCount > 0 && (
            <button
              type="button"
              onClick={onResetFilters}
              className="text-xs font-bold text-indigo-600 hover:text-indigo-800 transition cursor-pointer"
            >
              Reset All
            </button>
          )}
          {isMobileDrawer && (
            <button
              type="button"
              onClick={onCloseDrawer}
              className="w-8 h-8 rounded-full bg-slate-100 text-slate-500 hover:bg-slate-200 hover:text-slate-800 flex items-center justify-center font-bold text-sm cursor-pointer ml-2"
              aria-label="Close filters drawer"
            >
              ✕
            </button>
          )}
        </div>
      </div>

      <div className="space-y-6 flex-1">
        {/* Number of Stops */}
        <div>
          <label className="block text-xs font-bold uppercase tracking-wider text-slate-500 mb-2">
            Stops
          </label>
          <div className="grid grid-cols-2 gap-2">
            {stopOptions.map((opt) => (
              <button
                key={opt.id}
                type="button"
                onClick={() => setStops(opt.id)}
                className={`px-3 py-2 rounded-xl text-xs font-bold transition text-left cursor-pointer flex items-center justify-between border ${
                  stops === opt.id
                    ? 'bg-indigo-50 border-indigo-300 text-indigo-700 shadow-xs'
                    : 'border-slate-200 text-slate-700 hover:bg-slate-50'
                }`}
              >
                <span>{opt.label}</span>
                {stops === opt.id && <span className="text-indigo-600 font-extrabold">✓</span>}
              </button>
            ))}
          </div>
        </div>

        {/* Departure Time of Day */}
        <div className="pt-4 border-t border-slate-100">
          <label className="block text-xs font-bold uppercase tracking-wider text-slate-500 mb-2">
            Departure Time
          </label>
          <div className="grid grid-cols-1 gap-1.5">
            {timeSlots.map((slot) => (
              <button
                key={slot.id}
                type="button"
                onClick={() => setDepartureTime(slot.id)}
                className={`p-2.5 rounded-xl border transition text-left cursor-pointer flex items-center justify-between ${
                  departureTime === slot.id
                    ? 'bg-indigo-50 border-indigo-300 text-indigo-700 shadow-xs'
                    : 'border-slate-200 text-slate-700 hover:bg-slate-50'
                }`}
              >
                <div className="flex items-center gap-2.5">
                  <span className="text-base">{slot.icon}</span>
                  <div>
                    <div className="text-xs font-bold leading-tight">{slot.label}</div>
                    <div className="text-[10px] text-slate-400 font-normal">{slot.sub}</div>
                  </div>
                </div>
                {departureTime === slot.id && (
                  <span className="text-xs font-extrabold text-indigo-600">✓</span>
                )}
              </button>
            ))}
          </div>
        </div>

        {/* Price Range */}
        <div className="pt-4 border-t border-slate-100">
          <div className="flex items-center justify-between mb-2">
            <label className="text-xs font-bold uppercase tracking-wider text-slate-500">
              Max Flight Fare
            </label>
            <span className="text-xs font-extrabold text-indigo-600 font-mono">
              ₹{Number(maxPrice).toLocaleString('en-IN')}
            </span>
          </div>
          <input
            type="range"
            min="3000"
            max="35000"
            step="500"
            value={maxPrice}
            onChange={(e) => setMaxPrice(Number(e.target.value))}
            className="w-full accent-indigo-600 cursor-pointer"
          />
          <div className="flex justify-between text-[11px] text-slate-400 font-mono mt-1">
            <span>₹3,000</span>
            <span>₹35,000</span>
          </div>
        </div>

        {/* Max Duration */}
        <div className="pt-4 border-t border-slate-100">
          <div className="flex items-center justify-between mb-2">
            <label className="text-xs font-bold uppercase tracking-wider text-slate-500">
              Max Flight Duration
            </label>
            <span className="text-xs font-extrabold text-indigo-600 font-mono">
              {formatDurationDisplay(maxDuration)}
            </span>
          </div>
          <input
            type="range"
            min="120"
            max="720"
            step="30"
            value={maxDuration}
            onChange={(e) => setMaxDuration(Number(e.target.value))}
            className="w-full accent-indigo-600 cursor-pointer"
          />
          <div className="flex justify-between text-[11px] text-slate-400 font-mono mt-1">
            <span>2h</span>
            <span>12h+</span>
          </div>
        </div>

        {/* Airlines Filter */}
        <div className="pt-4 border-t border-slate-100">
          <label className="block text-xs font-bold uppercase tracking-wider text-slate-500 mb-2">
            Airlines
          </label>
          <div className="flex flex-wrap gap-1.5">
            <button
              type="button"
              onClick={() => setSelectedAirline('all')}
              className={`px-3 py-1.5 rounded-xl text-xs font-bold transition cursor-pointer border ${
                selectedAirline === 'all'
                  ? 'bg-slate-900 text-white border-slate-900 shadow-xs'
                  : 'bg-slate-50 text-slate-700 border-slate-200 hover:bg-slate-100'
              }`}
            >
              All Airlines
            </button>
            {availableAirlines.map((al) => (
              <button
                key={al}
                type="button"
                onClick={() => setSelectedAirline(al)}
                className={`px-3 py-1.5 rounded-xl text-xs font-bold transition cursor-pointer border ${
                  selectedAirline === al
                    ? 'bg-indigo-600 text-white border-indigo-600 shadow-xs'
                    : 'bg-slate-50 text-slate-700 border-slate-200 hover:bg-slate-100'
                }`}
              >
                {al}
              </button>
            ))}
          </div>
        </div>
      </div>

      {/* Drawer Action Bar */}
      {isMobileDrawer && (
        <div className="pt-6 border-t border-slate-100 mt-6 flex gap-3">
          <button
            type="button"
            onClick={onResetFilters}
            className="flex-1 py-3 px-4 border border-slate-200 text-slate-700 hover:bg-slate-50 font-bold text-xs rounded-xl transition cursor-pointer"
          >
            Clear Filters
          </button>
          <button
            type="button"
            onClick={() => {
              if (onApplyFilters) onApplyFilters();
              if (onCloseDrawer) onCloseDrawer();
            }}
            className="flex-1 py-3 px-4 bg-indigo-600 hover:bg-indigo-700 text-white font-bold text-xs rounded-xl shadow-xs transition cursor-pointer"
          >
            Apply Filters
          </button>
        </div>
      )}
    </div>
  );
};

export default FlightFilterPanel;
