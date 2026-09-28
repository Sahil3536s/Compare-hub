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

const MLPredictionSection = ({ productId, prediction: parentPred, loading: parentLoading, fetchError: parentError }) => {
  const [internalPrediction, setInternalPrediction] = useState(null);
  const [internalLoading, setInternalLoading] = useState(true);
  const [internalFetchError, setInternalFetchError] = useState(false);

  const isControlled = parentPred !== undefined;
  const prediction = isControlled ? parentPred : internalPrediction;
  const loading = isControlled ? parentLoading : internalLoading;
  const fetchError = isControlled ? parentError : internalFetchError;

  useEffect(() => {
    if (isControlled || !productId) return;
    setInternalLoading(true);
    setInternalFetchError(false);
    getProductPrediction(productId)
      .then((data) => setInternalPrediction(data))
      .catch(() => setInternalFetchError(true))
      .finally(() => setInternalLoading(false));
  }, [productId, isControlled]);

  // ── Loading ──
  if (loading) {
    return (
      <div className="border-t border-slate-100 pt-5 mt-2 space-y-3" data-testid="ml-loading-state">
        <div className="flex items-center gap-2">
          <span className="text-sm font-black text-slate-900 tracking-tight">
            💡 SMART PRICE INSIGHT
          </span>
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
      <div className="border-t border-slate-100 pt-5 mt-2 space-y-2">
        <div className="flex items-center gap-2 mb-2">
          <span className="text-sm font-black text-slate-900 tracking-tight">
            💡 SMART PRICE INSIGHT
          </span>
          <span className="text-[10px] font-bold text-violet-600 uppercase tracking-wider bg-violet-50 px-2.5 py-1 rounded-md">
            🤖 ML Price Prediction
          </span>
        </div>
        <div className="bg-rose-50 border border-rose-200 text-rose-700 text-xs rounded-xl p-3">
          Price prediction is temporarily unavailable. Could not reach the prediction service. Please try again later.
        </div>
      </div>
    );
  }

  const status = prediction?.status;

  // ── Insufficient data ──
  if (status === 'INSUFFICIENT_DATA') {
    return (
      <div className="border-t border-slate-100 pt-5 mt-2 space-y-3">
        <div className="flex items-center gap-2 mb-1">
          <span className="text-sm font-black text-slate-900 tracking-tight">
            💡 SMART PRICE INSIGHT
          </span>
          <span className="text-[10px] font-bold text-violet-600 uppercase tracking-wider bg-violet-50 px-2.5 py-1 rounded-md">
            🤖 ML Price Prediction
          </span>
        </div>
        <div className="bg-slate-50 border border-slate-200 p-4 rounded-2xl text-center space-y-1">
          <p className="text-2xl mb-1">📈</p>
          <p className="text-sm font-bold text-slate-800">Not enough price history yet</p>
          <p className="text-xs text-slate-600 font-medium">
            More price history is required before a reliable prediction can be generated.
          </p>
          <p className="text-[11px] text-slate-400 mt-1">
            {prediction?.message ||
              'More price observations are needed before a reliable ML prediction can be generated.'}
          </p>
        </div>
      </div>
    );
  }

  // ── ML service down / unavailable / invalid response ──
  if (status === 'TEMPORARILY_UNAVAILABLE' || status === 'ML_UNAVAILABLE' || status === 'MODEL_NOT_LOADED' || status === 'INVALID_RESPONSE' || status === 'ERROR') {
    return (
      <div className="border-t border-slate-100 pt-5 mt-2 space-y-3">
        <div className="flex items-center gap-2 mb-1">
          <span className="text-sm font-black text-slate-900 tracking-tight">
            💡 SMART PRICE INSIGHT
          </span>
          <span className="text-[10px] font-bold text-violet-600 uppercase tracking-wider bg-violet-50 px-2.5 py-1 rounded-md">
            🤖 ML Price Prediction
          </span>
        </div>
        <div className="bg-amber-50 border border-amber-200 p-3.5 rounded-2xl flex items-start gap-3">
          <span className="text-lg shrink-0">⚙️</span>
          <div>
            <p className="text-xs font-bold text-amber-900">Prediction temporarily unavailable</p>
            <p className="text-xs text-amber-800 font-medium mt-0.5">
              Price prediction is temporarily unavailable.
            </p>
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

  const predPrice = prediction.predictedPrice7d != null ? prediction.predictedPrice7d : prediction.predictedPrice7Days;
  const rangeLow = prediction.predictedPriceLow != null ? prediction.predictedPriceLow : prediction.predictionRangeLow;
  const rangeHigh = prediction.predictedPriceHigh != null ? prediction.predictedPriceHigh : prediction.predictionRangeHigh;
  const modelName = prediction.modelName || prediction.model || 'RandomForestRegressor';

  return (
    <div className="border-t border-slate-100 pt-5 mt-2 space-y-4">
      {/* Header */}
      <div className="flex items-center justify-between flex-wrap gap-2">
        <div className="flex items-center gap-2">
          <span className="text-sm font-black text-slate-900 tracking-tight flex items-center gap-1.5">
            💡 SMART PRICE INSIGHT
          </span>
          <span className="text-[10px] font-bold text-violet-600 uppercase tracking-wider bg-violet-50 px-2.5 py-1 rounded-md border border-violet-200/60">
            🤖 ML Price Prediction
          </span>
        </div>
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
            Estimated Price in 7 Days
          </span>
          <span className="text-sm sm:text-base font-black text-violet-900 font-mono mt-0.5 block">
            ₹{fmt(predPrice)}
          </span>
          <span className="text-[10px] text-violet-600/70 font-semibold block">
            Est. Price in 7d
          </span>
        </div>

        {/* Expected Change */}
        <div className="bg-slate-50 p-3 rounded-2xl border border-slate-100 text-center sm:text-left">
          <span className="text-[10px] font-bold uppercase tracking-wider text-slate-400 block">
            Expected Change
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
      {rangeLow != null && rangeHigh != null && (
        <div className="bg-slate-900 text-white p-4 rounded-2xl border border-slate-800 flex flex-col sm:flex-row items-start sm:items-center justify-between gap-3">
          <div className="space-y-0.5">
            <span className="text-[10px] font-bold text-violet-400 uppercase tracking-wider block">
              Estimated Range (7d)
            </span>
            <span className="text-sm font-mono font-bold text-white">
              ₹{fmt(rangeLow)} – ₹{fmt(rangeHigh)}
            </span>
          </div>
          <div className="text-right shrink-0">
            <span className="text-[10px] text-slate-400 block uppercase">Model</span>
            <span className="text-xs font-mono font-bold text-violet-300">
              {modelName}
            </span>
            {prediction.modelVersion && (
              <span className="text-[10px] text-slate-500 block">v{prediction.modelVersion}</span>
            )}
          </div>
        </div>
      )}

      {/* Disclaimers */}
      <div className="space-y-1 text-center">
        <p className="text-xs font-semibold text-slate-600">
          Prediction is based on historical price patterns and is not guaranteed.
        </p>
        <p className="text-[10px] text-slate-400 leading-relaxed">
          Based on historical price patterns. Predicted prices are estimates, not guaranteed future prices.
          Deal quality classification is statistical, not ML-based.
        </p>
      </div>
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

  const [prediction, setPrediction] = useState(null);
  const [predictionLoading, setPredictionLoading] = useState(true);
  const [predictionError, setPredictionError] = useState(false);

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
        const prodId = product.id || Math.abs((product.productName || 'prod').hashCode() % 500) || 1;
        const res = await getProductPriceHistory(prodId, period);
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

  useEffect(() => {
    if (!isOpen || !product) return;
    const prodId = product.id || Math.abs((product.productName || 'prod').hashCode() % 500) || 1;
    setPredictionLoading(true);
    setPredictionError(false);
    getProductPrediction(prodId)
      .then((pred) => setPrediction(pred))
      .catch((err) => {
        console.error('Failed to load prediction:', err);
        setPredictionError(true);
      })
      .finally(() => setPredictionLoading(false));
  }, [isOpen, product]);

  if (!isOpen || !product) return null;

  // Derive numeric product ID for prediction
  const productId = product.id || Math.abs((product.productName || 'prod').hashCode() % 500) || 1;

  // Prediction value for chart projection
  const predVal = (prediction?.status === 'SUCCESS' && (prediction.predictedPrice7Days != null || prediction.predictedPrice7d != null || prediction.predicted_price_7d != null))
    ? Number(prediction.predictedPrice7Days ?? prediction.predictedPrice7d ?? prediction.predicted_price_7d)
    : null;
  const hasValidPred = predVal !== null && !isNaN(predVal) && predVal > 0;

  // Chart coordinate calculations
  const pricePoints = data?.pricePoints || [];
  let minPrice = data?.lowestPrice ? Number(data.lowestPrice) : 1000;
  let maxPrice = data?.highestPrice ? Number(data.highestPrice) : 2000;
  if (hasValidPred) {
    minPrice = Math.min(minPrice, predVal);
    maxPrice = Math.max(maxPrice, predVal);
  }
  const priceRange = Math.max(1, maxPrice - minPrice);
  const avgPrice = data?.averagePrice ? Number(data.averagePrice) : (minPrice + maxPrice) / 2;

  const chartWidth = 560;
  const chartHeight = 180;
  const paddingX = 40;
  const paddingY = 25;

  // If we have a future predicted point, give historical points space to leave room for the +7d forecast point
  const histEndX = hasValidPred && pricePoints.length > 0 ? chartWidth - paddingX - 60 : chartWidth - paddingX;

  const points = pricePoints.map((p, index) => {
    const x = paddingX + (index / Math.max(1, pricePoints.length - 1)) * (histEndX - paddingX);
    const y = chartHeight - paddingY - ((Number(p.price) - minPrice) / priceRange) * (chartHeight - paddingY * 2);
    return { ...p, x, y };
  });

  const lastActualPoint = points.length > 0 ? points[points.length - 1] : null;
  const predPoint = hasValidPred && lastActualPoint ? {
    x: chartWidth - paddingX,
    y: chartHeight - paddingY - ((predVal - minPrice) / priceRange) * (chartHeight - paddingY * 2),
    price: predVal,
    date: '+7d Forecast',
    merchant: `ML Forecast (${prediction.model || prediction.modelVersion || 'Random Forest'})`,
    isPredicted: true,
  } : null;

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

        {/* Loading / Error / Empty / Content States */}
        {loading ? (
          <div className="space-y-4 animate-pulse" data-testid="price-history-skeleton">
            {/* Metric summary skeleton */}
            <div className="grid grid-cols-2 sm:grid-cols-4 gap-2.5 sm:gap-3">
              {[1, 2, 3, 4].map((i) => (
                <div key={i} className="bg-slate-100 p-3 rounded-2xl space-y-2 border border-slate-200/80">
                  <div className="h-2.5 w-16 bg-slate-200 rounded"></div>
                  <div className="h-5 w-24 bg-slate-300 rounded"></div>
                </div>
              ))}
            </div>
            {/* Chart canvas skeleton */}
            <div className="bg-slate-900 rounded-2xl h-44 p-4 flex flex-col justify-between border border-slate-800">
              <div className="h-2.5 w-28 bg-slate-800 rounded"></div>
              <div className="h-20 w-full bg-slate-800/60 rounded-xl"></div>
              <div className="flex justify-between">
                <div className="h-2 w-12 bg-slate-800 rounded"></div>
                <div className="h-2 w-16 bg-slate-800 rounded"></div>
                <div className="h-2 w-12 bg-slate-800 rounded"></div>
              </div>
            </div>
          </div>
        ) : error ? (
          <div className="p-4 bg-rose-50 border border-rose-200 text-rose-700 text-xs rounded-xl">
            {error}
          </div>
        ) : (!data?.pricePoints || data.pricePoints.length === 0) ? (
          <div className="p-8 text-center bg-slate-50 rounded-2xl border border-slate-200 space-y-3" data-testid="empty-price-history">
            <div className="w-12 h-12 bg-indigo-50 text-indigo-600 rounded-2xl flex items-center justify-center mx-auto text-xl shadow-xs">
              📊
            </div>
            <div className="space-y-1">
              <h3 className="text-base font-bold text-slate-800">
                Price history is not available yet.
              </h3>
              <p className="text-xs text-slate-500 max-w-sm mx-auto">
                Historical price movement will be plotted here as live store feeds record periodic changes.
              </p>
            </div>
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

            {/* Metrics Overview Cards: Current Price, Average Price, Lowest Price, Highest Price */}
            <div className="grid grid-cols-2 sm:grid-cols-4 gap-2.5 sm:gap-3">
              <div className="bg-slate-50 p-3 rounded-2xl border border-slate-100 text-center sm:text-left">
                <span className="text-[10px] font-bold uppercase tracking-wider text-slate-400 block">Current Price</span>
                <span className="text-sm sm:text-base font-black text-slate-900 font-mono mt-0.5 block">
                  ₹{Number(data?.currentPrice || 0).toLocaleString('en-IN')}
                </span>
              </div>
              <div className="bg-emerald-50/70 p-3 rounded-2xl border border-emerald-100 text-center sm:text-left">
                <span className="text-[10px] font-bold uppercase tracking-wider text-emerald-700 block">Lowest Price</span>
                <span className="text-sm sm:text-base font-black text-emerald-900 font-mono mt-0.5 block">
                  ₹{Number(data?.lowestPrice || 0).toLocaleString('en-IN')}
                </span>
              </div>
              <div className="bg-rose-50/70 p-3 rounded-2xl border border-rose-100 text-center sm:text-left">
                <span className="text-[10px] font-bold uppercase tracking-wider text-rose-700 block">Highest Price</span>
                <span className="text-sm sm:text-base font-black text-rose-900 font-mono mt-0.5 block">
                  ₹{Number(data?.highestPrice || 0).toLocaleString('en-IN')}
                </span>
              </div>
              <div className="bg-slate-50 p-3 rounded-2xl border border-slate-100 text-center sm:text-left">
                <span className="text-[10px] font-bold uppercase tracking-wider text-slate-400 block">Average Price</span>
                <span className="text-sm sm:text-base font-black text-slate-900 font-mono mt-0.5 block">
                  ₹{Number(data?.averagePrice || 0).toLocaleString('en-IN')}
                </span>
              </div>
            </div>

            {/* Price Chart SVG Canvas */}
            <div className="bg-slate-950 rounded-2xl p-3 sm:p-4 relative overflow-hidden border border-slate-800">
              {/* Legend: ACTUAL (Historical) vs PREDICTED (7-Day ML Forecast) */}
              <div className="flex flex-wrap items-center justify-between gap-2 text-[11px] mb-2 px-1">
                <div className="flex items-center gap-4">
                  <span className="flex items-center gap-1.5 text-slate-300 font-medium">
                    <span className="inline-block w-2.5 h-2.5 rounded-full bg-indigo-500 border border-indigo-400" />
                    ACTUAL (Historical)
                  </span>
                  {hasValidPred && (
                    <span className="flex items-center gap-1.5 text-purple-300 font-medium">
                      <span className="inline-block w-2.5 h-2.5 rotate-45 bg-purple-500 border border-purple-300" />
                      PREDICTED (7-Day ML Forecast)
                    </span>
                  )}
                </div>
                {hasValidPred && (
                  <span className="text-[10px] text-purple-300 font-mono font-semibold">
                    7d Est: ₹{Number(predVal).toLocaleString('en-IN')}
                  </span>
                )}
              </div>

              {/* Tooltip on hover with Date, Price, Provider */}
              {hoveredPoint && (
                <div
                  className="absolute z-20 bg-white text-slate-900 px-3 py-1.5 rounded-xl text-[11px] font-mono font-bold shadow-xl border border-slate-200 pointer-events-none -translate-x-1/2 -translate-y-full mb-2 whitespace-nowrap"
                  style={{ left: `${(hoveredPoint.x / chartWidth) * 100}%`, top: `${(hoveredPoint.y / chartHeight) * 100}%` }}
                >
                  <div className={hoveredPoint.isPredicted ? 'text-purple-600 font-black' : 'text-indigo-600 font-black'}>
                    {hoveredPoint.isPredicted ? '🔮 PREDICTED: ' : ''}₹{Number(hoveredPoint.price).toLocaleString('en-IN')}
                  </div>
                  <div className="text-[10px] text-slate-600 font-sans font-medium flex items-center gap-1.5 mt-0.5">
                    <span>{hoveredPoint.isPredicted ? 'Type' : 'Provider'}: <strong className="text-slate-900">{hoveredPoint.merchant || 'Store'}</strong></span>
                    <span>•</span>
                    <span>Date: {hoveredPoint.date}</span>
                  </div>
                </div>
              )}

              <svg
                role="img"
                aria-label="Price history trend chart"
                viewBox={`0 0 ${chartWidth} ${chartHeight}`}
                className="w-full h-36 sm:h-44 overflow-visible"
              >
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

                {/* Main Trend Line (ACTUAL - Solid) */}
                <path
                  d={svgPath}
                  fill="none"
                  stroke="#818cf8"
                  strokeWidth="3"
                  strokeLinecap="round"
                  strokeLinejoin="round"
                />

                {/* Projected Trend Line (PREDICTED - Dashed) */}
                {predPoint && lastActualPoint && (
                  <line
                    x1={lastActualPoint.x}
                    y1={lastActualPoint.y}
                    x2={predPoint.x}
                    y2={predPoint.y}
                    stroke="#c084fc"
                    strokeWidth="2.5"
                    strokeDasharray="6 4"
                    strokeLinecap="round"
                  />
                )}

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

                {/* Predicted Point (Diamond Marker) */}
                {predPoint && (
                  <g
                    className="cursor-pointer"
                    onMouseEnter={() => setHoveredPoint(predPoint)}
                    onMouseLeave={() => setHoveredPoint(null)}
                  >
                    <polygon
                      points={`${predPoint.x},${predPoint.y - 7} ${predPoint.x + 7},${predPoint.y} ${predPoint.x},${predPoint.y + 7} ${predPoint.x - 7},${predPoint.y}`}
                      fill={hoveredPoint === predPoint ? '#ffffff' : '#a855f7'}
                      stroke="#e9d5ff"
                      strokeWidth="2"
                      className="transition-all duration-150 drop-shadow-sm"
                    />
                    <text
                      x={predPoint.x}
                      y={predPoint.y - 10}
                      textAnchor="middle"
                      fontSize="9"
                      fontWeight="bold"
                      fill="#c084fc"
                    >
                      +7d
                    </text>
                  </g>
                )}
              </svg>

              {/* Chart Date Range Labels */}
              <div className="flex justify-between text-[10px] font-bold text-slate-400 font-mono px-2 pt-1 border-t border-slate-800">
                <span>{points[0]?.date || 'Start'}</span>
                <span className="text-slate-500">Average: ₹{Number(data.averagePrice || 0).toLocaleString('en-IN')}</span>
                <span>{points[points.length - 1]?.date || 'Today'}</span>
                {predPoint && <span className="text-purple-400">+7d Forecast</span>}
              </div>
            </div>

            {/* ── ML Price Prediction Section ─────────────────────────────── */}
            <MLPredictionSection
              productId={productId}
              prediction={prediction}
              loading={predictionLoading}
              fetchError={predictionError}
            />

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
