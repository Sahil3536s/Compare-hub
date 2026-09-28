import React from 'react';

export const DashboardSkeleton = () => {
  return (
    <div className="space-y-8 animate-pulse" data-testid="dashboard-skeleton">
      {/* Welcome Header Skeleton */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
        <div className="space-y-2">
          <div className="h-8 bg-slate-200 rounded-xl w-64 sm:w-80"></div>
          <div className="h-4 bg-slate-200 rounded-lg w-48 sm:w-96"></div>
        </div>
        <div className="h-10 bg-slate-200 rounded-xl w-36"></div>
      </div>

      {/* 5 Summary Cards Grid Skeleton */}
      <div className="grid grid-cols-2 sm:grid-cols-3 lg:grid-cols-5 gap-3.5 sm:gap-4">
        {[1, 2, 3, 4, 5].map((i) => (
          <div key={i} className="bg-white rounded-2xl border border-slate-200 p-4 space-y-2 shadow-2xs">
            <div className="h-3 bg-slate-200 rounded w-16"></div>
            <div className="h-7 bg-slate-200 rounded w-20"></div>
            <div className="h-2.5 bg-slate-200 rounded w-24"></div>
          </div>
        ))}
      </div>

      {/* Sections Grid Skeleton */}
      <div className="grid grid-cols-1 lg:grid-cols-12 gap-8">
        {/* Main Content Area (8 Cols) */}
        <div className="lg:col-span-8 space-y-8">
          {/* Price Drops Section Skeleton */}
          <div className="bg-white rounded-3xl border border-slate-200 p-6 space-y-4 shadow-xs">
            <div className="flex justify-between items-center">
              <div className="h-6 bg-slate-200 rounded-lg w-48"></div>
              <div className="h-4 bg-slate-200 rounded w-20"></div>
            </div>
            <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
              {[1, 2].map((i) => (
                <div key={i} className="p-4 rounded-2xl border border-slate-100 bg-slate-50/50 space-y-3">
                  <div className="flex gap-3">
                    <div className="w-14 h-14 bg-slate-200 rounded-xl shrink-0"></div>
                    <div className="flex-1 space-y-1.5">
                      <div className="h-4 bg-slate-200 rounded w-3/4"></div>
                      <div className="h-3 bg-slate-200 rounded w-1/2"></div>
                    </div>
                  </div>
                  <div className="h-6 bg-slate-200 rounded w-full"></div>
                </div>
              ))}
            </div>
          </div>

          {/* Saved Products Section Skeleton */}
          <div className="bg-white rounded-3xl border border-slate-200 p-6 space-y-4 shadow-xs">
            <div className="flex justify-between items-center">
              <div className="h-6 bg-slate-200 rounded-lg w-44"></div>
              <div className="h-4 bg-slate-200 rounded w-16"></div>
            </div>
            <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
              {[1, 2].map((i) => (
                <div key={i} className="p-4 rounded-2xl border border-slate-100 bg-slate-50/50 space-y-3">
                  <div className="flex gap-3">
                    <div className="w-14 h-14 bg-slate-200 rounded-xl shrink-0"></div>
                    <div className="flex-1 space-y-1.5">
                      <div className="h-4 bg-slate-200 rounded w-3/4"></div>
                      <div className="h-3 bg-slate-200 rounded w-1/2"></div>
                    </div>
                  </div>
                  <div className="h-8 bg-slate-200 rounded w-full"></div>
                </div>
              ))}
            </div>
          </div>
        </div>

        {/* Sidebar Feed Area (4 Cols) */}
        <div className="lg:col-span-4 space-y-8">
          {/* Active Alerts Skeleton */}
          <div className="bg-white rounded-3xl border border-slate-200 p-6 space-y-4 shadow-xs">
            <div className="h-5 bg-slate-200 rounded-lg w-32"></div>
            {[1, 2, 3].map((i) => (
              <div key={i} className="p-3 rounded-xl border border-slate-100 space-y-2">
                <div className="h-4 bg-slate-200 rounded w-3/4"></div>
                <div className="h-3 bg-slate-200 rounded w-1/2"></div>
              </div>
            ))}
          </div>

          {/* Recent Activity Skeleton */}
          <div className="bg-white rounded-3xl border border-slate-200 p-6 space-y-4 shadow-xs">
            <div className="h-5 bg-slate-200 rounded-lg w-36"></div>
            {[1, 2, 3].map((i) => (
              <div key={i} className="flex gap-3 items-center">
                <div className="w-8 h-8 rounded-full bg-slate-200 shrink-0"></div>
                <div className="flex-1 space-y-1">
                  <div className="h-3.5 bg-slate-200 rounded w-4/5"></div>
                  <div className="h-2.5 bg-slate-200 rounded w-1/3"></div>
                </div>
              </div>
            ))}
          </div>
        </div>
      </div>
    </div>
  );
};

export default DashboardSkeleton;
