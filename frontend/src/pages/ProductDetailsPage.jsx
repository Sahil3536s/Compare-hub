import React, { useState, useEffect } from 'react';
import { useParams, Link, useNavigate } from 'react-router-dom';
import { getProductDetails, getProductPriceHistory } from '../services/productService';
import { createPriceAlert } from '../services/alertService';
import { saveProduct } from '../services/savedService';
import { useAuth } from '../context/AuthContext';
import Spinner from '../components/Spinner';
import DemoBadge from '../components/shopping/DemoBadge';

// Chart.js imports
import {
  Chart as ChartJS,
  CategoryScale,
  LinearScale,
  PointElement,
  LineElement,
  Title,
  Tooltip,
  Legend,
  Filler,
} from 'chart.js';
import { Line } from 'react-chartjs-2';

ChartJS.register(
  CategoryScale,
  LinearScale,
  PointElement,
  LineElement,
  Title,
  Tooltip,
  Legend,
  Filler
);

const PERIOD_OPTIONS = [
  { label: '7 Days', value: '7D' },
  { label: '30 Days', value: '30D' },
  { label: '90 Days', value: '90D' },
  { label: '6 Months', value: '6M' },
  { label: '1 Year', value: '1Y' },
  { label: 'All Time', value: 'ALL' },
];

