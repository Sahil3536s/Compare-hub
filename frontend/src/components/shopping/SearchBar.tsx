import { useState, useEffect, FormEvent } from 'react';
import { FaSearch, FaTimes, FaSpinner } from 'react-icons/fa';

interface SearchBarProps {
  initialQuery?: string;
  onSearch: (query: string) => void;
  loading: boolean;
  clearable?: boolean;
}

export const SearchBar: React.FC<SearchBarProps> = ({
  initialQuery = '',
  onSearch,
  loading,
  clearable = true,
}) => {
  const [value, setValue] = useState(initialQuery);

  useEffect(() => {
    setValue(initialQuery);
  }, [initialQuery]);

  const handleSubmit = (e: FormEvent) => {
    e.preventDefault();
    const trimmed = value.trim();
    if (trimmed) {
      onSearch(trimmed);
    }
  };

  const handleClear = () => {
    setValue('');
    onSearch('');
  };

  return (
    <form
      onSubmit={handleSubmit}
      className="flex items-center gap-2 p-2 border rounded-md bg-white shadow-sm"
      aria-label="Product search form"
    >
      <input
        type="text"
        className="flex-grow px-3 py-2 border rounded focus:outline-none focus:ring-2 focus:ring-blue-500"
        placeholder="Search products…"
        value={value}
        onChange={(e) => setValue(e.target.value)}
        aria-label="Search query"
        disabled={loading}
      />
      {loading ? (
        <FaSpinner className="animate-spin text-gray-500" aria-label="Loading" />
      ) : (
        <button
          type="submit"
          className="p-2 text-white bg-blue-600 rounded hover:bg-blue-700"
          aria-label="Search"
        >
          <FaSearch />
        </button>
      )}
      {clearable && value && !loading && (
        <button
          type="button"
          onClick={handleClear}
          className="p-2 text-gray-600 hover:text-gray-800"
          aria-label="Clear search"
        >
          <FaTimes />
        </button>
      )}
    </form>
  );
};
