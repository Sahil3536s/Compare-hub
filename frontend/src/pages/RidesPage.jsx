import React, { useState, useEffect, useCallback } from 'react';
import { geocodeAddress, reverseGeocode, estimateRoute } from '../services/locationService';
import { compareRides } from '../services/rideService';
import InteractiveMap from '../components/InteractiveMap';
import LocationAutocomplete from '../components/LocationAutocomplete';
import RideOfferCard from '../components/RideOfferCard';
import RideCardSkeleton from '../components/RideCardSkeleton';
import RideComparisonModal from '../components/rides/RideComparisonModal';
import AiRecommendationCard from '../components/AiRecommendationCard';
import RankingExplanationBanner from '../components/RankingExplanationBanner';
import ErrorState from '../components/ErrorState';
import { recordSearchHistory } from '../services/historyService';

// Optional route suggestion chips (non-authoritative presets)
export const SUGGESTED_ROUTES = [
  {
    name: 'Bhopal Route',
    pickupText: 'Bhopal Railway Station',
    dropText: 'Bhopal Airport',
    pickup: {
      name: 'Bhopal Railway Station',
      formattedAddress: 'Hamidia Road, Bhopal, Madhya Pradesh 462001',
      latitude: 23.2599,
      longitude: 77.4126,
      city: 'Bhopal',
      state: 'Madhya Pradesh',
      country: 'India',
      providerPlaceId: 'bho-railway',
    },
    drop: {
      name: 'Raja Bhoj Airport',
      formattedAddress: 'Airport Road, Gandhi Nagar, Bhopal, Madhya Pradesh 462036',
      latitude: 23.2875,
      longitude: 77.3378,
      city: 'Bhopal',
      state: 'Madhya Pradesh',
      country: 'India',
      providerPlaceId: 'bho-airport',
    },
    pLat: 23.2599,
    pLon: 77.4126,
    dLat: 23.2875,
    dLon: 77.3378,
  },
  {
    name: 'VIT Bhopal to Sehore',
    pickupText: 'VIT Bhopal University',
    dropText: 'Sehore',
    pickup: {
      name: 'VIT Bhopal University',
      formattedAddress: 'Bhopal-Indore Highway, Kothri Kalan, Sehore, Madhya Pradesh 466114',
      latitude: 23.0768,
      longitude: 76.8524,
      city: 'Sehore',
      state: 'Madhya Pradesh',
      country: 'India',
      providerPlaceId: 'vit-bhopal',
    },
    drop: {
      name: 'Sehore Railway Station',
      formattedAddress: 'Station Road, Sehore, Madhya Pradesh 466001',
      latitude: 23.2032,
      longitude: 77.0844,
      city: 'Sehore',
      state: 'Madhya Pradesh',
      country: 'India',
      providerPlaceId: 'sehore-station',
    },
    pLat: 23.0768,
    pLon: 76.8524,
    dLat: 23.2032,
    dLon: 77.0844,
  },
  {
    name: 'Indore Route',
    pickupText: 'Indore Airport',
    dropText: 'Rajwada Palace',
    pickup: {
      name: 'Devi Ahilyabai Holkar Airport',
      formattedAddress: 'Depalpur Road, Indore, Madhya Pradesh 452005',
      latitude: 22.7218,
      longitude: 75.8011,
      city: 'Indore',
      state: 'Madhya Pradesh',
      country: 'India',
      providerPlaceId: 'idr-airport',
    },
    drop: {
      name: 'Rajwada Palace',
      formattedAddress: 'MG Road, Rajwada, Indore, Madhya Pradesh 452002',
      latitude: 22.7186,
      longitude: 75.8554,
      city: 'Indore',
      state: 'Madhya Pradesh',
      country: 'India',
      providerPlaceId: 'idr-rajwada',
    },
    pLat: 22.7218,
    pLon: 75.8011,
    dLat: 22.7186,
    dLon: 75.8554,
  },
  {
    name: 'Delhi NCR Route',
    pickupText: 'New Delhi Railway Station',
    dropText: 'India Gate',
    pickup: {
      name: 'New Delhi Railway Station',
      formattedAddress: 'Bhavbhuti Marg, Kamla Market, New Delhi 110006',
      latitude: 28.6429,
      longitude: 77.2195,
      city: 'New Delhi',
      state: 'Delhi',
      country: 'India',
      providerPlaceId: 'del-ndls',
    },
    drop: {
      name: 'India Gate',
      formattedAddress: 'Kartavya Path, India Gate, New Delhi 110001',
      latitude: 28.6129,
      longitude: 77.2295,
      city: 'New Delhi',
      state: 'Delhi',
      country: 'India',
      providerPlaceId: 'del-india-gate',
    },
    pLat: 28.6429,
    pLon: 77.2195,
    dLat: 28.6129,
    dLon: 77.2295,
  },
  {
    name: 'Mumbai Route',
    pickupText: 'Mumbai Airport (BOM)',
    dropText: 'Gateway of India',
    pickup: {
      name: 'Chhatrapati Shivaji Maharaj International Airport',
      formattedAddress: 'Navpada, Vile Parle East, Mumbai, Maharashtra 400099',
      latitude: 19.0896,
      longitude: 72.8656,
      city: 'Mumbai',
      state: 'Maharashtra',
      country: 'India',
      providerPlaceId: 'bom-airport',
    },
    drop: {
      name: 'Gateway of India',
      formattedAddress: 'Apollo Bandar, Colaba, Mumbai, Maharashtra 400001',
      latitude: 18.9220,
      longitude: 72.8347,
      city: 'Mumbai',
      state: 'Maharashtra',
      country: 'India',
      providerPlaceId: 'bom-gateway',
    },
    pLat: 19.0896,
    pLon: 72.8656,
    dLat: 18.9220,
    dLon: 72.8347,
  },
];
export const POPULAR_ROUTES = SUGGESTED_ROUTES;

