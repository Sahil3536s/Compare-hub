import apiClient from './api';

export const searchProducts = async ({
  query = '',
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
} = {}) => {
  const params = {};
  if (query) params.q = query;
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

  const response = await apiClient.get('/products/search', { params });
  return response.data; // { query, totalOffers, cheapestPrice, cheapestMerchant, offers: [...], failedProviders: [...] }
};

export const getProductPriceHistory = async (productId, period = '30D') => {
  const response = await apiClient.get(`/products/${productId}/price-history`, {
    params: { period },
  });
  return response.data; // { productId, productName, period, currentPrice, lowestPrice, highestPrice, averagePrice, analysisText, pricePoints: [...] }
};

export default {
  searchProducts,
  getProductPriceHistory,
};
