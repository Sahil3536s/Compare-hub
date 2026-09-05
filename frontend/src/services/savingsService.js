import api from './api';

export const getSavingsDashboard = async (userId = null) => {
  try {
    const params = userId ? { userId } : {};
    const res = await api.get('/savings/dashboard', { params });
    return res.data?.data || res.data;
  } catch (error) {
    console.error('Failed to load savings dashboard:', error);
    throw error;
  }
};

export const recordSavingsEvent = async (eventData, userId = null) => {
  try {
    const params = userId ? { userId } : {};
    const res = await api.post('/savings/events', eventData, { params });
    return res.data?.data || res.data;
  } catch (error) {
    console.error('Failed to record savings event:', error);
    throw error;
  }
};

export const confirmSavingsEvent = async (eventId) => {
  try {
    const res = await api.post(`/savings/events/${eventId}/confirm`);
    return res.data?.data || res.data;
  } catch (error) {
    console.error('Failed to confirm savings event:', error);
    throw error;
  }
};
