import React, { useState } from 'react';
import { optimizeCart } from '../services/cartService';
import LoadingSkeleton from '../components/LoadingSkeleton';
import ErrorState from '../components/ErrorState';

export const SmartCartPage = () => {
  const [items, setItems] = useState([
    { name: 'Logitech Wireless Mouse', quantity: 1 },
    { name: 'Mechanical Keyboard', quantity: 1 },
    { name: '1TB NVMe SSD', quantity: 1 },
  ]);
  const [newItemName, setNewItemName] = useState('');
  const [strategy, setStrategy] = useState('MINIMIZE_PRICE'); // 'MINIMIZE_PRICE' | 'MINIMIZE_DELIVERIES'

  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);
  const [results, setResults] = useState(null);

  const sampleTemplates = [
    {
      title: '🖥️ WFH Desk Setup',
      items: [
        { name: 'Logitech Wireless Mouse', quantity: 1 },
        { name: 'Mechanical Keyboard', quantity: 1 },
        { name: '27-inch 4K Monitor', quantity: 1 },
      ],
    },
    {
      title: '🎮 Student Rig',
      items: [
        { name: 'Gaming Mouse', quantity: 1 },
        { name: '1TB NVMe SSD', quantity: 1 },
        { name: '16GB DDR5 RAM', quantity: 2 },
      ],
    },
    {
      title: '🎧 Creator Essentials',
      items: [
        { name: 'Sony WH-1000XM5 Headphones', quantity: 1 },
        { name: 'USB-C Hub Multiport Adapter', quantity: 1 },
        { name: '2TB Portable External SSD', quantity: 1 },
      ],
    },
  ];

  const handleAddItem = (e) => {
    e.preventDefault();
    if (!newItemName.trim()) return;
    setItems([...items, { name: newItemName.trim(), quantity: 1 }]);
    setNewItemName('');
  };

  const handleRemoveItem = (index) => {
    const updated = items.filter((_, idx) => idx !== index);
    setItems(updated);
  };

  const handleQuantityChange = (index, delta) => {
    const updated = [...items];
    const newQty = (updated[index].quantity || 1) + delta;
    if (newQty > 0) {
      updated[index].quantity = newQty;
      setItems(updated);
    }
  };

  const handleOptimize = async () => {
    if (items.length === 0) {
      alert('Please add at least one item to compare.');
      return;
    }

    setLoading(true);
    setError(null);
    try {
      const data = await optimizeCart({
        items,
        strategy,
      });
      setResults(data);
    } catch (err) {
      console.error('Cart optimization failed:', err);
      setError(err.message || 'Failed to optimize shopping cart.');
    } finally {
      setLoading(false);
    }
  };

  const merchantColors = {
    Amazon: 'bg-amber-50 text-amber-900 border-amber-200',
    Flipkart: 'bg-blue-50 text-blue-900 border-blue-200',
    Croma: 'bg-teal-50 text-teal-900 border-teal-200',
  };

  return (
    <div className="space-y-6 sm:space-y-8 pb-16 min-w-0" data-testid="smart-cart-page">
      
      {/* Header Banner */}
      <div className="bg-linear-to-r from-emerald-900 via-teal-900 to-slate-900 rounded-3xl p-6 sm:p-10 text-white shadow-xl">
        <div className="max-w-3xl space-y-4">
          <div className="inline-flex items-center gap-1.5 px-3 py-1 rounded-full text-xs font-semibold bg-emerald-500/30 text-emerald-200 border border-emerald-400/30">
            <span>🛒</span>
            <span>Multi-Item Cart Optimization</span>
          </div>
          <h1 className="text-2xl sm:text-4xl lg:text-5xl font-black tracking-tight leading-tight">
            Compare & Optimize Your <span className="text-emerald-400">Entire Shopping List</span>
          </h1>
          <p className="text-xs sm:text-base text-emerald-100/80 font-normal leading-relaxed">
            Uncover the cheapest single-store basket vs smart multi-store combinations with automated shipping & fee aggregation.
          </p>
        </div>
      </div>

      {/* Cart Builder & Strategy Layout */}
      <div className="grid grid-cols-1 lg:grid-cols-3 gap-6 sm:gap-8 items-start">
        
        {/* Left: Shopping List Input */}
        <div className="lg:col-span-1 bg-white p-5 sm:p-6 rounded-3xl border border-slate-200 shadow-xs space-y-5">
          <div className="flex items-center justify-between pb-3 border-b border-slate-100">
            <h3 className="font-extrabold text-sm sm:text-base text-slate-900 flex items-center gap-2">
              <span>📋</span>
              <span>Your Shopping List ({items.length})</span>
            </h3>
            {items.length > 0 && (
              <button
                type="button"
                onClick={() => setItems([])}
                className="text-xs font-bold text-rose-600 hover:text-rose-800 cursor-pointer"
              >
                Clear
              </button>
            )}
          </div>

          {/* Add Item Form */}
          <form onSubmit={handleAddItem} className="flex gap-2">
            <input
              type="text"
              value={newItemName}
              onChange={(e) => setNewItemName(e.target.value)}
              placeholder="Add item (e.g., Wireless Mouse)..."
              className="flex-1 bg-slate-50 border border-slate-200 rounded-xl px-3 py-2 text-xs sm:text-sm text-slate-900 focus:outline-hidden focus:border-emerald-500"
            />
            <button
              type="submit"
              className="px-4 py-2 bg-emerald-600 hover:bg-emerald-700 text-white text-xs font-bold rounded-xl transition cursor-pointer"
            >
              + Add
            </button>
          </form>

          {/* Sample Templates */}
          <div className="space-y-2 pt-2 border-t border-slate-100">
            <span className="text-[11px] font-bold text-slate-400 uppercase tracking-wider block">
              Quick Templates
            </span>
            <div className="flex flex-wrap gap-1.5">
              {sampleTemplates.map((t, idx) => (
                <button
                  key={idx}
                  type="button"
                  onClick={() => setItems(t.items)}
                  className="text-xs font-semibold px-2.5 py-1.5 rounded-lg bg-slate-100 hover:bg-emerald-50 hover:text-emerald-800 text-slate-700 border border-slate-200 transition cursor-pointer"
                >
                  {t.title}
                </button>
              ))}
            </div>
          </div>

          {/* Current Items List */}
          <div className="space-y-2 pt-2">
            {items.map((item, idx) => (
              <div
                key={idx}
                className="flex items-center justify-between p-2.5 rounded-xl bg-slate-50 border border-slate-200 text-xs"
              >
                <div className="font-semibold text-slate-800 truncate max-w-[160px]">
                  {item.name}
                </div>
                <div className="flex items-center gap-2">
                  <div className="flex items-center border border-slate-300 rounded-lg bg-white overflow-hidden">
                    <button
                      type="button"
                      onClick={() => handleQuantityChange(idx, -1)}
                      className="px-2 py-0.5 text-slate-600 hover:bg-slate-100 font-bold"
                    >
                      -
                    </button>
                    <span className="px-2 py-0.5 font-mono font-bold text-slate-900 text-[11px]">
                      {item.quantity}
                    </span>
                    <button
                      type="button"
                      onClick={() => handleQuantityChange(idx, 1)}
                      className="px-2 py-0.5 text-slate-600 hover:bg-slate-100 font-bold"
                    >
                      +
                    </button>
                  </div>
                  <button
                    type="button"
                    onClick={() => handleRemoveItem(idx)}
                    className="text-slate-400 hover:text-rose-600 font-bold px-1"
                    title="Remove item"
                  >
                    ×
                  </button>
                </div>
              </div>
            ))}
          </div>

          {/* Optimization Strategy Switcher */}
          <div className="pt-4 border-t border-slate-100 space-y-2">
            <span className="text-xs font-bold uppercase tracking-wider text-slate-500 block">
              Optimization Goal
            </span>
            <div className="grid grid-cols-2 gap-2">
              <button
                type="button"
                onClick={() => setStrategy('MINIMIZE_PRICE')}
                className={`p-2.5 rounded-xl text-left border transition cursor-pointer text-xs ${
                  strategy === 'MINIMIZE_PRICE'
                    ? 'bg-emerald-600 text-white font-bold border-emerald-600 shadow-xs'
                    : 'bg-slate-50 text-slate-700 border-slate-200 hover:bg-slate-100'
                }`}
              >
                <div className="font-bold">💰 Lowest Cost</div>
                <div className="text-[10px] opacity-80 mt-0.5">Allow split orders</div>
              </button>

              <button
                type="button"
                onClick={() => setStrategy('MINIMIZE_DELIVERIES')}
                className={`p-2.5 rounded-xl text-left border transition cursor-pointer text-xs ${
                  strategy === 'MINIMIZE_DELIVERIES'
                    ? 'bg-emerald-600 text-white font-bold border-emerald-600 shadow-xs'
                    : 'bg-slate-50 text-slate-700 border-slate-200 hover:bg-slate-100'
                }`}
              >
                <div className="font-bold">📦 Fewest Deliveries</div>
                <div className="text-[10px] opacity-80 mt-0.5">Prefer single store</div>
              </button>
            </div>
          </div>

          {/* Optimize Button */}
          <button
            type="button"
            onClick={handleOptimize}
            disabled={loading || items.length === 0}
            className="w-full py-3.5 bg-emerald-600 hover:bg-emerald-700 disabled:opacity-50 text-white text-sm font-black rounded-2xl transition shadow-md cursor-pointer flex items-center justify-center gap-2"
          >
            <span>⚡ Compare & Optimize Cart</span>
          </button>
        </div>

        {/* Right: Results Comparison Dashboard */}
        <div className="lg:col-span-2 space-y-6 min-w-0">
          
          {loading ? (
            <LoadingSkeleton type="cart" count={2} />
          ) : error ? (
            <ErrorState
              title="Cart Optimization Error"
              message={error}
              onRetry={handleOptimize}
            />
          ) : results ? (
            <div className="space-y-6 animate-fadeIn">
              
              {/* Savings Summary Banner */}
              {Number(results.estimatedSavings) > 0 && (
                <div className="bg-linear-to-r from-amber-500 via-orange-500 to-amber-600 text-white p-5 rounded-3xl shadow-lg flex items-center justify-between gap-4">
                  <div className="space-y-1">
                    <span className="text-xs font-bold uppercase tracking-wider bg-white/20 px-2.5 py-0.5 rounded-full">
                      🎉 Smart Savings Discovered
                    </span>
                    <h3 className="text-xl sm:text-2xl font-black">
                      Save ₹{Number(results.estimatedSavings).toLocaleString('en-IN')} with Smart Split!
                    </h3>
                    <p className="text-xs text-white/90">
                      {results.explanation}
                    </p>
                  </div>
                  <div className="text-3xl sm:text-5xl font-black font-mono shrink-0">
                    ₹{Number(results.estimatedSavings).toLocaleString('en-IN')}
                  </div>
                </div>
              )}

              {/* Recommended Plan Spotlight */}
              {results.recommendedPlan && (
                <div className="bg-white rounded-3xl border-2 border-emerald-500 shadow-lg p-6 space-y-4">
                  <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-2 border-b border-slate-100 pb-3">
                    <div>
                      <span className="px-2.5 py-1 rounded-full text-[10px] font-black uppercase tracking-wider bg-emerald-100 text-emerald-800 border border-emerald-200">
                        🏆 Recommended Plan
                      </span>
                      <h3 className="text-lg font-black text-slate-900 mt-1">
                        {results.recommendedPlan.title}
                      </h3>
                      <p className="text-xs text-slate-500">
                        {results.recommendedPlan.description}
                      </p>
                    </div>

                    <div className="text-right">
                      <div className="text-2xl sm:text-3xl font-black text-emerald-600 font-mono">
                        ₹{Number(results.recommendedPlan.grandTotal).toLocaleString('en-IN')}
                      </div>
                      <div className="text-xs text-slate-400">
                        {results.recommendedPlan.totalOrders} {results.recommendedPlan.totalOrders === 1 ? 'Order' : 'Orders'} Total
                      </div>
                    </div>
                  </div>

                  {/* Merchant Orders Breakdown */}
                  <div className="space-y-3">
                    <span className="text-xs font-bold uppercase tracking-wider text-slate-500 block">
                      Order Breakdown ({results.recommendedPlan.merchantOrders?.length || 0} Stores)
                    </span>

                    <div className="grid grid-cols-1 md:grid-cols-2 gap-3">
                      {results.recommendedPlan.merchantOrders?.map((order, idx) => (
                        <div
                          key={idx}
                          className="p-4 rounded-2xl bg-slate-50 border border-slate-200 space-y-3 flex flex-col justify-between"
                        >
                          <div>
                            <div className="flex items-center justify-between border-b border-slate-200 pb-2 mb-2">
                              <span className={`px-2.5 py-0.5 rounded-lg text-xs font-bold border ${
                                merchantColors[order.merchant] || 'bg-slate-100 text-slate-800'
                              }`}>
                                {order.merchant}
                              </span>
                              <span className="font-mono font-bold text-slate-900 text-sm">
                                ₹{Number(order.merchantTotal).toLocaleString('en-IN')}
                              </span>
                            </div>

                            <div className="space-y-1.5">
                              {order.items?.map((it, iIdx) => (
                                <div key={iIdx} className="flex justify-between text-xs text-slate-700">
                                  <span className="truncate max-w-[150px]">
                                    {it.quantity}x {it.matchedProductName || it.itemName}
                                  </span>
                                  <span className="font-mono font-semibold">
                                    ₹{Number(it.totalPrice).toLocaleString('en-IN')}
                                  </span>
                                </div>
                              ))}
                            </div>
                          </div>

                          <div className="pt-2 border-t border-slate-200 text-[11px] text-slate-500 flex justify-between">
                            <span>
                              Delivery: {Number(order.deliveryFee) === 0 ? <strong className="text-emerald-600">FREE</strong> : `+₹${order.deliveryFee}`}
                            </span>
                            {Number(order.platformFee) > 0 && (
                              <span>Fee: +₹{order.platformFee}</span>
                            )}
                          </div>
                        </div>
                      ))}
                    </div>
                  </div>
                </div>
              )}

              {/* Single Store Comparisons */}
              {results.singleStorePlans && results.singleStorePlans.length > 0 && (
                <div className="bg-white rounded-3xl border border-slate-200 shadow-xs p-6 space-y-4">
                  <h3 className="font-extrabold text-sm sm:text-base text-slate-900 flex items-center gap-2">
                    <span>🏪</span>
                    <span>All-in-One Single Store Comparisons</span>
                  </h3>

                  <div className="grid grid-cols-1 sm:grid-cols-3 gap-3">
                    {results.singleStorePlans.map((plan, idx) => (
                      <div
                        key={idx}
                        className={`p-4 rounded-2xl border transition text-center space-y-2 ${
                          plan.grandTotal === results.cheapestSingleStore?.grandTotal
                            ? 'bg-indigo-50/60 border-indigo-300 ring-1 ring-indigo-400'
                            : 'bg-slate-50 border-slate-200'
                        }`}
                      >
                        <div className="font-bold text-xs text-slate-800">
                          {plan.title}
                        </div>
                        <div className="text-xl font-black font-mono text-slate-900">
                          ₹{Number(plan.grandTotal).toLocaleString('en-IN')}
                        </div>
                        <div className="text-[11px] text-slate-500">
                          1 Delivery • ₹{Number(plan.totalDeliveryFees) === 0 ? 'Free Ship' : `₹${plan.totalDeliveryFees} Ship`}
                        </div>
                      </div>
                    ))}
                  </div>
                </div>
              )}

            </div>
          ) : (
            <div className="bg-white rounded-3xl border border-slate-200 p-12 text-center space-y-4 shadow-xs">
              <div className="text-5xl">🛒</div>
              <h3 className="font-extrabold text-lg text-slate-900">
                Ready to find the cheapest way to buy your cart?
              </h3>
              <p className="text-xs sm:text-sm text-slate-500 max-w-md mx-auto">
                Add your items on the left or select a quick template, then click <strong>Compare & Optimize Cart</strong>.
              </p>
            </div>
          )}

        </div>

      </div>

    </div>
  );
};

export default SmartCartPage;
