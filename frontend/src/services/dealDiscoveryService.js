import api from './api';

export const getSmartDeals = async ({ category = 'ALL', page = 0, size = 8 } = {}) => {
  const response = await api.get('/deals/smart', {
    params: {
      category,
      page,
      size,
    },
  });
  return response.data;
};

export default {
  getSmartDeals,
};
