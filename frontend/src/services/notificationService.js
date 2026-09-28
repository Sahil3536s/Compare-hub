import apiClient from './api';

export const getNotifications = async () => {
  const response = await apiClient.get('/notifications');
  return response.data; // { unreadCount: 2, notifications: [...] }
};

export const markNotificationAsRead = async (id) => {
  const response = await apiClient.patch(`/notifications/${id}/read`);
  return response.data;
};

export const markAllNotificationsAsRead = async () => {
  const response = await apiClient.patch('/notifications/read-all');
  return response.data;
};

export default {
  getNotifications,
  markNotificationAsRead,
  markAllNotificationsAsRead,
};
