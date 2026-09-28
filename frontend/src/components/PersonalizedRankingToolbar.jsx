import React, { useState } from 'react';
import { useAuth } from '../context/AuthContext';
import { saveRankingPreferences } from '../services/rankingService';

export const PersonalizedRankingToolbar = ({ onPreferenceChange, currentWeights, currentPreset = 'BALANCED' }) => {
  let user = null;
  try {
    const auth = useAuth();
    user = auth?.user || null;
  } catch (e) {
    user = null;
  }

  const presets = [
    {
      id: 'BALANCED',
      label: '⚖️ Balanced',
      desc: 'Price 40%, Rating 20%, Delivery 15%, Discount 15%, Trust 10%',
      weights: { price: 40, rating: 20, discount: 15, delivery: 15, reliability: 10 },
    },
    {
      id: 'CHEAPEST',
      label: '💰 Save Money',
      desc: 'Price 70%, Delivery 10%, Rating 10%, Trust 10%',
      weights: { price: 70, rating: 10, discount: 0, delivery: 10, reliability: 10 },
    },
    {
      id: 'FASTEST',
      label: '⚡ Fastest Delivery',
      desc: 'Delivery 50%, Price 20%, Rating 15%, Trust 15%',
      weights: { price: 20, rating: 15, discount: 0, delivery: 50, reliability: 15 },
    },
    {
      id: 'BEST_RATED',
      label: '⭐ Best Rated',
      desc: 'Rating 50%, Trust 20%, Price 20%, Delivery 10%',
      weights: { price: 20, rating: 50, discount: 0, delivery: 10, reliability: 20 },
    },
  ];

  const [activePreset, setActivePreset] = useState(currentPreset);
  const [showCustomSliders, setShowCustomSliders] = useState(false);
  const [weights, setWeights] = useState(
    currentWeights || { price: 40, rating: 20, discount: 15, delivery: 15, reliability: 10 }
  );
  const [isSaving, setIsSaving] = useState(false);
  const [saveSuccess, setSaveSuccess] = useState(false);

  const handleSelectPreset = (preset) => {
    setActivePreset(preset.id);
    setShowCustomSliders(false);
    setWeights(preset.weights);
    if (onPreferenceChange) {
      onPreferenceChange({ preset: preset.id, weights: preset.weights });
    }
  };

  const handleSliderChange = (factor, value) => {
    const val = parseInt(value, 10);
    const updated = { ...weights, [factor]: val };
    setWeights(updated);
    setActivePreset('CUSTOM');
    if (onPreferenceChange) {
      onPreferenceChange({ preset: 'CUSTOM', weights: updated });
    }
  };

  const totalWeight = Object.values(weights).reduce((sum, v) => sum + (parseInt(v, 10) || 0), 0);

  const handleSaveAsDefault = async () => {
    if (!user) {
      alert('Please log in to save your ranking preferences.');
      return;
    }

    setIsSaving(true);
    setSaveSuccess(false);
    try {
      await saveRankingPreferences({
        preset: activePreset,
        product: weights,
      });
      setSaveSuccess(true);
      setTimeout(() => setSaveSuccess(false), 3000);
    } catch (err) {
      console.error('Failed to save ranking preferences:', err);
    } finally {
      setIsSaving(false);
    }
  };

  return (
    <div className="bg-slate-900 text-white rounded-3xl p-4 sm:p-6 shadow-xl space-y-4 border border-slate-800 animate-fadeIn" data-testid="ranking-toolbar">
      
      {/* Header & Description */}
      <div className="flex flex-col sm:flex-row items-start sm:items-center justify-between gap-3 border-b border-slate-800 pb-3">
        <div>
          <div className="flex items-center gap-2">
            <span className="text-lg">🎯</span>
            <h3 className="font-black text-sm sm:text-base tracking-tight text-white">
              What matters most to you?
            </h3>
            <span className="px-2 py-0.5 rounded-full text-[10px] font-extrabold bg-indigo-500/20 text-indigo-300 border border-indigo-500/30">
              Personalized Ranking
            </span>
          </div>
          <p className="text-xs text-slate-400 mt-0.5">
            Choose a priority preset or tune custom weights to personalize your Best Value rankings.
          </p>
        </div>

        <div className="flex items-center gap-2 self-end sm:self-auto">
          <button
            type="button"
            onClick={() => setShowCustomSliders(!showCustomSliders)}
            className={`px-3 py-1.5 rounded-xl text-xs font-bold transition cursor-pointer border ${
              showCustomSliders || activePreset === 'CUSTOM'
                ? 'bg-indigo-600 text-white border-indigo-500'
                : 'bg-slate-800 hover:bg-slate-700 text-slate-300 border-slate-700'
            }`}
          >
            ⚙️ Tune Sliders
          </button>

          {user && (
            <button
              type="button"
              onClick={handleSaveAsDefault}
              disabled={isSaving}
              className="px-3 py-1.5 rounded-xl text-xs font-bold bg-emerald-600 hover:bg-emerald-500 text-white transition cursor-pointer shadow-xs disabled:opacity-50"
            >
              {isSaving ? 'Saving...' : saveSuccess ? '✓ Saved!' : 'Save Default'}
            </button>
          )}
        </div>
      </div>

      {/* Quick Preset Buttons */}
      <div className="grid grid-cols-2 sm:grid-cols-4 gap-2.5">
        {presets.map((p) => {
          const isActive = activePreset === p.id && !showCustomSliders;
          return (
            <button
              key={p.id}
              type="button"
              onClick={() => handleSelectPreset(p)}
              className={`p-3 rounded-2xl text-left transition cursor-pointer border flex flex-col justify-between min-h-[64px] ${
                isActive
                  ? 'bg-indigo-600/30 border-indigo-400 text-white shadow-md ring-1 ring-indigo-400'
                  : 'bg-slate-800/80 hover:bg-slate-800 border-slate-700 text-slate-300 hover:text-white'
              }`}
            >
              <div className="font-extrabold text-xs sm:text-sm flex items-center justify-between">
                <span>{p.label}</span>
                {isActive && <span className="text-emerald-400 text-xs">● Active</span>}
              </div>
              <div className="text-[10px] text-slate-400 line-clamp-1 mt-1 font-mono">
                {p.desc}
              </div>
            </button>
          );
        })}
      </div>

      {/* Expandable Custom Weight Sliders */}
      {showCustomSliders && (
        <div className="pt-3 border-t border-slate-800 space-y-3 bg-slate-950/40 p-4 rounded-2xl border border-slate-800/80 animate-fadeIn" data-testid="custom-sliders-drawer">
          <div className="flex items-center justify-between text-xs">
            <span className="font-bold text-slate-300 uppercase text-[11px]">Adjust Factor Weights</span>
            <div className="flex items-center gap-1.5">
              <span className="text-slate-400 font-mono text-[11px]">Total:</span>
              <span className={`font-mono font-bold px-2 py-0.5 rounded-md ${
                totalWeight === 100 ? 'bg-emerald-500/20 text-emerald-400' : 'bg-amber-500/20 text-amber-400'
              }`}>
                {totalWeight}% {totalWeight !== 100 && '(will auto-normalize to 100%)'}
              </span>
            </div>
          </div>

          <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-5 gap-4">
            
            {/* Price Slider */}
            <div className="space-y-1">
              <div className="flex justify-between text-xs">
                <span className="text-slate-300 font-medium">💰 Price Weight</span>
                <span className="font-mono font-bold text-indigo-400">{weights.price}%</span>
              </div>
              <input
                type="range"
                min="0"
                max="100"
                step="5"
                value={weights.price}
                onChange={(e) => handleSliderChange('price', e.target.value)}
                className="w-full accent-indigo-500 cursor-pointer"
              />
            </div>

            {/* Rating Slider */}
            <div className="space-y-1">
              <div className="flex justify-between text-xs">
                <span className="text-slate-300 font-medium">⭐ Rating Weight</span>
                <span className="font-mono font-bold text-amber-400">{weights.rating}%</span>
              </div>
              <input
                type="range"
                min="0"
                max="100"
                step="5"
                value={weights.rating}
                onChange={(e) => handleSliderChange('rating', e.target.value)}
                className="w-full accent-amber-500 cursor-pointer"
              />
            </div>

            {/* Delivery Speed Slider */}
            <div className="space-y-1">
              <div className="flex justify-between text-xs">
                <span className="text-slate-300 font-medium">🚚 Delivery Speed</span>
                <span className="font-mono font-bold text-sky-400">{weights.delivery}%</span>
              </div>
              <input
                type="range"
                min="0"
                max="100"
                step="5"
                value={weights.delivery}
                onChange={(e) => handleSliderChange('delivery', e.target.value)}
                className="w-full accent-sky-500 cursor-pointer"
              />
            </div>

            {/* Discount Slider */}
            <div className="space-y-1">
              <div className="flex justify-between text-xs">
                <span className="text-slate-300 font-medium">🏷️ Discount %</span>
                <span className="font-mono font-bold text-emerald-400">{weights.discount}%</span>
              </div>
              <input
                type="range"
                min="0"
                max="100"
                step="5"
                value={weights.discount}
                onChange={(e) => handleSliderChange('discount', e.target.value)}
                className="w-full accent-emerald-500 cursor-pointer"
              />
            </div>

            {/* Seller Reliability Slider */}
            <div className="space-y-1">
              <div className="flex justify-between text-xs">
                <span className="text-slate-300 font-medium">🛡️ Seller Trust</span>
                <span className="font-mono font-bold text-purple-400">{weights.reliability}%</span>
              </div>
              <input
                type="range"
                min="0"
                max="100"
                step="5"
                value={weights.reliability}
                onChange={(e) => handleSliderChange('reliability', e.target.value)}
                className="w-full accent-purple-500 cursor-pointer"
              />
            </div>

          </div>
        </div>
      )}

    </div>
  );
};

export default PersonalizedRankingToolbar;
