import { useState, useEffect, useCallback } from 'react';
import { checkHealth } from '../services/healthService';

export const useHealthCheck = () => {
  const [status, setStatus] = useState(null); // 'UP' | 'DOWN' | null
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [lastChecked, setLastChecked] = useState(null);

  const performCheck = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      const data = await checkHealth();
      setStatus(data.status);
      setLastChecked(new Date());
    } catch (err) {
      setStatus('DOWN');
      setError(err.message || 'Unable to connect to backend');
      setLastChecked(new Date());
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    performCheck();
  }, [performCheck]);

  return {
    status,
    isConnected: status === 'UP',
    loading,
    error,
    lastChecked,
    refetch: performCheck,
  };
};

export default useHealthCheck;
