import React, { useState } from 'react';

export const SearchBar = ({
  placeholder = 'Search products, brands, flights or rides...',
  initialQuery = '',
  categories = [],
  selectedCategory = '',
  onCategoryChange,
  onSearch,
  className = '',
  size = 'md', // 'sm' | 'md' | 'lg'
}) => {
  const [query, setQuery] = useState(initialQuery);

  const handleSubmit = (e) => {
    e.preventDefault();
    if (onSearch) {
      onSearch(query);
    }
  };

  const handleClear = () => {
    setQuery('');
    if (onSearch) {
      onSearch('');
    }
  };

  const sizeClasses = {
    sm: 'py-2.5 px-3 text-xs sm:text-sm',
    md: 'py-3 px-3.5 text-sm sm:text-base',
    lg: 'py-3.5 sm:py-4 px-4 sm:px-5 text-sm sm:text-base md:text-lg',
  };

  return (
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
          onChange={(e) => setQuery(e.target.value)}
          placeholder={placeholder}
          aria-label="Search query"
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
  );
};

export default SearchBar;
