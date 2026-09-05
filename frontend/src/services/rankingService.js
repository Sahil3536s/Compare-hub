import apiClient from './api';

export const getRankingPreferences = async () => {
  const response = await apiClient.get('/ranking/preferences');
  return response.data;
};

export const saveRankingPreferences = async (preferences) => {
  const response = await apiClient.post('/ranking/preferences', preferences);
  return response.data;
};

export default {
  getRankingPreferences,
  saveRankingPreferences,
};
