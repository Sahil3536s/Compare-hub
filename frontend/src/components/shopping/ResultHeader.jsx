import React from 'react';

/**
 * Header displayed after a search.
 * Props:
 *  - query: string
 *  - totalOffers: number
 *  - successfulProviders: string[]
 *  - failedProviders: string[]
 */
export const ResultHeader = ({
  query = '',
  totalOffers = 0,
  successfulProviders = [],
  failedProviders = [],
}) => {
  const successCount = Array.isArray(successfulProviders) ? successfulProviders.length : 0;
  const failedCount = Array.isArray(failedProviders) ? failedProviders.length : 0;
  const safeTotal = typeof totalOffers === 'number' && !isNaN(totalOffers) ? totalOffers : 0;

  return (
    <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between p-4 bg-gray-50 border-b">
      <div className="text-lg font-medium text-gray-800">
        {query ? (
          <span>
            Showing {safeTotal} result{safeTotal !== 1 && 's'} for <strong className="text-indigo-600">{query}</strong>
          </span>
        ) : (
          <span>Start a product search above</span>
        )}
      </div>
      <div className="mt-2 sm:mt-0 text-sm text-gray-600">
        <span className="mr-3">✅ {successCount} provider{successCount !== 1 && 's'}</span>
        {failedCount > 0 && (
          <span className="text-red-600">⚠️ {failedCount} unavailable</span>
        )}
      </div>
    </div>
  );
};

export default ResultHeader;
