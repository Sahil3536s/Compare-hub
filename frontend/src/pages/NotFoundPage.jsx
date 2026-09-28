import React from 'react';
import { Link } from 'react-router-dom';
import { ROUTES } from '../utils/constants';

export const NotFoundPage = () => {
  return (
    <div className="max-w-7xl mx-auto px-4 py-20 text-center">
      <h1 className="text-6xl font-extrabold text-slate-900 mb-4">404</h1>
      <p className="text-lg text-slate-600 mb-6">The page you are looking for does not exist.</p>
      <Link
        to={ROUTES.HOME}
        className="inline-flex items-center px-4 py-2 rounded-lg bg-indigo-600 text-white text-sm font-medium hover:bg-indigo-700 transition shadow-sm"
      >
        Return to Home
      </Link>
    </div>
  );
};

export default NotFoundPage;
