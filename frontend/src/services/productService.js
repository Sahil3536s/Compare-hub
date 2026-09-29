import apiClient from './api';

export const searchProducts = async ({
  query = '',
  page = 1,
  pageSize = 20,
  merchant = '',
  brand = '',
  category = '',
  minPrice = '',
  maxPrice = '',
  inStock = '',
  sortBy = 'best',
  minRating = '',
  ram = '',
  storage = '',
  delivery = '',
  attributeFilters = {},
} = {}) => {
  const params = {};
  if (query) params.q = query;
  if (page) params.page = page;
  if (pageSize) params.pageSize = pageSize;
  if (merchant && merchant !== 'all') params.merchant = merchant;
  if (brand && brand !== 'all') params.brand = brand;
  if (category && category !== 'All Categories' && category !== 'all') params.category = category;
  if (minPrice) params.minPrice = minPrice;
  if (maxPrice) params.maxPrice = maxPrice;
  if (inStock !== '') params.inStock = inStock;
  if (sortBy) params.sortBy = sortBy;
  if (minRating) params.minRating = minRating;
  if (ram && ram !== 'all') params.ram = ram;
  if (storage && storage !== 'all') params.storage = storage;
  if (delivery && delivery !== 'all') params.delivery = delivery;

  // Append dynamic attribute filters (e.g. Size, Color, Capacity)
  if (attributeFilters && typeof attributeFilters === 'object') {
    Object.entries(attributeFilters).forEach(([k, v]) => {
      if (v && v !== 'all') {
        params[k] = v;
      }
    });
  }

  const response = await apiClient.get('/products/search', { params });
  return response.data; // { query, totalOffers, page, pageSize, totalPages, hasMore, cheapestPrice, cheapestMerchant, offers: [...], dynamicFilters: {...}, failedProviders: [...] }
};

export const getSearchSuggestions = async (query = '', signal) => {
  if (!query || query.trim().length < 2) return [];
  try {
    const response = await apiClient.get('/products/suggest', {
      params: { q: query.trim() },
      signal,
    });
    return response.data || [];
  } catch (err) {
    if (err.name === 'CanceledError' || err.code === 'ERR_CANCELED') {
      return [];
    }
    return [];
  }
};

export const getProductPriceHistory = async (productId, period = '30D') => {
  const response = await apiClient.get(`/products/${productId}/price-history`, {
    params: { period },
  });
  return response.data;
};

export const getProductDetails = async (productId) => {
  const response = await apiClient.get(`/products/${productId}`);
  return response.data;
};

export default {
  searchProducts,
  getSearchSuggestions,
  getProductPriceHistory,
  getProductDetails,
};
