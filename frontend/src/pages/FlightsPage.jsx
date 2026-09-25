import React, { useState, useEffect, useCallback, useRef } from 'react';
import { searchFlights } from '../services/flightService';
import AirportAutocomplete from '../components/AirportAutocomplete';
import FlightOfferCard from '../components/FlightOfferCard';
import FlightFilterPanel from '../components/flights/FlightFilterPanel';
import FlightCardSkeleton from '../components/FlightCardSkeleton';
import AiRecommendationCard from '../components/AiRecommendationCard';
import RankingExplanationBanner from '../components/RankingExplanationBanner';
import EmptyState from '../components/EmptyState';
import ErrorState from '../components/ErrorState';
import GroupTravelOptimizer from '../components/GroupTravelOptimizer';
import { recordSearchHistory } from '../services/historyService';

// Optional non-restrictive suggestion chips
export const SUGGESTED_AIRPORTS = [
  { code: 'DEL', city: 'Delhi', name: 'Indira Gandhi Intl' },
  { code: 'BOM', city: 'Mumbai', name: 'Chhatrapati Shivaji Intl' },
  { code: 'BHO', city: 'Bhopal', name: 'Raja Bhoj Airport' },
  { code: 'IDR', city: 'Indore', name: 'Devi Ahilyabai Holkar Airport' },
  { code: 'BLR', city: 'Bengaluru', name: 'Kempegowda Intl' },
  { code: 'DXB', city: 'Dubai', name: 'Dubai Intl' },
  { code: 'LHR', city: 'London', name: 'Heathrow Airport' },
  { code: 'JFK', city: 'New York', name: 'John F. Kennedy Intl' },
];
export const POPULAR_AIRPORTS = SUGGESTED_AIRPORTS;

