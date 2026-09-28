import api from './api';

export const decisionAdvisorService = {
  /**
   * Evaluate custom decision context
   * @param {Object} context - DecisionContext object
   * @returns {Promise<Object>} DecisionRecommendation
   */
  evaluateDecision: async (context) => {
    const response = await api.post('/advisor/evaluate', context);
    return response.data;
  },

  /**
   * Get sample product decision recommendation
   * @returns {Promise<Object>}
   */
  getSampleProductAdvise: async () => {
    const response = await api.get('/advisor/sample/product');
    return response.data;
  },

  /**
   * Get sample travel decision recommendation
   * @returns {Promise<Object>}
   */
  getSampleTravelAdvise: async () => {
    const response = await api.get('/advisor/sample/travel');
    return response.data;
  },

  /**
   * Get default sample recommendation
   * @returns {Promise<Object>}
   */
  getSampleAdvise: async () => {
    const response = await api.get('/advisor/sample');
    return response.data;
  }
};

export default decisionAdvisorService;
