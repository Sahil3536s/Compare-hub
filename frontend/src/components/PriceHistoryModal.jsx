import React, { useState, useEffect } from 'react';
import { getProductPriceHistory } from '../services/productService';
import { getProductPrediction } from '../services/predictionService';
import PurchaseTimingCard from './PurchaseTimingCard';

// ─── ML Prediction Section ──────────────────────────────────────────────────

const RecommendationBadge = ({ recommendation }) => {
  const config = {
    BUY_NOW: {
      bg: 'bg-emerald-500',
      text: 'text-white',
      label: '✅ BUY NOW',
      border: 'border-emerald-600',
    },
    WAIT: {
      bg: 'bg-amber-500',
      text: 'text-white',
      label: '⏳ WAIT',
      border: 'border-amber-600',
    },
    HOLD: {
      bg: 'bg-slate-500',
      text: 'text-white',
      label: '🔄 HOLD',
      border: 'border-slate-600',
    },
  };
  const c = config[recommendation] || config.HOLD;
  return (
    <span
      className={`inline-flex items-center px-3 py-1 rounded-full text-xs font-black tracking-wide border ${c.bg} ${c.text} ${c.border}`}
    >
      {c.label}
    </span>
  );
};

const DealQualityBadge = ({ dealQuality }) => {
  const config = {
    GOOD_DEAL: { bg: 'bg-emerald-100', text: 'text-emerald-800', label: '🏷️ Good Deal' },
    NORMAL_PRICE: { bg: 'bg-slate-100', text: 'text-slate-700', label: '📊 Normal Price' },
    EXPENSIVE: { bg: 'bg-rose-100', text: 'text-rose-800', label: '⚠️ Expensive' },
  };
  const c = config[dealQuality] || config.NORMAL_PRICE;
  return (
    <span className={`inline-flex items-center px-2.5 py-1 rounded-lg text-xs font-bold ${c.bg} ${c.text}`}>
      {c.label}
    </span>
  );
};

const ConfidenceDot = ({ label }) => {
  const config = {
    High: 'bg-emerald-500',
    Medium: 'bg-amber-500',
    Low: 'bg-rose-500',
  };
  return (
    <span className="flex items-center gap-1.5 text-xs text-slate-400">
      <span className={`inline-block w-2 h-2 rounded-full ${config[label] || 'bg-slate-400'}`} />
      {label} Confidence
    </span>
  );
};

