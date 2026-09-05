import axios from 'axios';
import { API_BASE_URL } from '../utils/constants';

const apiClient = axios.create({
  baseURL: API_BASE_URL,
  timeout: 10000,
  headers: {
    'Content-Type': 'application/json',
    'Accept': 'application/json',
  },
});

// Request interceptor: attach Bearer token if present
apiClient.interceptors.request.use(
  (config) => {
    const token = localStorage.getItem('comparehub_auth_token');
    if (token) {
      config.headers.Authorization = `Bearer ${token}`;
    }
    return config;
  },
  (error) => Promise.reject(error)
);

// Helper to sanitize error message and prevent Java stack traces or raw exceptions from reaching users
const sanitizeErrorMessage = (rawMessage, status) => {
  if (!rawMessage || typeof rawMessage !== 'string') {
    return getDefaultMessageForStatus(status);
  }

  // Detect and filter out Java stack traces or internal exception class patterns
  const isJavaStackTrace = 
    rawMessage.includes('java.lang.') ||
    rawMessage.includes('org.springframework.') ||
    rawMessage.includes('org.hibernate.') ||
    rawMessage.includes('Exception at ') ||
    rawMessage.includes('NullPointerException') ||
    rawMessage.includes('Cannot invoke');

  if (isJavaStackTrace) {
    return 'An unexpected server error occurred. Please try again later.';
  }

  return rawMessage;
};

const getDefaultMessageForStatus = (status) => {
  switch (status) {
    case 400:
      return 'Invalid request details. Please check your inputs.';
    case 401:
      return 'Authentication required or session expired. Please sign in.';
    case 403:
      return 'You do not have permission to access this resource.';
    case 404:
      return 'The requested resource was not found.';
    case 409:
      return 'A conflict occurred with this request. It may already exist.';
    case 429:
      return 'Rate limit exceeded. Please wait a few moments before trying again.';
    case 503:
      return 'Comparison services are temporarily unavailable. Please retry shortly.';
    case 504:
      return 'Connection timed out while querying providers. Please retry.';
    default:
      return 'Unable to complete request. Please try again later.';
  }
};

// Response interceptor: handle errors & 401s
apiClient.interceptors.response.use(
  (response) => response,
  (error) => {
    const status = error.response?.status;
    const responseData = error.response?.data;

    // If 401 Unauthorized and not on login/register request, clear local storage
    if (status === 401) {
      const isAuthEndpoint = error.config?.url?.includes('/auth/login') || error.config?.url?.includes('/auth/register');
      if (!isAuthEndpoint) {
        localStorage.removeItem('comparehub_auth_token');
        localStorage.removeItem('comparehub_auth_user');
        window.dispatchEvent(new Event('comparehub:auth:logout'));
      }
    }

    const rawMessage = responseData?.message || error.message;
    const userFriendlyMessage = sanitizeErrorMessage(rawMessage, status);

    const customError = {
      message: userFriendlyMessage,
      status: status || 0,
      validationErrors: responseData?.validationErrors || null,
      data: responseData,
      isNetworkError: !error.response,
    };

    console.warn(`[API Error ${status || 'Network'}]:`, userFriendlyMessage);
    return Promise.reject(customError);
  }
);

export default apiClient;
