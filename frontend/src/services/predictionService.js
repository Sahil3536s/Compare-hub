import apiClient from './api';

/**
 * Fetch ML price prediction for a product.
 *
 * Calls: GET /api/products/{productId}/prediction
 *
 * Spring Boot proxies the call to the FastAPI ML service.
 * React NEVER contacts the ML service directly.
 *
 * @param {number|string} productId
 * @returns {Promise<Object>} PricePredictionResponseDto
 *   status: 'SUCCESS' | 'INSUFFICIENT_DATA' | 'ML_UNAVAILABLE' | 'MODEL_NOT_LOADED' | 'ERROR'
 */
export const getProductPrediction = async (productId) => {
  const response = await apiClient.get(`/products/${productId}/prediction`);
  return response.data;
};

export default { getProductPrediction };
