import React from 'react';

/**
 * Header displayed after a search.
 * Props:
 *  - query: string
 *  - totalOffers: number
 *  - successfulProviders: string[]
 *  - failedProviders: string[]
 */
export const ResultHeader = ({ query, totalOffers, successfulProviders, failedProviders }) => {
  return (
    <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between p-4 bg-gray-50 border-b">
      <div className="text-lg font-medium text-gray-800">
        {query ? (
          <span>
            Showing {totalOffers} result{totalOffers !== 1 && 's'} for <strong className="text-indigo-600">{query}</strong>
          </span>
        ) : (
          <span>Start a product search above</span>
        )}
      </div>
      <div className="mt-2 sm:mt-0 text-sm text-gray-600">
        <span className="mr-3">✅ {successfulProviders.length} provider{successfulProviders.length !== 1 && 's'}</span>
        {failedProviders.length > 0 && (
          <span className="text-red-600">⚠️ {failedProviders.length} unavailable</span>
        )}
      </div>
    </div>
  );
};

export default ResultHeader;
