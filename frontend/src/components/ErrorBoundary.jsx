import React from 'react';
import { Link } from 'react-router-dom';

/**
 * Robust Error Boundary component for CompareHub.
 * Catches JavaScript errors anywhere in its child component tree,
 * logs the errors in development, and renders a clean, user-friendly fallback
 * instead of an unexplained blank white screen.
 */
export class ErrorBoundary extends React.Component {
  constructor(props) {
    super(props);
    this.state = { hasError: false, error: null, errorInfo: null };
  }

  static getDerivedStateFromError(error) {
    return { hasError: true, error };
  }

  componentDidCatch(error, errorInfo) {
    this.setState({ errorInfo });
    // Log the actual error in development for debugging
    console.error('CompareHub ErrorBoundary caught a rendering exception:', error, errorInfo);
  }

  handleReset = () => {
    this.setState({ hasError: false, error: null, errorInfo: null });
    if (this.props.onReset) {
      this.props.onReset();
    }
  };

  render() {
    if (this.state.hasError) {
      if (this.props.fallback) {
        return this.props.fallback;
      }

      return (
        <div className="min-h-[50vh] flex flex-col items-center justify-center p-6 sm:p-12 text-center max-w-lg mx-auto">
          <div className="w-16 h-16 bg-rose-50 text-rose-500 rounded-2xl flex items-center justify-center text-3xl mb-4 border border-rose-100 shadow-xs">
            ⚠️
          </div>
          <h2 className="text-xl sm:text-2xl font-black text-slate-900 mb-2">
            {this.props.title || 'Something went wrong loading this content'}
          </h2>
          <p className="text-xs sm:text-sm text-slate-600 mb-6 leading-relaxed">
            {this.props.message ||
              "An unexpected issue occurred while displaying this page. You can reload the view or return to shopping search."}
          </p>
          <div className="flex items-center gap-3">
            <button
              type="button"
              onClick={this.handleReset}
              className="px-5 py-2.5 rounded-xl bg-indigo-600 hover:bg-indigo-700 text-white font-bold text-xs sm:text-sm transition cursor-pointer shadow-xs"
            >
              Try Again
            </button>
            <Link
              to="/shopping"
              className="px-5 py-2.5 rounded-xl bg-slate-100 hover:bg-slate-200 text-slate-800 font-semibold text-xs sm:text-sm transition cursor-pointer"
            >
              Return to Search
            </Link>
          </div>
          {import.meta.env?.DEV && this.state.error && (
            <div className="mt-8 text-left w-full bg-slate-900 text-rose-300 p-4 rounded-xl text-xs font-mono overflow-x-auto max-h-48 border border-slate-800">
              <div className="font-bold text-rose-400 mb-1">Developer Error Details:</div>
              <div>{this.state.error.toString()}</div>
              {this.state.errorInfo?.componentStack && (
                <div className="text-slate-400 text-[10px] mt-2 whitespace-pre-wrap">
                  {this.state.errorInfo.componentStack}
                </div>
              )}
            </div>
          )}
        </div>
      );
    }

    return this.props.children;
  }
}

export default ErrorBoundary;
