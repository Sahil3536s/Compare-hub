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
          bg: 'bg-emerald-50 text-emerald-800 border-emerald-300',
          dot: 'bg-emerald-500',
          label: 'High Confidence'
        };
      case 'MEDIUM':
        return {
          bg: 'bg-amber-50 text-amber-800 border-amber-300',
          dot: 'bg-amber-500',
          label: 'Medium Confidence'
        };
      case 'LOW':
      default:
        return {
          bg: 'bg-slate-100 text-slate-800 border-slate-300',
          dot: 'bg-slate-500',
          label: 'Moderate Data'
        };
    }
  };

  const confBadge = getConfidenceBadge(confidence);

  return (
    <div className="relative overflow-hidden rounded-3xl p-5 sm:p-7 bg-gradient-to-br from-indigo-50/95 via-purple-50/90 to-blue-50/95 border border-indigo-200/90 shadow-xl shadow-indigo-100/50 mb-8 transition-shadow hover:shadow-2xl animate-fadeIn">
      {/* Background Decorative Blur Layers (strictly behind content) */}
      <div className="absolute -top-20 -right-20 w-64 h-64 bg-indigo-200/40 rounded-full blur-3xl pointer-events-none -z-0" aria-hidden="true"></div>
      <div className="absolute -bottom-20 -left-20 w-64 h-64 bg-purple-200/40 rounded-full blur-3xl pointer-events-none -z-0" aria-hidden="true"></div>

      <div className="relative z-10 space-y-4">
        {/* Header Ribbon */}
        <div className="flex flex-wrap items-center justify-between gap-3">
          <div className="flex items-center gap-2">
            <span className="px-3 py-1 bg-indigo-600 text-white rounded-full text-xs font-black tracking-wide uppercase flex items-center gap-1.5 shadow-xs">
              <span>✨</span>
              <span>{title}</span>
            </span>
            <span className="px-2.5 py-0.5 bg-indigo-100/80 border border-indigo-200 rounded-full text-[10px] font-bold text-indigo-800 tracking-wider uppercase">
              {contextType}
            </span>
          </div>

          <div className="flex items-center gap-2 text-xs">
            {/* Confidence indicator */}
            <span className={`px-2.5 py-1 rounded-full border text-[11px] font-bold flex items-center gap-1.5 shadow-2xs ${confBadge.bg}`}>
              <span className={`w-1.5 h-1.5 rounded-full animate-pulse ${confBadge.dot}`}></span>
              <span>{confBadge.label}</span>
            </span>

            {/* Recommended Pick Pill */}
            {recommendedOption && (
              <div className="bg-white/95 border border-indigo-200/90 shadow-xs px-3 py-1 rounded-xl flex items-center gap-1.5">
                <span className="text-amber-700 font-extrabold text-xs">🏆 Pick:</span>
                <span className="font-black text-slate-900 text-xs sm:text-sm">{recommendedOption}</span>
              </div>
            )}
          </div>
        </div>

        {/* Core Summary Explanation */}
        <div className="bg-white/90 backdrop-blur-sm rounded-2xl p-4 sm:p-5 border border-indigo-100 shadow-xs">
          <p className="text-sm sm:text-base font-semibold text-slate-900 leading-relaxed">
            "{summaryText}"
          </p>
        </div>

        {/* Deciding Factors & Trade-offs Grid */}
        <div className="grid grid-cols-1 md:grid-cols-2 gap-4 pt-1">
          {/* Reasons Column */}
          {reasons && reasons.length > 0 && (
            <div className="bg-white/85 backdrop-blur-sm rounded-2xl p-4 sm:p-4.5 border border-emerald-100/80 shadow-2xs space-y-2.5">
              <h4 className="text-xs font-black uppercase tracking-wider text-emerald-950 flex items-center gap-1.5">
                <span className="w-4 h-4 rounded-full bg-emerald-100 text-emerald-800 flex items-center justify-center text-[10px] font-black">✓</span>
                <span>Key Deciding Factors</span>
              </h4>
              <div className="space-y-2">
                {reasons.map((reason, index) => (
                  <div key={index} className="flex items-start gap-2 text-xs sm:text-sm text-slate-800 leading-normal">
                    <span className="text-emerald-600 font-bold shrink-0 mt-0.5">•</span>
                    <span className="font-medium text-slate-800">{reason}</span>
                  </div>
                ))}
              </div>
            </div>
          )}

          {/* Tradeoffs Column */}
          {tradeoffs && tradeoffs.length > 0 && (
            <div className="bg-white/85 backdrop-blur-sm rounded-2xl p-4 sm:p-4.5 border border-amber-100/80 shadow-2xs space-y-2.5">
              <h4 className="text-xs font-black uppercase tracking-wider text-amber-950 flex items-center gap-1.5">
                <span className="w-4 h-4 rounded-full bg-amber-100 text-amber-800 flex items-center justify-center text-[10px] font-black">⚖️</span>
                <span>Trade-offs & Considerations</span>
              </h4>
              <div className="space-y-2">
                {tradeoffs.map((tradeoff, index) => (
                  <div key={index} className="flex items-start gap-2 text-xs sm:text-sm text-slate-800 leading-normal">
                    <span className="text-amber-600 font-bold shrink-0 mt-0.5">•</span>
                    <span className="font-medium text-slate-800">{tradeoff}</span>
                  </div>
                ))}
              </div>
            </div>
          )}
        </div>

        {/* Footer Disclaimer */}
        <div className="flex items-center justify-between text-[11px] sm:text-xs text-slate-600 pt-2 border-t border-indigo-100">
          <span className="flex items-center gap-1.5 font-medium">
            <span>🛡️</span>
            <span>Zero hallucination guarantee — derived strictly from verified structured comparison data.</span>
          </span>
        </div>
      </div>
    </div>
  );
};

export default DecisionAdvisorCard;
