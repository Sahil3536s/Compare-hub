import apiClient from './api';

export const register = async (name, email, password) => {
  const response = await apiClient.post('/auth/register', {
    name,
    email,
    password,
  });
  return response.data; // { token, user: { id, name, email } }
};

export const login = async (email, password) => {
  const response = await apiClient.post('/auth/login', {
    email,
    password,
  });
  return response.data; // { token, user: { id, name, email } }
};

export const getMe = async () => {
  const response = await apiClient.get('/auth/me');
  return response.data; // { id, name, email }
};

export default {
  register,
  login,
  getMe,
};
