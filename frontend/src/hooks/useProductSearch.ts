import { useState, useEffect, useCallback } from 'react';
import productService from '../services/productService';

export interface NormalizedProductOfferDto {
  productId: string;
  productName: string;
  brand?: string;
  merchant?: string;
  price: number;
  originalPrice?: number;
  discount?: number;
  rating?: number;
  delivery?: string;
  availability?: string;
  attributes?: Record<string, string | number>;
  imageUrl?: string;
  dataSource?: string;
  live?: boolean;
}

export interface ProviderLog {
  providerName: string;
  providerMode: string;
  status: string;
  rawResultCount: number;
  normalizedResultCount: number;
  acceptedResultCount: number;
  failureReason?: string;
}

export interface SearchResponse {
  query: string;
  totalOffers: number;
  cheapestPrice?: number;
  cheapestMerchant?: string;
  offers: NormalizedProductOfferDto[];
  providerDiagnostics: ProviderLog[];
  successfulProviders: string[];
  failedProviders: string[];
  page: number;
  pageSize: number;
  totalPages: number;
  hasMore: boolean;
  dynamicFilters: Record<string, string[]>;
  availableCategories: string[];
  availableBrands: string[];
  availableMerchants: string[];
}

/**
 * Hook to perform product search with pagination and expose loading/error state.
 */
export function useProductSearch(initialQuery: string = '') {
  const [query, setQuery] = useState(initialQuery);
  const [page, setPage] = useState(1);
  const [pageSize] = useState(20);
  const [data, setData] = useState<SearchResponse | null>(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const fetch = useCallback(async (q: string, p: number) => {
    setLoading(true);
    setError(null);
    try {
      const result = await productService.searchProducts({ query: q, page: p, pageSize });
      setData(result as SearchResponse);
    } catch (e: any) {
      setError(e.message || 'Search failed');
      setData(null);
    } finally {
      setLoading(false);
    }
  }, [pageSize]);

  // initial load
  useEffect(() => {
    if (query) fetch(query, page);
  }, [query, page, fetch]);

  const setSearchQuery = (newQuery: string) => {
    setQuery(newQuery);
    setPage(1);
  };

  const goToPage = (newPage: number) => {
    setPage(newPage);
  };

  return { query, setSearchQuery, page, pageSize, data, loading, error, goToPage };
}
