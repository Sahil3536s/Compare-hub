import React, { useState, useEffect } from 'react';
import { useParams, Link, useNavigate } from 'react-router-dom';
import { getProductDetails, getProductPriceHistory, getProductPriceMeter } from '../services/productService';
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

// Horizon options for hero price meter
const TIME_HORIZONS = [
  { label: '2-3 Days', period: '3D' },
  { label: '1 Week', period: '7D' },
  { label: '1 Month', period: '30D' },
];

// Range options for full-width Price History section
const HISTORY_RANGES = [
  { label: '7D', value: '7D' },
  { label: '30D', value: '30D' },
  { label: '90D', value: '90D' },
  { label: '6M', value: '6M' },
  { label: '1Y', value: '1Y' },
  { label: 'ALL', value: 'ALL' },
];

export const ProductDetailsPage = () => {
  const { productId } = useParams();
  const navigate = useNavigate();
  const { isAuthenticated } = useAuth();

  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [productData, setProductData] = useState(null);

  // Gallery state
  const [selectedImage, setSelectedImage] = useState(null);
  const [galleryImages, setGalleryImages] = useState([]);

  // Price meter time-horizon state
  const [meterHorizon, setMeterHorizon] = useState('30D');
  const [priceMeter, setPriceMeter] = useState(null);
  const [meterLoading, setMeterLoading] = useState(false);

  // Price history section state
  const [historyRange, setHistoryRange] = useState('90D');
  const [historyData, setHistoryData] = useState(null);
  const [historyLoading, setHistoryLoading] = useState(false);

  // Offers sorting
  const [offerSortBy, setOfferSortBy] = useState('price_asc'); // 'price_asc' | 'rating' | 'delivery'

  // Variant selections
  const [selectedStorage, setSelectedStorage] = useState(null);
  const [selectedColor, setSelectedColor] = useState(null);

  // Price alert modal state
  const [alertOpen, setAlertOpen] = useState(false);
  const [targetPrice, setTargetPrice] = useState('');
  const [alertSubmitting, setAlertSubmitting] = useState(false);
  const [alertSuccess, setAlertSuccess] = useState(false);
  const [alertError, setAlertError] = useState(null);

  // Wishlist state
  const [saved, setSaved] = useState(false);
  const [saveLoading, setSaveLoading] = useState(false);
  const [copiedLink, setCopiedLink] = useState(false);

  // Initial load
  useEffect(() => {
    let isMounted = true;
    const loadDetails = async () => {
      setLoading(true);
      setError(null);
      try {
        const data = await getProductDetails(productId);
        if (isMounted) {
          setProductData(data);
          setPriceMeter(data?.priceMeter || null);
          setHistoryData(data?.priceHistory || null);

          // Setup gallery images
          const images = [];
          if (data.imageUrl) images.push(data.imageUrl);
          if (data.merchantOffers) {
            data.merchantOffers.forEach((o) => {
              if (o.imageUrl && !images.includes(o.imageUrl)) {
                images.push(o.imageUrl);
              }
            });
          }
          setGalleryImages(images);
          setSelectedImage(images[0] || null);

          // Setup variants
          if (data.specifications?.Storage) setSelectedStorage(data.specifications.Storage);
          if (data.specifications?.Color) setSelectedColor(data.specifications.Color);

          // Suggest alert price (5% lower than current lowest)
          const currentPrice = Number(data.currentLowestPrice || data.bestCurrentPrice);
          if (!isNaN(currentPrice) && currentPrice > 0) {
            setTargetPrice(Math.round(currentPrice * 0.95).toString());
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

  // Handle Horizon change for hero PriceMeter
  const handleHorizonChange = async (period) => {
    setMeterHorizon(period);
    setMeterLoading(true);
    try {
      const bestPrice = productData?.currentLowestPrice || productData?.bestCurrentPrice;
      const meter = await getProductPriceMeter(productId, period, bestPrice);
      setPriceMeter(meter);
    } catch (err) {
      console.warn('Failed to fetch price meter for horizon:', period, err);
    } finally {
      setMeterLoading(false);
    }
  };

  // Handle Range change for full Price History chart
  const handleHistoryRangeChange = async (period) => {
    setHistoryRange(period);
    setHistoryLoading(true);
    try {
      const history = await getProductPriceHistory(productId, period);
      setHistoryData(history);
    } catch (err) {
      console.warn('Failed to fetch price history for range:', period, err);
    } finally {
      setHistoryLoading(false);
    }
  };

  // Price alert handler
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
        merchant: productData?.cheapestMerchant || productData?.bestMerchant,
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
        savedPrice: productData?.currentLowestPrice || productData?.bestCurrentPrice,
      });
      setSaved(true);
    } catch (err) {
      console.warn('Failed to save product:', err);
    } finally {
      setSaveLoading(false);
    }
  };

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
    name,
    brand,
    category,
    rating,
    reviewCount,
    currentLowestPrice,
    bestCurrentPrice,
    cheapestMerchant,
    bestMerchant,
    merchantOffers = [],
    offers = [],
    specifications = {},
    mlPrediction,
    alternatives = [],
  } = productData;

  const rawOffers = offers.length > 0 ? offers : merchantOffers;
  const effectiveBestPrice = currentLowestPrice || bestCurrentPrice || (rawOffers[0]?.price);
  const primaryMerchant = cheapestMerchant || bestMerchant || rawOffers[0]?.merchant || 'Stores';
  const primaryDealUrl = rawOffers[0]?.productUrl || '#';

  // Genuine discount: only when genuinely supplied and originalPrice > effectiveBestPrice
  const originalPrice = rawOffers[0]?.originalPrice;
  const hasGenuineDiscount =
    originalPrice &&
    Number(originalPrice) > Number(effectiveBestPrice) &&
    Number(effectiveBestPrice) > 0;
  const genuineDiscountPct = hasGenuineDiscount
    ? Math.round(((Number(originalPrice) - Number(effectiveBestPrice)) / Number(originalPrice)) * 100)
    : 0;

  // Genuine Rating / Reviews: only when genuine data exists
  const hasGenuineRating = rating != null && Number(rating) > 0;
  const hasGenuineReviews = reviewCount != null && Number(reviewCount) > 0;

  // Sorted offers for Section 1 (Compare Prices)
  const sortedOffers = [...rawOffers].sort((a, b) => {
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

  // Price Meter Status & Calculations
  const hasSufficientHistory = Boolean(
    priceMeter && (priceMeter.hasSufficientData === true || priceMeter.status === 'CALCULATED')
  );
  const meterScore = hasSufficientHistory && priceMeter.score != null ? priceMeter.score : null;

  // Recommendation Badge info
  const getRecommendationBadge = () => {
    if (!hasSufficientHistory) {
      return {
        label: 'Not enough price history yet',
        colorClass: 'bg-slate-100 text-slate-700 border-slate-300',
        zone: 'Insufficient Data',
      };
    }
    const classification = priceMeter?.classification || 'AVERAGE_PRICE';
    switch (classification) {
      case 'EXCELLENT_DEAL':
        return {
          label: 'EXCELLENT DEAL',
          colorClass: 'bg-emerald-600 text-white border-emerald-600 shadow-xs',
          zone: 'Good buying time',
        };
      case 'GOOD_PRICE':
        return {
          label: 'GOOD PRICE',
          colorClass: 'bg-teal-600 text-white border-teal-600 shadow-xs',
          zone: 'Good buying time',
        };
      case 'AVERAGE_PRICE':
        return {
          label: 'FAIR VALUE',
          colorClass: 'bg-amber-500 text-white border-amber-500 shadow-xs',
          zone: 'Average',
        };
      case 'ABOVE_AVERAGE':
        return {
          label: 'ABOVE AVERAGE',
          colorClass: 'bg-orange-500 text-white border-orange-500 shadow-xs',
          zone: 'Poor buying time',
        };
      case 'HIGH_PRICE':
        return {
          label: 'HIGH PRICE',
          colorClass: 'bg-rose-600 text-white border-rose-600 shadow-xs',
          zone: 'Poor buying time',
        };
      default:
        return {
          label: priceMeter?.classificationLabel || 'FAIR VALUE',
          colorClass: 'bg-indigo-600 text-white border-indigo-600',
          zone: 'Average',
        };
    }
  };

  const recBadge = getRecommendationBadge();

  // Price history chart data preparation
  const pricePoints = historyData?.pricePoints || [];
  const chartLabels = pricePoints.map((p) => p.date || p.recordedAt?.substring(0, 10) || '');
  const chartValues = pricePoints.map((p) => Number(p.price));
  const avgLineValue = priceMeter?.historicalAverage ? Number(priceMeter.historicalAverage) : null;

  const chartData = {
    labels: chartLabels,
    datasets: [
      {
        label: 'Historical Price',
        data: chartValues,
        borderColor: '#4f46e5',
        backgroundColor: 'rgba(79, 70, 229, 0.08)',
        fill: true,
        tension: 0.2,
        pointRadius: chartValues.length > 25 ? 2 : 4,
        pointHoverRadius: 6,
        pointBackgroundColor: '#4f46e5',
        pointBorderColor: '#ffffff',
        pointBorderWidth: 2,
      },
      ...(avgLineValue && chartValues.length > 1
        ? [
            {
              label: `Average Price (₹${avgLineValue.toLocaleString('en-IN')})`,
              data: Array(chartLabels.length).fill(avgLineValue),
              borderColor: '#94a3b8',
              borderDash: [5, 5],
              borderWidth: 1.5,
              fill: false,
              pointRadius: 0,
              pointHoverRadius: 0,
            },
          ]
        : []),
    ],
  };

  const chartOptions = {
    responsive: true,
    maintainAspectRatio: false,
    plugins: {
      legend: {
        display: Boolean(avgLineValue && chartValues.length > 1),
        position: 'top',
        align: 'end',
        labels: { boxWidth: 14, font: { size: 11, weight: 'bold' } },
      },
      tooltip: {
        backgroundColor: '#0f172a',
        padding: 12,
        titleFont: { size: 12, weight: 'bold' },
        bodyFont: { size: 13 },
        callbacks: {
          label: (context) => {
            if (context.datasetIndex === 1) {
              return ` Historical Average: ₹${avgLineValue.toLocaleString('en-IN')}`;
            }
            const pt = pricePoints[context.dataIndex];
            const merchantStr = pt?.merchant ? ` on ${pt.merchant}` : '';
            return ` Price: ₹${Number(context.raw).toLocaleString('en-IN')}${merchantStr}`;
          },
        },
      },
    },
    scales: {
      x: {
        grid: { display: false },
        ticks: { font: { size: 11 }, maxTicksLimit: 10 },
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

  return (
    <div className="bg-slate-50 min-h-screen pb-24 text-slate-800">
      {/* Breadcrumb Navigation */}
      <nav className="bg-white border-b border-slate-200">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-3">
          <ol className="flex items-center gap-2 text-xs sm:text-sm text-slate-500 overflow-x-auto whitespace-nowrap">
            <li>
              <Link to="/" className="hover:text-indigo-600 transition">Home</Link>
            </li>
            <li>/</li>
            <li>
              <Link to="/shopping" className="hover:text-indigo-600 transition">Shopping</Link>
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
            <li className="font-semibold text-slate-800 truncate max-w-sm">{name}</li>
          </ol>
        </div>
      </nav>

      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 pt-6 space-y-10">
        {/* ========================================================================= */}
        {/* DESKTOP FIRST VIEW: RESPONSIVE 3-COLUMN HERO SECTION                     */}
        {/* ========================================================================= */}
        <section className="bg-white rounded-3xl border border-slate-200/90 shadow-sm p-6 lg:p-8">
          <div className="grid grid-cols-1 lg:grid-cols-12 gap-8 items-start">
            
            {/* ------------------------------------------------------------------- */}
            {/* LEFT COLUMN — PRODUCT MEDIA                                         */}
            {/* ------------------------------------------------------------------- */}
            <div className="lg:col-span-4 flex flex-col items-center space-y-4">
              {/* Main Image Stage */}
              <div className="w-full bg-slate-50 border border-slate-100 rounded-2xl p-6 flex items-center justify-center min-h-[300px] max-h-[380px] aspect-square relative group overflow-hidden">
                {selectedImage ? (
                  <img
                    src={selectedImage}
                    alt={name}
                    className="max-h-[340px] w-auto object-contain transition-transform duration-300 group-hover:scale-105"
                  />
                ) : (
                  <div className="text-6xl text-slate-300">📦</div>
                )}
                <div className="absolute top-3 left-3">
                  {brand && (
                    <span className="bg-slate-900/90 text-white text-xs font-bold px-3 py-1 rounded-full shadow-2xs">
                      {brand}
                    </span>
                  )}
                </div>
              </div>

              {/* Thumbnail Gallery Underneath */}
              {galleryImages.length > 1 && (
                <div className="flex items-center gap-2.5 overflow-x-auto py-1 max-w-full no-scrollbar">
                  {galleryImages.map((img, idx) => (
                    <button
                      key={idx}
                      type="button"
                      onClick={() => setSelectedImage(img)}
                      className={`w-14 h-14 rounded-xl p-1.5 bg-slate-50 border transition-all shrink-0 cursor-pointer ${
                        selectedImage === img
                          ? 'border-indigo-600 ring-2 ring-indigo-500/20 shadow-xs'
                          : 'border-slate-200 hover:border-slate-400 opacity-70 hover:opacity-100'
                      }`}
                      aria-label={`Select product image ${idx + 1}`}
                    >
                      <img src={img} alt="" className="w-full h-full object-contain" />
                    </button>
                  ))}
                </div>
              )}
            </div>

            {/* ------------------------------------------------------------------- */}
            {/* CENTER COLUMN — PRODUCT INFORMATION                                 */}
            {/* ------------------------------------------------------------------- */}
            <div className="lg:col-span-4 flex flex-col justify-between space-y-5">
              <div>
                {/* Merchant Provider Badge */}
                <div className="flex items-center gap-2 mb-2">
                  <span className="inline-flex items-center gap-1.5 text-xs font-bold bg-indigo-50 text-indigo-700 px-3 py-1 rounded-full border border-indigo-100">
                    <span className="w-1.5 h-1.5 rounded-full bg-indigo-600" />
                    Available on {primaryMerchant}
                  </span>
                  {rawOffers.length > 1 && (
                    <span className="text-xs font-semibold text-slate-500">
                      + {rawOffers.length - 1} more {rawOffers.length === 2 ? 'store' : 'stores'}
                    </span>
                  )}
                </div>

                {/* Full Canonical Product Name */}
                <h1 className="text-xl sm:text-2xl font-black text-slate-900 tracking-tight leading-snug">
                  {name}
                </h1>

                {/* Exact Selected Variant */}
                <div className="flex flex-wrap items-center gap-2 mt-2.5">
                  {specifications?.Storage && (
                    <span className="text-xs font-bold bg-slate-100 text-slate-700 px-2.5 py-1 rounded-lg">
                      {specifications.Storage}
                    </span>
                  )}
                  {specifications?.RAM && (
                    <span className="text-xs font-bold bg-slate-100 text-slate-700 px-2.5 py-1 rounded-lg">
                      {specifications.RAM} RAM
                    </span>
                  )}
                  {specifications?.Color && (
                    <span className="text-xs font-bold bg-slate-100 text-slate-700 px-2.5 py-1 rounded-lg">
                      {specifications.Color}
                    </span>
                  )}
                </div>

                {/* Rating & Review Count (ONLY when genuine data exists) */}
                {hasGenuineRating && (
                  <div className="flex items-center gap-2 mt-3">
                    <div className="flex items-center gap-1 bg-amber-50 text-amber-900 border border-amber-200 px-2.5 py-0.5 rounded-md text-xs font-bold">
                      <span className="text-amber-500">★</span>
                      <span>{Number(rating).toFixed(1)}</span>
                    </div>
                    {hasGenuineReviews && (
                      <span className="text-xs text-slate-500">
                        ({reviewCount.toLocaleString()} verified ratings)
                      </span>
                    )}
                  </div>
                )}
              </div>

              {/* Price Banner */}
              <div className="p-4 bg-slate-50 rounded-2xl border border-slate-200/80 space-y-1">
                <span className="text-xs font-semibold text-slate-500 uppercase tracking-wider block">
                  Best Available Price
                </span>
                <div className="flex items-baseline gap-3">
                  <span className="text-3xl font-black text-slate-900 tracking-tight">
                    {effectiveBestPrice != null
                      ? `₹${Number(effectiveBestPrice).toLocaleString('en-IN')}`
                      : 'Check Stores'}
                  </span>
                  {hasGenuineDiscount && (
                    <>
                      <span className="text-sm text-slate-400 line-through">
                        ₹{Number(originalPrice).toLocaleString('en-IN')}
                      </span>
                      <span className="text-xs font-bold text-emerald-700 bg-emerald-50 border border-emerald-200 px-2 py-0.5 rounded-md">
                        {genuineDiscountPct}% OFF
                      </span>
                    </>
                  )}
                </div>
              </div>

              {/* Variant Selectors (when genuine options exist) */}
              {(specifications?.Storage || specifications?.Color) && (
                <div className="space-y-3 pt-1">
                  {specifications.Storage && (
                    <div>
                      <span className="text-xs font-bold text-slate-500 block mb-1.5">Storage Variant</span>
                      <div className="flex items-center gap-2">
                        <button
                          type="button"
                          className="px-3.5 py-1.5 rounded-xl text-xs font-bold bg-indigo-600 text-white shadow-2xs cursor-default"
                        >
                          {specifications.Storage}
                        </button>
                      </div>
                    </div>
                  )}
                  {specifications.Color && (
                    <div>
                      <span className="text-xs font-bold text-slate-500 block mb-1.5">Color</span>
                      <div className="flex items-center gap-2">
                        <button
                          type="button"
                          className="px-3.5 py-1.5 rounded-xl text-xs font-bold bg-slate-100 border border-slate-300 text-slate-800 cursor-default"
                        >
                          {specifications.Color}
                        </button>
                      </div>
                    </div>
                  )}
                </div>
              )}

              {/* Primary Action Buttons */}
              <div className="space-y-2.5 pt-2">
                <a
                  href={primaryDealUrl}
                  target="_blank"
                  rel="noopener noreferrer"
                  className="w-full py-3.5 px-5 bg-indigo-600 hover:bg-indigo-700 text-white text-sm font-black rounded-xl transition duration-200 flex items-center justify-center gap-2 shadow-sm cursor-pointer"
                >
                  <span>Buy on {primaryMerchant}</span>
                  <span>→</span>
                </a>

                <div className="flex items-center gap-2">
                  <button
                    type="button"
                    onClick={() => setAlertOpen(true)}
                    className="flex-1 py-2 px-3 bg-slate-100 hover:bg-slate-200 text-slate-800 text-xs font-bold rounded-xl transition flex items-center justify-center gap-1.5"
                  >
                    <span>🔔 Price Alert</span>
                  </button>
                  <button
                    type="button"
                    onClick={handleSaveProduct}
                    disabled={saveLoading}
                    className={`py-2 px-3 rounded-xl text-xs font-bold transition flex items-center justify-center gap-1.5 ${
                      saved
                        ? 'bg-rose-50 text-rose-600 border border-rose-200'
                        : 'bg-slate-100 hover:bg-slate-200 text-slate-700'
                    }`}
                  >
                    <span>{saved ? '❤️ Saved' : '🤍 Wishlist'}</span>
                  </button>
                  <button
                    type="button"
                    onClick={handleCopyLink}
                    className="py-2 px-3 bg-slate-100 hover:bg-slate-200 text-slate-700 text-xs font-bold rounded-xl transition"
                    title="Share Link"
                  >
                    {copiedLink ? '✓' : '🔗'}
                  </button>
                </div>
              </div>
            </div>

            {/* ------------------------------------------------------------------- */}
            {/* RIGHT COLUMN — PRICE INTELLIGENCE (PRICEMETER PANEL)                */}
            {/* Visible WITHOUT scrolling on desktop                                */}
            {/* ------------------------------------------------------------------- */}
            <div className="lg:col-span-4 bg-gradient-to-br from-slate-900 to-indigo-950 text-white rounded-3xl p-6 shadow-md border border-slate-800 flex flex-col justify-between space-y-5">
              <div>
                {/* Header */}
                <div className="flex items-center justify-between gap-2 border-b border-indigo-900/80 pb-3">
                  <div className="flex items-center gap-2">
                    <span className="text-xl">⚖️</span>
                    <div>
                      <h2 className="text-base font-black text-white leading-tight">
                        Should you buy now?
                      </h2>
                      <span className="text-[11px] text-indigo-300">Statistical Price Meter</span>
                    </div>
                  </div>
                </div>

                {/* Time-horizon Controls */}
                <div className="mt-3.5 flex items-center justify-between gap-1 bg-white/10 p-1 rounded-xl">
                  {TIME_HORIZONS.map((h) => (
                    <button
                      key={h.label}
                      type="button"
                      onClick={() => handleHorizonChange(h.period)}
                      className={`flex-1 py-1 px-1.5 rounded-lg text-[11px] font-bold transition cursor-pointer text-center whitespace-nowrap ${
                        meterHorizon === h.period
                          ? 'bg-white text-indigo-950 shadow-xs'
                          : 'text-indigo-200 hover:text-white'
                      }`}
                    >
                      {h.label}
                    </button>
                  ))}
                </div>

                {/* PRICE METER GAUGE: 0 to 100 */}
                <div className="mt-5 space-y-2">
                  <div className="flex items-center justify-between text-[11px] font-bold text-indigo-300">
                    <span>Buying Opportunity Gauge</span>
                    {hasSufficientHistory && meterScore != null ? (
                      <span className="text-white font-mono bg-indigo-500/30 px-2 py-0.5 rounded-md border border-indigo-400/30 font-bold">
                        Score: {meterScore} / 100
                      </span>
                    ) : (
                      <span className="text-slate-400 font-normal italic text-[10px]">
                        Insufficient data
                      </span>
                    )}
                  </div>

                  {/* Visual Track Divided into 3 Zones */}
                  <div className="relative pt-6 pb-1">
                    {/* Score Pointer / Indicator */}
                    {hasSufficientHistory && meterScore != null && (
                      <div
                        className="absolute top-0 transform -translate-x-1/2 transition-all duration-500 flex flex-col items-center z-10"
                        style={{ left: `${Math.max(4, Math.min(96, meterScore))}%` }}
                      >
                        <span className="text-[10px] font-mono font-black text-indigo-950 bg-white px-1.5 py-0.5 rounded shadow-xs mb-0.5">
                          {meterScore}
                        </span>
                        <div className="w-0 h-0 border-l-[4px] border-l-transparent border-r-[4px] border-r-transparent border-t-[5px] border-t-white" />
                      </div>
                    )}

                    <div className="h-4 rounded-full overflow-hidden flex bg-slate-800/90 shadow-inner border border-white/10 p-0.5">
                      <div className="w-[35%] bg-gradient-to-r from-rose-500 to-rose-400 rounded-l-full" title="Poor buying time (0-35)" />
                      <div className="w-[34%] bg-gradient-to-r from-amber-400 to-amber-300" title="Average (36-69)" />
                      <div className="w-[31%] bg-gradient-to-r from-emerald-400 to-emerald-500 rounded-r-full" title="Good buying time (70-100)" />
                    </div>

                    {/* Scale Markings: 0 ----------------- 100 */}
                    <div className="flex justify-between items-center text-[10px] font-mono text-slate-400 mt-1 px-1">
                      <span>0</span>
                      <span className="text-[9px] tracking-widest text-slate-500">──────────</span>
                      <span>50</span>
                      <span className="text-[9px] tracking-widest text-slate-500">──────────</span>
                      <span>100</span>
                    </div>

                    {/* Zone Labels */}
                    <div className="flex justify-between items-center text-[10px] font-bold text-slate-300 mt-1 px-0.5">
                      <span className="text-rose-400">Poor buying time</span>
                      <span className="text-amber-300">Average</span>
                      <span className="text-emerald-400">Good buying time</span>
                    </div>
                  </div>
                </div>

                {/* Recommendation Classification */}
                <div className="mt-4 pt-4 border-t border-indigo-900/60 space-y-1.5">
                  <span className="text-[10px] font-bold text-indigo-300 uppercase tracking-wider block">
                    Our Recommendation
                  </span>

                  <div className="flex items-center gap-2">
                    <span className={`text-xs font-black px-3 py-1 rounded-lg border ${recBadge.colorClass}`}>
                      {recBadge.label}
                    </span>
                  </div>

                  {/* Short Explanation Statement */}
                  <p className="text-xs text-slate-300 leading-relaxed pt-1">
                    {hasSufficientHistory
                      ? (priceMeter.summaryText || priceMeter.advice)
                      : 'Not enough price history yet'}
                  </p>
                </div>
              </div>

              {/* REAL PRICE STATS (All strictly from ProductPriceHistory database records) */}
              <div className="pt-3 border-t border-indigo-900/60">
                <span className="text-[10px] font-bold text-indigo-300 uppercase tracking-wider block mb-2">
                  Historical Price Stats
                </span>

                <div className="grid grid-cols-2 gap-2 text-xs">
                  <div className="bg-white/5 border border-white/10 rounded-xl p-2.5">
                    <span className="text-[10px] text-slate-400 block font-medium">Highest Price</span>
                    <span className="font-mono font-bold text-white text-sm">
                      {hasSufficientHistory && (priceMeter.historicalMaximum != null || priceMeter.historicalHighest != null)
                        ? `₹${Number(priceMeter.historicalMaximum ?? priceMeter.historicalHighest).toLocaleString('en-IN')}`
                        : '—'}
                    </span>
                  </div>

                  <div className="bg-white/5 border border-white/10 rounded-xl p-2.5">
                    <span className="text-[10px] text-slate-400 block font-medium">Average Price</span>
                    <span className="font-mono font-bold text-white text-sm">
                      {hasSufficientHistory && priceMeter.historicalAverage != null
                        ? `₹${Number(priceMeter.historicalAverage).toLocaleString('en-IN')}`
                        : '—'}
                    </span>
                  </div>

                  <div className="bg-white/5 border border-white/10 rounded-xl p-2.5">
                    <span className="text-[10px] text-slate-400 block font-medium">Lowest Price</span>
                    <span className="font-mono font-bold text-emerald-400 text-sm">
                      {hasSufficientHistory && (priceMeter.historicalMinimum != null || priceMeter.historicalLowest != null)
                        ? `₹${Number(priceMeter.historicalMinimum ?? priceMeter.historicalLowest).toLocaleString('en-IN')}`
                        : '—'}
                    </span>
                  </div>

                  <div className="bg-white/5 border border-white/10 rounded-xl p-2.5">
                    <span className="text-[10px] text-slate-400 block font-medium">Current Price</span>
                    <span className="font-mono font-bold text-indigo-200 text-sm">
                      {effectiveBestPrice != null
                        ? `₹${Number(effectiveBestPrice).toLocaleString('en-IN')}`
                        : '—'}
                    </span>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </section>

        {/* ========================================================================= */}
        {/* AFTER HERO SECTION IN EXACT ORDER:                                       */}
        {/* 1. COMPARE PRICES                                                        */}
        {/* 2. PRICE HISTORY                                                         */}
        {/* 3. PRICE INSIGHTS / ML PREDICTION                                        */}
        {/* 4. PRODUCT SPECIFICATIONS                                                */}
        {/* 5. ALTERNATIVE PRODUCTS                                                  */}
        {/* ========================================================================= */}

        {/* ------------------------------------------------------------------------- */}
        {/* 1. COMPARE PRICES                                                         */}
        {/* ------------------------------------------------------------------------- */}
        <section id="compare-prices" className="bg-white rounded-3xl border border-slate-200 shadow-sm p-6 sm:p-8 space-y-6">
          <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4 border-b border-slate-100 pb-4">
            <div>
              <div className="flex items-center gap-2">
                <span className="text-xl">🏪</span>
                <h2 className="text-xl font-black text-slate-900">Compare Merchant Prices</h2>
                <span className="text-xs font-bold bg-indigo-50 text-indigo-700 px-2.5 py-0.5 rounded-full border border-indigo-100">
                  {rawOffers.length} Store Offers
                </span>
              </div>
              <p className="text-xs sm:text-sm text-slate-500 mt-1">
                Verified multi-store pricing across authentic merchants for this exact variant.
              </p>
            </div>

            {/* Sorting Filter */}
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

          <div className="space-y-3">
            {sortedOffers.map((offer, idx) => {
              const isCheapestOffer = idx === 0 && offerSortBy === 'price_asc';
              const offerPrice = Number(offer.effectivePrice || offer.price || 0);
              const isDemo = !offer.live || offer.dataSource === 'DEMO';

              return (
                <div
                  key={offer.id || `${offer.merchant}-${idx}`}
                  className={`p-4 sm:p-5 rounded-2xl border transition-all flex flex-col md:flex-row md:items-center justify-between gap-4 ${
                    isCheapestOffer
                      ? 'bg-indigo-50/40 border-indigo-200 shadow-2xs ring-1 ring-indigo-500/10'
                      : 'bg-white border-slate-200 hover:border-slate-300'
                  }`}
                >
                  {/* Store Name & Logo */}
                  <div className="flex items-center gap-4 min-w-[200px]">
                    <div className="w-12 h-12 rounded-xl bg-slate-100 border border-slate-200/80 flex items-center justify-center text-lg font-black text-slate-700 uppercase shrink-0">
                      {offer.merchant?.substring(0, 2) || 'ST'}
                    </div>
                    <div>
                      <div className="flex items-center gap-2">
                        <span className="font-extrabold text-slate-900 text-base">{offer.merchant}</span>
                        {isCheapestOffer && (
                          <span className="bg-emerald-600 text-white text-[10px] font-black uppercase px-2 py-0.5 rounded-full shadow-2xs">
                            Cheapest Deal
                          </span>
                        )}
                        {isDemo && <DemoBadge />}
                      </div>

                      {offer.rating != null && (
                        <div className="flex items-center gap-1 text-xs text-amber-600 font-semibold mt-0.5">
                          <span>★</span>
                          <span>{Number(offer.rating).toFixed(1)}</span>
                          <span className="text-slate-400 font-normal">rating</span>
                        </div>
                      )}
                    </div>
                  </div>

                  {/* Delivery & Stock */}
                  <div className="flex flex-wrap items-center gap-4 text-xs text-slate-600">
                    <div className="flex items-center gap-1.5">
                      <span className="text-base">🚚</span>
                      <span className="font-medium">{offer.deliveryText || offer.delivery || 'Standard Delivery'}</span>
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

                  {/* Price & Primary Link */}
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

        {/* ------------------------------------------------------------------------- */}
        {/* 2. PRICE HISTORY                                                          */}
        {/* Full-width interactive line chart with real database observations         */}
        {/* ------------------------------------------------------------------------- */}
        <section id="price-history" className="bg-white rounded-3xl border border-slate-200 shadow-sm p-6 sm:p-8 space-y-6">
          <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4 border-b border-slate-100 pb-4">
            <div>
              <div className="flex items-center gap-2">
                <span className="text-xl">📈</span>
                <h2 className="text-xl font-black text-slate-900">Price History</h2>
              </div>
              <p className="text-xs sm:text-sm text-slate-500 mt-1">
                Authentic longitudinal price recordings from real store offerings.
              </p>
            </div>

            {/* Range Selectors: 7D, 30D, 90D, 6M, 1Y, ALL */}
            <div className="flex items-center gap-1 bg-slate-100 p-1 rounded-xl self-start sm:self-auto overflow-x-auto">
              {HISTORY_RANGES.map((r) => (
                <button
                  key={r.value}
                  type="button"
                  onClick={() => handleHistoryRangeChange(r.value)}
                  className={`px-3 py-1.5 rounded-lg text-xs font-bold transition cursor-pointer whitespace-nowrap ${
                    historyRange === r.value
                      ? 'bg-white text-indigo-600 shadow-xs'
                      : 'text-slate-600 hover:text-slate-900'
                  }`}
                >
                  {r.label}
                </button>
              ))}
            </div>
          </div>

          {/* Interactive Chart Canvas */}
          <div className="h-[300px] sm:h-[360px] w-full relative">
            {historyLoading ? (
              <div className="h-full flex items-center justify-center">
                <Spinner />
              </div>
            ) : pricePoints.length === 0 ? (
              <div className="h-full flex flex-col items-center justify-center text-center p-6 bg-slate-50 rounded-2xl border border-dashed border-slate-200">
                <span className="text-3xl mb-2">⏱️</span>
                <h3 className="font-bold text-slate-700 text-sm">Price tracking has just started for this product.</h3>
                <p className="text-xs text-slate-500 max-w-sm mt-1">
                  We are now polling stores regularly. Verified trend curves will be plotted as price points accumulate.
                </p>
              </div>
            ) : (
              <Line data={chartData} options={chartOptions} />
            )}
          </div>
        </section>

        {/* ------------------------------------------------------------------------- */}
        {/* 3. PRICE INSIGHTS / ML PREDICTION                                         */}
        {/* Strictly separated from statistical price meter                            */}
        {/* ------------------------------------------------------------------------- */}
        <section id="price-insights" className="bg-white rounded-3xl border border-slate-200 shadow-sm p-6 sm:p-8 space-y-6">
          <div className="border-b border-slate-100 pb-4">
            <div className="flex items-center gap-2">
              <span className="text-xl">🧠</span>
              <h2 className="text-xl font-black text-slate-900">Price Insights & ML Prediction</h2>
            </div>
            <p className="text-xs sm:text-sm text-slate-500 mt-1">
              Forward-looking machine learning forecast distinctly separated from empirical historical stats.
            </p>
          </div>

          <div className="bg-gradient-to-br from-slate-900 to-indigo-950 text-white rounded-2xl p-6 sm:p-7 shadow-sm">
            {mlPrediction && mlPrediction.confidenceScore && Number(mlPrediction.confidenceScore) > 0.5 ? (
              <div className="space-y-4">
                <div className="flex items-center justify-between">
                  <span className="text-xs font-bold text-indigo-300 uppercase tracking-wider">
                    Predictive Horizon: 7–14 Days
                  </span>
                  <span className="text-xs font-bold bg-indigo-500/30 text-indigo-200 border border-indigo-400/30 px-2.5 py-0.5 rounded-full">
                    Confidence: {Math.round(Number(mlPrediction.confidenceScore) * 100)}%
                  </span>
                </div>

                <div className="text-2xl font-black tracking-tight text-white">
                  Expected Trend: {mlPrediction.predictedTrend || 'STABLE'}
                </div>

                <p className="text-xs sm:text-sm text-slate-300 leading-relaxed max-w-2xl">
                  {mlPrediction.summary ||
                    `Model anticipates price to stay around ₹${Number(mlPrediction.predictedPrice || effectiveBestPrice).toLocaleString('en-IN')}.`}
                </p>
              </div>
            ) : (
              <div className="space-y-2">
                <div className="flex items-center gap-2 text-indigo-300 text-xs font-bold uppercase tracking-wider">
                  <span>ML Forecasting Engine</span>
                </div>
                <h3 className="text-lg font-bold text-white">Longitudinal Price Data Accumulating</h3>
                <p className="text-xs sm:text-sm text-slate-300 leading-relaxed max-w-2xl">
                  ML price prediction models require multi-week price point density before generating confident forecasts. No fabricated predictions are shown.
                </p>
                <div className="pt-2 text-xs text-indigo-300">
                  <span>✓ CompareHub authentic prediction guarantee</span>
                </div>
              </div>
            )}
          </div>
        </section>

        {/* ------------------------------------------------------------------------- */}
        {/* 4. PRODUCT SPECIFICATIONS                                                 */}
        {/* ------------------------------------------------------------------------- */}
        {specifications && Object.keys(specifications).length > 0 && (
          <section id="specifications" className="bg-white rounded-3xl border border-slate-200 shadow-sm p-6 sm:p-8 space-y-6">
            <div className="border-b border-slate-100 pb-4">
              <div className="flex items-center gap-2">
                <span className="text-xl">⚙️</span>
                <h2 className="text-xl font-black text-slate-900">Product Specifications</h2>
              </div>
              <p className="text-xs sm:text-sm text-slate-500 mt-1">
                Verified attributes and hardware specifications.
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

        {/* ------------------------------------------------------------------------- */}
        {/* 5. ALTERNATIVE PRODUCTS                                                   */}
        {/* ------------------------------------------------------------------------- */}
        {alternatives && alternatives.length > 0 && (
          <section id="alternatives" className="bg-white rounded-3xl border border-slate-200 shadow-sm p-6 sm:p-8 space-y-6">
            <div className="border-b border-slate-100 pb-4">
              <div className="flex items-center gap-2">
                <span className="text-xl">🔄</span>
                <h2 className="text-xl font-black text-slate-900">Alternative Products</h2>
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

      {/* Inline Price Drop Alert Modal */}
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
                <p className="text-xs text-slate-500">Instant notification when prices fall below target.</p>
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
                    Current lowest price: ₹{Number(effectiveBestPrice || 0).toLocaleString('en-IN')}
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
