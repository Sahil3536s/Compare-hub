import React, { useState, useEffect, useCallback } from 'react';
import { Link } from 'react-router-dom';
import { getSavedProducts, removeSavedProduct, getSavedRoutes, deleteSavedRoute, saveRoute } from '../services/savedService';
import { getFlightHistory } from '../services/historyService';
import LoadingSkeleton from '../components/LoadingSkeleton';
import EmptyState from '../components/EmptyState';
import ErrorState from '../components/ErrorState';

export const SavedPage = () => {
  const [activeTab, setActiveTab] = useState('products'); // 'products', 'flights', 'routes'
  
  // Data states
  const [products, setProducts] = useState([]);
  const [flights, setFlights] = useState([]);
  const [routes, setRoutes] = useState([]);

  // Route form state
  const [newPickup, setNewPickup] = useState('');
  const [newDrop, setNewDrop] = useState('');
  const [showAddRoute, setShowAddRoute] = useState(false);

  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  const loadSavedData = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      const [prodsData, routesData, flightsData] = await Promise.allSettled([
        getSavedProducts(),
        getSavedRoutes(),
        getFlightHistory(),
      ]);

      if (prodsData.status === 'fulfilled') setProducts(prodsData.value || []);
      if (routesData.status === 'fulfilled') setRoutes(routesData.value || []);
      if (flightsData.status === 'fulfilled') setFlights(flightsData.value || []);
    } catch (err) {
      console.error('Failed to load saved items:', err);
      setError('Unable to load saved items. Please make sure you are logged in.');
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    loadSavedData();
  }, [loadSavedData]);

  // Handlers
  const handleRemoveProduct = async (id) => {
    try {
      await removeSavedProduct(id);
      setProducts((prev) => prev.filter((p) => p.id !== id));
    } catch (e) {
      alert('Failed to remove saved product.');
    }
  };

  const handleDeleteRoute = async (id) => {
    try {
      await deleteSavedRoute(id);
      setRoutes((prev) => prev.filter((r) => r.id !== id));
    } catch (e) {
      alert('Failed to delete saved route.');
    }
  };

  const handleCreateRoute = async (e) => {
    e.preventDefault();
    if (!newPickup || !newDrop) return;
    try {
      const created = await saveRoute({ pickup: newPickup, destination: newDrop });
      setRoutes((prev) => [created, ...prev]);
      setNewPickup('');
      setNewDrop('');
      setShowAddRoute(false);
    } catch (e) {
      alert('Failed to save route.');
    }
  };

  return (
    <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8 space-y-8">
      
      {/* Header */}
      <div>
        <div className="flex items-center gap-2 text-xs text-slate-500 mb-2">
          <span>Home</span>
          <span>/</span>
          <span className="text-indigo-600 font-medium">Saved Items</span>
        </div>
        <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
          <div>
            <h1 className="text-3xl sm:text-4xl font-extrabold text-slate-900 tracking-tight">
              Saved Favorites & Routes
            </h1>
            <p className="text-sm text-slate-500 mt-1">
              Your personalized hub for tracked products, pinned flight itineraries, and common ride routes.
            </p>
          </div>
        </div>
      </div>

      {/* Tabs */}
      <div className="flex bg-white p-1.5 rounded-2xl border border-slate-200 shadow-xs max-w-md">
        {[
          { id: 'products', label: '🛍️ Saved Products', count: products.length },
          { id: 'flights', label: '✈️ Saved Flights', count: flights.length },
          { id: 'routes', label: '🚗 Common Routes', count: routes.length },
        ].map((tab) => (
          <button
            key={tab.id}
            onClick={() => setActiveTab(tab.id)}
            className={`flex-1 py-2.5 px-3 rounded-xl text-xs font-bold transition flex items-center justify-center gap-1.5 cursor-pointer ${
              activeTab === tab.id
                ? 'bg-indigo-600 text-white shadow-xs'
                : 'text-slate-600 hover:text-slate-900 hover:bg-slate-50'
            }`}
          >
            <span>{tab.label}</span>
            <span className={`px-1.5 py-0.5 rounded-full text-[10px] ${
              activeTab === tab.id ? 'bg-indigo-700 text-white' : 'bg-slate-100 text-slate-600'
            }`}>
              {tab.count}
            </span>
          </button>
        ))}
      </div>

      {/* Main Content Area */}
      {loading ? (
        <LoadingSkeleton type="product-card" count={3} />
      ) : error ? (
        <ErrorState title="Unable to load saved items" message={error} onRetry={loadSavedData} />
      ) : (
        <div>
          {/* TAB 1: PRODUCTS */}
          {activeTab === 'products' && (
            <div>
              {products.length > 0 ? (
                <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
                  {products.map((item) => (
                    <div
                      key={item.id}
                      className="bg-white rounded-3xl border border-slate-200 p-6 shadow-xs hover:shadow-md transition flex flex-col justify-between"
                    >
                      <div className="flex items-start gap-4">
                        <div className="w-16 h-16 rounded-2xl bg-slate-50 border border-slate-100 p-2 flex items-center justify-center shrink-0">
                          {item.productImageUrl ? (
                            <img src={item.productImageUrl} alt={item.productName} className="w-full h-full object-contain" />
                          ) : (
                            <span className="text-2xl">📦</span>
                          )}
                        </div>
                        <div className="flex-1 min-w-0">
                          <span className="text-[10px] font-bold uppercase tracking-wider text-indigo-600 bg-indigo-50 px-2 py-0.5 rounded-md">
                            {item.productCategory || 'General'}
                          </span>
                          <h3 className="font-extrabold text-slate-900 text-base mt-1 truncate">
                            {item.productName}
                          </h3>
                          <p className="text-[11px] text-slate-400 mt-0.5">
                            Saved on {item.createdAt ? new Date(item.createdAt).toLocaleDateString() : 'Recently'}
                          </p>
                        </div>
                      </div>

                      <div className="pt-4 mt-4 border-t border-slate-100 flex items-center justify-between gap-3">
                        <Link
                          to={`/shopping?q=${encodeURIComponent(item.productName)}`}
                          className="px-4 py-2 bg-indigo-50 hover:bg-indigo-100 text-indigo-700 font-bold text-xs rounded-xl transition flex items-center gap-1.5"
                        >
                          <span>Compare Prices</span>
                          <span>→</span>
                        </Link>
                        <button
                          onClick={() => handleRemoveProduct(item.id)}
                          className="text-xs font-semibold text-rose-600 hover:text-rose-800 p-2 cursor-pointer"
                          title="Remove from saved"
                        >
                          Remove
                        </button>
                      </div>
                    </div>
                  ))}
                </div>
              ) : (
                <EmptyState
                  title="No saved products yet"
                  description="When searching products across Amazon, Flipkart, and Croma, click Save to bookmark them here."
                  actionLabel="Browse Shopping"
                  actionUrl="/shopping"
                />
              )}
            </div>
          )}

          {/* TAB 2: FLIGHTS */}
          {activeTab === 'flights' && (
            <div>
              {flights.length > 0 ? (
                <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
                  {flights.map((f) => (
                    <div
                      key={f.id}
                      className="bg-white rounded-3xl border border-slate-200 p-6 shadow-xs hover:shadow-md transition flex flex-col justify-between"
                    >
                      <div>
                        <div className="flex items-center justify-between border-b border-slate-100 pb-3">
                          <span className="text-xs font-bold text-slate-900">
                            {f.cabinClass || 'Economy'} Class • {f.passengers || 1} Passenger
                          </span>
                          <span className="text-xs text-slate-400">
                            {f.departureDate || 'Anytime'}
                          </span>
                        </div>
                        <div className="py-4 flex items-center justify-between">
                          <div className="text-center">
                            <span className="text-2xl font-black text-slate-900 font-mono">{f.fromAirport}</span>
                            <span className="text-[11px] text-slate-400 block">Origin</span>
                          </div>
                          <div className="text-slate-300 font-bold text-lg">✈️ ➔</div>
                          <div className="text-center">
                            <span className="text-2xl font-black text-slate-900 font-mono">{f.toAirport}</span>
                            <span className="text-[11px] text-slate-400 block">Destination</span>
                          </div>
                        </div>
                      </div>

                      <div className="pt-3 border-t border-slate-100">
                        <Link
                          to={`/flights`}
                          className="w-full py-2.5 bg-indigo-600 hover:bg-indigo-700 text-white font-bold text-xs rounded-xl transition flex items-center justify-center gap-1.5"
                        >
                          <span>Re-check Flight Fares</span>
                          <span>→</span>
                        </Link>
                      </div>
                    </div>
                  ))}
                </div>
              ) : (
                <EmptyState
                  title="No saved flight itineraries"
                  description="Flight routes you search will automatically appear here for quick access."
                  actionLabel="Search Flights"
                  actionUrl="/flights"
                />
              )}
            </div>
          )}

          {/* TAB 3: ROUTES */}
          {activeTab === 'routes' && (
            <div className="space-y-6">
              
              {/* Add New Route Bar */}
              <div className="flex justify-end">
                <button
                  onClick={() => setShowAddRoute(!showAddRoute)}
                  className="px-4 py-2 bg-indigo-50 text-indigo-700 hover:bg-indigo-100 font-bold text-xs rounded-xl transition cursor-pointer"
                >
                  {showAddRoute ? 'Cancel' : '+ Save Common Route'}
                </button>
              </div>

              {showAddRoute && (
                <form onSubmit={handleCreateRoute} className="bg-white p-5 rounded-2xl border border-indigo-200 shadow-sm grid grid-cols-1 sm:grid-cols-3 gap-3 items-center">
                  <input
                    type="text"
                    required
                    placeholder="Pickup address..."
                    value={newPickup}
                    onChange={(e) => setNewPickup(e.target.value)}
                    className="px-3 py-2 bg-slate-50 border border-slate-200 rounded-xl text-xs font-semibold text-slate-900 focus:outline-hidden focus:border-indigo-500"
                  />
                  <input
                    type="text"
                    required
                    placeholder="Drop destination..."
                    value={newDrop}
                    onChange={(e) => setNewDrop(e.target.value)}
                    className="px-3 py-2 bg-slate-50 border border-slate-200 rounded-xl text-xs font-semibold text-slate-900 focus:outline-hidden focus:border-indigo-500"
                  />
                  <button
                    type="submit"
                    className="py-2 bg-indigo-600 hover:bg-indigo-700 text-white font-bold text-xs rounded-xl transition cursor-pointer"
                  >
                    Save Route
                  </button>
                </form>
              )}

              {routes.length > 0 ? (
                <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
                  {routes.map((r) => (
                    <div
                      key={r.id}
                      className="bg-white rounded-3xl border border-slate-200 p-6 shadow-xs hover:shadow-md transition flex flex-col justify-between space-y-4"
                    >
                      <div className="space-y-3">
                        <div className="flex items-start gap-2 text-xs">
                          <span className="text-emerald-500 font-bold">🟢</span>
                          <div>
                            <span className="text-[10px] font-bold text-slate-400 uppercase tracking-wider block">Pickup</span>
                            <span className="font-bold text-slate-900">{r.pickup}</span>
                          </div>
                        </div>
                        <div className="flex items-start gap-2 text-xs">
                          <span className="text-rose-500 font-bold">🏁</span>
                          <div>
                            <span className="text-[10px] font-bold text-slate-400 uppercase tracking-wider block">Destination</span>
                            <span className="font-bold text-slate-900">{r.destination}</span>
                          </div>
                        </div>
                      </div>

                      <div className="pt-3 border-t border-slate-100 flex items-center justify-between gap-3">
                        <Link
                          to="/rides"
                          className="px-4 py-2 bg-indigo-50 hover:bg-indigo-100 text-indigo-700 font-bold text-xs rounded-xl transition flex items-center gap-1"
                        >
                          <span>Compare Cabs</span>
                          <span>→</span>
                        </Link>
                        <button
                          onClick={() => handleDeleteRoute(r.id)}
                          className="text-xs font-semibold text-rose-600 hover:text-rose-800 p-2 cursor-pointer"
                          title="Delete route"
                        >
                          Delete
                        </button>
                      </div>
                    </div>
                  ))}
                </div>
              ) : (
                <EmptyState
                  title="No common routes saved"
                  description="Save your daily commute (Home to Work, Airport, etc.) for 1-click fare comparisons."
                  actionLabel="Explore Rides"
                  actionUrl="/rides"
                />
              )}
            </div>
          )}

        </div>
      )}

    </div>
  );
};

export default SavedPage;
