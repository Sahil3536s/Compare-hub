import api from './api';

export const optimizeBudgetPlan = async (constraint) => {
  const response = await api.post('/budget/optimize', constraint);
  return response.data;
};

export const parseBudgetQuery = async (query) => {
  const response = await api.post('/budget/parse-query', { query });
  return response.data;
};

export const getSampleBudgetPlan = async () => {
  const response = await api.get('/budget/sample');
  return response.data;
};

export default {
  optimizeBudgetPlan,
  parseBudgetQuery,
  getSampleBudgetPlan,
};
