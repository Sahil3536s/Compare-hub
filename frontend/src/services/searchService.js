import apiClient from './api';

export const universalSearch = async (query) => {
  const response = await apiClient.post('/search', { query });
  return response.data;
};

export default {
  universalSearch,
};