const MLPredictionSection = ({ productId }) => {
  const [prediction, setPrediction] = useState(null);
  const [loading, setLoading] = useState(true);
  const [fetchError, setFetchError] = useState(false);

  useEffect(() => {
    if (!productId) return;
    setLoading(true);
    setFetchError(false);
    getProductPrediction(productId)
      .then((data) => setPrediction(data))
      .catch(() => setFetchError(true))
      .finally(() => setLoading(false));
  }, [productId]);

  // ── Loading ──
  if (loading) {
    return (
      <div className="border-t border-slate-100 pt-5 mt-2 space-y-3">
        <div className="flex items-center gap-2">
          <span className="text-[10px] font-bold text-violet-600 uppercase tracking-wider bg-violet-50 px-2.5 py-1 rounded-md">
            🤖 ML Price Prediction
          </span>
        </div>
        <div className="flex items-center gap-3 text-xs text-slate-400">
          <div className="animate-spin w-4 h-4 border-2 border-violet-500 border-t-transparent rounded-full" />
          Analysing price patterns…
        </div>
      </div>
    );
  }

  // ── Network / fetch error ──
  if (fetchError) {
    return (
      <div className="border-t border-slate-100 pt-5 mt-2">
        <div className="flex items-center gap-2 mb-3">
          <span className="text-[10px] font-bold text-violet-600 uppercase tracking-wider bg-violet-50 px-2.5 py-1 rounded-md">
            🤖 ML Price Prediction
          </span>
        </div>
        <div className="bg-rose-50 border border-rose-200 text-rose-700 text-xs rounded-xl p-3">
          Could not reach the prediction service. Please try again later.
        </div>
      </div>
    );
  }

  const status = prediction?.status;

  // ── Insufficient data ──
  if (status === 'INSUFFICIENT_DATA') {
    return (
      <div className="border-t border-slate-100 pt-5 mt-2">
        <div className="flex items-center gap-2 mb-3">
          <span className="text-[10px] font-bold text-violet-600 uppercase tracking-wider bg-violet-50 px-2.5 py-1 rounded-md">
            🤖 ML Price Prediction
          </span>
        </div>
        <div className="bg-slate-50 border border-slate-200 p-4 rounded-2xl text-center">
          <p className="text-2xl mb-2">📈</p>
          <p className="text-sm font-bold text-slate-700">Not enough price history yet</p>
          <p className="text-xs text-slate-400 mt-1">
            {prediction?.message ||
              'More price observations are needed before a reliable ML prediction can be generated.'}
          </p>
        </div>
      </div>
    );
  }

  // ── ML service down ──
  if (status === 'ML_UNAVAILABLE' || status === 'MODEL_NOT_LOADED') {
    return (
      <div className="border-t border-slate-100 pt-5 mt-2">
        <div className="flex items-center gap-2 mb-3">
          <span className="text-[10px] font-bold text-violet-600 uppercase tracking-wider bg-violet-50 px-2.5 py-1 rounded-md">
            🤖 ML Price Prediction
          </span>
        </div>
        <div className="bg-amber-50 border border-amber-200 p-3.5 rounded-2xl flex items-start gap-3">
          <span className="text-lg shrink-0">⚙️</span>
          <div>
            <p className="text-xs font-bold text-amber-900">Prediction temporarily unavailable</p>
            <p className="text-[11px] text-amber-700 mt-0.5">
              {prediction?.message ||
                'The ML service is temporarily unavailable. All other comparison features continue to work normally.'}
            </p>
          </div>
        </div>
      </div>
    );
  }

  // ── Success ──
  if (status !== 'SUCCESS') return null;

  const fmt = (n) => (n != null ? Number(n).toLocaleString('en-IN') : '—');
  const fmtPct = (n) => (n != null ? `${n > 0 ? '+' : ''}${Number(n).toFixed(2)}%` : '—');

  const changeIsPositive = prediction.predictedChangePercent > 0;
  const changeIsNegative = prediction.predictedChangePercent < 0;
  const changeColor = changeIsPositive
    ? 'text-rose-600'
    : changeIsNegative
    ? 'text-emerald-600'
    : 'text-slate-500';
  const changeArrow = changeIsPositive ? '↑' : changeIsNegative ? '↓' : '→';

  return (
    <div className="border-t border-slate-100 pt-5 mt-2 space-y-4">
      {/* Header */}
      <div className="flex items-center justify-between flex-wrap gap-2">
        <span className="text-[10px] font-bold text-violet-600 uppercase tracking-wider bg-violet-50 px-2.5 py-1 rounded-md">
          🤖 ML Price Prediction
        </span>
        {prediction.confidenceLabel && (
          <ConfidenceDot label={prediction.confidenceLabel} />
        )}
      </div>

      {/* Main Prediction Cards */}
      <div className="grid grid-cols-2 sm:grid-cols-4 gap-2.5">
        {/* Current Price */}
        <div className="bg-slate-50 p-3 rounded-2xl border border-slate-100 text-center sm:text-left">
          <span className="text-[10px] font-bold uppercase tracking-wider text-slate-400 block">
            Current Price
          </span>
          <span className="text-sm sm:text-base font-black text-slate-900 font-mono mt-0.5 block">
            ₹{fmt(prediction.currentPrice)}
          </span>
        </div>

        {/* Estimated Price in 7 Days */}
        <div className="bg-violet-50 p-3 rounded-2xl border border-violet-100 text-center sm:text-left">
          <span className="text-[10px] font-bold uppercase tracking-wider text-violet-700 block">
            Est. Price in 7d
          </span>
          <span className="text-sm sm:text-base font-black text-violet-900 font-mono mt-0.5 block">
            ₹{fmt(prediction.predictedPrice7d)}
          </span>
        </div>

        {/* Estimated Change */}
        <div className="bg-slate-50 p-3 rounded-2xl border border-slate-100 text-center sm:text-left">
          <span className="text-[10px] font-bold uppercase tracking-wider text-slate-400 block">
            Est. Change
          </span>
          <span className={`text-sm sm:text-base font-black font-mono mt-0.5 block ${changeColor}`}>
            {changeArrow} ₹{fmt(Math.abs(prediction.predictedChange))}
          </span>
          <span className={`text-[11px] font-bold ${changeColor}`}>
            {fmtPct(prediction.predictedChangePercent)}
          </span>
        </div>

        {/* Deal Quality */}
        <div className="bg-slate-50 p-3 rounded-2xl border border-slate-100 text-center sm:text-left flex flex-col justify-center gap-1.5">
          <span className="text-[10px] font-bold uppercase tracking-wider text-slate-400 block">
            Deal Quality
          </span>
          {prediction.dealQuality && (
            <DealQualityBadge dealQuality={prediction.dealQuality} />
          )}
        </div>
      </div>

      {/* Recommendation Banner */}
      {prediction.recommendation && (
        <div
          className={`p-4 rounded-2xl border flex flex-col sm:flex-row items-start sm:items-center justify-between gap-3 ${
            prediction.recommendation === 'BUY_NOW'
              ? 'bg-emerald-50 border-emerald-200'
              : prediction.recommendation === 'WAIT'
              ? 'bg-amber-50 border-amber-200'
              : 'bg-slate-50 border-slate-200'
          }`}
        >
          <div className="space-y-1.5">
            <div className="flex items-center gap-2 flex-wrap">
              <span className="text-xs font-bold text-slate-600">Recommendation:</span>
              <RecommendationBadge recommendation={prediction.recommendation} />
            </div>
            {prediction.recommendationReason && (
              <p className="text-xs text-slate-600 leading-relaxed">
                {prediction.recommendationReason}
              </p>
            )}
          </div>
        </div>
      )}

      {/* Estimated Range */}
      {prediction.predictedPriceLow != null && prediction.predictedPriceHigh != null && (
        <div className="bg-slate-900 text-white p-4 rounded-2xl border border-slate-800 flex flex-col sm:flex-row items-start sm:items-center justify-between gap-3">
          <div className="space-y-0.5">
            <span className="text-[10px] font-bold text-violet-400 uppercase tracking-wider block">
              Estimated Price Range (7d)
            </span>
            <span className="text-sm font-mono font-bold text-white">
              ₹{fmt(prediction.predictedPriceLow)} – ₹{fmt(prediction.predictedPriceHigh)}
            </span>
          </div>
          <div className="text-right shrink-0">
            <span className="text-[10px] text-slate-400 block uppercase">Model</span>
            <span className="text-xs font-mono font-bold text-violet-300">
              {prediction.modelName || '—'}
            </span>
            {prediction.modelVersion && (
              <span className="text-[10px] text-slate-500 block">v{prediction.modelVersion}</span>
            )}
          </div>
        </div>
      )}

      {/* Disclaimer */}
      <p className="text-[10px] text-slate-400 text-center leading-relaxed">
        Based on historical price patterns. Predicted prices are estimates, not guaranteed future prices.
        Deal quality classification is statistical, not ML-based.
      </p>
    </div>
  );
};