export const FlightsPage = () => {
  // Mode switcher: 'standard' (Individual) | 'group' (Group Travel)
  const [travelMode, setTravelMode] = useState('standard');

  // Search parameters & structured airport states
  const [tripType, setTripType] = useState('oneWay'); // 'oneWay' | 'roundTrip'
  const [origin, setOrigin] = useState('DEL');
  const [destination, setDestination] = useState('BOM');
  const [fromInput, setFromInput] = useState('DEL');
  const [toInput, setToInput] = useState('BOM');
  const [fromAirport, setFromAirport] = useState({
    iataCode: 'DEL',
    name: 'Indira Gandhi International Airport',
    cityName: 'Delhi',
    countryName: 'India',
    displayName: 'DEL (Delhi)',
  });
  const [toAirport, setToAirport] = useState({
    iataCode: 'BOM',
    name: 'Chhatrapati Shivaji Maharaj International Airport',
    cityName: 'Mumbai',
    countryName: 'India',
    displayName: 'BOM (Mumbai)',
  });

  const [departureDate, setDepartureDate] = useState(() => {
    const d = new Date();
    d.setDate(d.getDate() + 7);
    return d.toISOString().split('T')[0];
  });
  const [returnDate, setReturnDate] = useState('');
  const [adults, setAdults] = useState(1);
  const [cabinClass, setCabinClass] = useState('ECONOMY');

  // Filter & ranking state
  const [selectedRankingTab, setSelectedRankingTab] = useState('best'); // 'best' | 'cheapest' | 'fastest'
  const [stops, setStops] = useState(''); // '' (all), '0', '1', '2'
  const [departureTime, setDepartureTime] = useState('all'); // 'all', 'before_6am', '6am_12pm', '12pm_6pm', 'after_6pm'
  const [selectedAirline, setSelectedAirline] = useState('all');
  const [maxPrice, setMaxPrice] = useState(35000);
  const [maxDuration, setMaxDuration] = useState(720); // up to 12 hours

  // UI & Drawer state
  const [isMobileFilterOpen, setIsMobileFilterOpen] = useState(false);
  const [validationError, setValidationError] = useState('');

  // Results & Lifecycle states: 'IDLE' | 'LOADING' | 'SUCCESS' | 'EMPTY' | 'PARTIAL_SUCCESS' | 'ERROR'
  const [pageState, setPageState] = useState('LOADING');
  const [flights, setFlights] = useState([]);
  const [totalOffers, setTotalOffers] = useState(0);
  const [cheapestPrice, setCheapestPrice] = useState(null);
  const [fastestDuration, setFastestDuration] = useState(null);
  const [bestAirline, setBestAirline] = useState(null);
  const [failedProviders, setFailedProviders] = useState([]);
  const [aiRecommendation, setAiRecommendation] = useState(null);
  const [rankingSummary, setRankingSummary] = useState(null);
  const [errorMessage, setErrorMessage] = useState('');

  // Client-side validations
  const validateSearch = () => {
    const normOrigin = (origin || fromInput || '').trim().toUpperCase();
    const normDest = (destination || toInput || '').trim().toUpperCase();

    if (!normOrigin || !normDest) {
      return 'Please enter both origin and destination airport codes.';
    }

    // Extract raw 3-letter IATA code if text contains formatted "(DEL)" or similar
    const extractCode = (str) => {
      const match = str.match(/\b([A-Z]{3})\b/);
      return match ? match[1] : str;
    };

    const originCode = extractCode(normOrigin);
    const destCode = extractCode(normDest);

    if (originCode === destCode) {
      return 'Origin and destination airports must be different.';
    }

    if (!departureDate) {
      return 'Please select a valid departure date.';
    }

    const todayStr = new Date().toISOString().split('T')[0];
    if (departureDate < todayStr) {
      return 'Departure date cannot be in the past.';
    }

    if (tripType === 'roundTrip') {
      if (!returnDate) {
        return 'Please select a return date for round trip search.';
      }
      if (returnDate < departureDate) {
        return 'Return date must be on or after the departure date.';
      }
    }

    if (!adults || Number(adults) < 1) {
      return 'Number of passengers must be at least 1.';
    }

    return null;
  };

  const handleSelectFromAirport = (airport) => {
    setFromAirport(airport);
    setOrigin(airport.iataCode);
    setFromInput(airport.iataCode);
    setValidationError('');
  };

  const handleSelectToAirport = (airport) => {
    setToAirport(airport);
    setDestination(airport.iataCode);
    setToInput(airport.iataCode);
    setValidationError('');
  };

  const handleFromInputChange = (val) => {
    setFromInput(val);
    const upper = val.trim().toUpperCase();
    if (upper.length === 3 && /^[A-Z]{3}$/.test(upper)) {
      setOrigin(upper);
      setFromAirport((prev) => ({ ...prev, iataCode: upper }));
    } else {
      setOrigin(upper);
    }
    setValidationError('');
  };

  const handleToInputChange = (val) => {
    setToInput(val);
    const upper = val.trim().toUpperCase();
    if (upper.length === 3 && /^[A-Z]{3}$/.test(upper)) {
      setDestination(upper);
      setToAirport((prev) => ({ ...prev, iataCode: upper }));
    } else {
      setDestination(upper);
    }
    setValidationError('');
  };

  const handleSwapAirports = () => {
    const prevOrigin = origin;
    const prevFromInput = fromInput;
    const prevFromAirport = fromAirport;

    setOrigin(destination);
    setFromInput(toInput);
    setFromAirport(toAirport);

    setDestination(prevOrigin);
    setToInput(prevFromInput);
    setToAirport(prevFromAirport);
    setValidationError('');
  };

  const handleSelectChip = (apt) => {
    const airportObj = {
      iataCode: apt.code,
      name: apt.name,
      cityName: apt.city,
      displayName: `${apt.code} (${apt.city})`,
    };
    setDestination(apt.code);
    setToInput(apt.code);
    setToAirport(airportObj);
    setValidationError('');
  };

  const executeFlightSearch = useCallback(async () => {
    const error = validateSearch();
    if (error) {
      setValidationError(error);
      return;
    }
    setValidationError('');
    setPageState('LOADING');
    setErrorMessage('');

    // Extract clean 3-letter IATA code
    const extractCode = (str) => {
      const match = str.match(/\b([A-Z]{3})\b/);
      return match ? match[1] : str.slice(0, 3).toUpperCase();
    };

    const originCode = extractCode((origin || fromInput).trim().toUpperCase());
    const destCode = extractCode((destination || toInput).trim().toUpperCase());

    try {
      const data = await searchFlights({
        origin: originCode,
        destination: destCode,
        departureDate,
        returnDate: tripType === 'roundTrip' ? returnDate : null,
        adults: Number(adults),
        cabinClass,
        maxStops: stops !== '' ? Number(stops) : null,
        airline: selectedAirline,
        maxPrice: Number(maxPrice),
        maxDurationMinutes: maxDuration < 720 ? Number(maxDuration) : null,
        timeOfDay: departureTime,
        sortBy: selectedRankingTab,
      });

      const offers = data.offers || [];
      const failed = data.failedProviders || [];
      setFlights(offers);
      setTotalOffers(data.totalOffers || offers.length);
      setCheapestPrice(data.cheapestPrice);
      setFastestDuration(data.fastestDurationMinutes);
      setBestAirline(data.bestAirline);
      setFailedProviders(failed);
      setAiRecommendation(data.aiRecommendation || null);
      setRankingSummary(data.rankingSummary || null);

      recordSearchHistory(
        `${originCode} → ${destCode}`,
        'FLIGHTS',
        `${tripType === 'roundTrip' ? 'Round Trip' : 'One Way'} • ${cabinClass}`,
        `/flights`
      );

      if (offers.length === 0) {
        setPageState('EMPTY');
      } else if (failed.length > 0) {
        setPageState('PARTIAL_SUCCESS');
      } else {
        setPageState('SUCCESS');
      }
    } catch (err) {
      console.error('Flight search error:', err);
      setErrorMessage(
        err.message || 'Unable to connect to flight providers. Please check your connection and try again.'
      );
      setPageState('ERROR');
    }
  }, [
    origin,
    destination,
    fromInput,
    toInput,
    departureDate,
    returnDate,
    tripType,
    adults,
    cabinClass,
    selectedRankingTab,
    stops,
    departureTime,
    selectedAirline,
    maxPrice,
    maxDuration,
  ]);

  // Initial load once on mount
  useEffect(() => {
    executeFlightSearch();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  // Re-run search when filters or ranking tabs change
  const isFirstRender = useRef(true);
  useEffect(() => {
    if (isFirstRender.current) {
      isFirstRender.current = false;
      return;
    }
    executeFlightSearch();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [selectedRankingTab, stops, departureTime, selectedAirline, maxPrice, maxDuration]);

  const handleFormSubmit = (e) => {
    e.preventDefault();
    executeFlightSearch();
  };

  const handleResetFilters = () => {
    setStops('');
    setDepartureTime('all');
    setSelectedAirline('all');
    setMaxPrice(35000);
    setMaxDuration(720);
    setSelectedRankingTab('best');
  };

  const activeFiltersCount = [
    stops !== '' && stops !== null,
    departureTime !== 'all' && departureTime !== '',
    maxPrice < 35000,
    selectedAirline !== 'all' && selectedAirline !== '',
    maxDuration < 720,
  ].filter(Boolean).length;

  return (
    <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8 space-y-8">
      {/* Breadcrumb & Title */}
      <div>
        <div className="flex items-center gap-2 text-xs text-slate-500 mb-2">
          <span>Home</span>
          <span>/</span>
          <span className="text-indigo-600 font-semibold">Flights Engine</span>
        </div>
        <div className="flex flex-col md:flex-row md:items-center justify-between gap-4">
          <div>
            <h1 className="text-3xl sm:text-4xl font-extrabold text-slate-900 tracking-tight">
              Flight Fare Comparison
            </h1>
            <p className="text-sm text-slate-500 mt-1">
              Compare genuine flight offers across airlines and booking providers to find the cheapest, fastest, and best value options.
            </p>
          </div>

          {/* Quick Insights Badges */}
          {pageState !== 'LOADING' && pageState !== 'ERROR' && pageState !== 'IDLE' && (
            <div className="flex items-center gap-2 flex-wrap">
              {cheapestPrice && (
                <div className="bg-emerald-50 border border-emerald-200 px-3.5 py-2 rounded-xl text-left shadow-2xs">
                  <span className="text-[10px] font-bold text-emerald-800 uppercase block tracking-wider">
                    Cheapest Fare
                  </span>
                  <span className="text-sm font-black text-slate-900 font-mono">
                    ₹{Number(cheapestPrice).toLocaleString('en-IN')}
                  </span>
                </div>
              )}
              {fastestDuration && (
                <div className="bg-indigo-50 border border-indigo-200 px-3.5 py-2 rounded-xl text-left shadow-2xs">
                  <span className="text-[10px] font-bold text-indigo-800 uppercase block tracking-wider">
                    Fastest Route
                  </span>
                  <span className="text-sm font-black text-slate-900 font-mono">
                    {Math.floor(fastestDuration / 60)}h {fastestDuration % 60}m
                  </span>
                </div>
              )}
              {bestAirline && (
                <div className="bg-amber-50 border border-amber-200 px-3.5 py-2 rounded-xl text-left shadow-2xs">
                  <span className="text-[10px] font-bold text-amber-900 uppercase block tracking-wider">
                    Top Value Airline
                  </span>
                  <span className="text-sm font-black text-slate-900">{bestAirline}</span>
                </div>
              )}
            </div>
          )}
        </div>
      </div>

      {/* Travel Engine Switcher Tabs */}
      <div className="flex items-center gap-2 p-1.5 bg-slate-100 rounded-2xl w-fit">
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
          {/* Flight Search Panel Card */}
          <div className="bg-white rounded-3xl p-6 sm:p-8 border border-slate-200 shadow-md">
            {/* Trip Type Selector */}
            <div className="flex items-center gap-2 mb-6">
              <button
                type="button"
                onClick={() => setTripType('oneWay')}
                className={`px-4 py-2 rounded-xl text-xs sm:text-sm font-bold transition cursor-pointer flex items-center gap-1.5 ${
                  tripType === 'oneWay'
                    ? 'bg-indigo-600 text-white shadow-xs'
                    : 'bg-slate-100 text-slate-600 hover:text-slate-900'
                }`}
              >
                <span>→</span>
                <span>One Way</span>
              </button>
              <button
                type="button"
                onClick={() => setTripType('roundTrip')}
                className={`px-4 py-2 rounded-xl text-xs sm:text-sm font-bold transition cursor-pointer flex items-center gap-1.5 ${
                  tripType === 'roundTrip'
                    ? 'bg-indigo-600 text-white shadow-xs'
                    : 'bg-slate-100 text-slate-600 hover:text-slate-900'
                }`}
              >
                <span>⇄</span>
                <span>Round Trip</span>
              </button>
            </div>

            {/* Validation Notice Banner */}
            {validationError && (
              <div
                role="alert"
                className="mb-6 p-4 bg-rose-50 border border-rose-200 text-rose-800 rounded-2xl flex items-center justify-between text-xs sm:text-sm font-medium animate-fadeIn"
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

            {/* Search Form */}
            <form noValidate onSubmit={handleFormSubmit} className="space-y-4">
              <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-12 gap-3 items-center">
                {/* FROM Airport Autocomplete */}
                <div className="lg:col-span-3">
                  <label htmlFor="from-airport" className="block text-[11px] font-bold uppercase tracking-wider text-slate-500 mb-1">
                    From Airport / City
                  </label>
                  <AirportAutocomplete
                    id="from-airport"
                    aria-label="From Airport / City"
                    value={fromInput}
                    onChange={handleFromInputChange}
                    onSelect={handleSelectFromAirport}
                    placeholder="DEL (Delhi)"
                    icon="🛫"
                    required
                  />
                </div>

                {/* SWAP Button */}
                <div className="lg:col-span-1 flex justify-center pt-2 sm:pt-4 lg:pt-5">
                  <button
                    type="button"
                    onClick={handleSwapAirports}
                    title="Swap origin and destination"
                    className="w-10 h-10 rounded-full border border-slate-200 bg-white hover:bg-slate-50 text-indigo-600 hover:text-indigo-800 flex items-center justify-center font-bold text-base shadow-2xs transition transform hover:rotate-180 cursor-pointer"
                    aria-label="Swap origin and destination"
                  >
                    ⇄
                  </button>
                </div>

                {/* TO Airport Autocomplete */}
                <div className="lg:col-span-3">
                  <label htmlFor="to-airport" className="block text-[11px] font-bold uppercase tracking-wider text-slate-500 mb-1">
                    To Airport / City
                  </label>
                  <AirportAutocomplete
                    id="to-airport"
                    aria-label="To Airport / City"
                    value={toInput}
                    onChange={handleToInputChange}
                    onSelect={handleSelectToAirport}
                    placeholder="BOM (Mumbai)"
                    icon="🛬"
                    required
                  />
                </div>

                {/* DEPARTURE Date */}
                <div className="lg:col-span-2">
                  <label htmlFor="departure-date" className="block text-[11px] font-bold uppercase tracking-wider text-slate-500 mb-1">
                    Departure Date
                  </label>
                  <input
                    id="departure-date"
                    type="date"
                    required
                    min={new Date().toISOString().split('T')[0]}
                    value={departureDate}
                    onChange={(e) => setDepartureDate(e.target.value)}
                    className="w-full px-3 py-2.5 bg-slate-50 border border-slate-200 rounded-xl text-sm font-semibold text-slate-900 focus:outline-hidden focus:border-indigo-500 focus:bg-white cursor-pointer transition"
                  />
                </div>

                {/* RETURN Date (Active only for Round Trip) */}
                <div className="lg:col-span-2">
                  <label htmlFor="return-date" className="block text-[11px] font-bold uppercase tracking-wider text-slate-500 mb-1">
                    Return Date
                  </label>
                  <input
                    id="return-date"
                    type="date"
                    disabled={tripType === 'oneWay'}
                    min={departureDate || new Date().toISOString().split('T')[0]}
                    value={returnDate}
                    onChange={(e) => setReturnDate(e.target.value)}
                    className={`w-full px-3 py-2.5 border rounded-xl text-sm font-semibold transition ${
                      tripType === 'oneWay'
                        ? 'bg-slate-100 border-slate-200 text-slate-400 cursor-not-allowed opacity-60'
                        : 'bg-slate-50 border-slate-200 text-slate-900 focus:outline-hidden focus:border-indigo-500 focus:bg-white cursor-pointer'
                    }`}
                    placeholder={tripType === 'oneWay' ? 'One Way trip' : 'Select return'}
                  />
                </div>

                {/* PASSENGERS & CABIN & SEARCH BUTTON */}
                <div className="lg:col-span-12 grid grid-cols-1 sm:grid-cols-3 lg:grid-cols-12 gap-3 pt-2">
                  {/* Travellers / Adults */}
                  <div className="lg:col-span-3">
                    <label htmlFor="travellers-select" className="block text-[11px] font-bold uppercase tracking-wider text-slate-500 mb-1">
                      Travellers
                    </label>
                    <div className="relative">
                      <span className="absolute inset-y-0 left-0 pl-3 flex items-center text-slate-400">
                        👤
                      </span>
                      <select
                        id="travellers-select"
                        value={adults}
                        onChange={(e) => setAdults(Number(e.target.value))}
                        className="w-full pl-9 pr-3 py-2.5 bg-slate-50 border border-slate-200 rounded-xl text-sm font-bold text-slate-900 focus:outline-hidden focus:border-indigo-500 cursor-pointer"
                      >
                        {[1, 2, 3, 4, 5, 6, 7, 8, 9].map((num) => (
                          <option key={num} value={num}>
                            {num} Adult{num > 1 ? 's' : ''}
                          </option>
                        ))}
                      </select>
                    </div>
                  </div>

                  {/* Cabin Class */}
                  <div className="lg:col-span-3">
                    <label htmlFor="cabin-class-select" className="block text-[11px] font-bold uppercase tracking-wider text-slate-500 mb-1">
                      Cabin Class
                    </label>
                    <select
                      id="cabin-class-select"
                      value={cabinClass}
                      onChange={(e) => setCabinClass(e.target.value)}
                      className="w-full px-3 py-2.5 bg-slate-50 border border-slate-200 rounded-xl text-sm font-bold text-slate-900 focus:outline-hidden focus:border-indigo-500 cursor-pointer"
                    >
                      <option value="ECONOMY">Economy</option>
                      <option value="PREMIUM_ECONOMY">Premium Economy</option>
                      <option value="BUSINESS">Business Class</option>
                    </select>
                  </div>

                  {/* Empty Spacer */}
                  <div className="hidden lg:block lg:col-span-3"></div>

                  {/* Search Flights Primary Button */}
                  <div className="sm:col-span-1 lg:col-span-3 flex items-end">
                    <button
                      type="submit"
                      disabled={pageState === 'LOADING'}
                      className="w-full py-3 bg-indigo-600 hover:bg-indigo-700 disabled:opacity-50 text-white font-extrabold text-sm rounded-xl shadow-xs transition flex items-center justify-center gap-2 cursor-pointer"
                    >
                      {pageState === 'LOADING' ? (
                        <>
                          <div className="w-4 h-4 border-2 border-white border-t-transparent rounded-full animate-spin"></div>
                          <span>Searching...</span>
                        </>
                      ) : (
                        <>
                          <span>Search Flights</span>
                          <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2.5" d="M14 5l7 7m0 0l-7 7m7-7H3" />
                          </svg>
                        </>
                      )}
                    </button>
                  </div>
                </div>
              </div>

              {/* Quick Popular Airports Chips */}
              <div className="pt-3 border-t border-slate-100 flex items-center gap-2 flex-wrap text-xs">
                <span className="text-slate-400 font-medium">Popular Hubs:</span>
                {SUGGESTED_AIRPORTS.map((apt) => (
                  <button
                    key={apt.code}
                    type="button"
                    onClick={() => handleSelectChip(apt)}
                    className="px-2.5 py-1 rounded-lg bg-slate-100 hover:bg-slate-200 text-slate-700 font-semibold cursor-pointer transition"
                  >
                    {apt.code} ({apt.city})
                  </button>
                ))}
              </div>
            </form>
          </div>

          {/* Mobile Filter Trigger Button */}
          <div className="lg:hidden flex items-center justify-between">
            <button
              type="button"
              onClick={() => setIsMobileFilterOpen(true)}
              className="py-2.5 px-4 bg-white border border-slate-200 rounded-xl text-xs font-bold text-slate-800 shadow-2xs hover:bg-slate-50 flex items-center gap-2 cursor-pointer"
            >
              <span>🎛️</span>
              <span>Filter Flights</span>
              {activeFiltersCount > 0 && (
                <span className="w-5 h-5 rounded-full bg-indigo-600 text-white text-[10px] flex items-center justify-center font-black">
                  {activeFiltersCount}
                </span>
              )}
            </button>
            <span className="text-xs text-slate-500 font-medium">
              {totalOffers} offers found
            </span>
          </div>

          {/* Main Content Layout: LEFT Filters, RIGHT Results Feed */}
          <div className="grid grid-cols-1 lg:grid-cols-4 gap-8 items-start">
            {/* Desktop Left Filter Sidebar */}
            <div className="hidden lg:block lg:col-span-1 sticky top-24">
              <FlightFilterPanel
                stops={stops}
                setStops={setStops}
                departureTime={departureTime}
                setDepartureTime={setDepartureTime}
                maxPrice={maxPrice}
                setMaxPrice={setMaxPrice}
                selectedAirline={selectedAirline}
                setSelectedAirline={setSelectedAirline}
                maxDuration={maxDuration}
                setMaxDuration={setMaxDuration}
                onResetFilters={handleResetFilters}
              />
            </div>

            {/* Mobile Filter Drawer / Bottom Sheet */}
            {isMobileFilterOpen && (
              <div
                className="fixed inset-0 z-50 flex items-end sm:items-center justify-center p-0 sm:p-4 bg-slate-900/60 backdrop-blur-xs animate-fadeIn"
                onClick={() => setIsMobileFilterOpen(false)}
              >
                <div
                  className="bg-white w-full sm:max-w-md max-h-[85vh] rounded-t-3xl sm:rounded-3xl shadow-2xl overflow-hidden flex flex-col"
                  onClick={(e) => e.stopPropagation()}
                >
                  <FlightFilterPanel
                    stops={stops}
                    setStops={setStops}
                    departureTime={departureTime}
                    setDepartureTime={setDepartureTime}
                    maxPrice={maxPrice}
                    setMaxPrice={setMaxPrice}
                    selectedAirline={selectedAirline}
                    setSelectedAirline={setSelectedAirline}
                    maxDuration={maxDuration}
                    setMaxDuration={setMaxDuration}
                    onResetFilters={handleResetFilters}
                    onApplyFilters={() => setIsMobileFilterOpen(false)}
                    isMobileDrawer={true}
                    onCloseDrawer={() => setIsMobileFilterOpen(false)}
                  />
                </div>
              </div>
            )}

            {/* Right Main Flight Results Feed */}
            <div className="lg:col-span-3 space-y-6">
              {/* Ranking Tabs: Cheapest, Fastest, Best Value */}
              <div className="flex bg-white p-1.5 rounded-2xl border border-slate-200 shadow-xs text-xs font-bold">
                {[
                  {
                    id: 'best',
                    label: '⭐ Best Value',
                    sub: 'Optimal Price & Duration',
                  },
                  {
                    id: 'cheapest',
                    label: '💰 Cheapest',
                    sub: 'Lowest Available Fare',
                  },
                  {
                    id: 'fastest',
                    label: '⚡ Fastest',
                    sub: 'Shortest Flight Time',
                  },
                ].map((tab) => (
                  <button
                    key={tab.id}
                    type="button"
                    onClick={() => setSelectedRankingTab(tab.id)}
                    className={`flex-1 py-2.5 px-3 rounded-xl transition text-center cursor-pointer ${
                      selectedRankingTab === tab.id
                        ? 'bg-indigo-600 text-white shadow-xs'
                        : 'text-slate-600 hover:text-slate-900 hover:bg-slate-50'
                    }`}
                  >
                    <div className="font-extrabold">{tab.label}</div>
                    <div
                      className={`text-[10px] font-normal ${
                        selectedRankingTab === tab.id ? 'text-indigo-100' : 'text-slate-400'
                      }`}
                    >
                      {tab.sub}
                    </div>
                  </button>
                ))}
              </div>

              {/* Partial Success State Notice Banner */}
              {pageState === 'PARTIAL_SUCCESS' && failedProviders.length > 0 && (
                <div
                  role="status"
                  className="bg-amber-50 border border-amber-200 rounded-2xl p-4 flex items-start gap-3 text-xs text-amber-900"
                >
                  <span className="text-base">⚠️</span>
                  <div>
                    <h4 className="font-bold">Partial Provider Availability</h4>
                    <p className="mt-0.5 text-amber-800">
                      Live offers from <strong className="font-semibold">{failedProviders.join(', ')}</strong> were temporarily unavailable. Displaying confirmed fares from our active connected providers.
                    </p>
                  </div>
                </div>
              )}

              {/* Dynamic State Rendering */}
              {pageState === 'LOADING' ? (
                <div className="space-y-4">
                  {/* Route Loading Context Banner */}
                  <div className="bg-indigo-50/80 border border-indigo-100 rounded-2xl p-4 flex items-center gap-3 text-indigo-900 text-sm font-semibold">
                    <div className="w-5 h-5 border-2 border-indigo-600 border-t-transparent rounded-full animate-spin"></div>
                    <span>Finding the best fares from <strong className="font-extrabold">{origin}</strong> to <strong className="font-extrabold">{destination}</strong>...</span>
                  </div>
                  <FlightCardSkeleton count={4} />
                </div>
              ) : pageState === 'ERROR' ? (
                <ErrorState
                  title="Flight Comparison Engine Notice"
                  message={errorMessage}
                  onRetry={executeFlightSearch}
                />
              ) : pageState === 'EMPTY' ? (
                <EmptyState
                  title="No flight offers were returned for this route and date."
                  description={`No flights found for this route (${origin} → ${destination}) on ${departureDate}. Connected flight providers currently have no published fares or scheduled flights for this route and date.`}
                  actionLabel="Reset Search Filters"
                  onAction={handleResetFilters}
                />
              ) : pageState === 'IDLE' ? (
                <div className="bg-white rounded-3xl border border-slate-200 p-12 text-center space-y-3">
                  <span className="text-4xl block">🛫</span>
                  <h3 className="text-lg font-bold text-slate-900">Compare Flight Fares</h3>
                  <p className="text-xs text-slate-500 max-w-md mx-auto">
                    Select your departure and arrival airports above and click Search to instantly compare multi-airline fares and schedules.
                  </p>
                </div>
              ) : (
                <div className="space-y-4">
                  {rankingSummary && (
                    <RankingExplanationBanner rankingSummary={rankingSummary} type="flight" />
                  )}
                  {aiRecommendation && (
                    <AiRecommendationCard recommendation={aiRecommendation} />
                  )}
                  {flights.map((flight, idx) => (
                    <FlightOfferCard
                      key={`${flight.airline}-${flight.flightNumber || idx}-${idx}`}
                      flight={flight}
                    />
                  ))}
                </div>
              )}
            </div>
          </div>
        </>
      )}
    </div>
  );
};

export default FlightsPage;
