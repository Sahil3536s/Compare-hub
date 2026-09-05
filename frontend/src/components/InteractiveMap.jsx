import React, { useState } from 'react';

export const InteractiveMap = ({
  pickup,
  destination,
  distanceKm = 14.2,
  durationMinutes = 32,
  polylineCoordinates = [],
}) => {
  const [zoomLevel, setZoomLevel] = useState(1);

  return (
    <div className="relative w-full h-[320px] sm:h-[400px] lg:h-[480px] bg-slate-900 rounded-3xl overflow-hidden border border-slate-800 shadow-xl flex items-center justify-center select-none min-w-0">
      
      {/* Map Grid Pattern Background */}
      <div 
        className="absolute inset-0 opacity-20 pointer-events-none"
        style={{
          backgroundImage: `radial-gradient(#6366f1 1.5px, transparent 1.5px), radial-gradient(#6366f1 1.5px, #0f172a 1.5px)`,
          backgroundSize: '36px 36px',
          backgroundPosition: '0 0, 18px 18px',
          transform: `scale(${zoomLevel})`,
          transition: 'transform 0.3s ease'
        }}
      />

      {/* Decorative City Road Grid Lines */}
      <svg className="absolute inset-0 w-full h-full stroke-slate-800/80 pointer-events-none" xmlns="http://www.w3.org/2000/svg">
        <line x1="10%" y1="0" x2="10%" y2="100%" strokeWidth="1" strokeDasharray="4,4" />
        <line x1="35%" y1="0" x2="35%" y2="100%" strokeWidth="1" />
        <line x1="65%" y1="0" x2="65%" y2="100%" strokeWidth="1" strokeDasharray="6,6" />
        <line x1="85%" y1="0" x2="85%" y2="100%" strokeWidth="1" />
        <line x1="0" y1="25%" x2="100%" y2="25%" strokeWidth="1" />
        <line x1="0" y1="55%" x2="100%" y2="55%" strokeWidth="1.5" strokeDasharray="8,8" />
        <line x1="0" y1="80%" x2="100%" y2="80%" strokeWidth="1" />
      </svg>

      {/* Route Polyline Canvas */}
      <svg viewBox="0 0 1000 480" preserveAspectRatio="none" className="absolute inset-0 w-full h-full overflow-visible pointer-events-none" xmlns="http://www.w3.org/2000/svg">
        <defs>
          <linearGradient id="routeGradient" x1="0%" y1="0%" x2="100%" y2="100%">
            <stop offset="0%" stopColor="#10b981" />
            <stop offset="50%" stopColor="#6366f1" />
            <stop offset="100%" stopColor="#f43f5e" />
          </linearGradient>
          <filter id="glow" x="-20%" y="-20%" width="140%" height="140%">
            <feGaussianBlur stdDeviation="3" result="blur" />
            <feComposite in="SourceGraphic" in2="blur" operator="over" />
          </filter>
        </defs>

        {/* Outer Shadow Path */}
        <path
          d="M 180 340 Q 340 220, 540 260 T 820 140"
          fill="none"
          stroke="#4f46e5"
          strokeWidth="8"
          strokeOpacity="0.3"
          strokeLinecap="round"
        />

        {/* Main Glowing Route Polyline */}
        <path
          d="M 180 340 Q 340 220, 540 260 T 820 140"
          fill="none"
          stroke="url(#routeGradient)"
          strokeWidth="4.5"
          strokeLinecap="round"
          filter="url(#glow)"
        />

        {/* Animated Dash Overlay */}
        <path
          d="M 180 340 Q 340 220, 540 260 T 820 140"
          fill="none"
          stroke="#ffffff"
          strokeWidth="2"
          strokeDasharray="8,12"
          strokeLinecap="round"
          className="animate-pulse"
        />
      </svg>

      {/* Pickup Marker (Green / Emerald) */}
      <div className="absolute top-[70%] left-[18%] -translate-x-1/2 -translate-y-full flex flex-col items-center group cursor-pointer z-20">
        <div className="relative">
          <div className="w-7 h-7 sm:w-8 sm:h-8 rounded-2xl bg-emerald-500 text-white flex items-center justify-center font-bold text-xs shadow-lg ring-4 ring-emerald-500/20">
            📍
          </div>
          <span className="animate-ping absolute -top-1 -right-1 w-3 h-3 rounded-full bg-emerald-400 opacity-75"></span>
        </div>
        <div className="bg-slate-900/90 backdrop-blur-xs border border-emerald-500/40 text-emerald-300 text-[10px] sm:text-[11px] font-bold px-2 py-0.5 rounded-lg mt-1 shadow-md max-w-[120px] sm:max-w-[140px] truncate text-center">
          {pickup?.address ? pickup.address.split(',')[0] : 'Pickup'}
        </div>
      </div>

      {/* Destination Marker (Rose / Red) */}
      <div className="absolute top-[30%] left-[82%] -translate-x-1/2 -translate-y-full flex flex-col items-center group cursor-pointer z-20">
        <div className="relative">
          <div className="w-7 h-7 sm:w-8 sm:h-8 rounded-2xl bg-rose-500 text-white flex items-center justify-center font-bold text-xs shadow-lg ring-4 ring-rose-500/20">
            🏁
          </div>
        </div>
        <div className="bg-slate-900/90 backdrop-blur-xs border border-rose-500/40 text-rose-300 text-[10px] sm:text-[11px] font-bold px-2 py-0.5 rounded-lg mt-1 shadow-md max-w-[120px] sm:max-w-[140px] truncate text-center">
          {destination?.address ? destination.address.split(',')[0] : 'Destination'}
        </div>
      </div>

      {/* Route Badge in Top Left of Map */}
      <div className="absolute top-3 left-3 sm:top-5 sm:left-5 z-30 bg-slate-900/90 backdrop-blur-md border border-slate-700/80 px-3 py-2 sm:px-4 sm:py-3 rounded-2xl shadow-xl flex items-center gap-2.5 sm:gap-4">
        <div>
          <span className="text-[9px] sm:text-[10px] font-bold uppercase tracking-wider text-slate-400 block">Road Dist.</span>
          <span className="text-xs sm:text-base font-black text-white font-mono">{distanceKm} km</span>
        </div>
        <div className="w-[1px] h-6 sm:h-7 bg-slate-700"></div>
        <div>
          <span className="text-[9px] sm:text-[10px] font-bold uppercase tracking-wider text-slate-400 block">Travel Time</span>
          <span className="text-xs sm:text-base font-black text-emerald-400 font-mono">~{durationMinutes} mins</span>
        </div>
      </div>

      {/* Map Zoom Controls */}
      <div className="absolute bottom-3 right-3 sm:bottom-5 sm:right-5 z-30 flex flex-col gap-1 bg-slate-900/90 backdrop-blur-md border border-slate-700 p-1 rounded-xl shadow-lg">
        <button
          onClick={() => setZoomLevel((z) => Math.min(1.4, z + 0.1))}
          className="w-7 h-7 sm:w-8 sm:h-8 flex items-center justify-center text-white hover:bg-slate-800 rounded-lg transition font-bold text-xs sm:text-sm cursor-pointer"
          title="Zoom In"
          aria-label="Zoom in map"
        >
          +
        </button>
        <button
          onClick={() => setZoomLevel((z) => Math.max(0.8, z - 0.1))}
          className="w-7 h-7 sm:w-8 sm:h-8 flex items-center justify-center text-white hover:bg-slate-800 rounded-lg transition font-bold text-xs sm:text-sm cursor-pointer"
          title="Zoom Out"
          aria-label="Zoom out map"
        >
          −
        </button>
      </div>

    </div>
  );
};

export default InteractiveMap;
