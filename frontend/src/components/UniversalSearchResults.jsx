import React from 'react';
import { Link } from 'react-router-dom';
import ProductOfferCard from './ProductOfferCard';
import FlightOfferCard from './FlightOfferCard';
import RideOfferCard from './RideOfferCard';
import AiRecommendationCard from './AiRecommendationCard';
import RankingExplanationBanner from './RankingExplanationBanner';
import NluIntentBadge from './NluIntentBadge';
import NluClarificationModal from './NluClarificationModal';

export const UniversalSearchResults = ({ result, onClear, onResolveClarification }) => {
  if (!result) return null;

  const {
    intent,
    query,
    intentDetails,
    queryUnderstanding,
    requiresClarification,
    missingFields,
    clarificationPrompt,
    redirectRoute,
    productResults,
    flightResults,
    rideResults,
    aiRecommendation,
    rankingSummary,
    executionTimeMs,
  } = result;

  const intentBadges = {
    PRODUCT_SEARCH: { icon: '🛍️', label: 'Product Search', bg: 'bg-indigo-50 text-indigo-700 border-indigo-200' },
    FLIGHT_SEARCH: { icon: '✈️', label: 'Flight Route Comparison', bg: 'bg-sky-50 text-sky-700 border-sky-200' },
    RIDE_SEARCH: { icon: '🚗', label: 'Cab & Ride Comparison', bg: 'bg-emerald-50 text-emerald-700 border-emerald-200' },
    UNKNOWN: { icon: '🔍', label: 'General Search', bg: 'bg-slate-50 text-slate-700 border-slate-200' },
  };

  const badge = intentBadges[intent] || intentBadges.UNKNOWN;

  return (
    <div className="bg-white rounded-3xl border border-slate-200 shadow-xl p-5 sm:p-8 space-y-6 animate-fadeIn" data-testid="universal-results-container">
      
      {/* Top Header: Intent Recognition Badge & Routing */}
      <div className="flex flex-wrap items-center justify-between gap-3 border-b border-slate-100 pb-4">
        <div className="flex items-center gap-2.5">
          <span className={`px-3 py-1 rounded-xl text-xs font-bold border flex items-center gap-1.5 shadow-2xs ${badge.bg}`}>
            <span>{badge.icon}</span>
            <span>{badge.label}</span>
          </span>
          <span className="text-xs text-slate-400 font-mono">
            {executionTimeMs}ms
          </span>
        </div>

        <div className="flex items-center gap-3">
          {redirectRoute && (
            <Link
              to={redirectRoute}
              className="text-xs font-bold text-indigo-600 hover:text-indigo-800 flex items-center gap-1"
            >
              <span>Open Dedicated Page</span>
              <span>→</span>
            </Link>
          )}
          {onClear && (
            <button
              onClick={onClear}
              className="text-xs font-semibold text-slate-400 hover:text-slate-600 cursor-pointer"
            >
              Clear Results
            </button>
          )}
        </div>
      </div>

      {/* Clarification Prompt if required information was missing */}
      {requiresClarification && (
        <NluClarificationModal
          clarificationPrompt={clarificationPrompt}
          missingFields={missingFields}
          query={query}
          onResolveClarification={onResolveClarification}
          onCancel={onClear}
        />
      )}

      {/* Structured NLU Understanding Badge */}
      {queryUnderstanding ? (
        <NluIntentBadge queryUnderstanding={queryUnderstanding} intent={intent} />
      ) : intentDetails ? (
        <div className="bg-slate-50 border border-slate-200/70 p-3.5 rounded-2xl flex flex-wrap items-center gap-4 text-xs">
          <div>
            <span className="font-bold text-slate-500 uppercase text-[10px] block">Analyzed Query</span>
            <span className="font-extrabold text-slate-900">{intentDetails.query || query}</span>
          </div>

          {intentDetails.filters?.maxPrice && (
            <div>
              <span className="font-bold text-slate-500 uppercase text-[10px] block">Budget Limit</span>
              <span className="font-bold text-emerald-600 font-mono">
                ₹{Number(intentDetails.filters.maxPrice).toLocaleString('en-IN')}
              </span>
            </div>
          )}

          {intentDetails.filters?.storage && (
            <div>
              <span className="font-bold text-slate-500 uppercase text-[10px] block">Storage</span>
              <span className="font-bold text-slate-800">{intentDetails.filters.storage}</span>
            </div>
          )}

          {intentDetails.flightParams?.origin && (
            <div>
              <span className="font-bold text-slate-500 uppercase text-[10px] block">Route</span>
              <span className="font-bold text-slate-900 font-mono">
                {intentDetails.flightParams.origin} → {intentDetails.flightParams.destination}
              </span>
            </div>
          )}

          {intentDetails.rideParams?.pickup && (
            <div>
              <span className="font-bold text-slate-500 uppercase text-[10px] block">Pickup → Destination</span>
              <span className="font-bold text-slate-900">
                {intentDetails.rideParams.pickup} → {intentDetails.rideParams.destination}
              </span>
            </div>
          )}
        </div>
      ) : null}

      {/* AI Recommendation Card */}
      {aiRecommendation && (
        <AiRecommendationCard recommendation={aiRecommendation} />
      )}

      {/* Ranking Summary Banner */}
      {rankingSummary && (
        <RankingExplanationBanner
          rankingSummary={rankingSummary}
          type={intent === 'FLIGHT_SEARCH' ? 'flight' : intent === 'RIDE_SEARCH' ? 'ride' : 'product'}
        />
      )}

      {/* Product Search Results */}
      {intent === 'PRODUCT_SEARCH' && productResults && (
        <div className="space-y-4">
          <div className="flex items-center justify-between text-xs text-slate-500">
            <span>Found <strong>{productResults.offers?.length || 0}</strong> verified offers across merchants</span>
            {productResults.cheapestPrice && (
              <span className="text-emerald-700 font-bold font-mono">
                Lowest: ₹{Number(productResults.cheapestPrice).toLocaleString('en-IN')} ({productResults.cheapestMerchant})
              </span>
            )}
          </div>
          <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-5">
            {productResults.offers?.slice(0, 6).map((offer, idx) => (
              <ProductOfferCard key={`${offer.merchant}-${offer.productName}-${idx}`} offer={offer} />
            ))}
          </div>
        </div>
      )}

      {/* Flight Search Results */}
      {intent === 'FLIGHT_SEARCH' && flightResults && (
        <div className="space-y-4">
          <div className="flex items-center justify-between text-xs text-slate-500">
            <span>Showing <strong>{flightResults.offers?.length || 0}</strong> flight options for {flightResults.origin} → {flightResults.destination}</span>
            {flightResults.cheapestPrice && (
              <span className="text-emerald-700 font-bold font-mono">
                Lowest Fare: ₹{Number(flightResults.cheapestPrice).toLocaleString('en-IN')}
              </span>
            )}
          </div>
          <div className="space-y-4">
            {flightResults.offers?.slice(0, 4).map((flight, idx) => (
              <FlightOfferCard key={`${flight.airline}-${flight.flightNumber}-${idx}`} flight={flight} />
            ))}
          </div>
        </div>
      )}

      {/* Ride Search Results */}
      {intent === 'RIDE_SEARCH' && rideResults && (
        <div className="space-y-4">
          <div className="flex items-center justify-between text-xs text-slate-500">
            <span>Comparing <strong>{rideResults.rides?.length || 0}</strong> on-demand options</span>
            {rideResults.fastestEta && (
              <span className="text-emerald-700 font-bold">
                Fastest Pickup: {rideResults.fastestEta} mins ({rideResults.fastestProvider})
              </span>
            )}
          </div>
          <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-4">
            {rideResults.rides?.slice(0, 6).map((ride, idx) => (
              <RideOfferCard key={`${ride.provider}-${ride.rideType}-${idx}`} ride={ride} />
            ))}
          </div>
        </div>
      )}

    </div>
  );
};

export default UniversalSearchResults;