// ─── Main Modal ─────────────────────────────────────────────────────────────

export const PriceHistoryModal = ({ isOpen, onClose, product }) => {
  const [period, setPeriod] = useState('30D'); // '7D', '30D', '90D'
  const [data, setData] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [hoveredPoint, setHoveredPoint] = useState(null);

  useEffect(() => {
    const handleKeyDown = (e) => {
      if (e.key === 'Escape' && isOpen) {
        onClose();
      }
    };
    window.addEventListener('keydown', handleKeyDown);
    return () => window.removeEventListener('keydown', handleKeyDown);
  }, [isOpen, onClose]);

  useEffect(() => {
    if (!isOpen || !product) return;

    const fetchHistory = async () => {
      setLoading(true);
      setError(null);
      try {
        const productId = product.id || Math.abs((product.productName || 'prod').hashCode() % 500) || 1;
        const res = await getProductPriceHistory(productId, period);
        setData(res);
      } catch (err) {
        console.error('Failed to load price history:', err);
        setError('Unable to load price history.');
      } finally {
        setLoading(false);
      }
    };

    fetchHistory();
  }, [isOpen, product, period]);

  if (!isOpen || !product) return null;

  // Derive numeric product ID for prediction
  const productId = product.id || Math.abs((product.productName || 'prod').hashCode() % 500) || 1;

  // Chart coordinate calculations
  const pricePoints = data?.pricePoints || [];
  const minPrice = data?.lowestPrice ? Number(data.lowestPrice) : 1000;
  const maxPrice = data?.highestPrice ? Number(data.highestPrice) : 2000;
  const priceRange = Math.max(1, maxPrice - minPrice);
  const avgPrice = data?.averagePrice ? Number(data.averagePrice) : (minPrice + maxPrice) / 2;

  const chartWidth = 560;
  const chartHeight = 180;
  const paddingX = 40;
  const paddingY = 25;

  const points = pricePoints.map((p, index) => {
    const x = paddingX + (index / Math.max(1, pricePoints.length - 1)) * (chartWidth - paddingX * 2);
    const y = chartHeight - paddingY - ((Number(p.price) - minPrice) / priceRange) * (chartHeight - paddingY * 2);
    return { ...p, x, y };
  });

  const svgPath = points.length > 0
    ? points.reduce((acc, pt, idx) => (idx === 0 ? `M ${pt.x} ${pt.y}` : `${acc} L ${pt.x} ${pt.y}`), '')
    : '';

  const svgAreaPath = points.length > 0
    ? `${svgPath} L ${points[points.length - 1].x} ${chartHeight - paddingY} L ${points[0].x} ${chartHeight - paddingY} Z`
    : '';

  const avgY = chartHeight - paddingY - ((avgPrice - minPrice) / priceRange) * (chartHeight - paddingY * 2);

  return (
    <div
      role="dialog"
      aria-modal="true"
      aria-labelledby="price-history-title"
      className="fixed inset-0 z-50 flex items-center justify-center p-3 sm:p-4 bg-slate-900/60 backdrop-blur-xs animate-fadeIn"
      onClick={(e) => {
        if (e.target === e.currentTarget) onClose();
      }}
    >
      <div className="bg-white rounded-3xl max-w-2xl w-full p-5 sm:p-8 shadow-2xl border border-slate-100 relative space-y-6 max-h-[90vh] overflow-y-auto min-w-0">
        
        {/* Close Button */}
        <button
          onClick={onClose}
          aria-label="Close price history modal"
          className="absolute top-4 right-4 sm:top-5 sm:right-5 w-8 h-8 rounded-full bg-slate-100 hover:bg-slate-200 text-slate-500 hover:text-slate-800 flex items-center justify-center transition cursor-pointer min-h-[44px] min-w-[44px]"
        >
          ✕
        </button>

        {/* Header */}
        <div className="space-y-1 pr-8">
          <span className="text-[10px] font-bold text-indigo-600 uppercase tracking-wider bg-indigo-50 px-2.5 py-1 rounded-md">
            📊 Price Trend Analysis
          </span>
          <h2 id="price-history-title" className="text-lg sm:text-2xl font-black text-slate-900 leading-tight">
            {product.productName || product.name || 'Product'}
          </h2>
          <p className="text-xs text-slate-500">
            Historical price variations tracked across verified merchant feeds.
          </p>
        </div>

        {/* Period Selector Tabs */}
        <div className="flex bg-slate-100 p-1 rounded-xl max-w-[240px] text-xs font-bold">
          {['7D', '30D', '90D'].map((p) => (
            <button
              key={p}
              onClick={() => setPeriod(p)}
              className={`flex-1 py-1.5 rounded-lg transition cursor-pointer ${
                period === p
                  ? 'bg-white text-slate-900 shadow-xs font-black'
                  : 'text-slate-500 hover:text-slate-800'
              }`}
            >
              {p}
            </button>
          ))}
        </div>

        {/* Loading / Error States */}
        {loading ? (
          <div className="h-64 flex items-center justify-center">
            <div className="animate-spin w-8 h-8 border-4 border-indigo-600 border-t-transparent rounded-full"></div>
          </div>
        ) : error ? (
          <div className="p-4 bg-rose-50 border border-rose-200 text-rose-700 text-xs rounded-xl">
            {error}
          </div>
        ) : (
          <>
            {/* Analysis Insight, Purchase Timing & Deal Quality Callout */}
            <div className="space-y-3">
              {data?.purchaseTiming && (
                <PurchaseTimingCard timing={data.purchaseTiming} />
              )}

              {data?.dealQuality && (
                <div className="bg-slate-900 text-white p-4 rounded-2xl flex flex-col sm:flex-row items-start sm:items-center justify-between gap-3 shadow-md border border-slate-800">
                  <div className="space-y-0.5">
                    <div className="flex items-center gap-2">
                      <span className="text-xs font-bold text-indigo-400">🎯 Deal Quality:</span>
                      <span className="font-mono font-bold text-xs bg-indigo-500/20 text-indigo-300 px-2 py-0.5 rounded-md border border-indigo-500/30">
                        {data.dealQuality.dealScore}/100 ({data.dealQuality.classificationLabel})
                      </span>
                    </div>
                    <p className="text-xs text-slate-300 mt-1">
                      {data.dealQuality.summary}
                    </p>
                    {data.dealQuality.disclaimer && (
                      <p className="text-[11px] text-amber-300 font-medium mt-1">
                        💡 {data.dealQuality.disclaimer}
                      </p>
                    )}
                  </div>
                  <div className="text-right shrink-0">
                    <span className="text-[10px] font-bold text-slate-400 block uppercase">Historical Trend</span>
                    <span className="text-xs font-mono font-bold text-emerald-400">
                      {data.dealQuality.realDiscountPercentVsAverage > 0
                        ? `${data.dealQuality.realDiscountPercentVsAverage}% below average`
                        : 'At normal average'}
                    </span>
                  </div>
                </div>
              )}

              {data?.analysisText && !data?.dealQuality && !data?.purchaseTiming && (
                <div className="bg-indigo-50/70 border border-indigo-200/60 p-3.5 rounded-2xl flex items-center gap-3">
                  <span className="text-xl shrink-0" aria-hidden="true">💡</span>
                  <p className="text-xs font-bold text-indigo-950">
                    {data.analysisText}
                  </p>
                </div>
              )}
            </div>

            {/* Metrics Overview Cards */}
            <div className="grid grid-cols-2 sm:grid-cols-4 gap-2.5 sm:gap-3">
              <div className="bg-slate-50 p-3 rounded-2xl border border-slate-100 text-center sm:text-left">
                <span className="text-[10px] font-bold uppercase tracking-wider text-slate-400 block">Current</span>
                <span className="text-sm sm:text-base font-black text-slate-900 font-mono mt-0.5 block">
                  ₹{Number(data?.currentPrice || 0).toLocaleString('en-IN')}
                </span>
              </div>
              <div className="bg-emerald-50/70 p-3 rounded-2xl border border-emerald-100 text-center sm:text-left">
                <span className="text-[10px] font-bold uppercase tracking-wider text-emerald-700 block">Lowest</span>
                <span className="text-sm sm:text-base font-black text-emerald-900 font-mono mt-0.5 block">
                  ₹{Number(data?.lowestPrice || 0).toLocaleString('en-IN')}
                </span>
              </div>
              <div className="bg-rose-50/70 p-3 rounded-2xl border border-rose-100 text-center sm:text-left">
                <span className="text-[10px] font-bold uppercase tracking-wider text-rose-700 block">Highest</span>
                <span className="text-sm sm:text-base font-black text-rose-900 font-mono mt-0.5 block">
                  ₹{Number(data?.highestPrice || 0).toLocaleString('en-IN')}
                </span>
              </div>
              <div className="bg-slate-50 p-3 rounded-2xl border border-slate-100 text-center sm:text-left">
                <span className="text-[10px] font-bold uppercase tracking-wider text-slate-400 block">Average</span>
                <span className="text-sm sm:text-base font-black text-slate-900 font-mono mt-0.5 block">
                  ₹{Number(data?.averagePrice || 0).toLocaleString('en-IN')}
                </span>
              </div>
            </div>

            {/* Price Chart SVG Canvas */}
            <div className="bg-slate-950 rounded-2xl p-3 sm:p-4 relative overflow-hidden border border-slate-800">
              {/* Tooltip on hover */}
              {hoveredPoint && (
                <div
                  className="absolute z-20 bg-white text-slate-900 px-2.5 py-1 rounded-lg text-[11px] font-mono font-bold shadow-xl border border-slate-200 pointer-events-none -translate-x-1/2 -translate-y-full mb-2"
                  style={{ left: `${(hoveredPoint.x / chartWidth) * 100}%`, top: `${(hoveredPoint.y / chartHeight) * 100}%` }}
                >
                  <div>₹{Number(hoveredPoint.price).toLocaleString('en-IN')}</div>
                  <div className="text-[9px] text-slate-400 font-sans">{hoveredPoint.merchant} • {hoveredPoint.date}</div>
                </div>
              )}

              <svg viewBox={`0 0 ${chartWidth} ${chartHeight}`} className="w-full h-36 sm:h-44 overflow-visible">
                <defs>
                  <linearGradient id="chartGradient" x1="0" y1="0" x2="0" y2="1">
                    <stop offset="0%" stopColor="#6366f1" stopOpacity="0.4" />
                    <stop offset="100%" stopColor="#6366f1" stopOpacity="0.0" />
                  </linearGradient>
                </defs>

                {/* Average Price Dashed Line */}
                <line
                  x1={paddingX}
                  y1={avgY}
                  x2={chartWidth - paddingX}
                  y2={avgY}
                  stroke="#475569"
                  strokeDasharray="4 4"
                  strokeWidth="1"
                />

                {/* Shaded Area */}
                {svgAreaPath && <path d={svgAreaPath} fill="url(#chartGradient)" />}

                {/* Main Trend Line */}
                <path
                  d={svgPath}
                  fill="none"
                  stroke="#818cf8"
                  strokeWidth="3"
                  strokeLinecap="round"
                  strokeLinejoin="round"
                />

                {/* Data Points */}
                {points.map((pt, i) => (
                  <circle
                    key={i}
                    cx={pt.x}
                    cy={pt.y}
                    r={hoveredPoint === pt ? 6 : 3.5}
                    fill={hoveredPoint === pt ? '#ffffff' : '#6366f1'}
                    stroke="#818cf8"
                    strokeWidth="2"
                    className="cursor-pointer transition-all duration-150"
                    onMouseEnter={() => setHoveredPoint(pt)}
                    onMouseLeave={() => setHoveredPoint(null)}
                  />
                ))}
              </svg>

              {/* Chart Date Range Labels */}
              <div className="flex justify-between text-[10px] font-bold text-slate-400 font-mono px-2 pt-1 border-t border-slate-800">
                <span>{points[0]?.date || 'Start'}</span>
                <span className="text-slate-500">Average: ₹{Number(data.averagePrice || 0).toLocaleString('en-IN')}</span>
                <span>{points[points.length - 1]?.date || 'Today'}</span>
              </div>
            </div>

            {/* ── ML Price Prediction Section ─────────────────────────────── */}
            <MLPredictionSection productId={productId} />

          </>
        )}

      </div>
    </div>
  );
};

// Helper for synthetic ID
if (!String.prototype.hashCode) {
  String.prototype.hashCode = function() {
    let hash = 0;
    for (let i = 0; i < this.length; i++) {
      hash = (hash << 5) - hash + this.charCodeAt(i);
      hash |= 0;
    }
    return hash;
  };
}

export default PriceHistoryModal;
