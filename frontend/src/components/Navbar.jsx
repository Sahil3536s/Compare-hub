import React, { useState, useEffect, useRef } from 'react';
import { Link, useLocation } from 'react-router-dom';
import { APP_NAME, ROUTES } from '../utils/constants';
import { useAuth } from '../context/AuthContext';
import { getNotifications, markNotificationAsRead, markAllNotificationsAsRead } from '../services/notificationService';
import AuthModal from './AuthModal';

export const Navbar = () => {
  const location = useLocation();
  const { user, isAuthenticated, logout } = useAuth();
  
  const [mobileMenuOpen, setMobileMenuOpen] = useState(false);
  const [authModalOpen, setAuthModalOpen] = useState(false);
  const [userDropdownOpen, setUserDropdownOpen] = useState(false);
  const [notifDropdownOpen, setNotifDropdownOpen] = useState(false);

  // Notification state
  const [notifications, setNotifications] = useState([]);
  const [unreadCount, setUnreadCount] = useState(0);

  const notifRef = useRef(null);
  const userRef = useRef(null);

  // Close menus on Escape key and outside click
  useEffect(() => {
    const handleKeyDown = (e) => {
      if (e.key === 'Escape') {
        setUserDropdownOpen(false);
        setNotifDropdownOpen(false);
        setMobileMenuOpen(false);
      }
    };

    const handleClickOutside = (e) => {
      if (notifRef.current && !notifRef.current.contains(e.target)) {
        setNotifDropdownOpen(false);
      }
      if (userRef.current && !userRef.current.contains(e.target)) {
        setUserDropdownOpen(false);
      }
    };

    window.addEventListener('keydown', handleKeyDown);
    document.addEventListener('mousedown', handleClickOutside);
    return () => {
      window.removeEventListener('keydown', handleKeyDown);
      document.removeEventListener('mousedown', handleClickOutside);
    };
  }, []);

  // Close mobile drawer when route changes
  useEffect(() => {
    setMobileMenuOpen(false);
    setUserDropdownOpen(false);
    setNotifDropdownOpen(false);
  }, [location.pathname]);

  useEffect(() => {
    if (!isAuthenticated) {
      setNotifications([]);
      setUnreadCount(0);
      return;
    }

    const fetchNotifs = async () => {
      try {
        const data = await getNotifications();
        setNotifications(data.notifications || []);
        setUnreadCount(data.unreadCount || 0);
      } catch (e) {
        setNotifications([
          {
            id: 1,
            type: 'PRICE_DROP',
            title: 'Price Drop: Apple iPhone 15',
            message: 'Price dropped to ₹69,999 on Amazon (Save 12%)',
            read: false,
            createdAt: new Date(Date.now() - 3600000).toISOString(),
          },
          {
            id: 2,
            type: 'ALERT_REACHED',
            title: 'Alert Triggered: Sony WH-1000XM5',
            message: 'Target price of ₹24,990 reached on Flipkart!',
            read: false,
            createdAt: new Date(Date.now() - 7200000).toISOString(),
          },
        ]);
        setUnreadCount(2);
      }
    };

    fetchNotifs();
    const interval = setInterval(fetchNotifs, 30000);
    return () => clearInterval(interval);
  }, [isAuthenticated]);

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

  const navLinks = [
    { name: 'Decision Engine', path: ROUTES.DECISION_ENGINE, icon: '🧠' },
    { name: 'Shopping', path: ROUTES.SHOPPING, icon: '🛍️' },
    { name: 'Smart Cart', path: ROUTES.SMART_CART, icon: '🛒' },
    { name: 'Smart Journey', path: ROUTES.SMART_JOURNEY, icon: '🗺️' },
    { name: 'Budget', path: ROUTES.BUDGET, icon: '🎯' },
    { name: 'Flights', path: ROUTES.FLIGHTS, icon: '✈️' },
    { name: 'Rides', path: ROUTES.RIDES, icon: '🚗' },
    { name: 'Savings', path: ROUTES.SAVINGS, icon: '💰' },
    { name: 'Alerts', path: ROUTES.ALERTS, icon: '🔔' },
    { name: 'Saved', path: ROUTES.SAVED, icon: '❤️' },
  ];

  const isActive = (path) => {
    if (path === ROUTES.HOME) return location.pathname === ROUTES.HOME;
    return location.pathname.startsWith(path);
  };

  return (
    <>
      <header className="bg-white/95 backdrop-blur-md border-b border-slate-200/80 sticky top-0 z-40 transition-colors">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
          <div className="flex justify-between h-16 items-center">
            
            {/* Logo */}
            <div className="flex items-center gap-6">
              <Link
                to={ROUTES.HOME}
                className="flex items-center gap-2.5 group focus-visible:outline-2 focus-visible:outline-indigo-600 rounded-xl"
                aria-label={`${APP_NAME} Home`}
              >
                <div className="w-9 h-9 rounded-xl bg-indigo-600 flex items-center justify-center text-white font-black text-lg shadow-xs group-hover:bg-indigo-700 transition">
                  CH
                </div>
                <span className="font-extrabold text-xl tracking-tight text-slate-900 group-hover:text-indigo-600 transition">
                  {APP_NAME}
                </span>
              </Link>
            </div>

            {/* Desktop Navigation */}
            <nav className="hidden md:flex items-center gap-1.5" aria-label="Main navigation">
              {navLinks.map((link) => {
                const active = isActive(link.path);
                return (
                  <Link
                    key={link.name}
                    to={link.path}
                    className={`px-3.5 py-2 rounded-xl text-sm font-medium transition-all flex items-center gap-1.5 focus-visible:outline-2 focus-visible:outline-indigo-600 ${
                      active
                        ? 'bg-indigo-50 text-indigo-700 font-bold shadow-2xs'
                        : 'text-slate-600 hover:text-slate-900 hover:bg-slate-100/70'
                    }`}
                  >
                    <span className="text-xs" aria-hidden="true">{link.icon}</span>
                    <span>{link.name}</span>
                  </Link>
                );
              })}
            </nav>

            {/* Right Action / Auth / Notifications */}
            <div className="hidden md:flex items-center gap-3">
              {isAuthenticated && user ? (
                <div className="flex items-center gap-2">
                  
                  {/* Notification Bell */}
                  <div className="relative" ref={notifRef}>
                    <button
                      onClick={() => {
                        setNotifDropdownOpen(!notifDropdownOpen);
                        setUserDropdownOpen(false);
                      }}
                      aria-expanded={notifDropdownOpen}
                      aria-label={`Notifications, ${unreadCount} unread`}
                      className="relative p-2 rounded-xl border border-slate-200 hover:border-slate-300 hover:bg-slate-50 text-slate-600 hover:text-slate-900 transition cursor-pointer focus-visible:outline-2 focus-visible:outline-indigo-600"
                    >
                      <span className="text-lg" aria-hidden="true">🔔</span>
                      {unreadCount > 0 && (
                        <span className="absolute -top-1 -right-1 bg-rose-500 text-white text-[10px] font-black w-4.5 h-4.5 rounded-full flex items-center justify-center shadow-xs">
                          {unreadCount > 9 ? '9+' : unreadCount}
                        </span>
                      )}
                    </button>

                    {/* Notification Popover Dropdown */}
                    {notifDropdownOpen && (
                      <div
                        role="dialog"
                        aria-label="Notification center"
                        className="absolute right-0 mt-2 w-80 sm:w-96 bg-white rounded-2xl shadow-xl border border-slate-200 py-3 z-50 animate-fadeIn divide-y divide-slate-100"
                      >
                        <div className="px-4 pb-2 flex items-center justify-between">
                          <div className="flex items-center gap-2">
                            <h4 className="font-extrabold text-sm text-slate-900">Notifications</h4>
                            {unreadCount > 0 && (
                              <span className="px-2 py-0.5 rounded-full text-[10px] font-bold bg-indigo-50 text-indigo-700">
                                {unreadCount} new
                              </span>
                            )}
                          </div>
                          {unreadCount > 0 && (
                            <button
                              onClick={handleMarkAllRead}
                              className="text-xs font-semibold text-indigo-600 hover:text-indigo-800 cursor-pointer focus-visible:outline-hidden"
                            >
                              Mark all read
                            </button>
                          )}
                        </div>

                        {/* List */}
                        <div className="max-h-72 overflow-y-auto divide-y divide-slate-50">
                          {notifications.length > 0 ? (
                            notifications.slice(0, 5).map((n) => (
                              <div
                                key={n.id}
                                onClick={() => !n.read && handleMarkRead(n.id)}
                                className={`p-3.5 text-left transition cursor-pointer hover:bg-slate-50 flex items-start gap-3 ${
                                  !n.read ? 'bg-indigo-50/40' : ''
                                }`}
                              >
                                <span className="text-base mt-0.5" aria-hidden="true">
                                  {n.type === 'PRICE_DROP' ? '📉' : n.type === 'ALERT_REACHED' ? '🎯' : '🔔'}
                                </span>
                                <div className="flex-1 min-w-0">
                                  <div className="flex items-center justify-between gap-1">
                                    <p className={`text-xs ${!n.read ? 'font-black text-slate-900' : 'font-semibold text-slate-700'} truncate`}>
                                      {n.title}
                                    </p>
                                    {!n.read && (
                                      <span className="w-2 h-2 rounded-full bg-indigo-600 shrink-0"></span>
                                    )}
                                  </div>
                                  <p className="text-[11px] text-slate-500 mt-0.5 line-clamp-2">
                                    {n.message}
                                  </p>
                                  <span className="text-[10px] text-slate-400 mt-1 block">
                                    {n.createdAt ? new Date(n.createdAt).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' }) : 'Just now'}
                                  </span>
                                </div>
                              </div>
                            ))
                          ) : (
                            <div className="p-6 text-center text-xs text-slate-400">
                              No notifications yet
                            </div>
                          )}
                        </div>

                        {/* Footer View All */}
                        <div className="px-4 pt-2 text-center">
                          <Link
                            to={ROUTES.NOTIFICATIONS}
                            onClick={() => setNotifDropdownOpen(false)}
                            className="text-xs font-bold text-indigo-600 hover:text-indigo-800 block py-1"
                          >
                            View All Notifications →
                          </Link>
                        </div>
                      </div>
                    )}
                  </div>

                  {/* User Profile Dropdown */}
                  <div className="relative" ref={userRef}>
                    <button
                      onClick={() => {
                        setUserDropdownOpen(!userDropdownOpen);
                        setNotifDropdownOpen(false);
                      }}
                      aria-expanded={userDropdownOpen}
                      aria-label="User account menu"
                      className="flex items-center gap-2 px-3 py-1.5 rounded-xl border border-slate-200 hover:border-slate-300 hover:bg-slate-50 transition cursor-pointer focus-visible:outline-2 focus-visible:outline-indigo-600"
                    >
                      <div className="w-7 h-7 rounded-lg bg-indigo-600 text-white flex items-center justify-center text-xs font-bold shadow-xs">
                        {user.name ? user.name.charAt(0).toUpperCase() : 'U'}
                      </div>
                      <span className="text-xs font-semibold text-slate-700 max-w-[120px] truncate">
                        {user.name}
                      </span>
                      <svg className="w-4 h-4 text-slate-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                        <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M19 9l-7 7-7-7" />
                      </svg>
                    </button>

                    {/* Dropdown Menu */}
                    {userDropdownOpen && (
                      <div
                        role="menu"
                        className="absolute right-0 mt-2 w-52 bg-white rounded-2xl shadow-xl border border-slate-200 py-2 z-50 animate-fadeIn"
                      >
                        <div className="px-4 py-2 border-b border-slate-100">
                          <p className="text-xs font-bold text-slate-900 truncate">{user.name}</p>
                          <p className="text-[11px] text-slate-500 truncate">{user.email}</p>
                        </div>

                        <Link
                          to={ROUTES.SAVED}
                          role="menuitem"
                          onClick={() => setUserDropdownOpen(false)}
                          className="flex items-center gap-2 px-4 py-2 text-xs font-medium text-slate-700 hover:bg-slate-50"
                        >
                          <span aria-hidden="true">❤️</span>
                          <span>Saved Favorites</span>
                        </Link>

                        <Link
                          to={ROUTES.HISTORY}
                          role="menuitem"
                          onClick={() => setUserDropdownOpen(false)}
                          className="flex items-center gap-2 px-4 py-2 text-xs font-medium text-slate-700 hover:bg-slate-50"
                        >
                          <span aria-hidden="true">📜</span>
                          <span>Search History</span>
                        </Link>

                        <Link
                          to={ROUTES.NOTIFICATIONS}
                          role="menuitem"
                          onClick={() => setUserDropdownOpen(false)}
                          className="flex items-center gap-2 px-4 py-2 text-xs font-medium text-slate-700 hover:bg-slate-50"
                        >
                          <span aria-hidden="true">🔔</span>
                          <span>Notifications</span>
                        </Link>

                        <Link
                          to={ROUTES.ALERTS}
                          role="menuitem"
                          onClick={() => setUserDropdownOpen(false)}
                          className="flex items-center gap-2 px-4 py-2 text-xs font-medium text-slate-700 hover:bg-slate-50"
                        >
                          <span aria-hidden="true">🎯</span>
                          <span>Price Alerts</span>
                        </Link>

                        <div className="border-t border-slate-100 pt-1 mt-1">
                          <button
                            role="menuitem"
                            onClick={() => {
                              logout();
                              setUserDropdownOpen(false);
                            }}
                            className="w-full text-left flex items-center gap-2 px-4 py-2 text-xs font-semibold text-rose-600 hover:bg-rose-50 cursor-pointer"
                          >
                            <svg className="w-3.5 h-3.5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M17 16l4-4m0 0l-4-4m4 4H7m6 4v1a3 3 0 01-3 3H6a3 3 0 01-3-3V7a3 3 0 013-3h4a3 3 0 013 3v1" />
                            </svg>
                            <span>Sign Out</span>
                          </button>
                        </div>
                      </div>
                    )}
                  </div>

                </div>
              ) : (
                <>
                  <button
                    onClick={() => setAuthModalOpen(true)}
                    className="px-4 py-2 text-sm font-semibold text-slate-700 hover:text-indigo-600 rounded-xl hover:bg-slate-50 transition cursor-pointer focus-visible:outline-2 focus-visible:outline-indigo-600"
                  >
                    Login
                  </button>
                  <button
                    onClick={() => setAuthModalOpen(true)}
                    className="px-4 py-2 text-sm font-semibold text-white bg-indigo-600 hover:bg-indigo-700 rounded-xl transition shadow-xs cursor-pointer focus-visible:outline-2 focus-visible:outline-indigo-600"
                  >
                    Get Started
                  </button>
                </>
              )}
            </div>

            {/* Mobile Hamburger & Quick Actions */}
            <div className="flex items-center md:hidden gap-2">
              {!isAuthenticated ? (
                <button
                  onClick={() => setAuthModalOpen(true)}
                  className="px-3 py-1.5 text-xs font-bold text-indigo-600 bg-indigo-50 hover:bg-indigo-100 rounded-lg transition min-h-[44px] flex items-center"
                >
                  Sign In
                </button>
              ) : (
                <Link
                  to={ROUTES.NOTIFICATIONS}
                  aria-label={`Notifications, ${unreadCount} unread`}
                  className="relative p-2.5 text-slate-700 min-h-[44px] flex items-center justify-center"
                >
                  <span className="text-lg" aria-hidden="true">🔔</span>
                  {unreadCount > 0 && (
                    <span className="absolute top-1 right-1 bg-rose-500 text-white text-[9px] font-bold w-4 h-4 rounded-full flex items-center justify-center">
                      {unreadCount > 9 ? '9+' : unreadCount}
                    </span>
                  )}
                </Link>
              )}
              <button
                onClick={() => setMobileMenuOpen(!mobileMenuOpen)}
                aria-expanded={mobileMenuOpen}
                aria-label="Toggle navigation menu"
                className="p-2.5 rounded-xl text-slate-600 hover:text-slate-900 hover:bg-slate-100 transition min-h-[44px] flex items-center justify-center focus-visible:outline-2 focus-visible:outline-indigo-600"
              >
                {mobileMenuOpen ? (
                  <svg className="w-6 h-6" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M6 18L18 6M6 6l12 12" />
                  </svg>
                ) : (
                  <svg className="w-6 h-6" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M4 6h16M4 12h16M4 18h16" />
                  </svg>
                )}
              </button>
            </div>

          </div>
        </div>

        {/* Mobile Slide-down Drawer */}
        {mobileMenuOpen && (
          <div
            className="md:hidden border-b border-slate-200 bg-white px-4 pt-3 pb-6 space-y-2 animate-fadeIn max-h-[calc(100vh-4rem)] overflow-y-auto"
            aria-label="Mobile menu"
          >
            <div className="grid grid-cols-2 gap-2 pb-2">
              {navLinks.map((link) => {
                const active = isActive(link.path);
                return (
                  <Link
                    key={link.name}
                    to={link.path}
                    onClick={() => setMobileMenuOpen(false)}
                    className={`flex items-center gap-2.5 px-3 py-3 rounded-xl text-xs font-semibold transition min-h-[44px] ${
                      active
                        ? 'bg-indigo-50 text-indigo-700 font-bold border border-indigo-200/50'
                        : 'bg-slate-50 text-slate-700 hover:bg-slate-100'
                    }`}
                  >
                    <span className="text-base" aria-hidden="true">{link.icon}</span>
                    <span>{link.name}</span>
                  </Link>
                );
              })}
            </div>

            <div className="pt-3 border-t border-slate-100 flex flex-col gap-2">
              {isAuthenticated && user ? (
                <>
                  <div className="px-3 py-2 bg-slate-50 rounded-xl flex items-center gap-3">
                    <div className="w-8 h-8 rounded-lg bg-indigo-600 text-white flex items-center justify-center text-xs font-bold shrink-0">
                      {user.name ? user.name.charAt(0).toUpperCase() : 'U'}
                    </div>
                    <div className="min-w-0 flex-1">
                      <p className="text-xs font-bold text-slate-900 truncate">{user.name}</p>
                      <p className="text-[11px] text-slate-500 truncate">{user.email}</p>
                    </div>
                  </div>

                  <div className="grid grid-cols-2 gap-2">
                    <Link
                      to={ROUTES.HISTORY}
                      onClick={() => setMobileMenuOpen(false)}
                      className="px-3 py-2.5 text-xs font-medium text-slate-700 bg-slate-50 rounded-xl flex items-center gap-2 min-h-[44px]"
                    >
                      <span>📜</span>
                      <span>History</span>
                    </Link>
                    <Link
                      to={ROUTES.NOTIFICATIONS}
                      onClick={() => setMobileMenuOpen(false)}
                      className="px-3 py-2.5 text-xs font-medium text-slate-700 bg-slate-50 rounded-xl flex items-center justify-between min-h-[44px]"
                    >
                      <span className="flex items-center gap-1.5">
                        <span>🔔</span>
                        <span>Notifs</span>
                      </span>
                      {unreadCount > 0 && (
                        <span className="px-1.5 py-0.5 rounded-full text-[9px] font-black bg-rose-500 text-white">
                          {unreadCount}
                        </span>
                      )}
                    </Link>
                  </div>

                  <button
                    onClick={() => {
                      logout();
                      setMobileMenuOpen(false);
                    }}
                    className="w-full py-3 text-center text-xs font-bold text-rose-600 bg-rose-50 hover:bg-rose-100 rounded-xl transition min-h-[44px]"
                  >
                    Sign Out
                  </button>
                </>
              ) : (
                <button
                  onClick={() => {
                    setMobileMenuOpen(false);
                    setAuthModalOpen(true);
                  }}
                  className="w-full py-3 text-center text-sm font-bold text-white bg-indigo-600 hover:bg-indigo-700 rounded-xl shadow-xs transition min-h-[44px]"
                >
                  Sign In / Register
                </button>
              )}
            </div>
          </div>
        )}
      </header>

      {/* Auth Modal */}
      <AuthModal isOpen={authModalOpen} onClose={() => setAuthModalOpen(false)} />
    </>
  );
};

export default Navbar;