export const ProductDetailsPage = () => {
  const { productId } = useParams();
  const navigate = useNavigate();
  const { isAuthenticated } = useAuth();

  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [productData, setProductData] = useState(null);

  // Price history period state
  const [historyPeriod, setHistoryPeriod] = useState('90D');
  const [historyData, setHistoryData] = useState(null);
  const [historyLoading, setHistoryLoading] = useState(false);

  // Offers sorting
  const [offerSortBy, setOfferSortBy] = useState('price_asc'); // 'price_asc' | 'rating' | 'delivery'

  // Price alert state
  const [alertOpen, setAlertOpen] = useState(false);
  const [targetPrice, setTargetPrice] = useState('');
  const [alertSubmitting, setAlertSubmitting] = useState(false);
  const [alertSuccess, setAlertSuccess] = useState(false);
  const [alertError, setAlertError] = useState(null);

  // Wishlist state
  const [saved, setSaved] = useState(false);
  const [saveLoading, setSaveLoading] = useState(false);
  const [copiedLink, setCopiedLink] = useState(false);

  // Initial load of product details
  useEffect(() => {
    let isMounted = true;
    const loadDetails = async () => {
      setLoading(true);
      setError(null);
      try {
        const data = await getProductDetails(productId);
        if (isMounted) {
          setProductData(data);
          if (data?.priceHistory) {
            setHistoryData(data.priceHistory);
          }
          if (data?.currentLowestPrice) {
            // Suggest target price as 5% below current lowest
            const currentNum = Number(data.currentLowestPrice);
            if (!isNaN(currentNum) && currentNum > 0) {
              setTargetPrice(Math.round(currentNum * 0.95).toString());
            }
          }
        }
      } catch (err) {
        if (isMounted) {
          setError(err.response?.data?.message || err.message || 'Product not found');
        }
      } finally {
        if (isMounted) setLoading(false);
      }
    };

    if (productId) {
      loadDetails();
    }

    return () => {
      isMounted = false;
    };
  }, [productId]);

  // Load history when period selector changes
  const handlePeriodChange = async (newPeriod) => {
    setHistoryPeriod(newPeriod);
    setHistoryLoading(true);
    try {
      const history = await getProductPriceHistory(productId, newPeriod);
      setHistoryData(history);
    } catch (err) {
      console.warn('Failed to fetch price history for period:', newPeriod, err);
    } finally {
      setHistoryLoading(false);
    }
  };

  // Price alert creation
  const handleCreateAlert = async (e) => {
    e.preventDefault();
    if (!targetPrice || isNaN(Number(targetPrice)) || Number(targetPrice) <= 0) {
      setAlertError('Please enter a valid target price.');
      return;
    }
    setAlertSubmitting(true);
    setAlertError(null);
    try {
      await createPriceAlert({
        productId: Number(productId),
        productName: productData?.name,
        targetPrice: Number(targetPrice),
        merchant: productData?.cheapestMerchant,
      });
      setAlertSuccess(true);
      setTimeout(() => {
        setAlertOpen(false);
        setAlertSuccess(false);
      }, 2500);
    } catch (err) {
      setAlertError(err.response?.data?.message || 'Failed to create price alert. Please log in.');
    } finally {
      setAlertSubmitting(false);
    }
  };

  // Save product toggle
  const handleSaveProduct = async () => {
    if (saved) {
      setSaved(false);
      return;
    }
    setSaveLoading(true);
    try {
      await saveProduct({
        productId: Number(productId),
        savedPrice: productData?.currentLowestPrice,
      });
      setSaved(true);
    } catch (err) {
      console.warn('Failed to save product:', err);
    } finally {
      setSaveLoading(false);
    }
  };

  // Copy share link
  const handleCopyLink = () => {
    navigator.clipboard.writeText(window.location.href);
    setCopiedLink(true);
    setTimeout(() => setCopiedLink(false), 2000);
  };

  if (loading) {
    return (
      <div className="min-h-[70vh] flex flex-col items-center justify-center p-8 space-y-4">
        <Spinner />
        <p className="text-slate-500 font-medium text-sm">Aggregating live store prices & price intelligence...</p>
      </div>
    );
  }

  if (error || !productData) {
    return (
      <div className="max-w-4xl mx-auto px-4 py-16 text-center">
        <div className="w-16 h-16 bg-rose-50 text-rose-500 rounded-full flex items-center justify-center text-2xl mx-auto mb-4">
          ⚠️
        </div>
        <h2 className="text-2xl font-bold text-slate-800 mb-2">Product Not Found</h2>
        <p className="text-slate-600 mb-6">{error || 'Unable to retrieve details for this product.'}</p>
        <Link
          to="/shopping"
          className="inline-flex items-center gap-2 bg-indigo-600 text-white font-semibold px-5 py-2.5 rounded-xl hover:bg-indigo-700 transition"
        >
          ← Return to Shopping Search
        </Link>
      </div>
    );
  }

  const {
    id,
    name,
    brand,
    category,
    imageUrl,
    rating,
    reviewCount,
    currentLowestPrice,
    cheapestMerchant,
    offers = [],
    specifications = {},
    priceMeter,
    mlPrediction,
    alternatives = [],
  } = productData;

  // Sorted offers
  const sortedOffers = [...offers].sort((a, b) => {
    if (offerSortBy === 'price_asc') {
      const priceA = Number(a.effectivePrice || a.price || 0);
      const priceB = Number(b.effectivePrice || b.price || 0);
      return priceA - priceB;
    }
    if (offerSortBy === 'rating') {
      return (Number(b.rating) || 0) - (Number(a.rating) || 0);
    }
    if (offerSortBy === 'delivery') {
      const isFastA = a.deliveryText?.toLowerCase().includes('today') || a.deliveryText?.toLowerCase().includes('tomorrow') ? 1 : 0;
      const isFastB = b.deliveryText?.toLowerCase().includes('today') || b.deliveryText?.toLowerCase().includes('tomorrow') ? 1 : 0;
      return isFastB - isFastA;
    }
    return 0;
  });

  // Price meter needle position calculation
  const getMeterNeedlePercent = () => {
    if (!priceMeter || priceMeter.status === 'INSUFFICIENT_DATA') return 50;
    const min = Number(priceMeter.historicalLowest);
    const max = Number(priceMeter.historicalHighest);
    const cur = Number(priceMeter.currentPrice);
    if (isNaN(min) || isNaN(max) || min === max) return 50;
    const clamped = Math.max(min, Math.min(max, cur));
    return Math.round(((clamped - min) / (max - min)) * 100);
  };

  // Price meter color badge
  const getMeterBadgeInfo = () => {
    const classification = priceMeter?.classification || 'INSUFFICIENT_DATA';
    switch (classification) {
      case 'EXCELLENT_DEAL':
        return { label: '🔥 Excellent Deal', bg: 'bg-emerald-100 text-emerald-800 border-emerald-300' };
      case 'GOOD_PRICE':
        return { label: '✓ Good Price', bg: 'bg-blue-100 text-blue-800 border-blue-300' };
      case 'AVERAGE_PRICE':
        return { label: '≈ Fair / Average', bg: 'bg-amber-100 text-amber-800 border-amber-300' };
      case 'ABOVE_AVERAGE':
        return { label: '↑ Above Average', bg: 'bg-orange-100 text-orange-800 border-orange-300' };
      case 'HIGH_PRICE':
        return { label: '⚠️ High Price', bg: 'bg-rose-100 text-rose-800 border-rose-300' };
      default:
        return { label: 'Collecting Data', bg: 'bg-slate-100 text-slate-700 border-slate-300' };
    }
  };

  // Chart dataset preparation
  const pricePoints = historyData?.pricePoints || [];
  const chartLabels = pricePoints.map((p) => p.date || p.recordedAt?.substring(0, 10) || '');
  const chartValues = pricePoints.map((p) => Number(p.price));

  const chartData = {
    labels: chartLabels,
    datasets: [
      {
        label: 'Historical Price (₹)',
        data: chartValues,
        borderColor: '#4f46e5',
        backgroundColor: 'rgba(79, 70, 229, 0.08)',
        fill: true,
        tension: 0.25,
        pointRadius: chartValues.length > 20 ? 1 : 4,
        pointHoverRadius: 6,
        pointBackgroundColor: '#4f46e5',
        pointBorderColor: '#ffffff',
        pointBorderWidth: 2,
      },
    ],
  };

  const chartOptions = {
    responsive: true,
    maintainAspectRatio: false,
    plugins: {
      legend: { display: false },
      tooltip: {
        backgroundColor: '#0f172a',
        padding: 12,
        titleFont: { size: 12, weight: 'bold' },
        bodyFont: { size: 13 },
        callbacks: {
          label: (context) => {
            const pt = pricePoints[context.dataIndex];
            const merchantStr = pt?.merchant ? ` (${pt.merchant})` : '';
            return ` ₹${Number(context.raw).toLocaleString('en-IN')}${merchantStr}`;
          },
        },
      },
    },
    scales: {
      x: {
        grid: { display: false },
        ticks: { font: { size: 11 }, maxTicksLimit: 8 },
      },
      y: {
        grid: { color: 'rgba(226, 232, 240, 0.6)' },
        ticks: {
          font: { size: 11 },
          callback: (value) => `₹${Number(value).toLocaleString('en-IN')}`,
        },
      },
    },
  };

  const meterBadge = getMeterBadgeInfo();

  return (
    <div className="bg-slate-50 min-h-screen pb-20">
      {/* 1. Breadcrumbs */}
      <nav className="bg-white border-b border-slate-200">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-3.5">
          <ol className="flex items-center gap-2 text-xs sm:text-sm text-slate-500 overflow-x-auto whitespace-nowrap">
            <li>
              <Link to="/" className="hover:text-indigo-600 transition">
                Home
              </Link>
            </li>
            <li>/</li>
            <li>
              <Link to="/shopping" className="hover:text-indigo-600 transition">
                Shopping
              </Link>
            </li>
            {category && (
              <>
                <li>/</li>
                <li>
                  <Link to={`/shopping?category=${encodeURIComponent(category)}`} className="hover:text-indigo-600 transition">
                    {category}
                  </Link>
                </li>
              </>
            )}
            <li>/</li>
            <li className="font-semibold text-slate-800 truncate max-w-xs">{name}</li>
          </ol>
        </div>
      </nav>

      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 pt-6 space-y-8">
        {/* 2. Product Hero Section */}
        <section className="bg-white rounded-3xl border border-slate-200 shadow-xs overflow-hidden p-6 sm:p-8">
          <div className="grid grid-cols-1 lg:grid-cols-12 gap-8 items-start">
            {/* Left: Product Media */}
            <div className="lg:col-span-5 flex flex-col items-center">
              <div className="w-full bg-slate-50 border border-slate-100 rounded-2xl p-6 flex items-center justify-center min-h-[320px] max-h-[420px] relative group overflow-hidden">
                {imageUrl ? (
                  <img
                    src={imageUrl}
                    alt={name}
                    className="max-h-[320px] w-auto object-contain group-hover:scale-105 transition-transform duration-300"
                  />
                ) : (
                  <div className="text-6xl text-slate-300">📦</div>
                )}
                <div className="absolute top-3 left-3 flex flex-col gap-1.5">
                  {brand && (
                    <span className="bg-slate-900 text-white text-xs font-bold px-3 py-1 rounded-full shadow-xs">
                      {brand}
                    </span>
                  )}
                  {category && (
                    <span className="bg-indigo-50 text-indigo-700 text-xs font-semibold px-3 py-1 rounded-full border border-indigo-100">
                      {category}
                    </span>
                  )}
                </div>
              </div>
            </div>

            {/* Right: Product Header & Price Meta */}
            <div className="lg:col-span-7 flex flex-col justify-between space-y-6">
              <div>
                <div className="flex items-center justify-between gap-4 mb-2">
                  <span className="text-xs font-bold text-indigo-600 uppercase tracking-wider">
                    Canonical Verified Product
                  </span>
                  <div className="flex items-center gap-2">
                    <button
                      onClick={handleCopyLink}
                      className="p-2 text-slate-500 hover:text-indigo-600 bg-slate-100 hover:bg-slate-200 rounded-xl text-xs font-medium transition flex items-center gap-1.5"
                      title="Share Product Link"
                    >
                      {copiedLink ? '✓ Copied' : '🔗 Share'}
                    </button>
                    <button
                      onClick={handleSaveProduct}
                      disabled={saveLoading}
                      className={`p-2 rounded-xl text-xs font-medium transition flex items-center gap-1.5 ${
                        saved
                          ? 'bg-rose-50 text-rose-600 border border-rose-200'
                          : 'bg-slate-100 hover:bg-slate-200 text-slate-700'
                      }`}
                      title={saved ? 'In Wishlist' : 'Save to Wishlist'}
                    >
                      {saved ? '❤️ Saved' : '🤍 Wishlist'}
                    </button>
                  </div>
                </div>

                <h1 className="text-2xl sm:text-3xl font-black text-slate-900 tracking-tight leading-snug">
                  {name}
                </h1>

                {/* Rating & Review counts */}
                <div className="flex items-center gap-3 mt-3">
                  {rating != null && (
                    <div className="flex items-center gap-1 bg-amber-50 text-amber-900 border border-amber-200 px-2.5 py-1 rounded-lg text-sm font-bold">
                      <span>★</span>
                      <span>{Number(rating).toFixed(1)}</span>
                    </div>
                  )}
                  {reviewCount != null && (
                    <span className="text-xs text-slate-500">
                      Based on {reviewCount.toLocaleString()} customer reviews
                    </span>
                  )}
                  <span className="text-xs text-slate-400">•</span>
                  <span className="text-xs text-emerald-700 font-medium bg-emerald-50 px-2.5 py-0.5 rounded-full border border-emerald-200">
                    {offers.length} {offers.length === 1 ? 'Store Verified' : 'Stores Compared'}
                  </span>
                </div>
              </div>

              {/* Price Callout Banner */}
              <div className="bg-gradient-to-r from-slate-900 to-indigo-950 text-white rounded-2xl p-6 shadow-md">
                <div className="flex flex-col sm:flex-row sm:items-end justify-between gap-4">
                  <div>
                    <span className="text-xs font-semibold text-indigo-300 uppercase tracking-wider block">
                      Best Available Price
                    </span>
                    <div className="flex items-baseline gap-3 mt-1">
                      <span className="text-3xl sm:text-4xl font-black tracking-tight text-white">
                        {currentLowestPrice != null
                          ? `₹${Number(currentLowestPrice).toLocaleString('en-IN')}`
                          : 'Check Stores'}
                      </span>
                      {cheapestMerchant && (
                        <span className="text-xs font-bold bg-indigo-500/30 text-indigo-200 border border-indigo-400/30 px-2.5 py-1 rounded-md">
                          Cheapest on {cheapestMerchant}
                        </span>
                      )}
                    </div>
                  </div>

                  {/* Price vs historical average comparison */}
                  {priceMeter && priceMeter.differenceFromAvg != null && (
                    <div className="text-left sm:text-right">
                      <span
                        className={`text-xs font-bold px-3 py-1.5 rounded-lg inline-block ${
                          Number(priceMeter.differenceFromAvg) <= 0
                            ? 'bg-emerald-500/20 text-emerald-300 border border-emerald-400/30'
                            : 'bg-rose-500/20 text-rose-300 border border-rose-400/30'
                        }`}
                      >
                        {Number(priceMeter.differenceFromAvg) <= 0
                          ? `₹${Math.abs(Number(priceMeter.differenceFromAvg)).toLocaleString('en-IN')} below historical average`
                          : `₹${Number(priceMeter.differenceFromAvg).toLocaleString('en-IN')} above historical average`}
                      </span>
                    </div>
                  )}
                </div>

                {/* Call to action triggers */}
                <div className="mt-6 pt-5 border-t border-indigo-900/60 flex flex-wrap items-center gap-3">
                  <button
                    onClick={() => setAlertOpen(true)}
                    className="flex-1 min-w-[200px] bg-indigo-500 hover:bg-indigo-600 text-white font-bold py-3 px-5 rounded-xl transition duration-200 flex items-center justify-center gap-2 shadow-sm cursor-pointer"
                  >
                    <span>🔔 Set Price Drop Alert</span>
                  </button>

                  {offers.length > 0 && (
                    <a
                      href="#merchant-offers"
                      className="bg-white/10 hover:bg-white/20 text-white font-semibold py-3 px-5 rounded-xl transition duration-200 flex items-center justify-center gap-2"
                    >
                      <span>View All {offers.length} Store Deals ↓</span>
                    </a>
                  )}
                </div>
              </div>
            </div>
          </div>
        </section>

        {/* 3. Price Meter & Statistical Price Intelligence */}
        <section className="bg-white rounded-3xl border border-slate-200 shadow-xs p-6 sm:p-8 space-y-6">
          <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
            <div>
              <div className="flex items-center gap-2">
                <span className="text-xl">📊</span>
                <h2 className="text-xl font-black text-slate-900">Statistical Price Meter</h2>
                <span className={`text-xs font-extrabold px-3 py-0.5 rounded-full border ${meterBadge.bg}`}>
                  {meterBadge.label}
                </span>
              </div>
              <p className="text-xs sm:text-sm text-slate-500 mt-1">
                Deterministic statistical analysis calculated against verified historical store recordings.
              </p>
            </div>

            {priceMeter && priceMeter.percentile != null && (
              <div className="text-left sm:text-right bg-slate-50 px-4 py-2 rounded-xl border border-slate-100">
                <span className="text-[11px] font-semibold text-slate-400 block uppercase">Price Percentile</span>
                <span className="text-lg font-black text-slate-800">{priceMeter.percentile}th percentile</span>
              </div>
            )}
          </div>

          {/* Visual Gauge Bar */}
          <div className="space-y-3 pt-2">
            <div className="relative pt-6 pb-2">
              {/* Needle Indicator */}
              <div
                className="absolute top-0 transform -translate-x-1/2 transition-all duration-500 flex flex-col items-center z-10"
                style={{ left: `${getMeterNeedlePercent()}%` }}
              >
                <span className="bg-slate-900 text-white text-[10px] font-bold px-2 py-0.5 rounded shadow-xs whitespace-nowrap">
                  Current: ₹{Number(currentLowestPrice || 0).toLocaleString('en-IN')}
                </span>
                <div className="w-0 h-0 border-l-[5px] border-l-transparent border-r-[5px] border-r-transparent border-t-[6px] border-t-slate-900"></div>
              </div>

              {/* 4 Multi-color Gauge Segments */}
              <div className="h-4 rounded-full overflow-hidden flex bg-slate-200 shadow-inner">
                <div className="w-1/4 bg-emerald-500" title="Excellent Deal (Lowest Range)" />
                <div className="w-1/4 bg-blue-500" title="Good Price" />
                <div className="w-1/4 bg-amber-400" title="Average Price" />
                <div className="w-1/4 bg-rose-500" title="High Price" />
              </div>

              {/* Segment Labels */}
              <div className="grid grid-cols-4 text-center mt-2 text-[11px] font-bold text-slate-500">
                <span className="text-emerald-700">Excellent Deal</span>
                <span className="text-blue-700">Good Price</span>
                <span className="text-amber-700">Average</span>
                <span className="text-rose-700">High Price</span>
              </div>
            </div>

            {/* Advice Statement */}
            {priceMeter?.advice && (
              <div className="bg-indigo-50/70 border border-indigo-100 rounded-xl p-4 flex items-start gap-3 text-xs sm:text-sm text-indigo-950">
                <span className="text-lg shrink-0">💡</span>
                <div className="leading-relaxed font-medium">{priceMeter.advice}</div>
              </div>
            )}
          </div>

          {/* Price Statistics Grid */}
          <div className="grid grid-cols-2 sm:grid-cols-4 gap-4 pt-4 border-t border-slate-100">
            <div className="bg-slate-50 p-4 rounded-2xl border border-slate-100 text-center">
              <span className="text-xs font-semibold text-slate-500 block">Lowest Recorded</span>
              <span className="text-lg font-black text-emerald-600 mt-0.5 block">
                {priceMeter?.historicalLowest
                  ? `₹${Number(priceMeter.historicalLowest).toLocaleString('en-IN')}`
                  : '—'}
              </span>
            </div>

            <div className="bg-slate-50 p-4 rounded-2xl border border-slate-100 text-center">
              <span className="text-xs font-semibold text-slate-500 block">Average Price</span>
              <span className="text-lg font-black text-slate-800 mt-0.5 block">
                {priceMeter?.historicalAverage
                  ? `₹${Number(priceMeter.historicalAverage).toLocaleString('en-IN')}`
                  : '—'}
              </span>
            </div>

            <div className="bg-slate-50 p-4 rounded-2xl border border-slate-100 text-center">
              <span className="text-xs font-semibold text-slate-500 block">Highest Recorded</span>
              <span className="text-lg font-black text-rose-600 mt-0.5 block">
                {priceMeter?.historicalHighest
                  ? `₹${Number(priceMeter.historicalHighest).toLocaleString('en-IN')}`
                  : '—'}
              </span>
            </div>

            <div className="bg-slate-50 p-4 rounded-2xl border border-slate-100 text-center">
              <span className="text-xs font-semibold text-slate-500 block">Difference vs Avg</span>
              <span
                className={`text-lg font-black mt-0.5 block ${
                  Number(priceMeter?.differencePercentage || 0) <= 0
                    ? 'text-emerald-600'
                    : 'text-rose-600'
                }`}
              >
                {priceMeter?.differencePercentage != null
                  ? `${priceMeter.differencePercentage > 0 ? '+' : ''}${priceMeter.differencePercentage}%`
                  : '—'}
              </span>
            </div>
          </div>
        </section>

        {/* 4. Price History Graph Section */}
        <section className="bg-white rounded-3xl border border-slate-200 shadow-xs p-6 sm:p-8 space-y-6">
          <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
            <div>
              <div className="flex items-center gap-2">
                <span className="text-xl">📈</span>
                <h2 className="text-xl font-black text-slate-900">Historical Price Trend</h2>
              </div>
              <p className="text-xs sm:text-sm text-slate-500 mt-1">
                Real price history recorded from merchant listings over time.
              </p>
            </div>

            {/* Range Selector Buttons */}
            <div className="flex items-center gap-1.5 bg-slate-100 p-1 rounded-xl self-start sm:self-auto overflow-x-auto">
              {PERIOD_OPTIONS.map((opt) => (
                <button
                  key={opt.value}
                  onClick={() => handlePeriodChange(opt.value)}
                  className={`px-3 py-1.5 rounded-lg text-xs font-bold transition cursor-pointer whitespace-nowrap ${
                    historyPeriod === opt.value
                      ? 'bg-white text-indigo-600 shadow-xs'
                      : 'text-slate-600 hover:text-slate-900'
                  }`}
                >
                  {opt.label}
                </button>
              ))}
            </div>
          </div>

          {/* Interactive Chart Canvas */}
          <div className="h-[280px] sm:h-[340px] w-full relative">
            {historyLoading ? (
              <div className="h-full flex items-center justify-center">
                <Spinner />
              </div>
            ) : pricePoints.length < 2 ? (
              <div className="h-full flex flex-col items-center justify-center text-center p-6 bg-slate-50 rounded-2xl border border-dashed border-slate-200">
                <span className="text-3xl mb-2">⏱️</span>
                <h3 className="font-bold text-slate-700 text-sm">Accumulating Price Points</h3>
                <p className="text-xs text-slate-500 max-w-sm mt-1">
                  We have captured {pricePoints.length} verified price record for this canonical product. As live prices are checked, full trend graphs will appear.
                </p>
              </div>
            ) : (
              <Line data={chartData} options={chartOptions} />
            )}
          </div>
        </section>

        {/* 5. Merchant Price Comparison Table */}
        <section id="merchant-offers" className="bg-white rounded-3xl border border-slate-200 shadow-xs p-6 sm:p-8 space-y-6">
          <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
            <div>
              <div className="flex items-center gap-2">
                <span className="text-xl">🏪</span>
                <h2 className="text-xl font-black text-slate-900">Compare Merchant Prices</h2>
                <span className="text-xs font-bold bg-indigo-50 text-indigo-700 px-2.5 py-0.5 rounded-full border border-indigo-100">
                  {offers.length} Store Offers
                </span>
              </div>
              <p className="text-xs sm:text-sm text-slate-500 mt-1">
                Verified live merchant offers for this exact hardware variant.
              </p>
            </div>

            {/* Sorting Dropdown */}
            <div className="flex items-center gap-2 self-start sm:self-auto">
              <label htmlFor="merchant-sort" className="text-xs font-bold text-slate-500">
                Sort by:
              </label>
              <select
                id="merchant-sort"
                value={offerSortBy}
                onChange={(e) => setOfferSortBy(e.target.value)}
                className="text-xs font-bold bg-slate-50 border border-slate-200 rounded-xl px-3 py-2 text-slate-800 focus:outline-hidden focus:ring-2 focus:ring-indigo-500"
              >
                <option value="price_asc">Cheapest Price</option>
                <option value="rating">Highest Store Rating</option>
                <option value="delivery">Fastest Delivery</option>
              </select>
            </div>
          </div>

          {/* Offers List */}
          <div className="space-y-3">
            {sortedOffers.map((offer, idx) => {
              const isCheapestOffer = idx === 0 && offerSortBy === 'price_asc';
              const offerPrice = Number(offer.effectivePrice || offer.price || 0);
              const isDemo = !offer.live || offer.dataSource === 'DEMO';

              return (
                <div
                  key={offer.id || `${offer.merchant}-${idx}`}
                  className={`p-4 sm:p-5 rounded-2xl border transition-all duration-200 flex flex-col md:flex-row md:items-center justify-between gap-4 ${
                    isCheapestOffer
                      ? 'bg-indigo-50/40 border-indigo-200 shadow-2xs ring-1 ring-indigo-500/10'
                      : 'bg-white border-slate-200 hover:border-slate-300'
                  }`}
                >
                  {/* Left: Merchant & Store info */}
                  <div className="flex items-center gap-4 min-w-[200px]">
                    <div className="w-12 h-12 rounded-xl bg-slate-100 flex items-center justify-center text-xl font-bold text-slate-700 uppercase shrink-0">
                      {offer.merchant?.substring(0, 2) || 'ST'}
                    </div>
                    <div>
                      <div className="flex items-center gap-2">
                        <span className="font-extrabold text-slate-900 text-base">{offer.merchant}</span>
                        {isCheapestOffer && (
                          <span className="bg-emerald-600 text-white text-[10px] font-black uppercase px-2 py-0.5 rounded-full shadow-2xs">
                            Best Deal
                          </span>
                        )}
                        {isDemo && <DemoBadge />}
                      </div>

                      {offer.rating != null && (
                        <div className="flex items-center gap-1 text-xs text-amber-600 font-semibold mt-0.5">
                          <span>★</span>
                          <span>{Number(offer.rating).toFixed(1)}</span>
                          <span className="text-slate-400 font-normal">store rating</span>
                        </div>
                      )}
                    </div>
                  </div>

                  {/* Middle: Delivery & Stock */}
                  <div className="flex flex-wrap items-center gap-4 text-xs text-slate-600">
                    <div className="flex items-center gap-1.5">
                      <span className="text-base">🚚</span>
                      <span className="font-medium">{offer.deliveryText || 'Standard Delivery'}</span>
                    </div>

                    <div className="flex items-center gap-1.5">
                      <span
                        className={`w-2 h-2 rounded-full ${
                          offer.inStock !== false ? 'bg-emerald-500' : 'bg-rose-500'
                        }`}
                      />
                      <span className="font-medium">
                        {offer.inStock !== false ? 'In Stock' : 'Out of Stock'}
                      </span>
                    </div>
                  </div>

                  {/* Right: Price & CTA */}
                  <div className="flex items-center justify-between md:justify-end gap-5 pt-3 md:pt-0 border-t md:border-t-0 border-slate-100">
                    <div className="text-left md:text-right">
                      <div className="text-2xl font-black text-slate-900">
                        ₹{offerPrice.toLocaleString('en-IN')}
                      </div>
                      {offer.originalPrice != null && Number(offer.originalPrice) > offerPrice && (
                        <div className="text-xs text-slate-400 line-through">
                          ₹{Number(offer.originalPrice).toLocaleString('en-IN')}
                        </div>
                      )}
                    </div>

                    <a
                      href={offer.productUrl || '#'}
                      target="_blank"
                      rel="noopener noreferrer"
                      className={`px-5 py-2.5 rounded-xl font-bold text-xs sm:text-sm transition flex items-center gap-1.5 shadow-2xs whitespace-nowrap ${
                        isCheapestOffer
                          ? 'bg-indigo-600 hover:bg-indigo-700 text-white'
                          : 'bg-slate-900 hover:bg-slate-800 text-white'
                      }`}
                    >
                      <span>View Deal</span>
                      <span>→</span>
                    </a>
                  </div>
                </div>
              );
            })}
          </div>
        </section>

        {/* 6. Buy / Wait Intelligence: Statistical vs ML Forecast */}
        <section className="bg-white rounded-3xl border border-slate-200 shadow-xs p-6 sm:p-8 space-y-6">
          <div>
            <div className="flex items-center gap-2">
              <span className="text-xl">🤖</span>
              <h2 className="text-xl font-black text-slate-900">Purchase Timing Intelligence</h2>
            </div>
            <p className="text-xs sm:text-sm text-slate-500 mt-1">
              Transparent intelligence clearly distinguishing empirical price statistics from ML forecasting.
            </p>
          </div>

          <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
            {/* Card A: Statistical Buy/Wait Assessment */}
            <div className="bg-slate-50 rounded-2xl border border-slate-200 p-6 flex flex-col justify-between">
              <div>
                <div className="flex items-center justify-between mb-3">
                  <span className="text-xs font-bold text-slate-500 uppercase tracking-wider">
                    Empirical Timing Indicator
                  </span>
                  <span className="text-xs font-semibold bg-indigo-50 text-indigo-700 px-2 py-0.5 rounded-md">
                    Deterministic
                  </span>
                </div>

                <h3 className="text-lg font-bold text-slate-900 mb-2">
                  {priceMeter?.classification === 'EXCELLENT_DEAL' || priceMeter?.classification === 'GOOD_PRICE'
                    ? '🟢 Recommendation: Buy Now'
                    : priceMeter?.classification === 'HIGH_PRICE'
                    ? '🔴 Recommendation: Wait for Price Drop'
                    : '🟡 Recommendation: Fair Market Price'}
                </h3>

                <p className="text-xs sm:text-sm text-slate-600 leading-relaxed">
                  {priceMeter?.advice ||
                    'Currently comparing prices across multiple merchants. Prices fluctuate based on store promotions and bank discounts.'}
                </p>
              </div>

              <div className="mt-6 pt-4 border-t border-slate-200/80 text-xs text-slate-500 flex items-center justify-between">
                <span>Calculated from store observations</span>
                <span className="font-bold text-slate-700">100% Verified</span>
              </div>
            </div>

            {/* Card B: ML Model Price Forecast */}
            <div className="bg-gradient-to-br from-slate-900 to-indigo-950 text-white rounded-2xl p-6 flex flex-col justify-between">
              <div>
                <div className="flex items-center justify-between mb-3">
                  <span className="text-xs font-bold text-indigo-300 uppercase tracking-wider">
                    Machine Learning Forecast
                  </span>
                  <span className="text-xs font-semibold bg-indigo-500/30 text-indigo-200 border border-indigo-400/30 px-2 py-0.5 rounded-md">
                    Predictive Model
                  </span>
                </div>

                {mlPrediction && mlPrediction.confidenceScore && Number(mlPrediction.confidenceScore) > 0.5 ? (
                  <>
                    <h3 className="text-lg font-bold text-white mb-2">
                      Trend Forecast: {mlPrediction.predictedTrend || 'Stable'}
                    </h3>
                    <p className="text-xs sm:text-sm text-slate-300 leading-relaxed mb-4">
                      {mlPrediction.summary ||
                        `Model projects price to remain near ₹${Number(mlPrediction.predictedPrice || currentLowestPrice).toLocaleString('en-IN')} over the next 7–14 days.`}
                    </p>
                    <div className="text-xs text-indigo-300 flex items-center gap-2">
                      <span>Model Confidence:</span>
                      <strong className="text-white">
                        {Math.round(Number(mlPrediction.confidenceScore) * 100)}%
                      </strong>
                    </div>
                  </>
                ) : (
                  <>
                    <h3 className="text-lg font-bold text-white mb-2">Collecting Longitudinal History</h3>
                    <p className="text-xs sm:text-sm text-slate-300 leading-relaxed">
                      Our ML price prediction model requires multi-week price point density before generating high-confidence forecasts. No fabricated predictions are shown.
                    </p>
                    <div className="mt-4 text-xs text-indigo-300 flex items-center gap-1.5">
                      <span>✓ Strictly authentic ML data policy</span>
                    </div>
                  </>
                )}
              </div>

              <div className="mt-6 pt-4 border-t border-indigo-900/60 text-xs text-indigo-300 flex items-center justify-between">
                <span>Predictive Engine</span>
                <span className="font-semibold text-white">CompareHub ML</span>
              </div>
            </div>
          </div>
        </section>

        {/* 7. Product Specifications & Attributes */}
        {specifications && Object.keys(specifications).length > 0 && (
          <section className="bg-white rounded-3xl border border-slate-200 shadow-xs p-6 sm:p-8 space-y-6">
            <div>
              <div className="flex items-center gap-2">
                <span className="text-xl">⚙️</span>
                <h2 className="text-xl font-black text-slate-900">Specifications & Attributes</h2>
              </div>
              <p className="text-xs sm:text-sm text-slate-500 mt-1">
                Verified technical specifications extracted for this canonical product.
              </p>
            </div>

            <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-3">
              {Object.entries(specifications).map(([key, val]) => (
                <div key={key} className="bg-slate-50 p-3.5 rounded-xl border border-slate-100">
                  <span className="text-[11px] font-bold text-slate-400 uppercase tracking-wider block">
                    {key}
                  </span>
                  <span className="text-sm font-bold text-slate-800 mt-0.5 block truncate">
                    {val}
                  </span>
                </div>
              ))}
            </div>
          </section>
        )}

        {/* 8. Alternative Products */}
        {alternatives && alternatives.length > 0 && (
          <section className="bg-white rounded-3xl border border-slate-200 shadow-xs p-6 sm:p-8 space-y-6">
            <div>
              <div className="flex items-center gap-2">
                <span className="text-xl">🔄</span>
                <h2 className="text-xl font-black text-slate-900">Similar & Alternative Products</h2>
              </div>
              <p className="text-xs sm:text-sm text-slate-500 mt-1">
                Explore closely related alternatives in the same category.
              </p>
            </div>

            <div className="grid grid-cols-1 sm:grid-cols-2 md:grid-cols-3 lg:grid-cols-4 gap-4">
              {alternatives.map((alt) => (
                <div
                  key={alt.id}
                  onClick={() => navigate(`/products/${alt.id}`)}
                  className="bg-slate-50 hover:bg-white rounded-2xl border border-slate-200 p-4 transition-all duration-200 hover:shadow-md cursor-pointer group flex flex-col justify-between"
                >
                  <div className="flex flex-col items-center mb-3">
                    {alt.imageUrl ? (
                      <img
                        src={alt.imageUrl}
                        alt={alt.name}
                        className="h-28 w-auto object-contain group-hover:scale-105 transition-transform"
                      />
                    ) : (
                      <div className="w-16 h-16 rounded-xl bg-indigo-50 flex items-center justify-center text-2xl text-indigo-400">
                        📦
                      </div>
                    )}
                  </div>

                  <div>
                    <span className="text-[10px] font-bold text-indigo-600 bg-indigo-50 px-2 py-0.5 rounded">
                      {alt.brand || 'Electronics'}
                    </span>
                    <h4 className="text-xs font-bold text-slate-900 group-hover:text-indigo-600 transition line-clamp-2 mt-1.5">
                      {alt.name}
                    </h4>
                  </div>

                  <div className="mt-3 pt-2 border-t border-slate-200/60 flex items-baseline justify-between">
                    <span className="text-xs text-slate-400 font-medium">Price</span>
                    <span className="text-sm font-black text-slate-900">
                      {alt.cheapestPrice ? `₹${Number(alt.cheapestPrice).toLocaleString('en-IN')}` : 'View'}
                    </span>
                  </div>
                </div>
              ))}
            </div>
          </section>
        )}
      </div>

      {/* Inline Price Alert Modal */}
      {alertOpen && (
        <div className="fixed inset-0 z-50 overflow-y-auto bg-slate-900/60 backdrop-blur-xs flex items-center justify-center p-4">
          <div className="bg-white rounded-3xl max-w-md w-full p-6 shadow-2xl border border-slate-200 relative animate-in fade-in zoom-in-95 duration-200">
            <button
              onClick={() => setAlertOpen(false)}
              className="absolute top-4 right-4 p-2 text-slate-400 hover:text-slate-600 rounded-full hover:bg-slate-100 transition"
              aria-label="Close modal"
            >
              ✕
            </button>

            <div className="flex items-center gap-3 mb-4">
              <div className="w-12 h-12 rounded-2xl bg-indigo-50 text-indigo-600 flex items-center justify-center text-2xl">
                🔔
              </div>
              <div>
                <h3 className="text-lg font-black text-slate-900">Set Price Drop Alert</h3>
                <p className="text-xs text-slate-500">We will notify you immediately when price drops.</p>
              </div>
            </div>

            {alertSuccess ? (
              <div className="bg-emerald-50 border border-emerald-200 rounded-2xl p-5 text-center text-emerald-800 space-y-2">
                <span className="text-3xl">🎉</span>
                <p className="font-bold text-sm">Alert Created Successfully!</p>
                <p className="text-xs text-emerald-700">We are monitoring this product for your target price.</p>
              </div>
            ) : (
              <form onSubmit={handleCreateAlert} className="space-y-4">
                <div>
                  <label className="text-xs font-bold text-slate-700 block mb-1">
                    Target Price (₹)
                  </label>
                  <input
                    type="number"
                    value={targetPrice}
                    onChange={(e) => setTargetPrice(e.target.value)}
                    placeholder="e.g. 64999"
                    className="w-full bg-slate-50 border border-slate-200 rounded-xl px-4 py-2.5 text-slate-900 font-bold focus:outline-hidden focus:ring-2 focus:ring-indigo-500"
                    required
                  />
                  <span className="text-[11px] text-slate-400 mt-1 block">
                    Current lowest price: ₹{Number(currentLowestPrice || 0).toLocaleString('en-IN')}
                  </span>
                </div>

                {alertError && (
                  <div className="text-xs text-rose-600 bg-rose-50 border border-rose-200 rounded-xl p-3">
                    {alertError}
                  </div>
                )}

                <div className="flex items-center gap-3 pt-2">
                  <button
                    type="button"
                    onClick={() => setAlertOpen(false)}
                    className="flex-1 py-2.5 px-4 bg-slate-100 hover:bg-slate-200 text-slate-700 font-semibold rounded-xl text-xs transition"
                  >
                    Cancel
                  </button>
                  <button
                    type="submit"
                    disabled={alertSubmitting}
                    className="flex-1 py-2.5 px-4 bg-indigo-600 hover:bg-indigo-700 text-white font-bold rounded-xl text-xs transition disabled:opacity-50"
                  >
                    {alertSubmitting ? 'Creating Alert...' : 'Activate Alert'}
                  </button>
                </div>
              </form>
            )}
          </div>
        </div>
      )}
    </div>
  );
};

export default ProductDetailsPage;
