import apiClient from './api';

/**
 * Get all active price alerts for the authenticated user.
 * Calls: GET /api/alerts
 */
export const getPriceAlerts = async () => {
  const response = await apiClient.get('/alerts');
  return response.data;
};

/**
 * Create a new price alert for a product.
 * Calls: POST /api/alerts
 * @param {Object} alertData - { productId, productName, targetPrice, merchant }
 */
export const createPriceAlert = async ({ productId, productName, targetPrice, merchant }) => {
  const response = await apiClient.post('/alerts', {
    productId: Number(productId),
    productName: productName || undefined,
    merchant: merchant || undefined,
    targetPrice: Number(targetPrice),
    active: true,
  });
  return response.data;
};

/**
 * Toggle the active status of an alert.
 * Calls: PATCH /api/alerts/{id}/status?active={active}
 */
export const togglePriceAlertStatus = async (alertId, active) => {
  const response = await apiClient.patch(`/alerts/${alertId}/status`, null, {
    params: { active },
  });
  return response.data;
};

/**
 * Delete a price alert.
 * Calls: DELETE /api/alerts/{id}
 */
export const deletePriceAlert = async (alertId) => {
  const response = await apiClient.delete(`/alerts/${alertId}`);
  return response.data;
};

export default {
  getPriceAlerts,
  createPriceAlert,
  togglePriceAlertStatus,
  deletePriceAlert,
};
