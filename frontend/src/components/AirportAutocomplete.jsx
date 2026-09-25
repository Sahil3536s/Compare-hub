import React, { useState, useEffect, useRef, useCallback } from 'react';
import { searchAirports } from '../services/airportService';

/**
 * Reusable AirportAutocomplete component with:
 * - 300ms debounce
 * - Minimum 2 characters trigger (or 3-letter IATA code)
 * - Loading indicator
 * - Full keyboard navigation (ArrowDown, ArrowUp, Enter, Escape)
 * - Click selection & click-outside dismissal
 * - Clear button
 * - "No Results" state: No matching airports or cities found for "{query}"
 * - Error handling fallback
 * - Race-condition prevention (stale request cancellation)
 * - Resolves to structured airport model:
 *   { name, iataCode, cityName, countryName, airportType, latitude, longitude, displayName }
 */
export const AirportAutocomplete = ({
  id,
  value = '',
  onChange,
  onSelect,
  placeholder = 'Search city, airport name, or IATA (e.g. DEL, London)...',
  required = false,
  disabled = false,
  icon = '✈️',
  className = '',
  'aria-label': ariaLabel,
}) => {
  const [inputValue, setInputValue] = useState(value);
  const [suggestions, setSuggestions] = useState([]);
  const [loading, setLoading] = useState(false);
  const [isOpen, setIsOpen] = useState(false);
  const [highlightedIndex, setHighlightedIndex] = useState(-1);
  const [hasSearched, setHasSearched] = useState(false);
  const [searchError, setSearchError] = useState(null);

  const containerRef = useRef(null);
  const inputRef = useRef(null);
  const activeRequestId = useRef(0);
  const isSelectedRef = useRef(false);

  // Sync internal state when parent updates value
  useEffect(() => {
    setInputValue(value || '');
  }, [value]);

  // Click-outside listener
  useEffect(() => {
    const handleClickOutside = (e) => {
      if (containerRef.current && !containerRef.current.contains(e.target)) {
        setIsOpen(false);
        setHighlightedIndex(-1);
      }
    };
    document.addEventListener('mousedown', handleClickOutside);
    document.addEventListener('touchstart', handleClickOutside);
    return () => {
      document.removeEventListener('mousedown', handleClickOutside);
      document.removeEventListener('touchstart', handleClickOutside);
    };
  }, []);

  // Debounced airport search
  const fetchSuggestions = useCallback(async (query) => {
    const trimmed = query.trim();
    if (trimmed.length < 2) {
      setSuggestions([]);
      setLoading(false);
      setIsOpen(false);
      setHasSearched(false);
      setSearchError(null);
      return;
    }

    const currentReq = ++activeRequestId.current;
    setLoading(true);
    setSearchError(null);

    try {
      const results = await searchAirports(trimmed);
      if (currentReq === activeRequestId.current) {
        setSuggestions(results || []);
        setHasSearched(true);
        setIsOpen(true);
        setHighlightedIndex(-1);
      }
    } catch (err) {
      if (currentReq === activeRequestId.current) {
        console.error('Airport search error:', err);
        setSearchError('Unable to fetch airport suggestions. You can enter IATA code directly.');
        setSuggestions([]);
        setHasSearched(true);
        setIsOpen(true);
      }
    } finally {
      if (currentReq === activeRequestId.current) {
        setLoading(false);
      }
    }
  }, []);

  // Handle typing with 300ms debounce
  const handleInputChange = (e) => {
    const val = e.target.value;
    isSelectedRef.current = false;
    setInputValue(val);
    if (onChange) {
      onChange(val);
    }

    if (val.trim().length >= 2) {
      setLoading(true);
      const timer = setTimeout(() => {
        fetchSuggestions(val);
      }, 300);
      return () => clearTimeout(timer);
    } else {
      setSuggestions([]);
      setIsOpen(false);
      setLoading(false);
      setHasSearched(false);
    }
  };

  const handleSelectSuggestion = (item) => {
    isSelectedRef.current = true;
    const iataCode = item.iataCode || '';
    const cityName = item.cityName || '';
    const name = item.name || '';
    const displayLabel = iataCode ? `${iataCode} (${cityName || name})` : name;

    const structured = {
      name,
      iataCode,
      cityName,
      countryName: item.countryName || '',
      airportType: item.airportType || 'AIRPORT',
      latitude: item.latitude,
      longitude: item.longitude,
      displayName: displayLabel,
      code: iataCode,
    };

    setInputValue(displayLabel);
    if (onChange) {
      onChange(displayLabel);
    }
    setIsOpen(false);
    setSuggestions([]);
    setHighlightedIndex(-1);

    if (onSelect) {
      onSelect(structured);
    }
  };

  const handleClear = () => {
    setInputValue('');
    setSuggestions([]);
    setIsOpen(false);
    setHighlightedIndex(-1);
    setHasSearched(false);
    setSearchError(null);
    if (onChange) {
      onChange('');
    }
    if (inputRef.current) {
      inputRef.current.focus();
    }
  };

  const handleKeyDown = (e) => {
    if (!isOpen && (e.key === 'ArrowDown' || e.key === 'ArrowUp')) {
      if (suggestions.length > 0) {
        setIsOpen(true);
        return;
      }
    }

    if (e.key === 'ArrowDown') {
      e.preventDefault();
      if (suggestions.length > 0) {
        setHighlightedIndex((prev) => (prev + 1) % suggestions.length);
      }
    } else if (e.key === 'ArrowUp') {
      e.preventDefault();
      if (suggestions.length > 0) {
        setHighlightedIndex((prev) => (prev - 1 + suggestions.length) % suggestions.length);
      }
    } else if (e.key === 'Enter') {
      if (isOpen && highlightedIndex >= 0 && highlightedIndex < suggestions.length) {
        e.preventDefault();
        handleSelectSuggestion(suggestions[highlightedIndex]);
      }
    } else if (e.key === 'Escape') {
      setIsOpen(false);
      setHighlightedIndex(-1);
    }
  };

  return (
    <div ref={containerRef} className={`relative ${className}`}>
      <div className="relative">
        {/* Leading Icon */}
        <span className="absolute inset-y-0 left-0 pl-3 flex items-center pointer-events-none text-base select-none">
          {icon}
        </span>

        {/* Input */}
        <input
          ref={inputRef}
          id={id}
          type="text"
          role="combobox"
          aria-expanded={isOpen}
          aria-autocomplete="list"
          aria-label={ariaLabel}
          required={required}
          disabled={disabled}
          value={inputValue}
          onChange={handleInputChange}
          onFocus={() => {
            if (suggestions.length > 0) setIsOpen(true);
          }}
          onKeyDown={handleKeyDown}
          placeholder={placeholder}
          autoComplete="off"
          className="w-full pl-9 pr-9 py-2.5 bg-slate-50 border border-slate-200 rounded-xl text-sm font-bold text-slate-900 placeholder:text-slate-400 placeholder:font-normal focus:outline-hidden focus:border-indigo-500 focus:bg-white uppercase transition"
        />

        {/* Trailing Spinner or Clear Button */}
        <div className="absolute inset-y-0 right-0 pr-3 flex items-center gap-1.5">
          {loading && (
            <div
              data-testid="airport-autocomplete-loading"
              className="w-4 h-4 border-2 border-indigo-600 border-t-transparent rounded-full animate-spin"
            />
          )}

          {inputValue && !disabled && (
            <button
              type="button"
              onClick={handleClear}
              aria-label="Clear airport input"
              className="w-5 h-5 rounded-full hover:bg-slate-200 text-slate-400 hover:text-slate-600 flex items-center justify-center text-xs font-bold transition cursor-pointer"
            >
              ✕
            </button>
          )}
        </div>
      </div>

      {/* Dropdown Suggestions */}
      {isOpen && (
        <div
          role="listbox"
          className="absolute left-0 right-0 mt-1 bg-white rounded-2xl border border-slate-200 shadow-xl z-50 overflow-hidden divide-y divide-slate-100 max-h-72 overflow-y-auto animate-fadeIn"
        >
          {loading && suggestions.length === 0 && (
            <div className="p-3 text-center text-xs text-slate-500 flex items-center justify-center gap-2">
              <div className="w-3.5 h-3.5 border-2 border-indigo-500 border-t-transparent rounded-full animate-spin" />
              <span>Searching airports...</span>
            </div>
          )}

          {searchError && (
            <div className="p-3 text-xs text-amber-700 bg-amber-50">
              {searchError}
            </div>
          )}

          {!loading && !searchError && hasSearched && suggestions.length === 0 && (
            <div className="p-3.5 text-center text-xs text-slate-500 font-medium">
              No matching airports or cities found for &quot;<span className="font-semibold text-slate-800">{inputValue}</span>&quot;
            </div>
          )}

          {suggestions.map((airport, idx) => {
            const isHighlighted = idx === highlightedIndex;

            return (
              <div
                key={`${airport.iataCode}-${idx}`}
                role="option"
                aria-selected={isHighlighted}
                onMouseEnter={() => setHighlightedIndex(idx)}
                onClick={() => handleSelectSuggestion(airport)}
                className={`w-full p-3 text-left flex items-start gap-2.5 transition cursor-pointer select-none ${
                  isHighlighted ? 'bg-indigo-50/80 text-indigo-950' : 'hover:bg-slate-50 text-slate-800'
                }`}
              >
                <div className="w-9 h-9 rounded-xl bg-indigo-50 border border-indigo-200/60 flex items-center justify-center font-mono font-black text-indigo-700 text-xs shrink-0">
                  {airport.iataCode}
                </div>
                <div className="min-w-0 flex-1">
                  <div className="flex items-center gap-2">
                    <p className="text-xs font-bold truncate">
                      {airport.cityName || airport.name}
                    </p>
                    {airport.countryName && (
                      <span className="text-[10px] text-slate-400 uppercase font-medium">
                        • {airport.countryName}
                      </span>
                    )}
                  </div>
                  <p className="text-[11px] text-slate-500 truncate">
                    {airport.name}
                  </p>
                </div>
              </div>
            );
          })}
        </div>
      )}
    </div>
  );
};

export default AirportAutocomplete;
