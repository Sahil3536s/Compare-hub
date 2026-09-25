import React, { useState, useEffect, useRef, useCallback } from 'react';
import { suggestPlaces } from '../services/locationService';

/**
 * Reusable LocationAutocomplete component with:
 * - 300ms debounce
 * - Minimum 2 characters trigger
 * - Loading spinner
 * - Keyboard navigation (Up/Down/Enter/Escape)
 * - Click selection & click-outside close
 * - Clear button
 * - No results state ("No matching places found for \"{query}\"")
 * - API error state
 * - Race-condition prevention / stale request cancellation
 * - Resolves to structured location:
 *   { name, formattedAddress, latitude, longitude, city, state, country, providerPlaceId }
 */
export const LocationAutocomplete = ({
  id,
  value = '',
  onChange,
  onSelect,
  placeholder = 'Search place, station, airport...',
  required = false,
  disabled = false,
  icon = '📍',
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

  // Debounced place search
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
      const results = await suggestPlaces(trimmed);
      // Discard stale responses
      if (currentReq === activeRequestId.current) {
        setSuggestions(results || []);
        setHasSearched(true);
        setIsOpen(true);
        setHighlightedIndex(-1);
      }
    } catch (err) {
      if (currentReq === activeRequestId.current) {
        console.error('Location autocomplete search error:', err);
        setSearchError('Unable to fetch suggestions. You can enter address directly.');
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
    const name = item.name || item.mainText || item.formattedAddress || item.fullAddress || inputValue;
    const formattedAddress = item.formattedAddress || item.fullAddress || item.address || name;

    const structured = {
      name,
      formattedAddress,
      latitude: item.latitude,
      longitude: item.longitude,
      city: item.city || (formattedAddress.split(',')[1] ? formattedAddress.split(',')[1].trim() : ''),
      state: item.state || '',
      country: item.country || 'India',
      providerPlaceId: item.providerPlaceId || item.placeId || '',
      // Backward compatibility props
      placeId: item.placeId || item.providerPlaceId || '',
      mainText: item.mainText || name,
      secondaryText: item.secondaryText || formattedAddress,
      fullAddress: formattedAddress,
      address: formattedAddress,
    };

    setInputValue(name);
    if (onChange) {
      onChange(name);
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
        <span className="absolute inset-y-0 left-0 pl-3.5 flex items-center pointer-events-none text-base select-none">
          {icon}
        </span>

        {/* Text Input */}
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
          className="w-full pl-10 pr-10 py-2.5 bg-slate-50 border border-slate-200 rounded-xl text-sm font-bold text-slate-900 placeholder:text-slate-400 placeholder:font-normal focus:outline-hidden focus:border-indigo-500 focus:bg-white transition"
        />

        {/* Trailing Controls: Loading Spinner and/or Clear Button */}
        <div className="absolute inset-y-0 right-0 pr-3 flex items-center gap-1.5">
          {loading && (
            <div
              data-testid="autocomplete-loading"
              className="w-4 h-4 border-2 border-indigo-600 border-t-transparent rounded-full animate-spin"
            />
          )}

          {inputValue && !disabled && (
            <button
              type="button"
              onClick={handleClear}
              aria-label="Clear location input"
              className="w-5 h-5 rounded-full hover:bg-slate-200 text-slate-400 hover:text-slate-600 flex items-center justify-center text-xs font-bold transition cursor-pointer"
            >
              ✕
            </button>
          )}
        </div>
      </div>

      {/* Autocomplete Dropdown Popup */}
      {isOpen && (
        <div
          role="listbox"
          className="absolute left-0 right-0 mt-1 bg-white rounded-2xl border border-slate-200 shadow-xl z-50 overflow-hidden divide-y divide-slate-100 max-h-72 overflow-y-auto animate-fadeIn"
        >
          {loading && suggestions.length === 0 && (
            <div className="p-3 text-center text-xs text-slate-500 flex items-center justify-center gap-2">
              <div className="w-3.5 h-3.5 border-2 border-indigo-500 border-t-transparent rounded-full animate-spin" />
              <span>Searching locations...</span>
            </div>
          )}

          {searchError && (
            <div className="p-3 text-xs text-amber-700 bg-amber-50">
              {searchError}
            </div>
          )}

          {!loading && !searchError && hasSearched && suggestions.length === 0 && (
            <div className="p-3.5 text-center text-xs text-slate-500 font-medium">
              No matching places found for &quot;<span className="font-semibold text-slate-800">{inputValue}</span>&quot;
            </div>
          )}

          {suggestions.map((suggestion, idx) => {
            const isHighlighted = idx === highlightedIndex;
            const primaryText = suggestion.name || suggestion.mainText || suggestion.fullAddress;
            const secondaryText = suggestion.secondaryText || suggestion.formattedAddress || '';

            return (
              <div
                key={suggestion.placeId || suggestion.providerPlaceId || `${primaryText}-${idx}`}
                role="option"
                aria-selected={isHighlighted}
                onMouseEnter={() => setHighlightedIndex(idx)}
                onClick={() => handleSelectSuggestion(suggestion)}
                className={`w-full p-3 text-left flex items-start gap-2.5 transition cursor-pointer select-none ${
                  isHighlighted ? 'bg-indigo-50/80 text-indigo-950' : 'hover:bg-slate-50 text-slate-800'
                }`}
              >
                <span className="text-slate-400 mt-0.5 text-sm">📍</span>
                <div className="min-w-0 flex-1">
                  <p className="text-xs font-bold truncate">
                    {primaryText}
                  </p>
                  {secondaryText && (
                    <p className="text-[11px] text-slate-400 truncate">
                      {secondaryText}
                    </p>
                  )}
                </div>
              </div>
            );
          })}
        </div>
      )}
    </div>
  );
};

export default LocationAutocomplete;
