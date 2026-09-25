import React from 'react';
import { PRODUCT_CATEGORIES } from '../../utils/constants';

const POPULAR_BRANDS = ['all', 'Apple', 'Samsung', 'Sony', 'Google', 'OnePlus', 'Dell', 'HP', 'Lenovo'];
const RAM_OPTIONS = ['all', '4GB', '6GB', '8GB', '12GB', '16GB', '32GB'];
const STORAGE_OPTIONS = ['all', '64GB', '128GB', '256GB', '512GB', '1TB'];
const DELIVERY_OPTIONS = [
  { id: 'all', label: 'All Delivery Speeds' },
  { id: 'same_day', label: '⚡ Same Day' },
  { id: 'next_day', label: '🚚 Next Day / Tomorrow' },
  { id: 'express', label: '🚀 Express Delivery' },
  { id: 'free', label: '📦 Free Delivery' },
];
const RATING_OPTIONS = [
  { id: '', label: 'All Ratings' },
  { id: '4.5', label: '4.5★ & above' },
  { id: '4.0', label: '4.0★ & above' },
  { id: '3.5', label: '3.5★ & above' },
  { id: '3.0', label: '3.0★ & above' },
];

export const ProductFilterPanel = ({
  selectedCategory,
  setSelectedCategory,
  selectedMerchant,
  setSelectedMerchant,
  selectedBrand,
  setSelectedBrand,
  minPrice,
  setMinPrice,
  maxPrice,
  setMaxPrice,
  selectedRating,
  setSelectedRating,
  inStockOnly,
  setInStockOnly,
  selectedRam,
  setSelectedRam,
  selectedStorage,
  setSelectedStorage,
  selectedDelivery,
  setSelectedDelivery,
  onReset,
  activeFilterCount = 0,
}) => {
  return (
    <div className="space-y-6">
      {/* Header with Clear Filters */}
      <div className="flex items-center justify-between pb-3 border-b border-slate-100">
        <div className="flex items-center gap-2">
          <span className="font-extrabold text-sm text-slate-900 flex items-center gap-1.5">
            <span>🎛️</span>
            <span>Filters</span>
          </span>
          {activeFilterCount > 0 && (
            <span className="px-2 py-0.5 rounded-full text-[10px] font-bold bg-indigo-100 text-indigo-700">
              {activeFilterCount}
            </span>
          )}
        </div>
        <button
          type="button"
          onClick={onReset}
          className="text-xs font-bold text-indigo-600 hover:text-indigo-800 transition cursor-pointer min-h-[36px] flex items-center"
        >
          Clear Filters
        </button>
      </div>

      {/* Price Range Filter */}
      <div className="space-y-3">
        <div className="flex items-center justify-between">
          <label className="text-xs font-bold uppercase tracking-wider text-slate-500">
            Price Range
          </label>
          <span className="text-xs font-bold text-slate-900 font-mono">
            ₹{Number(minPrice || 0).toLocaleString('en-IN')} - ₹{Number(maxPrice).toLocaleString('en-IN')}
          </span>
        </div>
        <input
          type="range"
          min="5000"
          max="250000"
          step="5000"
          value={maxPrice}
          onChange={(e) => setMaxPrice(Number(e.target.value))}
          aria-label="Maximum price filter"
          className="w-full accent-indigo-600 cursor-pointer"
        />
        <div className="grid grid-cols-2 gap-2 pt-1">
          <div>
            <label className="text-[10px] font-semibold text-slate-400 block mb-1">Min Price (₹)</label>
            <input
              type="number"
              min="0"
              max={maxPrice}
              step="1000"
              value={minPrice}
              onChange={(e) => setMinPrice(e.target.value === '' ? '' : Number(e.target.value))}
              placeholder="0"
              className="w-full text-xs font-mono font-medium px-2.5 py-1.5 rounded-xl border border-slate-200 bg-slate-50 focus:bg-white focus:outline-hidden focus:border-indigo-500"
            />
          </div>
          <div>
            <label className="text-[10px] font-semibold text-slate-400 block mb-1">Max Price (₹)</label>
            <input
              type="number"
              min={minPrice || 0}
              max="500000"
              step="5000"
              value={maxPrice}
              onChange={(e) => setMaxPrice(Number(e.target.value))}
              placeholder="200000"
              className="w-full text-xs font-mono font-medium px-2.5 py-1.5 rounded-xl border border-slate-200 bg-slate-50 focus:bg-white focus:outline-hidden focus:border-indigo-500"
            />
          </div>
        </div>
      </div>

      {/* Brand Filter */}
      <div className="space-y-2 pt-4 border-t border-slate-100">
        <label className="text-xs font-bold uppercase tracking-wider text-slate-500 block">
          Brand
        </label>
        <div className="flex flex-wrap gap-1.5">
          {POPULAR_BRANDS.map((b) => (
            <button
              key={b}
              type="button"
              onClick={() => setSelectedBrand(b)}
              className={`text-xs font-semibold px-2.5 py-1.5 rounded-xl transition cursor-pointer ${
                selectedBrand.toLowerCase() === b.toLowerCase()
                  ? 'bg-indigo-600 text-white font-bold shadow-xs'
                  : 'bg-slate-100 hover:bg-slate-200 text-slate-700'
              }`}
            >
              {b === 'all' ? 'All Brands' : b}
            </button>
          ))}
        </div>
      </div>

      {/* Customer Rating Filter */}
      <div className="space-y-2 pt-4 border-t border-slate-100">
        <label className="text-xs font-bold uppercase tracking-wider text-slate-500 block">
          Customer Rating
        </label>
        <div className="space-y-1">
          {RATING_OPTIONS.map((r) => (
            <button
              key={r.id}
              type="button"
              onClick={() => setSelectedRating(r.id)}
              className={`w-full text-left text-xs font-semibold px-3 py-1.5 rounded-xl transition cursor-pointer flex items-center justify-between ${
                selectedRating === r.id
                  ? 'bg-indigo-50 text-indigo-700 font-bold border border-indigo-200'
                  : 'hover:bg-slate-50 text-slate-600'
              }`}
            >
              <span>{r.label}</span>
              {selectedRating === r.id && <span className="text-xs text-indigo-600">✓</span>}
            </button>
          ))}
        </div>
      </div>

      {/* Availability / In Stock Toggle */}
      <div className="pt-4 border-t border-slate-100">
        <label className="flex items-center justify-between cursor-pointer select-none py-1">
          <span className="text-xs font-bold text-slate-700">In Stock Only</span>
          <input
            type="checkbox"
            checked={inStockOnly}
            onChange={(e) => setInStockOnly(e.target.checked)}
            className="w-4 h-4 rounded text-indigo-600 focus:ring-indigo-500 cursor-pointer"
          />
        </label>
      </div>

      {/* RAM Filter */}
      <div className="space-y-2 pt-4 border-t border-slate-100">
        <label className="text-xs font-bold uppercase tracking-wider text-slate-500 block">
          RAM
        </label>
        <div className="flex flex-wrap gap-1.5">
          {RAM_OPTIONS.map((ram) => (
            <button
              key={ram}
              type="button"
              onClick={() => setSelectedRam(ram)}
              className={`text-xs font-semibold px-2.5 py-1.5 rounded-xl transition cursor-pointer ${
                selectedRam.toLowerCase() === ram.toLowerCase()
                  ? 'bg-indigo-600 text-white font-bold shadow-xs'
                  : 'bg-slate-100 hover:bg-slate-200 text-slate-700'
              }`}
            >
              {ram === 'all' ? 'All' : ram}
            </button>
          ))}
        </div>
      </div>

      {/* Storage Filter */}
      <div className="space-y-2 pt-4 border-t border-slate-100">
        <label className="text-xs font-bold uppercase tracking-wider text-slate-500 block">
          Internal Storage
        </label>
        <div className="flex flex-wrap gap-1.5">
          {STORAGE_OPTIONS.map((st) => (
            <button
              key={st}
              type="button"
              onClick={() => setSelectedStorage(st)}
              className={`text-xs font-semibold px-2.5 py-1.5 rounded-xl transition cursor-pointer ${
                selectedStorage.toLowerCase() === st.toLowerCase()
                  ? 'bg-indigo-600 text-white font-bold shadow-xs'
                  : 'bg-slate-100 hover:bg-slate-200 text-slate-700'
              }`}
            >
              {st === 'all' ? 'All' : st}
            </button>
          ))}
        </div>
      </div>

      {/* Delivery Speed Filter */}
      <div className="space-y-2 pt-4 border-t border-slate-100">
        <label className="text-xs font-bold uppercase tracking-wider text-slate-500 block">
          Delivery Speed
        </label>
        <div className="space-y-1">
          {DELIVERY_OPTIONS.map((d) => (
            <button
              key={d.id}
              type="button"
              onClick={() => setSelectedDelivery(d.id)}
              className={`w-full text-left text-xs font-semibold px-3 py-1.5 rounded-xl transition cursor-pointer flex items-center justify-between ${
                selectedDelivery === d.id
                  ? 'bg-indigo-50 text-indigo-700 font-bold border border-indigo-200'
                  : 'hover:bg-slate-50 text-slate-600'
              }`}
            >
              <span>{d.label}</span>
              {selectedDelivery === d.id && <span className="text-xs text-indigo-600">✓</span>}
            </button>
          ))}
        </div>
      </div>

      {/* Merchant / Store Filter */}
      <div className="space-y-2 pt-4 border-t border-slate-100">
        <label className="text-xs font-bold uppercase tracking-wider text-slate-500 block">
          Store / Merchant
        </label>
        <div className="grid grid-cols-2 gap-1.5">
          {[
            { id: 'all', label: 'All Stores' },
            { id: 'Amazon', label: 'Amazon' },
            { id: 'Flipkart', label: 'Flipkart' },
            { id: 'Croma', label: 'Croma' },
          ].map((m) => (
            <button
              key={m.id}
              type="button"
              onClick={() => setSelectedMerchant(m.id)}
              className={`text-xs font-semibold px-2.5 py-1.5 rounded-xl transition cursor-pointer text-center ${
                selectedMerchant.toLowerCase() === m.id.toLowerCase()
                  ? 'bg-slate-900 text-white font-bold shadow-xs'
                  : 'bg-slate-100 hover:bg-slate-200 text-slate-700'
              }`}
            >
              {m.label}
            </button>
          ))}
        </div>
      </div>

      {/* Category Filter */}
      <div className="space-y-2 pt-4 border-t border-slate-100">
        <label className="text-xs font-bold uppercase tracking-wider text-slate-500 block">
          Category
        </label>
        <div className="space-y-1">
          {PRODUCT_CATEGORIES.map((cat) => (
            <button
              key={cat}
              type="button"
              onClick={() => setSelectedCategory(cat)}
              className={`w-full text-left text-xs font-semibold px-3 py-1.5 rounded-xl transition cursor-pointer flex items-center justify-between ${
                selectedCategory === cat
                  ? 'bg-indigo-600 text-white font-bold shadow-xs'
                  : 'hover:bg-slate-50 text-slate-600'
              }`}
            >
              <span>{cat}</span>
              {selectedCategory === cat && <span className="text-xs">✓</span>}
            </button>
          ))}
        </div>
      </div>
    </div>
  );
};

export default ProductFilterPanel;
