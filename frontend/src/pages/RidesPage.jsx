import React, { useState, useEffect, useCallback } from 'react';
import { suggestPlaces, geocodeAddress, reverseGeocode, estimateRoute } from '../services/locationService';
import { compareRides } from '../services/rideService';
import { rankCostTimeClientSide } from '../services/costTimeOptimizationService';
import InteractiveMap from '../components/InteractiveMap';
import RideOfferCard from '../components/RideOfferCard';
import AiRecommendationCard from '../components/AiRecommendationCard';
import RankingExplanationBanner from '../components/RankingExplanationBanner';
import LoadingSkeleton from '../components/LoadingSkeleton';
import EmptyState from '../components/EmptyState';
import ErrorState from '../components/ErrorState';
import CostTimeSlider from '../components/CostTimeSlider';

export const RidesPage = () => {
  const [costWeight, setCostWeight] = useState(50); // 0 (Fastest ETA) to 100 (Cheapest Fare)
  // Location States
  const [pickupInput, setPickupInput] = useState('Connaught Place, New Delhi');
  const [dropInput, setDropInput] = useState('Indira Gandhi International Airport, New Delhi');
  
  const [pickupLocation, setPickupLocation] = useState({
    latitude: 28.6315,
    longitude: 77.2167,
    address: 'Connaught Place, Central Delhi, New Delhi, Delhi 110001',
    city: 'New Delhi',
  });
  
  const [dropLocation, setDropLocation] = useState({
    latitude: 28.5562,
    longitude: 77.1000,
    address: 'Indira Gandhi International Airport, New Delhi, Delhi 110037',
    city: 'New Delhi',
  });

  // Autocomplete suggestions
  const [pickupSuggestions, setPickupSuggestions] = useState([]);
  const [dropSuggestions, setDropSuggestions] = useState([]);
  const [showPickupDropdown, setShowPickupDropdown] = useState(false);
  const [showDropDropdown, setShowDropDropdown] = useState(false);

  // Filters and Sorting
  const [vehicleCategory, setVehicleCategory] = useState('all'); // 'all', 'cab', 'auto', 'bike'
  const [sortBy, setSortBy] = useState('best'); // 'best', 'cheapest', 'fastest'

  // Route & Ride Data
  const [distanceKm, setDistanceKm] = useState(14.2);
  const [durationMinutes, setDurationMinutes] = useState(32);
  const [polylineCoordinates, setPolylineCoordinates] = useState([]);
  const [rides, setRides] = useState([]);
  const [cheapestFare, setCheapestFare] = useState(null);
  const [fastestEta, setFastestEta] = useState(null);
  const [bestProvider, setBestProvider] = useState(null);
  const [aiRecommendation, setAiRecommendation] = useState(null);
  const [rankingSummary, setRankingSummary] = useState(null);
  const [loading, setLoading] = useState(true);
  const [locatingCurrent, setLocatingCurrent] = useState(false);
  const [error, setError] = useState(null);

  // Autocomplete fetch for Pickup
  const handlePickupChange = async (val) => {
    setPickupInput(val);
    if (val.length >= 2) {
      try {
        const results = await suggestPlaces(val);
        setPickupSuggestions(results);
        setShowPickupDropdown(true);
      } catch (e) {
        console.error('Failed to fetch place suggestions:', e);
      }
    } else {
      setPickupSuggestions([]);
      setShowPickupDropdown(false);
    }
  };

  // Autocomplete fetch for Drop
  const handleDropChange = async (val) => {
    setDropInput(val);
    if (val.length >= 2) {
      try {
        const results = await suggestPlaces(val);
        setDropSuggestions(results);
        setShowDropDropdown(true);
      } catch (e) {
        console.error('Failed to fetch place suggestions:', e);
      }
    } else {
      setDropSuggestions([]);
      setShowDropDropdown(false);
    }
  };

  const handleSelectPickup = (suggestion) => {
    setPickupInput(suggestion.mainText);
    setPickupLocation({
      latitude: suggestion.latitude,
      longitude: suggestion.longitude,
      address: suggestion.fullAddress,
      city: 'New Delhi',
    });
    setShowPickupDropdown(false);
  };

  const handleSelectDrop = (suggestion) => {
    setDropInput(suggestion.mainText);
    setDropLocation({
      latitude: suggestion.latitude,
      longitude: suggestion.longitude,
      address: suggestion.fullAddress,
      city: 'New Delhi',
    });
    setShowDropDropdown(false);
  };

  const handleUseCurrentLocation = () => {
    if (!navigator.geolocation) {
      alert('Geolocation is not supported by your browser.');
      return;
    }

    setLocatingCurrent(true);
    navigator.geolocation.getCurrentPosition(
      async (position) => {
        const lat = position.coords.latitude;
        const lon = position.coords.longitude;
        try {
          const loc = await reverseGeocode(lat, lon);
          setPickupInput('Current Location (' + loc.address.split(',')[0] + ')');
          setPickupLocation(loc);
        } catch (e) {
          setPickupInput(`Current Location (${lat.toFixed(4)}, ${lon.toFixed(4)})`);
          setPickupLocation({ latitude: lat, longitude: lon, address: 'Current GPS Location' });
        } finally {
          setLocatingCurrent(false);
        }
      },
      (err) => {
        console.warn('Geolocation failed or permission denied:', err.message);
        // Fallback to sample current coordinate
        setPickupInput('Current Location (Central Connaught Place)');
        setPickupLocation({ latitude: 28.6315, longitude: 77.2167, address: 'Connaught Place, New Delhi' });
        setLocatingCurrent(false);
      },
      { timeout: 8000 }
    );
  };

  // Main Ride Fare Comparison Search
  const fetchRides = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      // 1. Fetch Route Telemetry
      const routeData = await estimateRoute(pickupLocation, dropLocation);
      setDistanceKm(routeData.distanceKm);
      setDurationMinutes(routeData.durationMinutes);
      setPolylineCoordinates(routeData.polylineCoordinates || []);

      // 2. Fetch Fare Comparison
      const data = await compareRides({
        pickup: pickupLocation,
        destination: dropLocation,
        rideType: vehicleCategory,
        sortBy: sortBy,
      });

      setRides(data.offers || []);
      setCheapestFare(data.cheapestFare);
      setFastestEta(data.fastestEtaMinutes);
      setBestProvider(data.bestProvider);
      setAiRecommendation(data.aiRecommendation || null);
      setRankingSummary(data.rankingSummary || null);
    } catch (err) {
      console.error('Ride comparison failed:', err);
      setError(err.message || 'Unable to fetch fare estimates from ride providers');
    } finally {
      setLoading(false);
    }
  }, [pickupLocation, dropLocation, vehicleCategory, sortBy]);

  useEffect(() => {
    fetchRides();
  }, [fetchRides]);

  return (
    <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8 space-y-8">
      
      {/* Header */}
      <div>
        <div className="flex items-center gap-2 text-xs text-slate-500 mb-2">
          <span>Home</span>
          <span>/</span>
          <span className="text-indigo-600 font-medium">Rides Engine</span>
        </div>
        <div className="flex flex-col md:flex-row md:items-center justify-between gap-4">
          <div>
            <h1 className="text-3xl sm:text-4xl font-extrabold text-slate-900 tracking-tight">
              Ride Fare & Cab Comparison
            </h1>
            <p className="text-sm text-slate-500 mt-1">
              Real-time fare and ETA comparison across Uber, Ola, and Rapido with official deep-link app booking.
            </p>
          </div>

          {/* Quick Insights Pills */}
          <div className="flex items-center gap-2 flex-wrap">
            {cheapestFare && (
              <div className="bg-emerald-50 border border-emerald-200 px-3.5 py-2 rounded-xl text-left">
                <span className="text-[10px] font-bold text-emerald-800 uppercase block tracking-wider">Cheapest Fare</span>
                <span className="text-sm font-black text-slate-900 font-mono">₹{Number(cheapestFare).toLocaleString('en-IN')}</span>
              </div>
            )}
            {fastestEta && (
              <div className="bg-indigo-50 border border-indigo-200 px-3.5 py-2 rounded-xl text-left">
                <span className="text-[10px] font-bold text-indigo-800 uppercase block tracking-wider">Fastest Pickup</span>
                <span className="text-sm font-black text-slate-900 font-mono">{fastestEta} mins away</span>
              </div>
            )}
            {bestProvider && (
              <div className="bg-amber-50 border border-amber-200 px-3.5 py-2 rounded-xl text-left">
                <span className="text-[10px] font-bold text-amber-900 uppercase block tracking-wider">Best Value</span>
                <span className="text-sm font-black text-slate-900">{bestProvider}</span>
              </div>
            )}
          </div>
        </div>
      </div>

      {/* Location Input Bar */}
      <div className="bg-white rounded-3xl p-6 border border-slate-200 shadow-md">
        <div className="grid grid-cols-1 lg:grid-cols-12 gap-4 items-center">
          
          {/* Pickup Input */}
          <div className="lg:col-span-5 relative">
            <div className="flex justify-between items-center mb-1">
              <label className="text-[11px] font-bold uppercase tracking-wider text-slate-500">Pickup Location</label>
              <button
                type="button"
                onClick={handleUseCurrentLocation}
                disabled={locatingCurrent}
                className="text-[11px] font-bold text-indigo-600 hover:text-indigo-800 flex items-center gap-1 cursor-pointer disabled:opacity-50"
              >
                <span>📍</span>
                <span>{locatingCurrent ? 'Detecting...' : 'Use Current Location'}</span>
              </button>
            </div>
            <div className="relative">
              <span className="absolute inset-y-0 left-0 pl-3.5 flex items-center text-emerald-500 font-bold">🟢</span>
              <input
                type="text"
                value={pickupInput}
                onChange={(e) => handlePickupChange(e.target.value)}
                placeholder="Enter pickup address..."
                className="w-full pl-10 pr-4 py-3 bg-slate-50 border border-slate-200 rounded-2xl text-sm font-bold text-slate-900 focus:outline-hidden focus:border-indigo-500"
              />
            </div>

            {/* Pickup Suggestions Dropdown */}
            {showPickupDropdown && pickupSuggestions.length > 0 && (
              <div className="absolute left-0 right-0 mt-1.5 bg-white rounded-2xl border border-slate-200 shadow-xl z-50 overflow-hidden divide-y divide-slate-100">
                {pickupSuggestions.map((s) => (
                  <button
                    key={s.placeId}
                    type="button"
                    onClick={() => handleSelectPickup(s)}
                    className="w-full p-3 text-left hover:bg-slate-50 flex items-start gap-2.5 transition cursor-pointer"
                  >
                    <span className="text-slate-400 mt-0.5">📍</span>
                    <div>
                      <p className="text-xs font-bold text-slate-900">{s.mainText}</p>
                      <p className="text-[11px] text-slate-400 truncate">{s.secondaryText}</p>
                    </div>
                  </button>
                ))}
              </div>
            )}
          </div>

          {/* Dropoff Input */}
          <div className="lg:col-span-5 relative">
            <label className="block text-[11px] font-bold uppercase tracking-wider text-slate-500 mb-1">Destination</label>
            <div className="relative">
              <span className="absolute inset-y-0 left-0 pl-3.5 flex items-center text-rose-500 font-bold">🏁</span>
              <input
                type="text"
                value={dropInput}
                onChange={(e) => handleDropChange(e.target.value)}
                placeholder="Enter drop destination..."
                className="w-full pl-10 pr-4 py-3 bg-slate-50 border border-slate-200 rounded-2xl text-sm font-bold text-slate-900 focus:outline-hidden focus:border-indigo-500"
              />
            </div>

            {/* Drop Suggestions Dropdown */}
            {showDropDropdown && dropSuggestions.length > 0 && (
              <div className="absolute left-0 right-0 mt-1.5 bg-white rounded-2xl border border-slate-200 shadow-xl z-50 overflow-hidden divide-y divide-slate-100">
                {dropSuggestions.map((s) => (
                  <button
                    key={s.placeId}
                    type="button"
                    onClick={() => handleSelectDrop(s)}
                    className="w-full p-3 text-left hover:bg-slate-50 flex items-start gap-2.5 transition cursor-pointer"
                  >
                    <span className="text-slate-400 mt-0.5">🏁</span>
                    <div>
                      <p className="text-xs font-bold text-slate-900">{s.mainText}</p>
                      <p className="text-[11px] text-slate-400 truncate">{s.secondaryText}</p>
                    </div>
                  </button>
                ))}
              </div>
            )}
          </div>

          {/* Compare Rides Button */}
          <div className="lg:col-span-2 pt-4 lg:pt-5">
            <button
              onClick={fetchRides}
              disabled={loading}
              className="w-full py-3 bg-indigo-600 hover:bg-indigo-700 disabled:opacity-50 text-white font-bold text-sm rounded-2xl shadow-xs transition flex items-center justify-center gap-2 cursor-pointer"
            >
              <span>Compare Fares</span>
              <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M14 5l7 7m0 0l-7 7m7-7H3" />
              </svg>
            </button>
          </div>

        </div>
      </div>

      {/* Main Grid: Interactive Map + Ride Feed */}
      <div className="grid grid-cols-1 lg:grid-cols-12 gap-8 items-start">
        
        {/* Left Interactive Map Section (5 cols) */}
        <div className="lg:col-span-5 space-y-4">
          <div className="bg-white rounded-3xl p-3 border border-slate-200 shadow-xs">
            <InteractiveMap
              pickup={pickupLocation}
              destination={dropLocation}
              distanceKm={distanceKm}
              durationMinutes={durationMinutes}
              polylineCoordinates={polylineCoordinates}
            />
          </div>

          {/* Route Summary Card */}
          <div className="bg-white rounded-2xl p-4 border border-slate-200 shadow-xs space-y-3">
            <h4 className="text-xs font-bold uppercase tracking-wider text-slate-400">Route Telemetry</h4>
            <div className="space-y-2">
              <div className="flex items-start gap-2.5 text-xs">
                <span className="text-emerald-500 font-bold">🟢</span>
                <span className="text-slate-800 font-semibold">{pickupLocation.address}</span>
              </div>
              <div className="flex items-start gap-2.5 text-xs">
                <span className="text-rose-500 font-bold">🏁</span>
                <span className="text-slate-800 font-semibold">{dropLocation.address}</span>
              </div>
            </div>
          </div>
        </div>

        {/* Right Fare Options Feed (7 cols) */}
        <div className="lg:col-span-7 space-y-6">
          
          {/* Controls: Vehicle Filter Tabs & Sort Strategy */}
          <div className="bg-white p-3 rounded-2xl border border-slate-200 shadow-xs flex flex-col sm:flex-row items-center justify-between gap-3">
            
            {/* Vehicle Type Tabs */}
            <div className="flex bg-slate-100 p-1 rounded-xl w-full sm:w-auto text-xs font-bold">
              {[
                { id: 'all', label: 'All Rides', icon: '🚗' },
                { id: 'cab', label: 'Cabs', icon: '🚕' },
                { id: 'auto', label: 'Autos', icon: '🛺' },
                { id: 'bike', label: 'Bikes', icon: '🛵' },
              ].map((tab) => (
                <button
                  key={tab.id}
                  onClick={() => setVehicleCategory(tab.id)}
                  className={`px-3 py-1.5 rounded-lg transition flex items-center gap-1 cursor-pointer ${
                    vehicleCategory === tab.id
                      ? 'bg-white text-slate-900 shadow-xs'
                      : 'text-slate-500 hover:text-slate-900'
                  }`}
                >
                  <span>{tab.icon}</span>
                  <span>{tab.label}</span>
                </button>
              ))}
            </div>

            {/* Sort Dropdown */}
            <div className="flex items-center gap-2 w-full sm:w-auto justify-end">
              <label className="text-xs font-bold uppercase tracking-wider text-slate-400 shrink-0">
                Sort:
              </label>
              <select
                value={sortBy}
                onChange={(e) => setSortBy(e.target.value)}
                className="bg-slate-50 border border-slate-200 text-xs font-bold text-slate-800 rounded-xl px-3 py-1.5 focus:outline-hidden focus:border-indigo-500 cursor-pointer"
              >
                <option value="best">⭐ Best Value</option>
                <option value="cheapest">💰 Cheapest Fare</option>
                <option value="fastest">⚡ Fastest Pickup (ETA)</option>
              </select>
            </div>

          </div>

          {/* Cost vs Time Optimization Slider */}
          <CostTimeSlider
            costWeight={costWeight}
            onChange={(w) => setCostWeight(w)}
            compact={false}
          />

          {/* Ride Options Feed */}
          {loading ? (
            <LoadingSkeleton type="product-card" count={3} />
          ) : error ? (
            <ErrorState
              title="Ride Comparison Engine Error"
              message={error}
              onRetry={fetchRides}
            />
          ) : rides.length > 0 ? (() => {
            const sortedRides = rankCostTimeClientSide(
              rides,
              costWeight,
              (r) => r.estimatedPriceMin || r.estimatedPriceMax || 0,
              (r) => r.etaMinutes || 0
            );

            return (
            <div className="space-y-4">
              {rankingSummary && (
                <RankingExplanationBanner rankingSummary={rankingSummary} type="ride" />
              )}
              {aiRecommendation && (
                <AiRecommendationCard recommendation={aiRecommendation} />
              )}
              {sortedRides.map((ride, idx) => (
                <RideOfferCard
                  key={`${ride.provider}-${ride.rideType}-${idx}`}
                  ride={ride}
                />
              ))}
            </div>
            );
          })() : (
            <EmptyState
              title="No ride options available"
              description="No ride options match your selected vehicle filter. Try switching to 'All Rides'."
              actionLabel="Show All Rides"
              onAction={() => setVehicleCategory('all')}
            />
          )}

        </div>

      </div>

    </div>
  );
};

export default RidesPage;
