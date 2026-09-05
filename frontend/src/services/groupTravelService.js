import api from './api';

export const optimizeGroupTravel = async ({
  origin,
  destination,
  travelDate,
  numberOfTravelers = 1,
  budget = null,
  priority = 'BALANCED',
}) => {
  try {
    const res = await api.post('/travel/group-optimize', {
      origin,
      destination,
      travelDate,
      numberOfTravelers: Number(numberOfTravelers),
      budget: budget ? Number(budget) : null,
      priority,
    });
    return res.data?.data || res.data;
  } catch (error) {
    console.error('Group travel optimization failed:', error);
    throw error;
  }
};
