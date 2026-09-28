import React, { useState, useEffect } from 'react';
import { optimizeBudgetPlan, parseBudgetQuery } from '../services/budgetService';
import JourneyTimelineVisualizer from '../components/JourneyTimelineVisualizer';
import LoadingSkeleton from '../components/LoadingSkeleton';

const SAMPLE_BUDGET_QUERIES = [
  'We are 3 people going from Delhi to Goa. Our transport budget is ₹25,000.',
  '2 travelers Mumbai to Bangalore under ₹18,000 fastest',
  '4 friends Delhi to Jaipur under ₹10,000 cheapest',
];

const PREFERENCES = [
  { id: 'BALANCED', label: '⚖️ Best Value', icon: '⚖️' },
  { id: 'CHEAPEST', label: '💰 Cheapest', icon: '💰' },
  { id: 'FASTEST', label: '⚡ Fastest', icon: '⚡' },
  { id: 'COMFORT', label: '✨ Comfort', icon: '✨' },
];

export const BudgetAssistantPage = () => {
  const [nlQuery, setNlQuery] = useState('We are 3 people going from Delhi to Goa. Our transport budget is ₹25,000.');
  const [origin, setOrigin] = useState('Delhi');
  const [destination, setDestination] = useState('Goa');
  const [travelers, setTravelers] = useState(3);
  const [maxBudget, setMaxBudget] = useState(25000);
  const [preference, setPreference] = useState('BALANCED');

  const [budgetData, setBudgetData] = useState(null);
  const [selectedPlanId, setSelectedPlanId] = useState(null);
  const [loading, setLoading] = useState(true);
  const [parsing, setParsing] = useState(false);
  const [error, setError] = useState(null);

  const fetchBudgetPlan = async (overrides = {}) => {
    setLoading(true);
    setError(null);
    try {
      const constraint = {
        origin: overrides.origin || origin,
        destination: overrides.destination || destination,
        travelers: overrides.travelers || travelers,
        maxBudget: overrides.maxBudget || maxBudget,
        preference: overrides.preference || preference,
        airportBufferMinutes: 90,
      };
      const data = await optimizeBudgetPlan(constraint);
      setBudgetData(data);
      if (data.bestPlan) {
        setSelectedPlanId(data.bestPlan.id);
      } else if (data.plans && data.plans.length > 0) {
        setSelectedPlanId(data.plans[0].id);
      }
    } catch (err) {
      console.error('Failed to optimize budget plan:', err);
      setError(err.message || 'Could not optimize budget plan.');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchBudgetPlan();
  }, []);

  const handleParseNlQuery = async (queryToParse) => {
    const q = queryToParse || nlQuery;
    if (!q || !q.trim()) return;

    setParsing(true);
    try {
      const parsed = await parseBudgetQuery(q.trim());
      if (parsed) {
        setOrigin(parsed.origin || 'Delhi');
        setDestination(parsed.destination || 'Goa');
        setTravelers(parsed.travelers || 1);
        setMaxBudget(parsed.maxBudget ? Number(parsed.maxBudget) : 25000);
        setPreference(parsed.preference || 'BALANCED');

        fetchBudgetPlan({
          origin: parsed.origin,
          destination: parsed.destination,
          travelers: parsed.travelers,
          maxBudget: parsed.maxBudget,
          preference: parsed.preference,
        });
      }
    } catch (err) {
      console.error('Failed to parse query:', err);
    } finally {
      setParsing(false);
    }
  };

  const handleFormSubmit = (e) => {
    e.preventDefault();
    fetchBudgetPlan();
  };

  const handlePreferenceChange = (prefId) => {
    setPreference(prefId);
    fetchBudgetPlan({ preference: prefId });
  };

  const plans = budgetData?.plans || [];
  const selectedPlan = plans.find((p) => p.id === selectedPlanId) || budgetData?.bestPlan || plans[0];
  const cheapestPlan = budgetData?.cheapestPlan;
  const fastestPlan = budgetData?.fastestPlan;
  const bestValuePlan = budgetData?.bestValuePlan;

  return (
    <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8 space-y-8 animate-fadeIn" data-testid="budget-assistant-page">
      
      {/* Header */}
      <div>
        <div className="flex items-center gap-2 text-xs text-slate-500 mb-2">
          <span>Home</span>
          <span>/</span>
          <span className="text-indigo-600 font-medium">Budget Assistant 2.0</span>
        </div>
        <div className="flex flex-col md:flex-row md:items-center justify-between gap-4">
          <div>
            <h1 className="text-3xl sm:text-4xl font-extrabold text-slate-900 tracking-tight">
              Constraint-Based <span className="text-emerald-600">Budget Assistant</span>
            </h1>
            <p className="text-sm text-slate-500 mt-1 max-w-2xl">
              Specify your maximum group budget and travel parameters. CompareHub strictly prunes out-of-budget options and optimizes the remaining door-to-door itineraries.
            </p>
          </div>

          <div className="inline-flex items-center gap-2 px-3.5 py-1.5 rounded-2xl bg-emerald-50 border border-emerald-200 text-emerald-800 text-xs font-bold">
            <span>🛡️ Strict Financial Constraint Engine</span>
          </div>
        </div>
      </div>

      {/* Natural Language Prompt & Quick Suggestions */}
      <div className="bg-gradient-to-br from-indigo-900 via-slate-900 to-indigo-950 text-white rounded-3xl p-6 sm:p-8 shadow-xl space-y-4">
        <div className="space-y-1">
          <span className="text-xs font-bold text-indigo-300 uppercase tracking-wider">
            Natural-Language Budget Assistant
          </span>
          <h2 className="text-xl sm:text-2xl font-black text-white">
            Plan with Natural Intent
          </h2>
        </div>

        <div className="flex flex-col sm:flex-row items-center gap-3">
          <input
            type="text"
            value={nlQuery}
            onChange={(e) => setNlQuery(e.target.value)}
            placeholder="e.g. 'We are 3 people going from Delhi to Goa. Our transport budget is ₹25,000.'"
            aria-label="Natural language budget query"
            className="w-full px-5 py-3.5 rounded-2xl bg-white/10 border border-white/20 text-white placeholder-slate-400 text-xs sm:text-sm font-semibold focus:outline-hidden focus:bg-white/15 focus:border-indigo-400"
          />
          <button
            type="button"
            disabled={parsing}
            onClick={() => handleParseNlQuery(nlQuery)}
            className="w-full sm:w-auto px-6 py-3.5 bg-emerald-500 hover:bg-emerald-600 text-slate-950 font-black text-xs sm:text-sm rounded-2xl transition shadow-md cursor-pointer shrink-0 disabled:opacity-50"
          >
            {parsing ? 'Parsing Intent...' : 'Apply Query →'}
          </button>
        </div>

        <div className="flex flex-wrap items-center gap-2 text-xs pt-1">
          <span className="text-slate-400 font-medium">Try queries:</span>
          {SAMPLE_BUDGET_QUERIES.map((sample, idx) => (
            <button
              key={idx}
              type="button"
              onClick={() => {
                setNlQuery(sample);
                handleParseNlQuery(sample);
              }}
              className="px-3 py-1 rounded-xl bg-white/10 hover:bg-white/20 text-slate-300 font-medium transition cursor-pointer text-[11px]"
            >
              {sample}
            </button>
          ))}
        </div>
      </div>

      {/* Structured Constraint Controls */}
      <div className="bg-white rounded-3xl border border-slate-200 p-6 sm:p-7 shadow-xs space-y-6">
        <form onSubmit={handleFormSubmit} className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4">
          
          <div className="space-y-1.5">
            <label className="text-xs font-bold text-slate-600 uppercase tracking-wider block">
              Origin
            </label>
            <input
              type="text"
              value={origin}
              onChange={(e) => setOrigin(e.target.value)}
              className="w-full px-4 py-2.5 rounded-xl bg-slate-50 border border-slate-200 text-slate-900 text-xs sm:text-sm font-semibold focus:bg-white focus:outline-hidden focus:border-indigo-500"
            />
          </div>

          <div className="space-y-1.5">
            <label className="text-xs font-bold text-slate-600 uppercase tracking-wider block">
              Destination
            </label>
            <input
              type="text"
              value={destination}
              onChange={(e) => setDestination(e.target.value)}
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

          <div className="space-y-1.5">
            <label className="text-xs font-bold text-slate-600 uppercase tracking-wider block">
              Max Budget Cap (₹)
            </label>
            <input
              type="number"
              min="1000"
              step="500"
              value={maxBudget}
              onChange={(e) => setMaxBudget(Number(e.target.value))}
              className="w-full px-4 py-2.5 rounded-xl bg-slate-50 border border-slate-200 text-slate-900 text-xs sm:text-sm font-bold font-mono focus:bg-white focus:outline-hidden focus:border-indigo-500"
            />
          </div>

          <div className="sm:col-span-2 lg:col-span-3 flex items-center gap-2 flex-wrap">
            <span className="text-xs font-bold text-slate-500 mr-2">Target Priority:</span>
            {PREFERENCES.map((pref) => (
              <button
                key={pref.id}
                type="button"
                onClick={() => handlePreferenceChange(pref.id)}
                className={`px-3.5 py-1.5 rounded-xl text-xs font-bold transition cursor-pointer flex items-center gap-1.5 ${
                  preference === pref.id
                    ? 'bg-slate-900 text-white shadow-xs'
                    : 'bg-slate-100 hover:bg-slate-200 text-slate-700'
                }`}
              >
                <span>{pref.label}</span>
              </button>
            ))}
          </div>

          <div className="flex items-center justify-end">
            <button
              type="submit"
              disabled={loading}
              className="w-full py-2.5 bg-emerald-600 hover:bg-emerald-700 text-white font-bold text-xs sm:text-sm rounded-xl shadow-md transition cursor-pointer"
            >
              {loading ? 'Optimizing Plans...' : 'Update Budget Optimization'}
            </button>
          </div>

        </form>
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
            onClick={() => fetchBudgetPlan()}
            className="px-4 py-2 bg-rose-600 hover:bg-rose-700 text-white text-xs font-bold rounded-xl cursor-pointer"
          >
            Retry Budget Optimization
          </button>
        </div>
      )}

      {/* Main Budget Plan Results */}
      {!loading && !error && budgetData && (
        <div className="space-y-8">
          
          {/* Summary Banner & Financial Health Ribbon */}
          <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-5">
            
            <div className="bg-white rounded-3xl border border-slate-200 p-6 shadow-xs space-y-1">
              <span className="text-[10px] font-bold text-slate-400 uppercase tracking-wider block">Allocated Budget</span>
              <div className="text-2xl sm:text-3xl font-black text-slate-900 font-mono">
                ₹{Number(maxBudget).toLocaleString('en-IN')}
              </div>
              <p className="text-[11px] text-slate-500 font-medium">For {travelers} travelers</p>
            </div>

            <div className="bg-white rounded-3xl border border-emerald-200 p-6 shadow-xs space-y-1 bg-gradient-to-br from-white to-emerald-50/40">
              <span className="text-[10px] font-bold text-emerald-800 uppercase tracking-wider block">Optimal Plan Spend</span>
              <div className="text-2xl sm:text-3xl font-black text-emerald-900 font-mono">
                ₹{selectedPlan ? Number(selectedPlan.totalCost).toLocaleString('en-IN') : '0'}
              </div>
              <p className="text-[11px] text-emerald-700 font-medium">
                ₹{selectedPlan ? Number(selectedPlan.costPerTraveler).toLocaleString('en-IN') : '0'}/person
              </p>
            </div>

            <div className="bg-white rounded-3xl border border-slate-200 p-6 shadow-xs space-y-1">
              <span className="text-[10px] font-bold text-slate-400 uppercase tracking-wider block">Remaining Budget Reserve</span>
              <div className="text-2xl sm:text-3xl font-black text-indigo-600 font-mono">
                ₹{selectedPlan ? Number(selectedPlan.remainingBudget).toLocaleString('en-IN') : '0'}
              </div>
              <p className="text-[11px] text-slate-500 font-medium">
                {selectedPlan ? `${100 - selectedPlan.budgetUtilizationPercent}% unspent reserve` : '0%'}
              </p>
            </div>

            <div className="bg-white rounded-3xl border border-slate-200 p-6 shadow-xs space-y-1">
              <span className="text-[10px] font-bold text-slate-400 uppercase tracking-wider block">Constraint Results</span>
              <div className="text-2xl sm:text-3xl font-black text-slate-900 font-mono">
                {plans.length} <span className="text-xs text-slate-400 font-normal">plans fit</span>
              </div>
              <p className="text-[11px] text-slate-500 font-medium">
                {budgetData.exceededBudgetOptionsCount} pruned over-budget
              </p>
            </div>

          </div>

          {/* Diagnostic Text */}
          <div className="bg-slate-900 text-white rounded-3xl p-6 shadow-xl flex flex-col md:flex-row md:items-center justify-between gap-4">
            <div className="space-y-1">
              <span className="text-[10px] font-bold text-emerald-400 uppercase tracking-wider">
                Optimization Status
              </span>
              <p className="text-xs sm:text-sm font-medium text-slate-200 leading-relaxed">
                {budgetData.summaryText}
              </p>
            </div>
            <div className="px-4 py-2 rounded-xl bg-white/10 text-xs font-mono font-bold text-emerald-400 shrink-0">
              ⚡ Evaluated in {budgetData.executionTimeMs}ms
            </div>
          </div>

          {/* If No Plans Found Under Budget */}
          {plans.length === 0 && (
            <div className="bg-amber-50 border border-amber-200 rounded-3xl p-8 text-center space-y-3">
              <div className="text-3xl">⚠️</div>
              <h3 className="text-lg font-bold text-amber-900">Budget Limit Exceeded</h3>
              <p className="text-xs text-amber-800 max-w-lg mx-auto leading-relaxed">
                No available door-to-door combinations for {travelers} travelers meet the ₹{Number(maxBudget).toLocaleString('en-IN')} limit. Lowest verified rate starts at <strong>₹{Number(budgetData.lowestAvailablePrice).toLocaleString('en-IN')}</strong>.
              </p>
              <button
                type="button"
                onClick={() => {
                  const newBudget = Math.ceil(Number(budgetData.lowestAvailablePrice) / 1000) * 1000;
                  setMaxBudget(newBudget);
                  fetchBudgetPlan({ maxBudget: newBudget });
                }}
                className="px-5 py-2.5 bg-amber-600 hover:bg-amber-700 text-white font-bold text-xs rounded-xl shadow-xs cursor-pointer"
              >
                Set Budget to ₹{Number(budgetData.lowestAvailablePrice).toLocaleString('en-IN')}
              </button>
            </div>
          )}

          {/* 3 Core Recommended Plan Cards */}
          {plans.length > 0 && (
            <div className="grid grid-cols-1 md:grid-cols-3 gap-5">
              
              {/* Option A: Fastest */}
              {fastestPlan && (
                <div
                  onClick={() => setSelectedPlanId(fastestPlan.id)}
                  className={`p-6 rounded-3xl border transition-all duration-200 cursor-pointer flex flex-col justify-between ${
                    selectedPlan?.id === fastestPlan.id
                      ? 'bg-sky-50/70 border-sky-400 ring-2 ring-sky-500/20 shadow-md'
                      : 'bg-white hover:bg-slate-50 border-slate-200 shadow-xs'
                  }`}
                >
                  <div className="space-y-2">
                    <div className="flex items-center justify-between gap-2">
                      <span className="px-2.5 py-0.5 rounded-full bg-sky-100 text-sky-800 text-[10px] font-black uppercase tracking-wider">
                        Option A: ⚡ Fastest
                      </span>
                      <span className="text-[10px] font-bold text-slate-400 font-mono">
                        {fastestPlan.formattedDuration}
                      </span>
                    </div>
                    <h3 className="text-lg font-black text-slate-900">{fastestPlan.title}</h3>
                    <div className="text-2xl font-black text-slate-900 font-mono">
                      ₹{Number(fastestPlan.totalCost).toLocaleString('en-IN')}
                    </div>
                    <p className="text-[11px] text-slate-500 font-medium leading-relaxed">
                      {fastestPlan.recommendationReason}
                    </p>
                  </div>

                  <div className="pt-4 border-t border-slate-100 mt-4 text-xs font-bold text-sky-700 flex items-center justify-between">
                    <span>Reserve: ₹{Number(fastestPlan.remainingBudget).toLocaleString('en-IN')}</span>
                    <span>Inspect Plan →</span>
                  </div>
                </div>
              )}

              {/* Option B: Best Value */}
              {bestValuePlan && (
                <div
                  onClick={() => setSelectedPlanId(bestValuePlan.id)}
                  className={`p-6 rounded-3xl border transition-all duration-200 cursor-pointer flex flex-col justify-between ${
                    selectedPlan?.id === bestValuePlan.id
                      ? 'bg-indigo-50/70 border-indigo-400 ring-2 ring-indigo-500/20 shadow-md'
                      : 'bg-white hover:bg-slate-50 border-slate-200 shadow-xs'
                  }`}
                >
                  <div className="space-y-2">
                    <div className="flex items-center justify-between gap-2">
                      <span className="px-2.5 py-0.5 rounded-full bg-indigo-100 text-indigo-800 text-[10px] font-black uppercase tracking-wider">
                        Option B: ⚖️ Best Value
                      </span>
                      <span className="text-[10px] font-bold text-slate-400 font-mono">
                        {bestValuePlan.formattedDuration}
                      </span>
                    </div>
                    <h3 className="text-lg font-black text-slate-900">{bestValuePlan.title}</h3>
                    <div className="text-2xl font-black text-slate-900 font-mono">
                      ₹{Number(bestValuePlan.totalCost).toLocaleString('en-IN')}
                    </div>
                    <p className="text-[11px] text-slate-500 font-medium leading-relaxed">
                      {bestValuePlan.recommendationReason}
                    </p>
                  </div>

                  <div className="pt-4 border-t border-slate-100 mt-4 text-xs font-bold text-indigo-700 flex items-center justify-between">
                    <span>Reserve: ₹{Number(bestValuePlan.remainingBudget).toLocaleString('en-IN')}</span>
                    <span>Inspect Plan →</span>
                  </div>
                </div>
              )}

              {/* Option C: Cheapest */}
              {cheapestPlan && (
                <div
                  onClick={() => setSelectedPlanId(cheapestPlan.id)}
                  className={`p-6 rounded-3xl border transition-all duration-200 cursor-pointer flex flex-col justify-between ${
                    selectedPlan?.id === cheapestPlan.id
                      ? 'bg-emerald-50/70 border-emerald-400 ring-2 ring-emerald-500/20 shadow-md'
                      : 'bg-white hover:bg-slate-50 border-slate-200 shadow-xs'
                  }`}
                >
                  <div className="space-y-2">
                    <div className="flex items-center justify-between gap-2">
                      <span className="px-2.5 py-0.5 rounded-full bg-emerald-100 text-emerald-800 text-[10px] font-black uppercase tracking-wider">
                        Option C: 💰 Cheapest
                      </span>
                      <span className="text-[10px] font-bold text-slate-400 font-mono">
                        {cheapestPlan.formattedDuration}
                      </span>
                    </div>
                    <h3 className="text-lg font-black text-slate-900">{cheapestPlan.title}</h3>
                    <div className="text-2xl font-black text-slate-900 font-mono">
                      ₹{Number(cheapestPlan.totalCost).toLocaleString('en-IN')}
                    </div>
                    <p className="text-[11px] text-slate-500 font-medium leading-relaxed">
                      {cheapestPlan.recommendationReason}
                    </p>
                  </div>

                  <div className="pt-4 border-t border-slate-100 mt-4 text-xs font-bold text-emerald-700 flex items-center justify-between">
                    <span>Reserve: ₹{Number(cheapestPlan.remainingBudget).toLocaleString('en-IN')}</span>
                    <span>Inspect Plan →</span>
                  </div>
                </div>
              )}

            </div>
          )}

          {/* Timeline Visualizer for Selected Plan */}
          {selectedPlan && selectedPlan.journeyOption && (
            <div className="space-y-3">
              <h3 className="text-base font-extrabold text-slate-900">
                Door-to-Door Itinerary Breakdown for {selectedPlan.title}
              </h3>
              <JourneyTimelineVisualizer
                journey={selectedPlan.journeyOption}
                travelers={travelers}
              />
            </div>
          )}

        </div>
      )}

    </div>
  );
};

export default BudgetAssistantPage;
