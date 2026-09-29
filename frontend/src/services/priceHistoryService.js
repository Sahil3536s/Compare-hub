import axios from 'axios';

/**
 * Fetch price‑history for a product.
 * @param {number} productId - the product identifier
 * @param {string} period   - one of "7D", "30D", "90D" (default "30D")
 * @returns {Promise<any>}  - resolves to ProductPriceHistoryResponseDto
 */
export const fetchPriceHistory = async (productId, period = '30D') => {
  const response = await axios.get(`/api/price-history/${productId}`, {
    params: { period },
  });
  return response.data;
};
