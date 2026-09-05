import api from './api';

export const optimizeSmartJourney = async (requestData) => {
  const response = await api.post('/journey/optimize', requestData);
  return response.data;
};

export const getSampleSmartJourney = async (params = {}) => {
  const response = await api.get('/journey/sample', { params });
  return response.data;
};

export default {
  optimizeSmartJourney,
  getSampleSmartJourney,
};
