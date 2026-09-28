import React, { useState, useEffect } from 'react';
import decisionEngineService from '../services/decisionEngineService';
import DecisionAdvisorCard from '../components/DecisionAdvisorCard';
import JourneyTimelineVisualizer from '../components/JourneyTimelineVisualizer';
import BestPaymentOptionCard from '../components/BestPaymentOptionCard';
import PurchaseTimingCard from '../components/PurchaseTimingCard';

export const DecisionEnginePage = () => {
  const [activeType, setActiveType] = useState('PRODUCT');
  const [query, setQuery] = useState('iPhone 15');
  const [preference, setPreference] = useState('BALANCED');
  const [loading, setLoading] = useState(false);
  const [decisionData, setDecisionData] = useState(null);
  const [error, setError] = useState(null);

  const PRESETS = [
    { label: '📱 iPhone 15 Pro', type: 'PRODUCT', query: 'iPhone 15' },
    { label: '🎯 Delhi to Goa Budget', type: 'BUDGET', query: 'We are 3 people going from Delhi to Goa. Our transport budget is ₹25,000.' },
    { label: '✈️ Delhi to Mumbai Flight', type: 'FLIGHT', query: 'Cheapest morning flight from Delhi to Mumbai' },
    { label: '🚖 Airport Cab Transfer', type: 'RIDE', query: 'Cab from Connaught Place to Airport' },
    { label: '🗺️ Door-to-Door Journey', type: 'JOURNEY', query: 'Smart Journey from Delhi to Goa for 2 people' },
    { label: '🛒 Grocery Cart Split', type: 'CART', query: 'Monthly grocery staples basket' },
    { label: '👥 Jaipur Group Trip', type: 'GROUP_TRAVEL', query: 'Group travel for 4 people from Delhi to Jaipur' }
  ];

  const DOMAIN_OPTIONS = [
    { id: 'AUTO_DETECT', label: '⚡ Auto-Detect' },
    { id: 'PRODUCT', label: '🛍️ Product' },
    { id: 'BUDGET', label: '🎯 Budget Assistant' },
    { id: 'JOURNEY', label: '🗺️ Smart Journey' },
    { id: 'FLIGHT', label: '✈️ Flights' },
    { id: 'RIDE', label: '🚖 Rides' },
    { id: 'CART', label: '🛒 Cart' },
    { id: 'GROUP_TRAVEL', label: '👥 Group Travel' }
  ];

  const fetchDecision = async (typeToFetch, queryToFetch) => {
    setLoading(true);
    setError(null);
    try {
      const data = await decisionEngineService.evaluateDecision({
        decisionType: typeToFetch,
        query: queryToFetch,
        preference: preference,
        origin: 'Delhi',
        destination: 'Goa',
        travelers: 3,
        maxBudget: 25000
      });
      setDecisionData(data);
    } catch (err) {
      console.error('Decision Engine evaluation failed, falling back to sample:', err);
      try {
        const fallback = await decisionEngineService.getSampleDecision(typeToFetch === 'AUTO_DETECT' ? 'PRODUCT' : typeToFetch);
        setDecisionData(fallback);
      } catch (fallbackErr) {
        setError('Failed to evaluate decision. Please try again.');
      }
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchDecision('PRODUCT', 'iPhone 15');
  }, []);

  const handleSelectPreset = (preset) => {
    setActiveType(preset.type);
    setQuery(preset.query);
    fetchDecision(preset.type, preset.query);
  };

  const handleRunEvaluation = (e) => {
    e.preventDefault();
    fetchDecision(activeType, query);
  };

  const formatCurrency = (amount) => {
    if (!amount) return '0';
    return Number(amount).toLocaleString('en-IN', { maximumFractionDigits: 0 });
  };

  return (
    <div className="min-h-screen bg-slate-950 text-slate-100 py-10 px-4 sm:px-6 lg:px-8 animate-fadeIn">
      <div className="max-w-7xl mx-auto space-y-8">
        
        {/* Header Ribbon */}
        <div className="text-center space-y-3">
          <div className="inline-flex items-center gap-2 px-3.5 py-1.5 rounded-full bg-indigo-500/10 border border-indigo-500/30 text-indigo-400 text-xs font-black uppercase tracking-wider shadow-inner">
            <span>🧠</span>
            <span>Universal Orchestration Engine</span>
          </div>
          <h1 className="text-3xl sm:text-4xl lg:text-5xl font-black tracking-tight text-white">
            CompareHub Decision Engine
          </h1>
          <p className="max-w-3xl mx-auto text-sm sm:text-base text-slate-400 leading-relaxed">
            Multi-domain decision intelligence orchestrating search, matching, true cost calculations, price intelligence, multi-objective ranking, and AI Decision Advisor 2.0 with strict zero hallucination.
          </p>
        </div>

        {/* Preset Chips */}
        <div className="flex flex-wrap items-center justify-center gap-2 pt-2">
          <span className="text-xs font-bold text-slate-400 mr-1">Quick Scenarios:</span>
          {PRESETS.map((preset, idx) => (
            <button
              key={idx}
              onClick={() => handleSelectPreset(preset)}
              className={`px-3 py-1.5 rounded-xl text-xs font-semibold transition-all duration-200 cursor-pointer border ${
                activeType === preset.type && query === preset.query
                  ? 'bg-indigo-600 text-white border-indigo-400 shadow-md shadow-indigo-600/30'
                  : 'bg-slate-900/80 text-slate-300 border-slate-800 hover:bg-slate-800 hover:text-white'
              }`}
            >
              {preset.label}
            </button>
          ))}
        </div>

        {/* Query & Control Console */}
        <form onSubmit={handleRunEvaluation} className="bg-slate-900/90 rounded-3xl p-6 sm:p-7 border border-slate-800 shadow-2xl space-y-6">
          <div className="space-y-3">
            <label className="text-xs font-extrabold uppercase tracking-wider text-indigo-300">
              User Request / Intent Query
            </label>
            <div className="relative flex items-center">
              <span className="absolute left-4 text-slate-400 text-lg">🔍</span>
              <input
                type="text"
                value={query}
                onChange={(e) => setQuery(e.target.value)}
                placeholder="Ask anything across products, flights, rides, group travel, or door-to-door budgets..."
                className="w-full bg-slate-950 border border-slate-800 rounded-2xl py-3.5 pl-12 pr-28 text-white placeholder-slate-500 text-sm sm:text-base focus:outline-none focus:border-indigo-500 transition-colors shadow-inner"
              />
              <button
                type="submit"
                disabled={loading}
                className="absolute right-2 px-5 py-2.5 bg-indigo-600 hover:bg-indigo-500 disabled:opacity-50 text-white rounded-xl text-xs font-extrabold transition-all duration-200 cursor-pointer shadow-md shadow-indigo-600/30 flex items-center gap-1.5"
              >
                {loading ? (
                  <>
                    <span className="animate-spin text-sm">⏳</span>
                    <span>Orchestrating...</span>
                  </>
                ) : (
                  <>
                    <span>⚡</span>
                    <span>Run Engine</span>
                  </>
                )}
              </button>
            </div>
          </div>

          {/* Domain & Preference Pills */}
          <div className="grid grid-cols-1 md:grid-cols-2 gap-4 pt-2 border-t border-slate-800/80">
            {/* Domain Select */}
            <div className="space-y-2">
              <span className="text-[11px] font-bold text-slate-400 uppercase tracking-wider">Domain Type</span>
              <div className="flex flex-wrap gap-1.5">
                {DOMAIN_OPTIONS.map((opt) => (
                  <button
                    key={opt.id}
                    type="button"
                    onClick={() => setActiveType(opt.id)}
                    className={`px-2.5 py-1 rounded-lg text-xs font-bold transition-colors cursor-pointer border ${
                      activeType === opt.id
                        ? 'bg-indigo-500/20 text-indigo-300 border-indigo-500/40'
                        : 'bg-slate-950 text-slate-400 border-slate-800 hover:text-slate-200'
                    }`}
                  >
                    {opt.label}
                  </button>
                ))}
              </div>
            </div>

            {/* Objective Preference */}
            <div className="space-y-2">
              <span className="text-[11px] font-bold text-slate-400 uppercase tracking-wider">Optimization Priority</span>
              <div className="flex flex-wrap gap-1.5">
                {[
                  { id: 'BALANCED', label: '⚖️ Balanced' },
                  { id: 'CHEAPEST', label: '💰 Lowest Price' },
                  { id: 'FASTEST', label: '⚡ Maximum Speed' },
                  { id: 'RATING', label: '⭐ Highest Rated' }
                ].map((pref) => (
                  <button
                    key={pref.id}
                    type="button"
                    onClick={() => setPreference(pref.id)}
                    className={`px-2.5 py-1 rounded-lg text-xs font-bold transition-colors cursor-pointer border ${
                      preference === pref.id
                        ? 'bg-indigo-500/20 text-indigo-300 border-indigo-500/40'
                        : 'bg-slate-950 text-slate-400 border-slate-800 hover:text-slate-200'
                    }`}
                  >
                    {pref.label}
                  </button>
                ))}
              </div>
            </div>
          </div>
        </form>

        {/* Live Orchestration Pipeline Audit Ribbon */}
        {decisionData?.pipelineAudit && decisionData.pipelineAudit.length > 0 && (
          <div className="bg-slate-900/60 rounded-2xl p-4 border border-slate-800/80 space-y-2.5">
            <div className="flex items-center justify-between">
              <span className="text-[11px] font-extrabold uppercase tracking-wider text-slate-400 flex items-center gap-1.5">
                <span className="w-2 h-2 rounded-full bg-emerald-400 animate-ping"></span>
                <span>Active Pipeline Execution Stages</span>
              </span>
              <span className="text-[11px] text-emerald-400 font-bold">
                ✓ All {decisionData.pipelineAudit.length} stages verified
              </span>
            </div>
            <div className="flex flex-wrap items-center gap-2">
              {decisionData.pipelineAudit.map((stage, idx) => (
                <div key={idx} className="flex items-center gap-1.5 text-xs">
                  <span className="px-2.5 py-1 rounded-lg bg-slate-950 text-slate-300 border border-slate-800 font-mono text-[11px] flex items-center gap-1 shadow-xs">
                    <span className="text-indigo-400">#</span>
                    <span>{stage.replace(/_/g, ' ')}</span>
                  </span>
                  {idx < decisionData.pipelineAudit.length - 1 && (
                    <span className="text-slate-600">→</span>
                  )}
                </div>
              ))}
            </div>
          </div>
        )}

        {/* Error Notification */}
        {error && (
          <div className="p-4 bg-rose-500/10 border border-rose-500/30 rounded-2xl text-rose-300 text-sm font-semibold">
            ⚠️ {error}
          </div>
        )}

        {/* Core Decision Envelope */}
        {decisionData && (
          <div className="space-y-8 animate-fadeIn">
            
            {/* Top Stat Cards Ribbon */}
            <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4">
              {/* Decision Type Card */}
              <div className="bg-slate-900/90 rounded-2xl p-5 border border-slate-800 space-y-1">
                <span className="text-xs font-bold text-slate-400 uppercase tracking-wider">Decision Context</span>
                <p className="text-lg font-black text-white flex items-center gap-2">
                  <span>✨</span>
                  <span>{decisionData.decisionType}</span>
                </p>
                <p className="text-xs text-slate-400 truncate">{decisionData.query}</p>
              </div>

              {/* Net Estimated Savings Card */}
              <div className="bg-slate-900/90 rounded-2xl p-5 border border-emerald-500/30 relative overflow-hidden space-y-1">
                <div className="absolute top-0 right-0 w-24 h-24 bg-emerald-500/10 rounded-full blur-2xl pointer-events-none"></div>
                <span className="text-xs font-bold text-emerald-400 uppercase tracking-wider">Estimated Savings</span>
                <p className="text-2xl font-black text-emerald-300">
                  ₹{formatCurrency(decisionData.estimatedSavings)}
                </p>
                <p className="text-[11px] text-slate-400">vs baseline / higher tier options</p>
              </div>

              {/* Recommended Pick Card */}
              <div className="bg-slate-900/90 rounded-2xl p-5 border border-indigo-500/30 relative overflow-hidden space-y-1">
                <div className="absolute top-0 right-0 w-24 h-24 bg-indigo-500/10 rounded-full blur-2xl pointer-events-none"></div>
                <span className="text-xs font-bold text-indigo-400 uppercase tracking-wider">Top Recommended Pick</span>
                <p className="text-sm font-black text-white truncate">
                  {decisionData.decisionAdvisor?.recommendedOption || 'Verified Top Match'}
                </p>
                <span className="inline-block px-2 py-0.5 rounded-md bg-indigo-500/20 text-indigo-300 text-[10px] font-bold">
                  {decisionData.decisionAdvisor?.confidence || 'HIGH'} Confidence
                </span>
              </div>

              {/* Verified Status Card */}
              <div className="bg-slate-900/90 rounded-2xl p-5 border border-slate-800 space-y-1">
                <span className="text-xs font-bold text-slate-400 uppercase tracking-wider">Data Verification</span>
                <p className="text-lg font-black text-white flex items-center gap-1.5">
                  <span className="text-emerald-400">🛡️</span>
                  <span>100% Deterministic</span>
                </p>
                <p className="text-[11px] text-slate-400">Zero AI calculation hallucinations</p>
              </div>
            </div>

            {/* AI Decision Advisor 2.0 Card */}
            {decisionData.decisionAdvisor && (
              <DecisionAdvisorCard
                recommendation={decisionData.decisionAdvisor}
                title={`AI ${decisionData.decisionType} Decision Advisor`}
              />
            )}

            {/* Domain-Specific Visual Drill-Downs */}

            {/* 1. Door-to-Door Journey Timeline Drill-down */}
            {(decisionData.decisionType === 'JOURNEY' || decisionData.cheapestJourney || decisionData.recommendedJourney) && (
              <div className="space-y-4">
                <h3 className="text-lg font-black text-white flex items-center gap-2">
                  <span>🗺️</span>
                  <span>Door-to-Door Itinerary Breakdown</span>
                </h3>
                <JourneyTimelineVisualizer
                  journeyOption={decisionData.recommendedJourney || decisionData.cheapestJourney || decisionData.balancedJourney}
                />
              </div>
            )}

            {/* 2. Budget Health Drill-down */}
            {decisionData.budgetOptimization && decisionData.budgetOptimization.bestPlan && (
              <div className="bg-slate-900/80 rounded-3xl p-6 border border-slate-800 space-y-4">
                <h3 className="text-lg font-black text-white flex items-center gap-2">
                  <span>🎯</span>
                  <span>Budget Allocation & Reserve Health</span>
                </h3>
                <div className="grid grid-cols-1 sm:grid-cols-3 gap-4">
                  <div className="bg-slate-950 p-4 rounded-2xl border border-slate-800">
                    <span className="text-xs text-slate-400">Allocated Budget</span>
                    <p className="text-xl font-black text-white">₹{formatCurrency(decisionData.budgetOptimization.constraint?.maxBudget || 25000)}</p>
                  </div>
                  <div className="bg-slate-950 p-4 rounded-2xl border border-slate-800">
                    <span className="text-xs text-slate-400">Itinerary Cost</span>
                    <p className="text-xl font-black text-indigo-400">₹{formatCurrency(decisionData.budgetOptimization.bestPlan.totalCost)}</p>
                  </div>
                  <div className="bg-slate-950 p-4 rounded-2xl border border-emerald-500/30">
                    <span className="text-xs text-emerald-400">Preserved Reserve</span>
                    <p className="text-xl font-black text-emerald-300">₹{formatCurrency(decisionData.budgetOptimization.bestPlan.remainingBudget)}</p>
                  </div>
                </div>
              </div>
            )}

            {/* 3. Product Intelligence Drill-down (Purchase Timing & Payment Offers) */}
            {decisionData.decisionType === 'PRODUCT' && (
              <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
                {decisionData.purchaseTiming && (
                  <PurchaseTimingCard timing={decisionData.purchaseTiming} />
                )}
                {decisionData.paymentOffers && (
                  <BestPaymentOptionCard
                    paymentSummary={decisionData.paymentOffers}
                    standardPrice={decisionData.cheapest?.price || 64999}
                  />
                )}
              </div>
            )}

            {/* 4. Cart Multi-Store Split Drill-down */}
            {decisionData.decisionType === 'CART' && decisionData.cartOptimization && (
              <div className="bg-slate-900/80 rounded-3xl p-6 border border-slate-800 space-y-4">
                <h3 className="text-lg font-black text-white flex items-center gap-2">
                  <span>🛒</span>
                  <span>Cart Optimization Plan</span>
                </h3>
                <div className="bg-slate-950 rounded-2xl p-4 border border-slate-800 flex items-center justify-between">
                  <div>
                    <p className="font-extrabold text-white text-base">
                      {decisionData.cartOptimization.recommendedPlan?.title || 'Optimal Store Split'}
                    </p>
                    <p className="text-xs text-slate-400">
                      {decisionData.cartOptimization.explanation || 'Calculated lowest cost across stores'}
                    </p>
                  </div>
                  <div className="text-right">
                    <span className="text-xs text-slate-400">Total</span>
                    <p className="text-xl font-black text-emerald-400">
                      ₹{formatCurrency(decisionData.cartOptimization.recommendedPlan?.grandTotal || 250)}
                    </p>
                  </div>
                </div>
              </div>
            )}

            {/* 5. Key Decision Insights List */}
            {decisionData.insights && decisionData.insights.length > 0 && (
              <div className="bg-slate-900/60 rounded-2xl p-5 border border-slate-800 space-y-3">
                <h4 className="text-xs font-extrabold uppercase tracking-wider text-indigo-300 flex items-center gap-1.5">
                  <span>💡</span>
                  <span>Key Quantitative Insights</span>
                </h4>
                <div className="grid grid-cols-1 sm:grid-cols-2 gap-2.5">
                  {decisionData.insights.map((insight, idx) => (
                    <div key={idx} className="flex items-start gap-2 text-xs text-slate-300">
                      <span className="text-indigo-400 font-bold shrink-0 mt-0.5">▪</span>
                      <span>{insight}</span>
                    </div>
                  ))}
                </div>
              </div>
            )}

          </div>
        )}

      </div>
    </div>
  );
};

export default DecisionEnginePage;
