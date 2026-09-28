import React from 'react';

export const PriceBadge = ({ type = 'cheapest', text, className = '' }) => {
  const styles = {
    cheapest: 'bg-emerald-500/10 text-emerald-700 border-emerald-200 ring-emerald-500/20',
    'best-value': 'bg-indigo-500/10 text-indigo-700 border-indigo-200 ring-indigo-500/20',
    fastest: 'bg-amber-500/10 text-amber-700 border-amber-200 ring-amber-500/20',
    surge: 'bg-rose-500/10 text-rose-700 border-rose-200 ring-rose-500/20',
    discount: 'bg-blue-500/10 text-blue-700 border-blue-200 ring-blue-500/20',
    neutral: 'bg-slate-100 text-slate-700 border-slate-200 ring-slate-500/10',
  };

  const icons = {
    cheapest: (
      <svg className="w-3 h-3 text-emerald-600" fill="currentColor" viewBox="0 0 20 20">
        <path fillRule="evenodd" d="M10 18a8 8 0 100-16 8 8 0 000 16zm3.707-9.293a1 1 0 00-1.414-1.414L9 10.586 7.707 9.293a1 1 0 00-1.414 1.414l2 2a1 1 0 001.414 0l4-4z" clipRule="evenodd" />
      </svg>
    ),
    'best-value': (
      <svg className="w-3 h-3 text-indigo-600" fill="currentColor" viewBox="0 0 20 20">
        <path d="M9.049 2.927c.3-.921 1.603-.921 1.902 0l1.07 3.292a1 1 0 00.95.69h3.462c.969 0 1.371 1.24.588 1.81l-2.8 2.034a1 1 0 00-.364 1.118l1.07 3.292c.3.921-.755 1.688-1.54 1.118l-2.8-2.034a1 1 0 00-1.175 0l-2.8 2.034c-.784.57-1.838-.197-1.539-1.118l1.07-3.292a1 1 0 00-.364-1.118L2.98 8.72c-.783-.57-.38-1.81.588-1.81h3.461a1 1 0 00.951-.69l1.07-3.292z" />
      </svg>
    ),
    fastest: (
      <svg className="w-3 h-3 text-amber-600" fill="currentColor" viewBox="0 0 20 20">
        <path fillRule="evenodd" d="M11.3 1.046A1 1 0 0112 2v5h4a1 1 0 01.82 1.573l-7 10A1 1 0 018 18v-5H4a1 1 0 01-.82-1.573l7-10a1 1 0 011.12-.38z" clipRule="evenodd" />
      </svg>
    ),
    surge: (
      <svg className="w-3 h-3 text-rose-600" fill="currentColor" viewBox="0 0 20 20">
        <path fillRule="evenodd" d="M8.257 3.099c.765-1.36 2.722-1.36 3.486 0l5.58 9.92c.75 1.334-.213 2.98-1.742 2.98H4.42c-1.53 0-2.493-1.646-1.743-2.98l5.58-9.92zM11 13a1 1 0 11-2 0 1 1 0 012 0zm-1-8a1 1 0 00-1 1v3a1 1 0 002 0V6a1 1 0 00-1-1z" clipRule="evenodd" />
      </svg>
    ),
  };

  const defaultText = {
    cheapest: 'Cheapest Deal',
    'best-value': 'Best Value',
    fastest: 'Fastest Route',
    surge: 'Surge Pricing',
    discount: 'Price Dropped',
    neutral: 'Recommended',
  };

  return (
    <span
      className={`inline-flex items-center gap-1.5 px-2.5 py-0.5 rounded-full text-xs font-semibold border ring-1 ring-inset ${
        styles[type] || styles.neutral
      } ${className}`}
    >
      {icons[type]}
      <span>{text || defaultText[type]}</span>
    </span>
  );
};

export default PriceBadge;
