import api from './api';

export const decisionEngineService = {
  /**
   * Evaluate full orchestrated decision request
   * @param {Object} request - UnifiedDecisionRequest
   * @returns {Promise<Object>} UnifiedDecisionResponse
   */
  evaluateDecision: async (request) => {
    const response = await api.post('/decision/evaluate', request);
    return response.data;
  },

  /**
   * Get sample orchestrated decision by vertical type
   * @param {string} type - "PRODUCT", "CART", "FLIGHT", "RIDE", "JOURNEY", "BUDGET", "GROUP_TRAVEL"
   * @returns {Promise<Object>} UnifiedDecisionResponse
   */
  getSampleDecision: async (type = 'PRODUCT') => {
    const response = await api.get(`/decision/sample/${type}`);
    return response.data;
  }
};

export default decisionEngineService;
