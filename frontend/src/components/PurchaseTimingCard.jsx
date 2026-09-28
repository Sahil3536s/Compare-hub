import React from 'react';

export const PurchaseTimingCard = ({ timing }) => {
  if (!timing) return null;

  const statusConfig = {
    STRONG_BUY_PRICE: {
      color: 'bg-emerald-600 text-white',
      badgeBg: 'bg-emerald-50 text-emerald-800 border-emerald-200',
      icon: '🔥',
      label: 'Strong Buy Price',
    },
    GOOD_TIME_TO_BUY: {
      color: 'bg-teal-600 text-white',
      badgeBg: 'bg-teal-50 text-teal-800 border-teal-200',
      icon: '🟢',
      label: 'Good Time to Buy',
    },
    NEUTRAL: {
      color: 'bg-indigo-600 text-white',
      badgeBg: 'bg-indigo-50 text-indigo-800 border-indigo-200',
      icon: '⚖️',
      label: 'Fair Market Price',
    },
    CONSIDER_WAITING: {
      color: 'bg-amber-600 text-white',
      badgeBg: 'bg-amber-50 text-amber-900 border-amber-200',
      icon: '⏳',
      label: 'Consider Waiting',
    },
    INSUFFICIENT_DATA: {
      color: 'bg-slate-600 text-white',
      badgeBg: 'bg-slate-100 text-slate-700 border-slate-200',
      icon: 'ℹ️',
      label: 'Insufficient Data',
    },
  };

  const currentStatus = statusConfig[timing.status] || statusConfig.NEUTRAL;
  const buyScoreDecimal = timing.score ? (timing.score / 10).toFixed(1) : '5.0';

  return (
    <div
      className="p-4 sm:p-5 rounded-3xl bg-slate-900 text-white border border-slate-800 shadow-xl space-y-3.5"
      data-testid="purchase-timing-card"
    >
      {/* Top Header: Buy Score & Status Badge */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-3 border-b border-slate-800 pb-3">
        <div className="flex items-center gap-2.5">
          <div className="w-10 h-10 rounded-2xl bg-indigo-500/20 text-indigo-300 border border-indigo-500/30 flex items-center justify-center text-lg font-black shrink-0">
            {currentStatus.icon}
          </div>
          <div>
            <div className="flex items-center gap-2">
              <span className="text-xs font-bold text-slate-400 uppercase tracking-wider">
                Buy Decision Helper
              </span>
              <span className="text-[10px] font-mono px-2 py-0.5 rounded-full bg-slate-800 text-slate-300 border border-slate-700">
                {timing.confidence || 'Medium'} Confidence
              </span>
            </div>
            <h4 className="text-base font-black text-white mt-0.5">
              {timing.statusLabel || currentStatus.label}
            </h4>
          </div>
        </div>

        <div className="flex items-baseline gap-1.5 self-start sm:self-auto bg-slate-800/80 px-3 py-1.5 rounded-2xl border border-slate-700">
          <span className="text-xs text-slate-400 font-bold">Buy Score:</span>
          <span className="text-lg font-black font-mono text-emerald-400">
            {buyScoreDecimal}
          </span>
          <span className="text-xs text-slate-400 font-mono">/ 10</span>
        </div>
      </div>

      {/* Reasons Bulleted List */}
      {timing.reasons && timing.reasons.length > 0 && (
        <div className="space-y-1.5">
          <span className="text-[11px] font-bold text-slate-400 uppercase tracking-wider block">
            Why this decision?
          </span>
          <ul className="space-y-1 text-xs text-slate-200">
            {timing.reasons.map((reason, idx) => (
              <li key={idx} className="flex items-start gap-2">
                <span className="text-emerald-400 font-bold shrink-0 mt-0.5">✓</span>
                <span>{reason}</span>
              </li>
            ))}
          </ul>
        </div>
      )}

      {/* Non-Guarantee Disclaimer */}
      <p className="text-[10px] text-slate-400 font-normal leading-relaxed pt-2 border-t border-slate-800/80 italic">
        💡 {timing.disclaimer || 'Analysis is based purely on historical pricing trends and does not guarantee future price movements.'}
      </p>
    </div>
  );
};

export default PurchaseTimingCard;
