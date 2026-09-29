import React, { useEffect, useState } from 'react';
import { useParams } from 'react-router-dom';
import { fetchPriceHistory } from '../services/priceHistoryService';
import { Line } from 'react-chartjs-2';
import Spinner from '../components/Spinner';

const PERIOD_OPTIONS = [
  { label: '7 Days', value: '7D' },
  { label: '30 Days', value: '30D' },
  { label: '90 Days', value: '90D' },
];

function Metric({ label, value }) {
  return (
    <div className="bg-gray-50 p-2 rounded text-center">
      <span className="block text-sm text-gray-500">{label}</span>
      <span className="block font-medium text-lg">{value?.toFixed(2)}</span>
    </div>
  );
}

export default function PriceHistoryPage() {
  const { productId } = useParams();
  const [period, setPeriod] = useState('30D');
  const [data, setData] = useState(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);

  const loadHistory = async () => {
    setLoading(true);
    setError(null);
    try {
      const resp = await fetchPriceHistory(productId, period);
      setData(resp);
    } catch (e) {
      setError('Failed to load price history');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadHistory();
  }, [productId, period]);

  if (loading) return <Spinner />;
  if (error) return <div className="text-red-600">{error}</div>;
  if (!data) return null;

  const insufficient = !data.pricePoints || data.pricePoints.length === 0;
  if (insufficient) {
    return (
      <div className="p-4 text-gray-600">
        <p>{data.analysisText || 'Insufficient price history.'}</p>
      </div>
    );
  }

  const chartData = {
    labels: data.pricePoints.map(p => p.date),
    datasets: [
      {
        label: `${data.productName} price`,
        data: data.pricePoints.map(p => Number(p.price)),
        borderColor: '#3b82f6',
        backgroundColor: 'rgba(59,130,246,0.1)',
        fill: true,
        tension: 0.2,
      },
    ],
  };

  return (
    <section className="p-4">
      <h2 className="text-xl font-semibold mb-4">Price History</h2>

      <div className="mb-4 flex gap-2">
        {PERIOD_OPTIONS.map(opt => (
          <button
            key={opt.value}
            className={`px-3 py-1 rounded ${period === opt.value ? 'bg-indigo-600 text-white' : 'bg-gray-200 text-gray-800'}`}
            onClick={() => setPeriod(opt.value)}
          >
            {opt.label}
          </button>
        ))}
      </div>

      <div className="grid grid-cols-2 md:grid-cols-3 gap-4 mb-6">
        <Metric label="Current" value={data.currentPrice} />
        <Metric label="Lowest" value={data.lowestPrice} />
        <Metric label="Highest" value={data.highestPrice} />
        <Metric label="Average" value={data.averagePrice} />
      </div>

      <div className="max-w-xl mx-auto">
        <Line data={chartData} />
      </div>

      {data.analysisText && <p className="mt-4 text-gray-700">{data.analysisText}</p>}
    </section>
  );
}
