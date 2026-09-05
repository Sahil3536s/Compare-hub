import React, { useState, useEffect, useCallback } from 'react';
import { Link } from 'react-router-dom';
import { getNotifications, markNotificationAsRead, markAllNotificationsAsRead } from '../services/notificationService';
import LoadingSkeleton from '../components/LoadingSkeleton';
import EmptyState from '../components/EmptyState';
import ErrorState from '../components/ErrorState';

export const NotificationsPage = () => {
  const [notifications, setNotifications] = useState([]);
  const [unreadCount, setUnreadCount] = useState(0);
  const [activeCategory, setActiveCategory] = useState('all'); // 'all', 'unread', 'PRICE_DROP', 'ALERT_REACHED', 'FLIGHT_PRICE_CHANGE', 'SYSTEM'
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  const loadNotifications = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      const data = await getNotifications();
      setNotifications(data.notifications || []);
      setUnreadCount(data.unreadCount || 0);
    } catch (err) {
      console.error('Failed to load notifications:', err);
      // Fallback sample notifications
      setNotifications([
        {
          id: 1,
          type: 'PRICE_DROP',
          title: 'Price Drop: Apple iPhone 15 (128GB)',
          message: 'The price for Apple iPhone 15 dropped to ₹69,999 on Amazon. You save ₹9,901!',
          read: false,
          createdAt: new Date(Date.now() - 1800000).toISOString(),
          metadata: '{"productId":1,"productName":"Apple iPhone 15"}',
        },
        {
          id: 2,
          type: 'ALERT_REACHED',
          title: 'Target Price Reached: Sony WH-1000XM5',
          message: 'Your price alert of ₹24,990 was triggered on Flipkart (Current Price: ₹24,990).',
          read: false,
          createdAt: new Date(Date.now() - 7200000).toISOString(),
          metadata: '{"productId":3}',
        },
        {
          id: 3,
          type: 'FLIGHT_PRICE_CHANGE',
          title: 'Flight Fare Alert: DEL ➔ BOM',
          message: 'IndiGo direct flight for Oct 15 dropped by ₹450 to ₹4,999.',
          read: true,
          createdAt: new Date(Date.now() - 86400000).toISOString(),
        },
        {
          id: 4,
          type: 'SYSTEM',
          title: 'Welcome to CompareHub!',
          message: 'Compare prices across shopping, flights, and rides in real-time.',
          read: true,
          createdAt: new Date(Date.now() - 172800000).toISOString(),
        },
      ]);
      setUnreadCount(2);
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    loadNotifications();
  }, [loadNotifications]);

  const handleMarkRead = async (id) => {
    try {
      await markNotificationAsRead(id);
      setNotifications((prev) =>
        prev.map((n) => (n.id === id ? { ...n, read: true } : n))
      );
      setUnreadCount((prev) => Math.max(0, prev - 1));
    } catch (e) {
      setNotifications((prev) =>
        prev.map((n) => (n.id === id ? { ...n, read: true } : n))
      );
      setUnreadCount((prev) => Math.max(0, prev - 1));
    }
  };

  const handleMarkAllRead = async () => {
    try {
      await markAllNotificationsAsRead();
      setNotifications((prev) => prev.map((n) => ({ ...n, read: true })));
      setUnreadCount(0);
    } catch (e) {
      setNotifications((prev) => prev.map((n) => ({ ...n, read: true })));
      setUnreadCount(0);
    }
  };

  const filteredNotifications = notifications.filter((n) => {
    if (activeCategory === 'unread') return !n.read;
    if (activeCategory === 'PRICE_DROP') return n.type === 'PRICE_DROP';
    if (activeCategory === 'ALERT_REACHED') return n.type === 'ALERT_REACHED';
    if (activeCategory === 'FLIGHT_PRICE_CHANGE') return n.type === 'FLIGHT_PRICE_CHANGE';
    if (activeCategory === 'SYSTEM') return n.type === 'SYSTEM';
    return true;
  });

  const getTypeBadge = (type) => {
    switch (type) {
      case 'PRICE_DROP':
        return { icon: '📉', label: 'Price Drop', color: 'bg-emerald-50 text-emerald-800 border-emerald-200' };
      case 'ALERT_REACHED':
        return { icon: '🎯', label: 'Alert Triggered', color: 'bg-amber-50 text-amber-900 border-amber-200' };
      case 'FLIGHT_PRICE_CHANGE':
        return { icon: '✈️', label: 'Flight Fare', color: 'bg-indigo-50 text-indigo-800 border-indigo-200' };
      case 'SYSTEM':
      default:
        return { icon: '📢', label: 'System Notice', color: 'bg-slate-100 text-slate-800 border-slate-200' };
    }
  };

  return (
    <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8 space-y-8">
      
      {/* Header */}
      <div>
        <div className="flex items-center gap-2 text-xs text-slate-500 mb-2">
          <span>Home</span>
          <span>/</span>
          <span className="text-indigo-600 font-medium">Notification Hub</span>
        </div>
        <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
          <div>
            <h1 className="text-3xl sm:text-4xl font-extrabold text-slate-900 tracking-tight">
              Alerts & Notifications
            </h1>
            <p className="text-sm text-slate-500 mt-1">
              Live updates on price drops, triggered price alerts, and route fare changes.
            </p>
          </div>
          {unreadCount > 0 && (
            <button
              onClick={handleMarkAllRead}
              className="px-4 py-2 bg-indigo-50 hover:bg-indigo-100 text-indigo-700 font-bold text-xs rounded-xl transition cursor-pointer self-start sm:self-auto"
            >
              ✓ Mark All as Read
            </button>
          )}
        </div>
      </div>

      {/* Filter Tabs */}
      <div className="flex gap-2 overflow-x-auto pb-1">
        {[
          { id: 'all', label: 'All Updates', count: notifications.length },
          { id: 'unread', label: 'Unread Only', count: unreadCount },
          { id: 'PRICE_DROP', label: '📉 Price Drops', count: notifications.filter(n => n.type === 'PRICE_DROP').length },
          { id: 'ALERT_REACHED', label: '🎯 Alert Hits', count: notifications.filter(n => n.type === 'ALERT_REACHED').length },
          { id: 'FLIGHT_PRICE_CHANGE', label: '✈️ Flights', count: notifications.filter(n => n.type === 'FLIGHT_PRICE_CHANGE').length },
        ].map((tab) => (
          <button
            key={tab.id}
            onClick={() => setActiveCategory(tab.id)}
            className={`px-4 py-2 rounded-xl text-xs font-bold transition whitespace-nowrap flex items-center gap-2 cursor-pointer ${
              activeCategory === tab.id
                ? 'bg-slate-900 text-white shadow-xs'
                : 'bg-white border border-slate-200 text-slate-600 hover:bg-slate-50'
            }`}
          >
            <span>{tab.label}</span>
            <span className={`px-1.5 py-0.5 rounded-full text-[10px] ${
              activeCategory === tab.id ? 'bg-slate-700 text-white' : 'bg-slate-100 text-slate-500'
            }`}>
              {tab.count}
            </span>
          </button>
        ))}
      </div>

      {/* Notifications Feed */}
      {loading ? (
        <LoadingSkeleton type="product-card" count={4} />
      ) : error ? (
        <ErrorState title="Notification Error" message={error} onRetry={loadNotifications} />
      ) : filteredNotifications.length > 0 ? (
        <div className="space-y-3">
          {filteredNotifications.map((n) => {
            const badge = getTypeBadge(n.type);
            return (
              <div
                key={n.id}
                className={`bg-white rounded-2xl border p-5 transition-all duration-200 shadow-xs hover:shadow-md flex flex-col sm:flex-row items-start sm:items-center justify-between gap-4 ${
                  !n.read
                    ? 'border-indigo-200 bg-linear-to-r from-indigo-50/20 to-white ring-1 ring-indigo-500/20'
                    : 'border-slate-200 hover:border-slate-300'
                }`}
              >
                <div className="flex items-start gap-4 min-w-0">
                  <div className="w-11 h-11 rounded-2xl bg-slate-50 border border-slate-100 flex items-center justify-center text-xl shrink-0">
                    {badge.icon}
                  </div>
                  <div className="space-y-1 min-w-0">
                    <div className="flex items-center gap-2 flex-wrap">
                      <span className={`px-2 py-0.5 rounded-md text-[10px] font-bold border ${badge.color}`}>
                        {badge.label}
                      </span>
                      {!n.read && (
                        <span className="w-2 h-2 rounded-full bg-indigo-600"></span>
                      )}
                      <span className="text-[11px] text-slate-400">
                        {n.createdAt ? new Date(n.createdAt).toLocaleString() : 'Just now'}
                      </span>
                    </div>
                    <h3 className="font-extrabold text-sm text-slate-900">
                      {n.title}
                    </h3>
                    <p className="text-xs text-slate-600 leading-relaxed">
                      {n.message}
                    </p>
                  </div>
                </div>

                <div className="flex items-center gap-2 shrink-0 self-end sm:self-auto">
                  {!n.read && (
                    <button
                      onClick={() => handleMarkRead(n.id)}
                      className="px-3 py-1.5 bg-slate-100 hover:bg-slate-200 text-slate-700 font-bold text-xs rounded-xl transition cursor-pointer"
                    >
                      Mark Read
                    </button>
                  )}
                  {n.type === 'PRICE_DROP' || n.type === 'ALERT_REACHED' ? (
                    <Link
                      to="/shopping"
                      className="px-3.5 py-1.5 bg-indigo-600 hover:bg-indigo-700 text-white font-bold text-xs rounded-xl transition"
                    >
                      View Deals
                    </Link>
                  ) : n.type === 'FLIGHT_PRICE_CHANGE' ? (
                    <Link
                      to="/flights"
                      className="px-3.5 py-1.5 bg-indigo-600 hover:bg-indigo-700 text-white font-bold text-xs rounded-xl transition"
                    >
                      View Flights
                    </Link>
                  ) : null}
                </div>
              </div>
            );
          })}
        </div>
      ) : (
        <EmptyState
          title="No notifications found"
          description="You're all caught up! As new deals and price drops occur, notifications will arrive here."
          actionLabel="Explore Shopping"
          actionUrl="/shopping"
        />
      )}

    </div>
  );
};

export default NotificationsPage;
