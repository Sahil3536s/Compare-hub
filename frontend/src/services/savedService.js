import apiClient from './api';

// Saved Products
export const getSavedProducts = async () => {
  const response = await apiClient.get('/saved/products');
  return response.data; // [{ id, userId, productId, productName, productCategory, productImageUrl, createdAt }]
};

export const saveProduct = async (param) => {
  let payload = {};
  if (typeof param === 'object' && param !== null) {
    payload = {
      productId: Number(param.productId || param.id),
      savedPrice: param.savedPrice != null ? Number(param.savedPrice) : (param.price ? Number(param.price) : undefined),
      savedMerchant: param.savedMerchant || param.merchant || undefined,
    };
  } else {
    payload = { productId: Number(param) };
  }
  const response = await apiClient.post('/saved/products', payload);
  return response.data;
};

export const removeSavedProduct = async (savedProductId) => {
  const response = await apiClient.delete(`/saved/products/${savedProductId}`);
  return response.data;
};

// Saved Routes
export const getSavedRoutes = async () => {
  const response = await apiClient.get('/saved/routes');
  return response.data; // [{ id, pickup, destination, rideType, createdAt }]
};

export const saveRoute = async ({ pickup, destination, rideType = 'all' }) => {
  const response = await apiClient.post('/saved/routes', {
    pickup: pickup.trim(),
    destination: destination.trim(),
    rideType: rideType || 'all',
  });
  return response.data;
};

export const deleteSavedRoute = async (routeId) => {
  const response = await apiClient.delete(`/saved/routes/${routeId}`);
  return response.data;
};

export default {
  getSavedProducts,
  saveProduct,
  removeSavedProduct,
  getSavedRoutes,
  saveRoute,
  deleteSavedRoute,
};
