import FlightCardSkeleton from './FlightCardSkeleton';
import RideCardSkeleton from './RideCardSkeleton';
import DashboardSkeleton from './dashboard/DashboardSkeleton';

export const LoadingSkeleton = ({ type = 'product-card', count = 3 }) => {
  const items = Array.from({ length: count }, (_, i) => i);

  if (type === 'dashboard') {
    return <DashboardSkeleton />;
  }

  if (type === 'product-card') {
    return (
      <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-6">
        {items.map((i) => (
          <div key={i} className="bg-white rounded-2xl border border-slate-200 p-5 shadow-xs animate-pulse flex flex-col space-y-4">
            <div className="w-full h-48 bg-slate-200 rounded-xl"></div>
            <div className="h-4 bg-slate-200 rounded w-1/3"></div>
            <div className="h-5 bg-slate-200 rounded w-4/5"></div>
            <div className="h-4 bg-slate-200 rounded w-1/2"></div>
            <div className="pt-4 border-t border-slate-100 flex justify-between items-center">
              <div className="h-6 bg-slate-200 rounded w-1/3"></div>
              <div className="h-8 bg-slate-200 rounded w-24"></div>
            </div>
          </div>
        ))}
      </div>
    );
  }

  if (type === 'flight-card') {
    return <FlightCardSkeleton count={count} />;
  }

  if (type === 'ride-card') {
    return <RideCardSkeleton count={count} />;
  }

  return (
    <div className="space-y-3 animate-pulse">
      {items.map((i) => (
        <div key={i} className="h-10 bg-slate-200 rounded-lg w-full"></div>
      ))}
    </div>
  );
};

export default LoadingSkeleton;
