import React, { useState, useEffect, useCallback } from 'react';
import { searchFlights } from '../services/flightService';
import { rankCostTimeClientSide } from '../services/costTimeOptimizationService';
import FlightOfferCard from '../components/FlightOfferCard';
import AiRecommendationCard from '../components/AiRecommendationCard';
import RankingExplanationBanner from '../components/RankingExplanationBanner';
import LoadingSkeleton from '../components/LoadingSkeleton';
import EmptyState from '../components/EmptyState';
import ErrorState from '../components/ErrorState';
import GroupTravelOptimizer from '../components/GroupTravelOptimizer';
import CostTimeSlider from '../components/CostTimeSlider';

export const FlightsPage = () => {
  // Travel Mode: 'standard' | 'group'
  const [travelMode, setTravelMode] = useState('standard');
  const [costWeight, setCostWeight] = useState(50); // 0 (Time) to 100 (Money)

  // Flight search form state
  const [origin, setOrigin] = useState('DEL');
  const [destination, setDestination] = useState('BOM');
  const [departureDate, setDepartureDate] = useState(() => {
    const d = new Date();
    d.setDate(d.getDate() + 7);
    return d.toISOString().split('T')[0];
  });
  const [returnDate, setReturnDate] = useState('');
  const [adults, setAdults] = useState(1);
  const [cabinClass, setCabinClass] = useState('ECONOMY');

  // Filter & sorting state
  const [selectedTab, setSelectedTab] = useState('best'); // 'best' | 'cheapest' | 'fastest' | 'all'
  const [maxStops, setMaxStops] = useState(''); // '' (all), '0' (non-stop), '1', '2'
  const [selectedAirline, setSelectedAirline] = useState('all');
  const [timeOfDay, setTimeOfDay] = useState('all'); // 'all', 'morning', 'afternoon', 'evening', 'night'
  const [maxPrice, setMaxPrice] = useState(25000);

  // Response state
  const [flights, setFlights] = useState([]);
  const [totalOffers, setTotalOffers] = useState(0);
  const [cheapestPrice, setCheapestPrice] = useState(null);
  const [fastestDuration, setFastestDuration] = useState(null);
  const [bestAirline, setBestAirline] = useState(null);
  const [aiRecommendation, setAiRecommendation] = useState(null);
  const [rankingSummary, setRankingSummary] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  const executeFlightSearch = useCallback(async () => {
    setLoading(true);
    setError(null);

    let sortBy = 'best';
    if (selectedTab === 'cheapest') sortBy = 'cheapest';
    else if (selectedTab === 'fastest') sortBy = 'fastest';
    else if (selectedTab === 'all') sortBy = 'price_asc';

    try {
      const data = await searchFlights({
        origin,
        destination,
        departureDate,
        returnDate: returnDate || null,
        adults,
        cabinClass,
        maxStops: maxStops !== '' ? Number(maxStops) : null,
        airline: selectedAirline,
        maxPrice,
        timeOfDay,
        sortBy,
      });

      setFlights(data.offers || []);
      setTotalOffers(data.totalOffers || 0);
      setCheapestPrice(data.cheapestPrice);
      setFastestDuration(data.fastestDurationMinutes);
      setBestAirline(data.bestAirline);
      setAiRecommendation(data.aiRecommendation || null);
      setRankingSummary(data.rankingSummary || null);
    } catch (err) {
      console.error('Flight comparison search failed:', err);
      setError(err.message || 'Unable to fetch flight fares from providers');
    } finally {
      setLoading(false);
    }
  }, [origin, destination, departureDate, returnDate, adults, cabinClass, selectedTab, maxStops, selectedAirline, maxPrice, timeOfDay]);

  useEffect(() => {
    executeFlightSearch();
  }, [executeFlightSearch]);

  const handleFormSubmit = (e) => {
    e.preventDefault();
    executeFlightSearch();
  };

  const handleSwapAirports = () => {
    const temp = origin;
    setOrigin(destination);
    setDestination(temp);
  };

  const resetFilters = () => {
    setMaxStops('');
    setSelectedAirline('all');
    setTimeOfDay('all');
    setMaxPrice(25000);
    setSelectedTab('best');
  };

  const airlines = ['all', 'IndiGo', 'Air India', 'Vistara', 'SpiceJet', 'Akasa Air'];

  return (
    <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8 space-y-8">
      
      {/* Header */}
      <div>
        <div className="flex items-center gap-2 text-xs text-slate-500 mb-2">
          <span>Home</span>
          <span>/</span>
          <span className="text-indigo-600 font-medium">Flights Engine</span>
        </div>
        <div className="flex flex-col md:flex-row md:items-center justify-between gap-4">
          <div>
            <h1 className="text-3xl sm:text-4xl font-extrabold text-slate-900 tracking-tight">
              Flight Fare Comparison
            </h1>
            <p className="text-sm text-slate-500 mt-1">
              Compare live airfares across airlines and GDS channels to spot the cheapest and best value routes.
            </p>
          </div>

          {/* Quick Insights Pills */}
          <div className="flex items-center gap-2 flex-wrap">
            {cheapestPrice && (
              <div className="bg-emerald-50 border border-emerald-200 px-3.5 py-2 rounded-xl text-left">
                <span className="text-[10px] font-bold text-emerald-800 uppercase block tracking-wider">Cheapest Fare</span>
                <span className="text-sm font-black text-slate-900 font-mono">₹{Number(cheapestPrice).toLocaleString('en-IN')}</span>
              </div>
            )}
            {fastestDuration && (
              <div className="bg-indigo-50 border border-indigo-200 px-3.5 py-2 rounded-xl text-left">
                <span className="text-[10px] font-bold text-indigo-800 uppercase block tracking-wider">Fastest Flight</span>
                <span className="text-sm font-black text-slate-900 font-mono">{Math.floor(fastestDuration / 60)}h {fastestDuration % 60}m</span>
              </div>
            )}
            {bestAirline && (
              <div className="bg-amber-50 border border-amber-200 px-3.5 py-2 rounded-xl text-left">
                <span className="text-[10px] font-bold text-amber-900 uppercase block tracking-wider">Top Value Airline</span>
                <span className="text-sm font-black text-slate-900">{bestAirline}</span>
              </div>
            )}
          </div>
        </div>
      </div>

      {/* Travel Engine Switcher Tabs */}
      <div className="flex items-center gap-2 p-1.5 bg-slate-100/90 rounded-2xl w-fit">
        <button
          type="button"
          onClick={() => setTravelMode('standard')}
          className={`px-4 py-2.5 rounded-xl text-xs sm:text-sm font-bold transition cursor-pointer flex items-center gap-2 ${
            travelMode === 'standard'
              ? 'bg-white text-indigo-600 shadow-xs'
              : 'text-slate-600 hover:text-slate-900'
          }`}
        >
          <span>✈️</span>
          <span>Individual Flight Search</span>
        </button>

        <button
          type="button"
          onClick={() => setTravelMode('group')}
          className={`px-4 py-2.5 rounded-xl text-xs sm:text-sm font-bold transition cursor-pointer flex items-center gap-2 ${
            travelMode === 'group'
              ? 'bg-indigo-600 text-white shadow-xs'
              : 'text-slate-600 hover:text-slate-900'
          }`}
        >
          <span>👥</span>
          <span>Group Travel Optimizer</span>
          <span className="px-1.5 py-0.5 rounded-full bg-emerald-400 text-emerald-950 text-[10px] font-extrabold uppercase">
            New
          </span>
        </button>
      </div>

      {travelMode === 'group' ? (
        <GroupTravelOptimizer initialOrigin={origin} initialDestination={destination} />
      ) : (
        <>
          {/* Flight Search Form */}
          <form onSubmit={handleFormSubmit} className="bg-white rounded-3xl p-6 border border-slate-200 shadow-md space-y-4">
        <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-5 gap-3 items-center">
          
          {/* Origin */}
          <div>
            <label className="block text-[11px] font-bold uppercase tracking-wider text-slate-500 mb-1">From Airport</label>
            <div className="relative">
              <span className="absolute inset-y-0 left-0 pl-3 flex items-center text-slate-400">🛫</span>
              <input
                type="text"
                required
                value={origin}
                onChange={(e) => setOrigin(e.target.value.toUpperCase())}
                placeholder="DEL (New Delhi)"
                className="w-full pl-9 pr-3 py-2.5 bg-slate-50 border border-slate-200 rounded-xl text-sm font-bold text-slate-900 focus:outline-hidden focus:border-indigo-500 uppercase"
              />
            </div>
          </div>

          {/* Destination */}
          <div className="relative">
            <div className="flex justify-between items-center mb-1">
              <label className="text-[11px] font-bold uppercase tracking-wider text-slate-500">To Airport</label>
              <button
                type="button"
                onClick={handleSwapAirports}
                className="text-[11px] font-bold text-indigo-600 hover:text-indigo-800 cursor-pointer"
                title="Swap origin and destination"
              >
                ⇄ Swap
              </button>
            </div>
            <div className="relative">
              <span className="absolute inset-y-0 left-0 pl-3 flex items-center text-slate-400">🛬</span>
              <input
                type="text"
                required
                value={destination}
                onChange={(e) => setDestination(e.target.value.toUpperCase())}
                placeholder="BOM (Mumbai)"
                className="w-full pl-9 pr-3 py-2.5 bg-slate-50 border border-slate-200 rounded-xl text-sm font-bold text-slate-900 focus:outline-hidden focus:border-indigo-500 uppercase"
              />
            </div>
          </div>

          {/* Departure Date */}
          <div>
            <label className="block text-[11px] font-bold uppercase tracking-wider text-slate-500 mb-1">Departure Date</label>
            <input
              type="date"
              required
              value={departureDate}
              onChange={(e) => setDepartureDate(e.target.value)}
              className="w-full px-3 py-2 bg-slate-50 border border-slate-200 rounded-xl text-sm font-semibold text-slate-900 focus:outline-hidden focus:border-indigo-500 cursor-pointer"
            />
          </div>

          {/* Passengers & Cabin */}
          <div className="grid grid-cols-2 gap-2">
            <div>
              <label className="block text-[11px] font-bold uppercase tracking-wider text-slate-500 mb-1">Adults</label>
              <select
                value={adults}
                onChange={(e) => setAdults(Number(e.target.value))}
                className="w-full px-2 py-2.5 bg-slate-50 border border-slate-200 rounded-xl text-sm font-bold text-slate-900 focus:outline-hidden focus:border-indigo-500 cursor-pointer"
              >
                {[1, 2, 3, 4, 5, 6].map((num) => (
                  <option key={num} value={num}>{num} Adult{num > 1 ? 's' : ''}</option>
                ))}
              </select>
            </div>
            <div>
              <label className="block text-[11px] font-bold uppercase tracking-wider text-slate-500 mb-1">Class</label>
              <select
                value={cabinClass}
                onChange={(e) => setCabinClass(e.target.value)}
                className="w-full px-2 py-2.5 bg-slate-50 border border-slate-200 rounded-xl text-xs font-bold text-slate-900 focus:outline-hidden focus:border-indigo-500 cursor-pointer"
              >
                <option value="ECONOMY">Economy</option>
                <option value="PREMIUM_ECONOMY">Prem. Eco</option>
                <option value="BUSINESS">Business</option>
              </select>
            </div>
          </div>

          {/* Search Button */}
          <div className="sm:col-span-2 lg:col-span-1 pt-4 lg:pt-0">
            <button
              type="submit"
              disabled={loading}
              className="w-full py-2.5 bg-indigo-600 hover:bg-indigo-700 disabled:opacity-50 text-white font-bold text-sm rounded-xl shadow-xs transition flex items-center justify-center gap-2 cursor-pointer"
            >
              <span>Search Flights</span>
              <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M14 5l7 7m0 0l-7 7m7-7H3" />
              </svg>
            </button>
          </div>

        </div>
      </form>

      {/* Main Grid: Filters & Flight Feed */}
      <div className="grid grid-cols-1 lg:grid-cols-4 gap-8 items-start">
        
        {/* Left Sidebar Filters */}
        <div className="bg-white rounded-3xl border border-slate-200 p-6 shadow-xs space-y-6">
          <div className="flex items-center justify-between border-b border-slate-100 pb-3">
            <h3 className="font-bold text-slate-900 text-sm">Filter Flights</h3>
            <button
              onClick={resetFilters}
              className="text-xs font-semibold text-indigo-600 hover:text-indigo-800 cursor-pointer"
            >
              Reset All
            </button>
          </div>

          {/* Number of Stops */}
          <div className="space-y-2">
            <label className="block text-xs font-bold uppercase tracking-wider text-slate-500">
              Stops
            </label>
            <div className="flex flex-col gap-1.5">
              {[
                { label: 'Any number of stops', val: '' },
                { label: 'Non-stop only', val: '0' },
                { label: 'Up to 1 stop', val: '1' },
              ].map((stopOption) => (
                <button
                  key={stopOption.label}
                  type="button"
                  onClick={() => setMaxStops(stopOption.val)}
                  className={`text-left px-3 py-2 rounded-xl text-xs font-semibold transition cursor-pointer ${
                    maxStops === stopOption.val
                      ? 'bg-indigo-50 text-indigo-700 border border-indigo-200 font-bold'
                      : 'text-slate-700 hover:bg-slate-50'
                  }`}
                >
                  {stopOption.label}
                </button>
              ))}
            </div>
          </div>

          {/* Airlines Filter */}
          <div className="space-y-2 pt-4 border-t border-slate-100">
            <label className="block text-xs font-bold uppercase tracking-wider text-slate-500">
              Airlines
            </label>
            <div className="flex flex-wrap gap-1.5">
              {airlines.map((al) => (
                <button
                  key={al}
                  type="button"
                  onClick={() => setSelectedAirline(al)}
                  className={`px-3 py-1.5 rounded-xl text-xs font-semibold transition cursor-pointer ${
                    selectedAirline === al
                      ? 'bg-slate-900 text-white shadow-xs'
                      : 'bg-slate-100 text-slate-700 hover:bg-slate-200'
                  }`}
                >
                  {al === 'all' ? 'All Airlines' : al}
                </button>
              ))}
            </div>
          </div>

          {/* Departure Time of Day */}
          <div className="space-y-2 pt-4 border-t border-slate-100">
            <label className="block text-xs font-bold uppercase tracking-wider text-slate-500">
              Departure Time
            </label>
            <div className="grid grid-cols-2 gap-1.5 text-xs font-semibold">
              {[
                { key: 'all', label: 'Any Time', icon: '⏰' },
                { key: 'morning', label: 'Morning (5am-12pm)', icon: '🌅' },
                { key: 'afternoon', label: 'Afternoon (12-6pm)', icon: '☀️' },
                { key: 'evening', label: 'Evening (6pm-12am)', icon: '🌙' },
              ].map((slot) => (
                <button
                  key={slot.key}
                  type="button"
                  onClick={() => setTimeOfDay(slot.key)}
                  className={`p-2 rounded-xl border text-center transition cursor-pointer ${
                    timeOfDay === slot.key
                      ? 'bg-indigo-50 border-indigo-300 text-indigo-700 font-bold'
                      : 'border-slate-200 text-slate-700 hover:bg-slate-50'
                  }`}
                >
                  <div className="text-base">{slot.icon}</div>
                  <div className="text-[11px] mt-0.5 leading-tight">{slot.label}</div>
                </button>
              ))}
            </div>
          </div>

          {/* Max Price Range */}
          <div className="space-y-3 pt-4 border-t border-slate-100">
            <div className="flex items-center justify-between">
              <label className="text-xs font-bold uppercase tracking-wider text-slate-500">
                Max Flight Price
              </label>
              <span className="text-xs font-bold text-slate-900 font-mono">
                ₹{maxPrice.toLocaleString('en-IN')}
              </span>
            </div>
            <input
              type="range"
              min="3000"
              max="25000"
              step="500"
              value={maxPrice}
              onChange={(e) => setMaxPrice(Number(e.target.value))}
              className="w-full accent-indigo-600 cursor-pointer"
            />
            <div className="flex justify-between text-[11px] text-slate-400 font-mono">
              <span>₹3,000</span>
              <span>₹25,000</span>
            </div>
          </div>
        </div>

        {/* Right Main Flight Feed */}
        <div className="lg:col-span-3 space-y-6">
          
          {/* Quick Filter Tabs: Best, Cheapest, Fastest, All */}
          <div className="flex bg-white p-1.5 rounded-2xl border border-slate-200 shadow-xs text-xs font-bold">
            {[
              { id: 'best', label: '⭐ Best Value', sub: 'Balanced Price & Time' },
              { id: 'cheapest', label: '💰 Cheapest', sub: 'Lowest Fare' },
              { id: 'fastest', label: '⚡ Fastest', sub: 'Shortest Duration' },
              { id: 'all', label: '✈️ All Flights', sub: 'Standard Sort' },
            ].map((tab) => (
              <button
                key={tab.id}
                type="button"
                onClick={() => setSelectedTab(tab.id)}
                className={`flex-1 py-2.5 px-2 rounded-xl transition text-center cursor-pointer ${
                  selectedTab === tab.id
                    ? 'bg-indigo-600 text-white shadow-xs'
                    : 'text-slate-600 hover:text-slate-900 hover:bg-slate-50'
                }`}
              >
                <div className="font-extrabold">{tab.label}</div>
                <div className={`text-[10px] font-normal ${selectedTab === tab.id ? 'text-indigo-100' : 'text-slate-400'}`}>
                  {tab.sub}
                </div>
              </button>
            ))}
          </div>

          {/* Cost vs Time Optimization Slider */}
          <CostTimeSlider
            costWeight={costWeight}
            onChange={(w) => setCostWeight(w)}
            compact={false}
          />

          {/* Flights Feed */}
          {loading ? (
            <LoadingSkeleton type="flight-card" count={4} />
          ) : error ? (
            <ErrorState
              title="Flight Comparison Engine Error"
              message={error}
              onRetry={executeFlightSearch}
            />
          ) : flights.length > 0 ? (() => {
            const sortedFlights = rankCostTimeClientSide(
              flights,
              costWeight,
              (f) => f.price,
              (f) => f.durationMinutes
            );

            return (
            <div className="space-y-4">
              {rankingSummary && (
                <RankingExplanationBanner rankingSummary={rankingSummary} type="flight" />
              )}
              {aiRecommendation && (
                <AiRecommendationCard recommendation={aiRecommendation} />
              )}
              {sortedFlights.map((flight, idx) => (
                <FlightOfferCard
                  key={`${flight.airline}-${flight.flightNumber}-${idx}`}
                  flight={flight}
                />
              ))}
            </div>
            );
          })() : (
            <EmptyState
              title="No flights found for this route"
              description={`We couldn't find available flights between ${origin} and ${destination} on ${departureDate} matching your filters.`}
              actionLabel="Reset Search Filters"
              onAction={resetFilters}
            />
          )}

        </div>

      </div>
      </>
      )}

    </div>
  );
};

export default FlightsPage;
