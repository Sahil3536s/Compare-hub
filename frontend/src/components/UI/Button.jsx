import React from 'react';

/**
 * Primary button – consistent styling across the app.
 * Props are forwarded to the underlying button element.
 */
export const PrimaryButton = ({ children, className = '', ...props }) => (
  <button
    className={`bg-indigo-600 text-white font-semibold py-2 px-4 rounded-lg hover:bg-indigo-700 focus:outline-none focus-visible:ring-2 focus-visible:ring-indigo-500 transition ${className}`}
    {...props}
  >
    {children}
  </button>
);

/**
 * Secondary button – for less‑prominent actions.
 */
export const SecondaryButton = ({ children, className = '', ...props }) => (
  <button
    className={`bg-gray-100 text-gray-800 font-medium py-2 px-4 rounded-lg hover:bg-gray-200 focus:outline-none focus-visible:ring-2 focus-visible:ring-gray-500 transition ${className}`}
    {...props}
  >
    {children}
  </button>
);

export default PrimaryButton;
