import React from 'react';
import { Link } from 'react-router-dom';
import { APP_NAME, ROUTES } from '../utils/constants';

export const Footer = () => {
  return (
    <footer className="bg-slate-900 text-slate-300 border-t border-slate-800 mt-auto">
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-12 lg:py-16">
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-5 gap-8 lg:gap-12">
          
          {/* Brand Col */}
          <div className="lg:col-span-2 space-y-4">
            <div className="flex items-center gap-2.5">
              <div className="w-8 h-8 rounded-xl bg-indigo-500 flex items-center justify-center text-white font-black text-base">
                CH
              </div>
              <span className="font-bold text-xl text-white tracking-tight">
                {APP_NAME}
              </span>
            </div>
            <p className="text-sm text-slate-400 max-w-sm leading-relaxed">
              CompareHub is your universal comparison engine. Discover the best prices across online stores, cheapest flight fares, and affordable ride bookings in one single search.
            </p>
            <div className="flex items-center gap-3 pt-2 text-xs text-slate-400">
              <span className="px-2.5 py-1 rounded bg-slate-800 border border-slate-700 text-emerald-400 font-mono">
                ✓ 100% Real-Time
              </span>
              <span className="px-2.5 py-1 rounded bg-slate-800 border border-slate-700 text-indigo-400 font-mono">
                ✓ Multi-Engine
              </span>
            </div>
          </div>

          {/* Compare Engines */}
          <div>
            <h4 className="text-white text-sm font-semibold mb-4 tracking-wider uppercase">
              Comparison
            </h4>
            <ul className="space-y-2.5 text-sm">
              <li>
                <Link to={ROUTES.SHOPPING} className="hover:text-white transition">
                  Shopping & Products
                </Link>
              </li>
              <li>
                <Link to={ROUTES.FLIGHTS} className="hover:text-white transition">
                  Flight Fares
                </Link>
              </li>
              <li>
                <Link to={ROUTES.RIDES} className="hover:text-white transition">
                  Ride & Cab Fares
                </Link>
              </li>
              <li>
                <Link to={ROUTES.ALERTS} className="hover:text-white transition">
                  Price Alerts
                </Link>
              </li>
              <li>
                <Link to={ROUTES.SAVED} className="hover:text-white transition">
                  Saved Searches
                </Link>
              </li>
            </ul>
          </div>

          {/* Supported Providers */}
          <div>
            <h4 className="text-white text-sm font-semibold mb-4 tracking-wider uppercase">
              Providers
            </h4>
            <ul className="space-y-2 text-xs text-slate-400">
              <li>• Amazon & Flipkart</li>
              <li>• Croma & Reliance Digital</li>
              <li>• IndiGo, Air India & Vistara</li>
              <li>• Uber, Ola & Rapido</li>
              <li>• MakeMyTrip & Cleartrip</li>
            </ul>
          </div>

          {/* Stack & Info */}
          <div>
            <h4 className="text-white text-sm font-semibold mb-4 tracking-wider uppercase">
              Tech Stack
            </h4>
            <ul className="space-y-2 text-xs text-slate-400">
              <li>React 18 + Vite</li>
              <li>Tailwind CSS</li>
              <li>Spring Boot 3 + Java 21</li>
              <li>PostgreSQL DB</li>
              <li>REST APIs</li>
            </ul>
          </div>

        </div>

        <div className="border-t border-slate-800 mt-12 pt-8 flex flex-col sm:flex-row items-center justify-between gap-4 text-xs text-slate-500">
          <p>© {new Date().getFullYear()} {APP_NAME}. All rights reserved.</p>
          <div className="flex items-center gap-6">
            <a href="#privacy" onClick={(e) => e.preventDefault()} className="hover:text-slate-400">Privacy Policy</a>
            <a href="#terms" onClick={(e) => e.preventDefault()} className="hover:text-slate-400">Terms of Service</a>
            <a href="#security" onClick={(e) => e.preventDefault()} className="hover:text-slate-400">Security</a>
          </div>
        </div>
      </div>
    </footer>
  );
};

export default Footer;
