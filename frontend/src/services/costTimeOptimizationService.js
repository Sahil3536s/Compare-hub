import api from './api';

export const optimizeCostVsTime = async ({
  costWeight = 50,
  timeWeight = 50,
  options = [],
  domain = 'GENERAL',
}) => {
  try {
    const res = await api.post('/optimize/cost-time', {
      costWeight: Number(costWeight),
      timeWeight: Number(timeWeight),
      options,
      domain,
    });
    return res.data;
  } catch (error) {
    console.error('Cost vs Time optimization failed:', error);
    throw error;
  }
};

/**
 * High-performance client-side Cost vs Time normalizer and ranking utility.
 * Allows smooth, 60fps slider dragging with instantaneous re-ordering.
 */
export const rankCostTimeClientSide = (
  items = [],
  costWeight = 50, // 0 to 100 (% importance of saving money)
  getCost = (item) => item.price || item.totalCost || item.cost || (item.fare ? item.fare : 0),
  getDuration = (item) => item.durationMinutes || item.estimatedTravelTimeMinutes || item.etaMinutes || 0
) => {
  if (!items || items.length === 0) return [];

  const wCost = Math.max(0, Math.min(100, costWeight)) / 100.0;
  const wTime = 1.0 - wCost;

  const costs = items.map((i) => Number(getCost(i)) || 0);
  const durations = items.map((i) => Number(getDuration(i)) || 0);

  const minCost = Math.min(...costs);
  const maxCost = Math.max(...costs);

  const minTime = Math.min(...durations);
  const maxTime = Math.max(...durations);

  const scored = items.map((item) => {
    const cost = Number(getCost(item)) || 0;
    const time = Number(getDuration(item)) || 0;

    const costScore = maxCost === minCost ? 100.0 : ((maxCost - cost) / (maxCost - minCost)) * 100.0;
    const timeScore = maxTime === minTime ? 100.0 : ((maxTime - time) / (maxTime - minTime)) * 100.0;

    const compositeScore = costScore * wCost + timeScore * wTime;

    return {
      ...item,
      _costScore: Math.round(costScore * 10) / 10,
      _timeScore: Math.round(timeScore * 10) / 10,
      _compositeScore: Math.round(compositeScore * 10) / 10,
    };
  });

  return scored.sort((a, b) => b._compositeScore - a._compositeScore);
};
