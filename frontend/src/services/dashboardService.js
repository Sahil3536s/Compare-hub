import apiClient from './api';

export const getDashboardData = async () => {
  const response = await apiClient.get('/dashboard');
  return response.data;
};

export default {
  getDashboardData,
};
