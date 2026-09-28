import apiClient from './api';

export const checkHealth = async () => {
  const response = await apiClient.get('/health');
  return response.data;
};

export default {
  checkHealth,
};