export const RidesPage = () => {
  // Location text states
  const [pickupInput, setPickupInput] = useState('Bhopal Railway Station');
  const [dropInput, setDropInput] = useState('Bhopal Airport');

  // Resolved coordinate locations
  const [pickupLocation, setPickupLocation] = useState({
    name: 'Bhopal Railway Station',
    latitude: 23.2599,
    longitude: 77.4126,
    address: 'Bhopal Railway Station, Hamidia Road, Bhopal, Madhya Pradesh 462001',
    formattedAddress: 'Bhopal Railway Station, Hamidia Road, Bhopal, Madhya Pradesh 462001',
    city: 'Bhopal',
    state: 'Madhya Pradesh',
    country: 'India',
    providerPlaceId: 'bho-railway',
  });

  const [dropLocation, setDropLocation] = useState({
    name: 'Raja Bhoj Airport',
    latitude: 23.2875,
    longitude: 77.3378,
    address: 'Raja Bhoj Airport, Airport Road, Gandhi Nagar, Bhopal, Madhya Pradesh 462036',
    formattedAddress: 'Raja Bhoj Airport, Airport Road, Gandhi Nagar, Bhopal, Madhya Pradesh 462036',
    city: 'Bhopal',
    state: 'Madhya Pradesh',
    country: 'India',
    providerPlaceId: 'bho-airport',
  });

  // Filters & Ranking
  const [vehicleCategory, setVehicleCategory] = useState('all'); // 'all', 'cab', 'auto', 'bike'
  const [sortBy, setSortBy] = useState('best'); // 'best', 'cheapest', 'fastest'

  // Route telemetry & Ride data
  const [distanceKm, setDistanceKm] = useState(12.4);
  const [durationMinutes, setDurationMinutes] = useState(28);
  const [polylineCoordinates, setPolylineCoordinates] = useState([]);
  const [rides, setRides] = useState([]);
  const [cheapestFare, setCheapestFare] = useState(null);
  const [fastestEta, setFastestEta] = useState(null);
  const [bestProvider, setBestProvider] = useState(null);
  const [failedProviders, setFailedProviders] = useState([]);
  const [aiRecommendation, setAiRecommendation] = useState(null);
  const [rankingSummary, setRankingSummary] = useState(null);

  // UI & Lifecycle States: 'IDLE' | 'LOADING' | 'SUCCESS' | 'EMPTY' | 'PARTIAL_SUCCESS' | 'ERROR'
  const [pageState, setPageState] = useState('LOADING');
  const [validationError, setValidationError] = useState('');
  const [locatingCurrent, setLocatingCurrent] = useState(false);
  const [mapError, setMapError] = useState(false);
  const [isComparisonModalOpen, setIsComparisonModalOpen] = useState(false);
  const [errorMessage, setErrorMessage] = useState('');

  // Validation function
  const validateSearch = () => {
    const pTrim = (pickupInput || '').trim();
    const dTrim = (dropInput || '').trim();

    if (!pTrim) {
      return 'Pickup location is required.';
    }
    if (!dTrim) {
      return 'Destination location is required.';
    }
    if (pTrim.toLowerCase() === dTrim.toLowerCase()) {
      return 'Pickup and destination cannot be the same location.';
    }
    return null;
  };

  const handleSelectPickup = (loc) => {
    const label = loc.name || loc.mainText || loc.formattedAddress || pickupInput;
    setPickupInput(label);
    setPickupLocation({
      name: label,
      formattedAddress: loc.formattedAddress || loc.fullAddress || loc.address || label,
      address: loc.formattedAddress || loc.fullAddress || loc.address || label,
      latitude: loc.latitude,
      longitude: loc.longitude,
      city: loc.city || 'City',
      state: loc.state || '',
      country: loc.country || 'India',
      providerPlaceId: loc.providerPlaceId || loc.placeId || '',
    });
    setValidationError('');
  };

  const handleSelectDrop = (loc) => {
    const label = loc.name || loc.mainText || loc.formattedAddress || dropInput;
    setDropInput(label);
    setDropLocation({
      name: label,
      formattedAddress: loc.formattedAddress || loc.fullAddress || loc.address || label,
      address: loc.formattedAddress || loc.fullAddress || loc.address || label,
      latitude: loc.latitude,
      longitude: loc.longitude,
      city: loc.city || 'City',
      state: loc.state || '',
      country: loc.country || 'India',
      providerPlaceId: loc.providerPlaceId || loc.placeId || '',
    });
    setValidationError('');
  };

  const handleSwapLocations = () => {
    const tempInput = pickupInput;
    const tempLoc = pickupLocation;
    setPickupInput(dropInput);
    setPickupLocation(dropLocation);
    setDropInput(tempInput);
    setDropLocation(tempLoc);
    setValidationError('');
  };

  const handleSelectRoutePreset = (preset) => {
    setPickupInput(preset.pickupText);
    setDropInput(preset.dropText);
    setPickupLocation(
      preset.pickup || {
        name: preset.pickupText,
        latitude: preset.pLat,
        longitude: preset.pLon,
        address: preset.pickup,
        formattedAddress: preset.pickup,
        city: preset.name.split(' ')[0],
      }
    );
    setDropLocation(
      preset.drop || {
        name: preset.dropText,
        latitude: preset.dLat,
        longitude: preset.dLon,
        address: preset.drop,
        formattedAddress: preset.drop,
        city: preset.name.split(' ')[0],
      }
    );
    setValidationError('');
  };

  const handleUseCurrentLocation = () => {
    if (!navigator.geolocation) {
      setValidationError('Geolocation is not supported by your browser.');
      return;
    }

    setLocatingCurrent(true);
    navigator.geolocation.getCurrentPosition(
      async (position) => {
        const lat = position.coords.latitude;
        const lon = position.coords.longitude;
        try {
          const loc = await reverseGeocode(lat, lon);
          const name = loc.name || (loc.address ? loc.address.split(',')[0].trim() : 'Current Location');
          setPickupInput(name);
          setPickupLocation({
            name,
            latitude: loc.latitude || lat,
            longitude: loc.longitude || lon,
            address: loc.formattedAddress || loc.address || `GPS Location (${lat.toFixed(4)}, ${lon.toFixed(4)})`,
            formattedAddress: loc.formattedAddress || loc.address || `GPS Location (${lat.toFixed(4)}, ${lon.toFixed(4)})`,
            city: loc.city || 'Current Area',
            state: loc.state || '',
            country: loc.country || 'India',
            providerPlaceId: loc.providerPlaceId || '',
          });
        } catch (e) {
          const fallbackName = `Current Location (${lat.toFixed(4)}, ${lon.toFixed(4)})`;
          setPickupInput(fallbackName);
          setPickupLocation({
            name: fallbackName,
            latitude: lat,
            longitude: lon,
            address: fallbackName,
            formattedAddress: fallbackName,
            city: 'Current Area',
            state: '',
            country: 'India',
          });
        } finally {
          setLocatingCurrent(false);
        }
      },
      (err) => {
        console.warn('Geolocation failed or permission denied:', err.message);
        setValidationError('Location access was denied or timed out. Please enter pickup address manually.');
        setLocatingCurrent(false);
      },
      { timeout: 8000 }
    );
  };

  // Main Fare & Route Fetch
  const executeRideComparison = useCallback(async () => {
    const error = validateSearch();
    if (error) {
      setValidationError(error);
      return;
    }

    setValidationError('');
    setPageState('LOADING');
    setErrorMessage('');
    setMapError(false);

    try {
      let currentPickup = pickupLocation;
      let currentDrop = dropLocation;

      // Dynamic geocoding fallback if user typed freeform address without clicking autocomplete
      if (!currentPickup?.latitude && pickupInput) {
        try {
          const geo = await geocodeAddress(pickupInput);
          if (geo && typeof geo.latitude === 'number') {
            currentPickup = {
              name: geo.name || pickupInput,
              formattedAddress: geo.formattedAddress || geo.address || pickupInput,
              address: geo.address || geo.formattedAddress || pickupInput,
              latitude: geo.latitude,
              longitude: geo.longitude,
              city: geo.city || '',
              state: geo.state || '',
              country: geo.country || 'India',
              providerPlaceId: geo.providerPlaceId || '',
            };
            setPickupLocation(currentPickup);
          }
        } catch (e) {
          console.warn('Pickup geocoding lookup failed:', e);
        }
      }

      if (!currentDrop?.latitude && dropInput) {
        try {
          const geo = await geocodeAddress(dropInput);
          if (geo && typeof geo.latitude === 'number') {
            currentDrop = {
              name: geo.name || dropInput,
              formattedAddress: geo.formattedAddress || geo.address || dropInput,
              address: geo.address || geo.formattedAddress || dropInput,
              latitude: geo.latitude,
              longitude: geo.longitude,
              city: geo.city || '',
              state: geo.state || '',
              country: geo.country || 'India',
              providerPlaceId: geo.providerPlaceId || '',
            };
            setDropLocation(currentDrop);
          }
        } catch (e) {
          console.warn('Destination geocoding lookup failed:', e);
        }
      }

      if (!currentPickup || typeof currentPickup.latitude !== 'number' || typeof currentPickup.longitude !== 'number') {
        setValidationError('Please select a valid pickup location from the suggestions.');
        setPageState('IDLE');
        return;
      }

      if (!currentDrop || typeof currentDrop.latitude !== 'number' || typeof currentDrop.longitude !== 'number') {
        setValidationError('Please select a valid destination location from the suggestions.');
        setPageState('IDLE');
        return;
      }

      // 1. Fetch Route Telemetry (Graceful failure handled)
      try {
        const routeData = await estimateRoute(currentPickup, currentDrop);
        setDistanceKm(routeData.distanceKm || 12.4);
        setDurationMinutes(routeData.durationMinutes || 28);
        setPolylineCoordinates(routeData.polylineCoordinates || []);
      } catch (mapErr) {
        console.warn('Route estimation encountered an issue; continuing with fallback telemetry:', mapErr);
        setMapError(true);
      }

      // 2. Query Ride Providers
      const data = await compareRides({
        pickup: currentPickup,
        destination: currentDrop,
        rideType: vehicleCategory,
        sortBy: sortBy,
      });

      const offers = data.offers || [];
      const failed = data.failedProviders || [];
      setRides(offers);
      setCheapestFare(data.cheapestFare);
      setFastestEta(data.fastestEtaMinutes);
      setBestProvider(data.bestProvider);
      setFailedProviders(failed);
      setAiRecommendation(data.aiRecommendation || null);
      setRankingSummary(data.rankingSummary || null);

      recordSearchHistory(
        `${pickupInput || 'Origin'} → ${dropInput || 'Destination'}`,
        'RIDES',
        `Vehicle: ${vehicleCategory} • Sort: ${sortBy}`,
        `/rides`
      );

      if (offers.length === 0) {
        setPageState('EMPTY');
      } else if (failed.length > 0) {
        setPageState('PARTIAL_SUCCESS');
      } else {
        setPageState('SUCCESS');
      }
    } catch (err) {
      console.error('Ride comparison search failed:', err);
      setErrorMessage(
        err.message || 'Unable to connect to ride providers. Please check your network and try again.'
      );
      setPageState('ERROR');
    }
  }, [pickupLocation, dropLocation, vehicleCategory, sortBy, pickupInput, dropInput]);

  useEffect(() => {
    if (pickupLocation?.latitude && dropLocation?.latitude) {
      executeRideComparison();
    }
  }, [pickupLocation, dropLocation, vehicleCategory, sortBy]);

  const handleFormSubmit = (e) => {
    e.preventDefault();
    executeRideComparison();
  };

  return (
    <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8 space-y-8">
      {/* Header & Quick Insights */}
      <div>
        <div className="flex items-center gap-2 text-xs text-slate-500 mb-2">
          <span>Home</span>
          <span>/</span>
          <span className="text-indigo-600 font-semibold">Rides Engine</span>
        </div>
        <div className="flex flex-col md:flex-row md:items-center justify-between gap-4">
          <div>
            <h1 className="text-3xl sm:text-4xl font-extrabold text-slate-900 tracking-tight">
              Ride Fare & Cab Comparison
            </h1>
            <p className="text-sm text-slate-500 mt-1">
              Compare transparent fare estimates and pickup ETAs across Uber, Ola, and Rapido with official booking links.
            </p>
          </div>

          {/* Quick Insights Badges */}
          {pageState !== 'LOADING' && pageState !== 'ERROR' && (
            <div className="flex items-center gap-2 flex-wrap">
              {cheapestFare && (
                <div className="bg-emerald-50 border border-emerald-200 px-3.5 py-2 rounded-xl text-left shadow-2xs">
                  <span className="text-[10px] font-bold text-emerald-800 uppercase block tracking-wider">
                    Cheapest Fare
                  </span>
                  <span className="text-sm font-black text-slate-900 font-mono">
                    ₹{Number(cheapestFare).toLocaleString('en-IN')}
                  </span>
                </div>
              )}
              {fastestEta && (
                <div className="bg-indigo-50 border border-indigo-200 px-3.5 py-2 rounded-xl text-left shadow-2xs">
                  <span className="text-[10px] font-bold text-indigo-800 uppercase block tracking-wider">
                    Fastest Pickup
                  </span>
                  <span className="text-sm font-black text-slate-900 font-mono">
                    {fastestEta} mins away
                  </span>
                </div>
              )}
              {bestProvider && (
                <div className="bg-amber-50 border border-amber-200 px-3.5 py-2 rounded-xl text-left shadow-2xs">
                  <span className="text-[10px] font-bold text-amber-900 uppercase block tracking-wider">
                    Top Value Ride
                  </span>
                  <span className="text-sm font-black text-slate-900">{bestProvider}</span>
                </div>
              )}
            </div>
          )}
        </div>
      </div>

      {/* Premium Search Card */}
      <div className="bg-white rounded-3xl p-6 sm:p-8 border border-slate-200 shadow-md space-y-4">
        {/* Validation Notice Banner */}
        {validationError && (
          <div
            role="alert"
            className="p-4 bg-rose-50 border border-rose-200 text-rose-800 rounded-2xl flex items-center justify-between text-xs sm:text-sm font-medium animate-fadeIn"
          >
            <div className="flex items-center gap-2">
              <span className="text-rose-500 font-bold text-base">⚠️</span>
              <span>{validationError}</span>
            </div>
            <button
              type="button"
              onClick={() => setValidationError('')}
              className="text-rose-600 hover:text-rose-900 font-bold text-xs ml-4 cursor-pointer"
            >
              Dismiss
            </button>
          </div>
        )}

        <form onSubmit={handleFormSubmit} className="space-y-4">
          <div className="grid grid-cols-1 lg:grid-cols-12 gap-3 items-center">
            {/* Pickup Location Autocomplete */}
            <div className="lg:col-span-5 relative">
              <div className="flex justify-between items-center mb-1">
                <label
                  htmlFor="pickup-location"
                  className="text-[11px] font-bold uppercase tracking-wider text-slate-500"
                >
                  Pickup Location
                </label>
                <button
                  type="button"
                  onClick={handleUseCurrentLocation}
                  disabled={locatingCurrent}
                  className="text-[11px] font-bold text-indigo-600 hover:text-indigo-800 flex items-center gap-1 cursor-pointer disabled:opacity-50 transition"
                >
                  <span>📍</span>
                  <span>{locatingCurrent ? 'Detecting...' : 'Use Current Location'}</span>
                </button>
              </div>

              <LocationAutocomplete
                id="pickup-location"
                aria-label="Pickup Location"
                value={pickupInput}
                onChange={(val) => {
                  setPickupInput(val);
                  setPickupLocation((prev) => (prev && (prev.name === val || prev.formattedAddress === val) ? prev : null));
                  setValidationError('');
                }}
                onSelect={handleSelectPickup}
                placeholder="Enter pickup address, university, station, airport..."
                icon="🟢"
                required
              />
            </div>

            {/* Swap Button */}
            <div className="lg:col-span-1 flex justify-center pt-2 sm:pt-4 lg:pt-5">
              <button
                type="button"
                onClick={handleSwapLocations}
                title="Swap pickup and destination"
                className="w-10 h-10 rounded-full border border-slate-200 bg-white hover:bg-slate-50 text-indigo-600 hover:text-indigo-800 flex items-center justify-center font-bold text-base shadow-2xs transition transform hover:rotate-180 cursor-pointer"
                aria-label="Swap pickup and destination"
              >
                ⇄
              </button>
            </div>

            {/* Destination Location Autocomplete */}
            <div className="lg:col-span-4 relative">
              <label
                htmlFor="destination-location"
                className="block text-[11px] font-bold uppercase tracking-wider text-slate-500 mb-1"
              >
                Destination
              </label>

              <LocationAutocomplete
                id="destination-location"
                aria-label="Destination"
                value={dropInput}
                onChange={(val) => {
                  setDropInput(val);
                  setDropLocation((prev) => (prev && (prev.name === val || prev.formattedAddress === val) ? prev : null));
                  setValidationError('');
                }}
                onSelect={handleSelectDrop}
                placeholder="Enter destination, landmark, city, palace..."
                icon="🏁"
                required
              />
            </div>

            {/* Compare Rides Button */}
            <div className="lg:col-span-2 pt-2 sm:pt-4 lg:pt-5">
              <button
                type="submit"
                disabled={pageState === 'LOADING'}
                className="w-full py-2.5 bg-indigo-600 hover:bg-indigo-700 disabled:opacity-50 text-white font-extrabold text-sm rounded-xl shadow-xs transition flex items-center justify-center gap-2 cursor-pointer"
              >
                {pageState === 'LOADING' ? (
                  <>
                    <div className="w-4 h-4 border-2 border-white border-t-transparent rounded-full animate-spin"></div>
                    <span>Finding...</span>
                  </>
                ) : (
                  <>
                    <span>Compare Rides</span>
                    <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                      <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2.5" d="M14 5l7 7m0 0l-7 7m7-7H3" />
                    </svg>
                  </>
                )}
              </button>
            </div>
          </div>

          {/* Quick Route Preset Chips */}
          <div className="pt-3 border-t border-slate-100 flex items-center gap-2 flex-wrap text-xs">
            <span className="text-slate-400 font-medium">Quick Routes:</span>
            {SUGGESTED_ROUTES.map((route) => (
              <button
                key={route.name}
                type="button"
                onClick={() => handleSelectRoutePreset(route)}
                className="px-2.5 py-1 rounded-lg bg-slate-100 hover:bg-slate-200 text-slate-700 font-semibold cursor-pointer transition"
              >
                {route.name} ({route.pickupText} → {route.dropText})
              </button>
            ))}
          </div>
        </form>
      </div>

      {/* Main Grid: LEFT (Map + Route Progression) vs RIGHT (Controls + Results Feed) */}
      <div className="grid grid-cols-1 lg:grid-cols-12 gap-8 items-start">
        {/* Left Section: Map & Route Progression (5 cols) */}
        <div className="lg:col-span-5 space-y-4 lg:sticky lg:top-24">
          {/* Interactive Map */}
          <div className="bg-white rounded-3xl p-3 border border-slate-200 shadow-xs">
            <InteractiveMap
              pickup={pickupLocation}
              destination={dropLocation}
              distanceKm={distanceKm}
              durationMinutes={durationMinutes}
              polylineCoordinates={polylineCoordinates}
              hasError={mapError}
            />
          </div>

          {/* Route Information Progression Card */}
          <div
            data-testid="route-progression-card"
            className="bg-white rounded-2xl p-5 border border-slate-200 shadow-xs space-y-3"
          >
            <div className="flex items-center justify-between">
              <h4 className="text-xs font-bold uppercase tracking-wider text-slate-400">
                Route Information
              </h4>
              <span className="text-[11px] font-semibold text-emerald-600 bg-emerald-50 px-2 py-0.5 rounded-full">
                Fastest Road Path
              </span>
            </div>

            {/* Progression Visualization */}
            <div className="flex flex-col items-center text-center space-y-2 py-1">
              <div className="w-full bg-slate-50 border border-slate-100 rounded-xl p-3 text-left">
                <span className="text-[10px] font-bold text-slate-400 uppercase tracking-wider block">
                  Pickup Location
                </span>
                <span className="text-xs sm:text-sm font-extrabold text-slate-900 flex items-center gap-1.5 mt-0.5">
                  <span className="text-emerald-500">🟢</span>
                  <span className="truncate">{pickupInput}</span>
                </span>
              </div>

              {/* Middle Distance & Time Indicator */}
              <div className="flex flex-col items-center text-xs font-bold text-indigo-700 bg-indigo-50 border border-indigo-200 px-4 py-1.5 rounded-xl shadow-2xs">
                <span>↓</span>
                <span className="font-mono">{distanceKm} km</span>
                <span className="text-[11px] text-indigo-900 font-semibold">{durationMinutes} min travel</span>
                <span>↓</span>
              </div>

              <div className="w-full bg-slate-50 border border-slate-100 rounded-xl p-3 text-left">
                <span className="text-[10px] font-bold text-slate-400 uppercase tracking-wider block">
                  Destination
                </span>
                <span className="text-xs sm:text-sm font-extrabold text-slate-900 flex items-center gap-1.5 mt-0.5">
                  <span className="text-rose-500">🏁</span>
                  <span className="truncate">{dropInput}</span>
                </span>
              </div>
            </div>
          </div>
        </div>

        {/* Right Section: Results & Filter Controls (7 cols) */}
        <div className="lg:col-span-7 space-y-6">
          {/* Controls Bar: Vehicle Category Tabs & Ranking Tabs */}
          <div className="bg-white p-3 rounded-2xl border border-slate-200 shadow-xs flex flex-col sm:flex-row items-center justify-between gap-3">
            {/* Vehicle Type Filter Tabs */}
            <div className="flex bg-slate-100 p-1 rounded-xl w-full sm:w-auto text-xs font-bold">
              {[
                { id: 'all', label: 'All Rides', icon: '🚗' },
                { id: 'cab', label: 'Cabs', icon: '🚕' },
                { id: 'auto', label: 'Autos', icon: '🛺' },
                { id: 'bike', label: 'Bikes', icon: '🛵' },
              ].map((tab) => (
                <button
                  key={tab.id}
                  type="button"
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

            {/* Compare Matrix Button */}
            <button
              type="button"
              onClick={() => setIsComparisonModalOpen(true)}
              className="px-3.5 py-1.5 bg-slate-900 hover:bg-slate-800 text-white rounded-xl text-xs font-bold transition shadow-xs flex items-center gap-1.5 cursor-pointer whitespace-nowrap"
            >
              <span>📊</span>
              <span>Side-by-Side Matrix</span>
            </button>
          </div>

          {/* Ranking Tabs: Cheapest, Fastest Pickup, Best Value */}
          <div className="flex bg-white p-1.5 rounded-2xl border border-slate-200 shadow-xs text-xs font-bold">
            {[
              {
                id: 'best',
                label: '⭐ Best Value',
                sub: 'Weighted Fare & ETA',
              },
              {
                id: 'cheapest',
                label: '💰 Cheapest',
                sub: 'Lowest Estimated Fare',
              },
              {
                id: 'fastest',
                label: '⚡ Fastest Pickup',
                sub: 'Quickest Driver ETA',
              },
            ].map((tab) => (
              <button
                key={tab.id}
                type="button"
                onClick={() => setSortBy(tab.id)}
                className={`flex-1 py-2.5 px-3 rounded-xl transition text-center cursor-pointer ${
                  sortBy === tab.id
                    ? 'bg-indigo-600 text-white shadow-xs'
                    : 'text-slate-600 hover:text-slate-900 hover:bg-slate-50'
                }`}
              >
                <div className="font-extrabold">{tab.label}</div>
                <div
                  className={`text-[10px] font-normal ${
                    sortBy === tab.id ? 'text-indigo-100' : 'text-slate-400'
                  }`}
                >
                  {tab.sub}
                </div>
              </button>
            ))}
          </div>

          {/* Partial Failure Notice Banner */}
          {pageState === 'PARTIAL_SUCCESS' && failedProviders.length > 0 && (
            <div
              role="status"
              className="bg-amber-50 border border-amber-200 rounded-2xl p-4 flex items-start gap-3 text-xs text-amber-900 animate-fadeIn"
            >
              <span className="text-base">⚠️</span>
              <div>
                <h4 className="font-bold">Some providers couldn't be reached.</h4>
                <p className="mt-0.5 text-amber-800">
                  Live estimates from <strong className="font-semibold">{failedProviders.join(', ')}</strong> were temporarily unavailable. Displaying confirmed fares from our active ride partners.
                </p>
              </div>
            </div>
          )}

          {/* Dynamic Feed Rendering */}
          {pageState === 'LOADING' ? (
            <div className="space-y-4">
              <div className="bg-indigo-50/80 border border-indigo-100 rounded-2xl p-4 flex items-center gap-3 text-indigo-900 text-sm font-semibold">
                <div className="w-5 h-5 border-2 border-indigo-600 border-t-transparent rounded-full animate-spin"></div>
                <span>Finding available ride options...</span>
              </div>
              <RideCardSkeleton count={4} />
            </div>
          ) : pageState === 'ERROR' ? (
            <ErrorState
              title="Ride Comparison Engine Error"
              message={errorMessage}
              onRetry={executeRideComparison}
            />
          ) : pageState === 'EMPTY' ? (
            <div
              data-testid="provider-unavailable-card"
              className="bg-white rounded-3xl p-8 border border-slate-200 shadow-xs text-center space-y-4 animate-fadeIn"
            >
              <div className="w-14 h-14 mx-auto rounded-2xl bg-amber-50 border border-amber-200 flex items-center justify-center text-2xl shadow-2xs">
                📍
              </div>
              <div className="max-w-md mx-auto space-y-2">
                <h3 className="text-base sm:text-lg font-extrabold text-slate-900 leading-snug">
                  Location found, but ride estimates are currently unavailable from the connected providers.
                </h3>
                <p className="text-xs sm:text-sm text-slate-500">
                  No ride options available for this specific operating area or selected vehicle filter.
                  You can try switching vehicle types or picking another pickup/destination.
                </p>
              </div>
              <button
                type="button"
                onClick={() => setVehicleCategory('all')}
                className="px-4 py-2 bg-indigo-600 hover:bg-indigo-700 text-white rounded-xl text-xs font-bold transition shadow-xs cursor-pointer inline-flex items-center gap-1.5"
              >
                <span>🚗</span>
                <span>Show All Rides</span>
              </button>
            </div>
          ) : (
            <div className="space-y-4">
              {rankingSummary && (
                <RankingExplanationBanner rankingSummary={rankingSummary} type="ride" />
              )}
              {aiRecommendation && (
                <AiRecommendationCard recommendation={aiRecommendation} />
              )}
              {rides.map((ride, idx) => (
                <RideOfferCard
                  key={`${ride.provider}-${ride.rideType}-${idx}`}
                  ride={ride}
                />
              ))}
            </div>
          )}
        </div>
      </div>

      {/* Side-by-Side Comparison Matrix Modal */}
      <RideComparisonModal
        isOpen={isComparisonModalOpen}
        onClose={() => setIsComparisonModalOpen(false)}
        rides={rides}
        pickupAddress={pickupInput}
        destinationAddress={dropInput}
      />
    </div>
  );
};

export default RidesPage;
