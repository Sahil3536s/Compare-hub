import React, { useState, useEffect } from 'react';
import { useSearchParams } from 'react-router-dom';
import { optimizeSmartJourney, getSampleSmartJourney } from '../services/smartJourneyService';
import JourneyTimelineVisualizer from '../components/JourneyTimelineVisualizer';
import LoadingSkeleton from '../components/LoadingSkeleton';

export const SmartJourneyPage = () => {
  const [searchParams] = useSearchParams();

  const [origin, setOrigin] = useState(searchParams.get('origin') || 'Saket, South Delhi');
  const [destination, setDestination] = useState(searchParams.get('destination') || 'Calangute, North Goa');
  const [travelers, setTravelers] = useState(Number(searchParams.get('travelers')) || 1);
  const [bufferMinutes, setBufferMinutes] = useState(90);
  const [costWeight, setCostWeight] = useState(50);

  const [journeyData, setJourneyData] = useState(null);
  const [selectedJourneyId, setSelectedJourneyId] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  const handleOptimize = async (customCostWeight = costWeight, customBuffer = bufferMinutes) => {
    setLoading(true);
    setError(null);
    try {
      const data = await optimizeSmartJourney({
        origin: origin.trim(),
        destination: destination.trim(),
        travelers: Number(travelers),
        airportBufferMinutes: Number(customBuffer),
        costWeight: Number(customCostWeight),
        timeWeight: 100 - Number(customCostWeight),
      });
      setJourneyData(data);
      if (data.topRecommendedJourney) {
        setSelectedJourneyId(data.topRecommendedJourney.id);
      } else if (data.allCombinations && data.allCombinations.length > 0) {
        setSelectedJourneyId(data.allCombinations[0].id);
      }
    } catch (err) {
      console.error('Failed to optimize smart journey:', err);
      setError(err.message || 'Could not optimize door-to-door journey.');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    handleOptimize(costWeight, bufferMinutes);
  }, []);

  const handleSliderChange = (e) => {
    const val = Number(e.target.value);
    setCostWeight(val);
    handleOptimize(val, bufferMinutes);
  };

  const handlePreset = (weight) => {
    setCostWeight(weight);
    handleOptimize(weight, bufferMinutes);
  };

  const handleBufferSelect = (mins) => {
    setBufferMinutes(mins);
    handleOptimize(costWeight, mins);
  };

  const handleSubmit = (e) => {
    e.preventDefault();
    handleOptimize(costWeight, bufferMinutes);
  };

  const allCombinations = journeyData?.allCombinations || [];
  const selectedJourney =
    allCombinations.find((j) => j.id === selectedJourneyId) ||
    journeyData?.topRecommendedJourney ||
    journeyData?.balancedJourney ||
    allCombinations[0];

  const cheapest = journeyData?.cheapestJourney;
  const fastest = journeyData?.fastestJourney;
  const balanced = journeyData?.balancedJourney;

  return (
    <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8 space-y-8 animate-fadeIn" data-testid="smart-journey-page">
      
      {/* Header */}
      <div>
        <div className="flex items-center gap-2 text-xs text-slate-500 mb-2">
          <span>Home</span>
          <span>/</span>
          <span className="text-indigo-600 font-medium">Smart Journey 2.0</span>
        </div>
        <div className="flex flex-col md:flex-row md:items-center justify-between gap-4">
          <div>
            <h1 className="text-3xl sm:text-4xl font-extrabold text-slate-900 tracking-tight">
              Smart Journey <span className="text-indigo-600">2.0</span>
            </h1>
            <p className="text-sm text-slate-500 mt-1 max-w-2xl">
              Door-to-door transit intelligence calculating origin rides, synchronized airport safety buffers, flights, and destination hotel transfers.
            </p>
          </div>

          <div className="inline-flex items-center gap-2 px-3.5 py-1.5 rounded-2xl bg-indigo-50 border border-indigo-200 text-indigo-800 text-xs font-bold">
            <span>🗺️ Multi-Stage Optimization</span>
          </div>
        </div>
      </div>

      {/* Input Search Controls & Safety Buffer Bar */}
      <div className="bg-white rounded-3xl border border-slate-200 p-6 sm:p-7 shadow-xs space-y-6">
        <form onSubmit={handleSubmit} className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4">
          
          <div className="space-y-1.5">
            <label className="text-xs font-bold text-slate-600 uppercase tracking-wider block">
              Origin (Home / Office)
            </label>
            <input
              type="text"
              value={origin}
              onChange={(e) => setOrigin(e.target.value)}
              placeholder="e.g. Saket, South Delhi"
              className="w-full px-4 py-2.5 rounded-xl bg-slate-50 border border-slate-200 text-slate-900 text-xs sm:text-sm font-semibold focus:bg-white focus:outline-hidden focus:border-indigo-500"
            />
          </div>

          <div className="space-y-1.5">
            <label className="text-xs font-bold text-slate-600 uppercase tracking-wider block">
              Destination (Hotel / Area)
            </label>
            <input
              type="text"
              value={destination}
              onChange={(e) => setDestination(e.target.value)}
              placeholder="e.g. Calangute, North Goa"
              className="w-full px-4 py-2.5 rounded-xl bg-slate-50 border border-slate-200 text-slate-900 text-xs sm:text-sm font-semibold focus:bg-white focus:outline-hidden focus:border-indigo-500"
            />
          </div>

          <div className="space-y-1.5">
            <label className="text-xs font-bold text-slate-600 uppercase tracking-wider block">
              Travelers
            </label>
            <select
              value={travelers}
              onChange={(e) => setTravelers(Number(e.target.value))}
              className="w-full px-4 py-2.5 rounded-xl bg-slate-50 border border-slate-200 text-slate-900 text-xs sm:text-sm font-semibold focus:bg-white focus:outline-hidden focus:border-indigo-500"
            >
              {[1, 2, 3, 4, 5, 6].map((n) => (
                <option key={n} value={n}>
                  {n} {n === 1 ? 'Person' : 'People'}
                </option>
              ))}
            </select>
          </div>

          <div className="flex items-end">
            <button
              type="submit"
              disabled={loading}
              className="w-full py-2.5 bg-indigo-600 hover:bg-indigo-700 text-white text-xs sm:text-sm font-bold rounded-xl shadow-md transition cursor-pointer flex items-center justify-center gap-2"
            >
              {loading ? 'Optimizing Journey...' : 'Calculate Smart Route'}
            </button>
          </div>

        </form>

        {/* Airport Buffer Setting & Preference Preset Strip */}
        <div className="pt-4 border-t border-slate-100 flex flex-col md:flex-row md:items-center justify-between gap-4">
          
          <div className="flex items-center gap-2 flex-wrap text-xs">
            <span className="font-bold text-slate-500">Airport Security Buffer:</span>
            {[
              { label: '⚡ 60 min (Express)', value: 60 },
              { label: '🛡️ 90 min (Standard)', value: 90 },
              { label: '⏳ 120 min (Relaxed)', value: 120 },
            ].map((buf) => (
              <button
                key={buf.value}
                type="button"
                onClick={() => handleBufferSelect(buf.value)}
                className={`px-3 py-1.5 rounded-xl text-xs font-bold transition cursor-pointer ${
                  bufferMinutes === buf.value
                    ? 'bg-slate-900 text-white shadow-xs'
                    : 'bg-slate-100 hover:bg-slate-200 text-slate-700'
                }`}
              >
                {buf.label}
              </button>
            ))}
          </div>

          <div className="flex items-center gap-2 flex-wrap text-xs">
            <span className="font-bold text-slate-500">Preset:</span>
            <button
              type="button"
              onClick={() => handlePreset(100)}
              className={`px-3 py-1.5 rounded-xl text-xs font-bold transition cursor-pointer ${
                costWeight === 100
                  ? 'bg-emerald-600 text-white shadow-xs'
                  : 'bg-emerald-50 text-emerald-800 hover:bg-emerald-100'
              }`}
            >
              💰 Cheapest
            </button>
            <button
              type="button"
              onClick={() => handlePreset(50)}
              className={`px-3 py-1.5 rounded-xl text-xs font-bold transition cursor-pointer ${
                costWeight === 50
                  ? 'bg-indigo-600 text-white shadow-xs'
                  : 'bg-indigo-50 text-indigo-800 hover:bg-indigo-100'
              }`}
            >
              ⚖️ Balanced
            </button>
            <button
              type="button"
              onClick={() => handlePreset(0)}
              className={`px-3 py-1.5 rounded-xl text-xs font-bold transition cursor-pointer ${
                costWeight === 0
                  ? 'bg-sky-600 text-white shadow-xs'
                  : 'bg-sky-50 text-sky-800 hover:bg-sky-100'
              }`}
            >
              ⚡ Fastest
            </button>
          </div>

        </div>

        {/* Cost vs Time Interactive Slider */}
        <div className="bg-slate-50 rounded-2xl p-4 border border-slate-100 space-y-2">
          <div className="flex items-center justify-between text-xs font-extrabold">
            <span className="text-emerald-700">SAVE MONEY ({costWeight}%)</span>
            <span className="text-slate-400 font-mono">Dynamic Ranking Engine</span>
            <span className="text-indigo-600">SAVE TIME ({100 - costWeight}%)</span>
          </div>
          <input
            type="range"
            min="0"
            max="100"
            step="5"
            value={costWeight}
            onChange={handleSliderChange}
            className="w-full accent-indigo-600 cursor-pointer"
            aria-label="Cost vs Time Slider"
          />
        </div>

      </div>

      {/* Loading State */}
      {loading && (
        <div className="space-y-6">
          <LoadingSkeleton type="flight-card" count={3} />
        </div>
      )}

      {/* Error State */}
      {error && !loading && (
        <div className="bg-rose-50 border border-rose-200 rounded-3xl p-6 text-center space-y-3">
          <p className="text-xs text-rose-700 font-bold">{error}</p>
          <button
            type="button"
            onClick={() => handleOptimize(costWeight, bufferMinutes)}
            className="px-4 py-2 bg-rose-600 hover:bg-rose-700 text-white text-xs font-bold rounded-xl cursor-pointer"
          >
            Retry Optimization
          </button>
        </div>
      )}

      {/* Main Results Grid */}
      {!loading && !error && (
        <div className="space-y-8">
          
          {/* 3 Top Highlight Cards (Cheapest, Fastest, Balanced) */}
          <div className="grid grid-cols-1 md:grid-cols-3 gap-5">
            
            {/* Cheapest Card */}
            {cheapest && (
              <div
                onClick={() => setSelectedJourneyId(cheapest.id)}
                className={`p-6 rounded-3xl border transition-all duration-200 cursor-pointer flex flex-col justify-between ${
                  selectedJourney?.id === cheapest.id
                    ? 'bg-emerald-50/70 border-emerald-400 ring-2 ring-emerald-500/20 shadow-md'
                    : 'bg-white hover:bg-slate-50 border-slate-200 shadow-xs'
                }`}
              >
                <div className="space-y-2">
                  <div className="inline-flex items-center gap-1.5 px-2.5 py-0.5 rounded-full bg-emerald-100 text-emerald-800 text-[10px] font-black uppercase tracking-wider">
                    <span>💰 Cheapest Itinerary</span>
                  </div>
                  <h3 className="text-lg font-black text-slate-900">{cheapest.title}</h3>
                  <div className="text-2xl font-black text-slate-900 font-mono">
                    ₹{Number(cheapest.totalCost).toLocaleString('en-IN')}
                  </div>
                  <p className="text-[11px] text-slate-500 font-medium">
                    Total Duration: <strong className="text-slate-800">{cheapest.formattedTotalDuration}</strong>
                  </p>
                </div>
                <div className="pt-4 border-t border-slate-100 mt-4 text-xs font-bold text-emerald-700 flex items-center justify-between">
                  <span>₹{Number(cheapest.costPerTraveler).toLocaleString('en-IN')}/person</span>
                  <span>Inspect →</span>
                </div>
              </div>
            )}

            {/* Balanced Card */}
            {balanced && (
              <div
                onClick={() => setSelectedJourneyId(balanced.id)}
                className={`p-6 rounded-3xl border transition-all duration-200 cursor-pointer flex flex-col justify-between ${
                  selectedJourney?.id === balanced.id
                    ? 'bg-indigo-50/70 border-indigo-400 ring-2 ring-indigo-500/20 shadow-md'
                    : 'bg-white hover:bg-slate-50 border-slate-200 shadow-xs'
                }`}
              >
                <div className="space-y-2">
                  <div className="inline-flex items-center gap-1.5 px-2.5 py-0.5 rounded-full bg-indigo-100 text-indigo-800 text-[10px] font-black uppercase tracking-wider">
                    <span>⚖️ Recommended Balanced</span>
                  </div>
                  <h3 className="text-lg font-black text-slate-900">{balanced.title}</h3>
                  <div className="text-2xl font-black text-slate-900 font-mono">
                    ₹{Number(balanced.totalCost).toLocaleString('en-IN')}
                  </div>
                  <p className="text-[11px] text-slate-500 font-medium">
                    Total Duration: <strong className="text-slate-800">{balanced.formattedTotalDuration}</strong>
                  </p>
                </div>
                <div className="pt-4 border-t border-slate-100 mt-4 text-xs font-bold text-indigo-700 flex items-center justify-between">
                  <span>₹{Number(balanced.costPerTraveler).toLocaleString('en-IN')}/person</span>
                  <span>Inspect →</span>
                </div>
              </div>
            )}

            {/* Fastest Card */}
            {fastest && (
              <div
                onClick={() => setSelectedJourneyId(fastest.id)}
                className={`p-6 rounded-3xl border transition-all duration-200 cursor-pointer flex flex-col justify-between ${
                  selectedJourney?.id === fastest.id
                    ? 'bg-sky-50/70 border-sky-400 ring-2 ring-sky-500/20 shadow-md'
                    : 'bg-white hover:bg-slate-50 border-slate-200 shadow-xs'
                }`}
              >
                <div className="space-y-2">
                  <div className="inline-flex items-center gap-1.5 px-2.5 py-0.5 rounded-full bg-sky-100 text-sky-800 text-[10px] font-black uppercase tracking-wider">
                    <span>⚡ Fastest Transit</span>
                  </div>
                  <h3 className="text-lg font-black text-slate-900">{fastest.title}</h3>
                  <div className="text-2xl font-black text-slate-900 font-mono">
                    ₹{Number(fastest.totalCost).toLocaleString('en-IN')}
                  </div>
                  <p className="text-[11px] text-slate-500 font-medium">
                    Total Duration: <strong className="text-slate-800">{fastest.formattedTotalDuration}</strong>
                  </p>
                </div>
                <div className="pt-4 border-t border-slate-100 mt-4 text-xs font-bold text-sky-700 flex items-center justify-between">
                  <span>₹{Number(fastest.costPerTraveler).toLocaleString('en-IN')}/person</span>
                  <span>Inspect →</span>
                </div>
              </div>
            )}

          </div>

          {/* Tradeoff Summary */}
          {journeyData?.tradeoffSummary && (
            <div className="bg-slate-900 text-white rounded-3xl p-6 sm:p-7 flex flex-col md:flex-row md:items-center justify-between gap-4 shadow-xl">
              <div className="space-y-1 max-w-3xl">
                <span className="text-[10px] font-bold text-indigo-300 uppercase tracking-wider">
                  Cost vs Time Tradeoff Diagnostic
                </span>
                <p className="text-xs sm:text-sm font-medium text-slate-200 leading-relaxed">
                  {journeyData.tradeoffSummary}
                </p>
              </div>
              <div className="px-4 py-2 rounded-xl bg-white/10 border border-white/15 text-xs font-mono font-bold text-emerald-400 shrink-0">
                ⚡ Processed in {journeyData.executionTimeMs}ms
              </div>
            </div>
          )}

          {/* Selected Itinerary Timeline Visualizer */}
          {selectedJourney && (
            <JourneyTimelineVisualizer journey={selectedJourney} travelers={travelers} />
          )}

          {/* Alternative Combinations List */}
          <div className="bg-white rounded-3xl border border-slate-200 p-6 sm:p-7 shadow-xs space-y-4">
            <h3 className="text-base font-extrabold text-slate-900">
              All Validated Journey Combinations ({allCombinations.length})
            </h3>
            <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
              {allCombinations.map((combo) => {
                const isSelected = selectedJourney?.id === combo.id;
                return (
                  <div
                    key={combo.id}
                    onClick={() => setSelectedJourneyId(combo.id)}
                    className={`p-4 rounded-2xl border transition-all duration-200 cursor-pointer flex items-center justify-between gap-4 ${
                      isSelected
                        ? 'bg-indigo-50/70 border-indigo-500 shadow-xs'
                        : 'bg-slate-50 hover:bg-slate-100/80 border-slate-200'
                    }`}
                  >
                    <div className="space-y-1">
                      <div className="flex items-center gap-2">
                        <span className="text-xs font-extrabold text-slate-900">{combo.title}</span>
                        {combo.classification && (
                          <span className="px-2 py-0.5 rounded-md text-[9px] font-black bg-white text-slate-700 border border-slate-200">
                            {combo.classification}
                          </span>
                        )}
                      </div>
                      <p className="text-[11px] text-slate-500 font-medium">
                        {combo.formattedTotalDuration} • {combo.transferCount} transfers • Score {combo.compositeScore}
                      </p>
                    </div>

                    <div className="text-right shrink-0">
                      <div className="text-base font-black text-slate-900 font-mono">
                        ₹{Number(combo.totalCost).toLocaleString('en-IN')}
                      </div>
                      <span className="text-[10px] font-bold text-indigo-600 block">
                        {isSelected ? '✓ Viewing' : 'Select'}
                      </span>
                    </div>
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

export default SmartJourneyPage;
