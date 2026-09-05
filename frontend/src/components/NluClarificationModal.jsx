import React, { useState } from 'react';

export const NluClarificationModal = ({
  clarificationPrompt,
  missingFields = [],
  query,
  onResolveClarification,
  onCancel,
}) => {
  const [fieldValues, setFieldValues] = useState({});

  const handleInputChange = (field, value) => {
    setFieldValues((prev) => ({
      ...prev,
      [field]: value,
    }));
  };

  const handleSubmit = (e) => {
    e.preventDefault();
    // Build refined query or pass clarification answers
    const extraParts = Object.values(fieldValues).filter(Boolean);
    const refinedQuery = `${query} ${extraParts.join(' ')}`.trim();
    if (onResolveClarification) {
      onResolveClarification(refinedQuery, fieldValues);
    }
  };

  return (
    <div
      className="bg-amber-50/90 border-2 border-amber-200 rounded-3xl p-6 sm:p-8 space-y-5 shadow-lg animate-fadeIn"
      data-testid="nlu-clarification-card"
    >
      <div className="flex items-start gap-3.5">
        <div className="w-10 h-10 rounded-2xl bg-amber-100 text-amber-800 flex items-center justify-center text-xl shrink-0">
          🤔
        </div>
        <div className="space-y-1">
          <h3 className="text-base sm:text-lg font-black text-slate-900">
            A quick detail needed to compare accurately
          </h3>
          <p className="text-xs sm:text-sm text-slate-700 font-medium leading-relaxed">
            {clarificationPrompt || 'Please provide the missing details to find real prices.'}
          </p>
        </div>
      </div>

      <form onSubmit={handleSubmit} className="space-y-4 pt-2">
        <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
          {missingFields.map((field) => (
            <div key={field} className="space-y-1.5">
              <label className="text-xs font-bold text-slate-700 capitalize">
                {field === 'origin' ? 'Departure City / Airport' : field === 'destination' ? 'Destination' : field}
              </label>
              <input
                type="text"
                required
                placeholder={field === 'origin' ? 'e.g., Delhi, Mumbai, BLR' : field === 'destination' ? 'e.g., Goa, Dubai' : `Enter ${field}`}
                value={fieldValues[field] || ''}
                onChange={(e) => handleInputChange(field, e.target.value)}
                className="w-full px-3.5 py-2.5 bg-white rounded-xl border border-amber-300 text-slate-900 text-xs sm:text-sm focus:outline-hidden focus:ring-2 focus:ring-amber-500/30"
              />
            </div>
          ))}
        </div>

        <div className="flex flex-wrap items-center justify-end gap-2.5 pt-2">
          {onCancel && (
            <button
              type="button"
              onClick={onCancel}
              className="px-4 py-2 text-xs font-semibold text-slate-500 hover:text-slate-700 cursor-pointer"
            >
              Cancel
            </button>
          )}
          <button
            type="submit"
            className="px-5 py-2.5 bg-amber-600 hover:bg-amber-700 text-white font-bold text-xs sm:text-sm rounded-xl transition shadow-xs cursor-pointer"
          >
            Find Real Deals →
          </button>
        </div>
      </form>
    </div>
  );
};

export default NluClarificationModal;
