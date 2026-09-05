import apiClient from './api';

export const optimizeCart = async ({ items, strategy = 'MINIMIZE_PRICE' }) => {
  const response = await apiClient.post('/cart/optimize', {
    items,
    strategy,
  });
  return response.data;
};

export default {
  optimizeCart,
};
