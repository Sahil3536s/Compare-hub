import React, { useState, useEffect, useRef } from 'react';
import { getSearchSuggestions } from '../services/productService';

export const SearchBar = ({
  placeholder = 'Search products, brands, flights or rides...',
  initialQuery = '',
  categories = [],
  selectedCategory = '',
  onCategoryChange,
  onSearch,
  className = '',
  size = 'md', // 'sm' | 'md' | 'lg'
  enableSuggestions = true,
}) => {
  const [query, setQuery] = useState(initialQuery);
  const [suggestions, setSuggestions] = useState([]);
  const [showSuggestions, setShowSuggestions] = useState(false);
  const [selectedIndex, setSelectedIndex] = useState(-1);
  const [loadingSuggestions, setLoadingSuggestions] = useState(false);

  const containerRef = useRef(null);
  const abortControllerRef = useRef(null);
  const debounceTimerRef = useRef(null);

  // Sync initial query if it changes externally
  useEffect(() => {
    setQuery(initialQuery);
  }, [initialQuery]);

  // Fetch suggestions with debounce and request cancellation
  useEffect(() => {
    if (!enableSuggestions || !query || query.trim().length < 2) {
      setSuggestions([]);
      setShowSuggestions(false);
      return;
    }

    if (debounceTimerRef.current) {
      clearTimeout(debounceTimerRef.current);
    }
    if (abortControllerRef.current) {
      abortControllerRef.current.abort();
    }

    debounceTimerRef.current = setTimeout(async () => {
      const controller = new AbortController();
      abortControllerRef.current = controller;
      setLoadingSuggestions(true);

      try {
        const results = await getSearchSuggestions(query, controller.signal);
        setSuggestions(results || []);
        setShowSuggestions((results || []).length > 0);
        setSelectedIndex(-1);
      } catch (err) {
        if (err.name !== 'CanceledError' && err.code !== 'ERR_CANCELED') {
          setSuggestions([]);
        }
      } finally {
        setLoadingSuggestions(false);
      }
    }, 250);

    return () => {
      if (debounceTimerRef.current) clearTimeout(debounceTimerRef.current);
      if (abortControllerRef.current) abortControllerRef.current.abort();
    };
  }, [query, enableSuggestions]);

  // Close dropdown on outside click
  useEffect(() => {
    const handleOutsideClick = (e) => {
      if (containerRef.current && !containerRef.current.contains(e.target)) {
        setShowSuggestions(false);
      }
    };
    document.addEventListener('mousedown', handleOutsideClick);
    return () => document.removeEventListener('mousedown', handleOutsideClick);
  }, []);

  const handleSubmit = (e) => {
    e.preventDefault();
    setShowSuggestions(false);
    if (onSearch) {
      onSearch(query);
    }
  };

  const handleClear = () => {
    setQuery('');
    setSuggestions([]);
    setShowSuggestions(false);
    if (onSearch) {
      onSearch('');
    }
  };

  const handleSelectSuggestion = (suggestion) => {
    setQuery(suggestion);
    setShowSuggestions(false);
    if (onSearch) {
      onSearch(suggestion);
    }
  };

  const handleKeyDown = (e) => {
    if (!showSuggestions || suggestions.length === 0) return;

    if (e.key === 'ArrowDown') {
      e.preventDefault();
      setSelectedIndex((prev) => (prev < suggestions.length - 1 ? prev + 1 : 0));
    } else if (e.key === 'ArrowUp') {
      e.preventDefault();
      setSelectedIndex((prev) => (prev > 0 ? prev - 1 : suggestions.length - 1));
    } else if (e.key === 'Enter' && selectedIndex >= 0) {
      e.preventDefault();
      handleSelectSuggestion(suggestions[selectedIndex]);
    } else if (e.key === 'Escape') {
      setShowSuggestions(false);
    }
  };

  const sizeClasses = {
    sm: 'py-2.5 px-3 text-xs sm:text-sm',
    md: 'py-3 px-3.5 text-sm sm:text-base',
    lg: 'py-3.5 sm:py-4 px-4 sm:px-5 text-sm sm:text-base md:text-lg',
  };

  return (
    <div ref={containerRef} className="relative w-full">
      <form
        role="search"
        onSubmit={handleSubmit}
        className={`relative flex flex-col sm:flex-row items-stretch bg-white rounded-2xl border border-slate-200/90 shadow-xs hover:shadow-md focus-within:border-indigo-500 focus-within:ring-2 focus-within:ring-indigo-500/20 transition-all ${className}`}
      >
        {categories.length > 0 && (
          <div className="sm:border-r sm:border-slate-200 px-3 py-2 sm:py-0 flex items-center bg-slate-50/70 sm:rounded-l-2xl border-b sm:border-b-0 border-slate-100">
            <select
              value={selectedCategory}
              onChange={(e) => onCategoryChange && onCategoryChange(e.target.value)}
              aria-label="Filter by product category"
              className="bg-transparent text-xs sm:text-sm font-semibold text-slate-700 focus:outline-hidden cursor-pointer w-full sm:w-auto py-1"
            >
              {categories.map((cat) => (
                <option key={cat} value={cat}>
                  {cat}
                </option>
              ))}
            </select>
          </div>
        )}

        <div className="relative flex-1 flex items-center min-w-0">
          <div className="absolute left-3.5 text-slate-400 pointer-events-none" aria-hidden="true">
            <svg className="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M21 21l-6-6m2-5a7 7 0 11-14 0 7 7 0 0114 0z" />
            </svg>
          </div>

          <input
            type="text"
            value={query}
            onChange={(e) => {
              setQuery(e.target.value);
              setShowSuggestions(true);
            }}
            onFocus={() => {
              if (suggestions.length > 0) setShowSuggestions(true);
            }}
            onKeyDown={handleKeyDown}
            placeholder={placeholder}
            aria-label="Search query"
            aria-autocomplete="list"
            aria-expanded={showSuggestions}
            className={`w-full bg-transparent pl-10 pr-10 focus:outline-hidden text-slate-900 placeholder:text-slate-400 min-w-0 ${sizeClasses[size]}`}
          />

          {query && (
            <button
              type="button"
              onClick={handleClear}
              aria-label="Clear search input"
              className="absolute right-3 p-1.5 text-slate-400 hover:text-slate-600 rounded-full hover:bg-slate-100 transition cursor-pointer min-h-[36px] min-w-[36px] flex items-center justify-center"
            >
              <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M6 18L18 6M6 6l12 12" />
              </svg>
            </button>
          )}
        </div>

        <div className="p-1.5 sm:p-2 flex items-center justify-end">
          <button
            type="submit"
            className="w-full sm:w-auto inline-flex items-center justify-center gap-2 px-5 py-2.5 sm:py-3 bg-indigo-600 hover:bg-indigo-700 text-white text-xs sm:text-sm font-bold rounded-xl transition shadow-xs hover:shadow-md cursor-pointer min-h-[44px]"
          >
            <span>Search Deals</span>
            <svg className="w-4 h-4 shrink-0" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M14 5l7 7m0 0l-7 7m7-7H3" />
            </svg>
          </button>
        </div>
      </form>

      {/* Generic Suggestions Dropdown */}
      {showSuggestions && suggestions.length > 0 && (
        <div
          role="listbox"
          className="absolute left-0 right-0 top-full mt-2 bg-white rounded-2xl border border-slate-200/90 shadow-xl overflow-hidden z-50 divide-y divide-slate-100"
        >
          <div className="px-3 py-1.5 bg-slate-50 text-[10px] font-bold uppercase tracking-wider text-slate-400 flex items-center justify-between">
            <span>Suggestions</span>
            {loadingSuggestions && <span className="text-indigo-600 animate-pulse">Searching...</span>}
          </div>
          <ul className="max-h-60 overflow-y-auto">
            {suggestions.map((item, idx) => {
              const isSelected = idx === selectedIndex;
              return (
                <li
                  key={idx}
                  role="option"
                  aria-selected={isSelected}
                  onMouseDown={() => handleSelectSuggestion(item)}
                  className={`px-4 py-2.5 text-xs sm:text-sm text-slate-800 hover:bg-indigo-50 hover:text-indigo-700 flex items-center gap-2.5 cursor-pointer transition ${
                    isSelected ? 'bg-indigo-50 text-indigo-700 font-semibold' : ''
                  }`}
                >
                  <svg className="w-3.5 h-3.5 text-slate-400 shrink-0" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M21 21l-6-6m2-5a7 7 0 11-14 0 7 7 0 0114 0z" />
                  </svg>
                  <span className="truncate">{item}</span>
                </li>
              );
            })}
          </ul>
        </div>
      )}
    </div>
  );
};

export default SearchBar;
