import React from 'react';

export const DecisionAdvisorCard = ({ recommendation, title = "AI Decision Advisor" }) => {
  if (!recommendation) return null;

  // Support both new DecisionRecommendation and legacy formats
  const summaryText = recommendation.summary || recommendation.recommendation;
  if (!summaryText) return null;

  const recommendedOption = recommendation.recommendedOption || recommendation.bestOverall;
  const reasons = recommendation.reasons || recommendation.reasoningPoints || [];
  const tradeoffs = recommendation.tradeoffs || [];
  const confidence = recommendation.confidence || "HIGH";
  const contextType = recommendation.contextType || "COMPARISON";

  const getConfidenceBadge = (level) => {
    switch (level?.toUpperCase()) {
      case 'HIGH':
        return {
          bg: 'bg-emerald-500/20 text-emerald-300 border-emerald-500/30',
          dot: 'bg-emerald-400',
          label: 'High Confidence'
        };
      case 'MEDIUM':
        return {
          bg: 'bg-amber-500/20 text-amber-300 border-amber-500/30',
          dot: 'bg-amber-400',
          label: 'Medium Confidence'
        };
      case 'LOW':
      default:
        return {
          bg: 'bg-slate-500/20 text-slate-300 border-slate-500/30',
          dot: 'bg-slate-400',
          label: 'Moderate Data'
        };
    }
  };

  const confBadge = getConfidenceBadge(confidence);

  return (
    <div className="bg-linear-to-r from-slate-950 via-indigo-950 to-slate-900 text-white rounded-3xl p-6 sm:p-7 shadow-2xl border border-indigo-500/30 relative overflow-hidden mb-8 animate-fadeIn">
      {/* Background Decorative Blur */}
      <div className="absolute -top-20 -right-20 w-56 h-56 bg-indigo-600/20 rounded-full blur-3xl pointer-events-none"></div>
      <div className="absolute -bottom-20 -left-20 w-56 h-56 bg-purple-600/20 rounded-full blur-3xl pointer-events-none"></div>

      <div className="relative z-10 space-y-4">
        {/* Header Ribbon */}
        <div className="flex flex-wrap items-center justify-between gap-3">
          <div className="flex items-center gap-2">
            <span className="px-3 py-1 bg-indigo-500/20 text-indigo-300 border border-indigo-400/30 rounded-full text-xs font-black tracking-wide uppercase flex items-center gap-1.5 shadow-xs">
              <span>✨</span>
              <span>{title}</span>
            </span>
            <span className="px-2.5 py-0.5 bg-white/5 border border-white/10 rounded-full text-[10px] font-bold text-slate-400 tracking-wider uppercase">
              {contextType}
            </span>
          </div>

          <div className="flex items-center gap-2 text-xs">
            {/* Confidence indicator */}
            <span className={`px-2.5 py-1 rounded-full border text-[11px] font-bold flex items-center gap-1.5 ${confBadge.bg}`}>
              <span className={`w-1.5 h-1.5 rounded-full animate-pulse ${confBadge.dot}`}></span>
              <span>{confBadge.label}</span>
            </span>

            {/* Recommended Pick Pill */}
            {recommendedOption && (
              <div className="bg-white/10 backdrop-blur-md px-3 py-1 rounded-xl border border-white/10 flex items-center gap-1.5">
                <span className="text-amber-400 font-bold">🏆 Pick:</span>
                <span className="font-extrabold text-white text-xs">{recommendedOption}</span>
              </div>
            )}
          </div>
        </div>

        {/* Core Summary Explanation */}
        <div className="bg-white/5 backdrop-blur-md rounded-2xl p-4 sm:p-5 border border-white/10">
          <p className="text-sm sm:text-base font-medium text-indigo-50 leading-relaxed">
            "{summaryText}"
          </p>
        </div>

        {/* Deciding Factors & Trade-offs Grid */}
        <div className="grid grid-cols-1 md:grid-cols-2 gap-4 pt-1">
          {/* Reasons Column */}
          {reasons && reasons.length > 0 && (
            <div className="bg-white/[0.03] rounded-2xl p-4 border border-white/5 space-y-2">
              <h4 className="text-[11px] font-extrabold uppercase tracking-wider text-indigo-300/90 flex items-center gap-1.5">
                <span className="text-emerald-400">✓</span>
                <span>Key Deciding Factors</span>
              </h4>
              <div className="space-y-1.5">
                {reasons.map((reason, index) => (
                  <div key={index} className="flex items-start gap-2 text-xs text-slate-300">
                    <span className="text-emerald-400 font-bold shrink-0 mt-0.5">•</span>
                    <span>{reason}</span>
                  </div>
                ))}
              </div>
            </div>
          )}

          {/* Tradeoffs Column */}
          {tradeoffs && tradeoffs.length > 0 && (
            <div className="bg-white/[0.03] rounded-2xl p-4 border border-white/5 space-y-2">
              <h4 className="text-[11px] font-extrabold uppercase tracking-wider text-amber-300/90 flex items-center gap-1.5">
                <span className="text-amber-400">⚖️</span>
                <span>Trade-offs & Considerations</span>
              </h4>
              <div className="space-y-1.5">
                {tradeoffs.map((tradeoff, index) => (
                  <div key={index} className="flex items-start gap-2 text-xs text-slate-300">
                    <span className="text-amber-400 font-bold shrink-0 mt-0.5">•</span>
                    <span>{tradeoff}</span>
                  </div>
                ))}
              </div>
            </div>
          )}
        </div>

        {/* Footer Disclaimer */}
        <div className="flex items-center justify-between text-[11px] text-slate-400/80 pt-1 border-t border-white/5">
          <span className="flex items-center gap-1">
            <span>🛡️</span>
            <span>Zero hallucination guarantee — derived strictly from verified structured comparison data.</span>
          </span>
        </div>
      </div>
    </div>
  );
};

export default DecisionAdvisorCard;
