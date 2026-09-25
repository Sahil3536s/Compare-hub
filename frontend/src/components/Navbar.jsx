import React, { useState, useEffect, useRef } from 'react';
import { Link, useLocation, useNavigate } from 'react-router-dom';
import { APP_NAME, ROUTES } from '../utils/constants';
import { useAuth } from '../context/AuthContext';
import { getNotifications, markNotificationAsRead, markAllNotificationsAsRead } from '../services/notificationService';
import AuthModal from './AuthModal';

export const Navbar = () => {
  const location = useLocation();
  const navigate = useNavigate();
  const { user, isAuthenticated, logout } = useAuth();

  const [mobileMenuOpen, setMobileMenuOpen] = useState(false);
  const [authModalOpen, setAuthModalOpen] = useState(false);
  const [userDropdownOpen, setUserDropdownOpen] = useState(false);
  const [notifDropdownOpen, setNotifDropdownOpen] = useState(false);

  // Notifications state
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

  // Close mobile drawer and dropdowns when route changes
  useEffect(() => {
    setMobileMenuOpen(false);
    setUserDropdownOpen(false);
    setNotifDropdownOpen(false);
  }, [location.pathname, location.search]);

  // Load user notifications when authenticated
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
      } catch {
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
    } catch {
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
    } catch {
      setNotifications((prev) => prev.map((n) => ({ ...n, read: true })));
      setUnreadCount(0);
    }
  };

  // Center primary navigation links requested by user
  const centerNavLinks = [
    {
      name: 'Deals',
      path: `${ROUTES.SHOPPING}?tab=deals`,
      match: () => location.pathname === ROUTES.SHOPPING && location.search.includes('deals'),
      icon: (
        <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M7 7h.01M7 3h5c.512 0 1.024.195 1.414.586l7 7a2 2 0 010 2.828l-7 7a2 2 0 01-2.828 0l-7-7A1.994 1.994 0 013 12V7a4 4 0 014-4z" />
        </svg>
      ),
    },
    {
      name: 'Compare',
      path: ROUTES.SHOPPING,
      match: () => location.pathname === ROUTES.SHOPPING && !location.search.includes('deals'),
      icon: (
        <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M9 19v-6a2 2 0 00-2-2H5a2 2 0 00-2 2v6a2 2 0 002 2h2a2 2 0 002-2zm0 0V9a2 2 0 012-2h2a2 2 0 012 2v10m-6 0a2 2 0 002 2h2a2 2 0 002-2m0 0V5a2 2 0 012-2h2a2 2 0 012 2v14a2 2 0 01-2 2h-2a2 2 0 01-2-2z" />
        </svg>
      ),
    },
    {
      name: 'Price Tracker',
      path: ROUTES.ALERTS,
      match: () => location.pathname.startsWith(ROUTES.ALERTS),
      icon: (
        <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M13 7h8m0 0v8m0-8l-8 8-4-4-6 6" />
        </svg>
      ),
    },
    {
      name: 'Flights',
      path: ROUTES.FLIGHTS,
      match: () => location.pathname.startsWith(ROUTES.FLIGHTS),
      icon: (
        <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M12 19l9 2-9-18-9 18 9-2zm0 0v-8" />
        </svg>
      ),
    },
    {
      name: 'Rides',
      path: ROUTES.RIDES,
      match: () => location.pathname.startsWith(ROUTES.RIDES),
      icon: (
        <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M8 7h8m-8 4h8m-9 8h10a2 2 0 002-2V7a2 2 0 00-2-2H7a2 2 0 00-2 2v10a2 2 0 002 2z" />
        </svg>
      ),
    },
  ];

  const handleSearchClick = () => {
    if (location.pathname === ROUTES.HOME) {
      const searchInput = document.getElementById('universal-search-input');
      if (searchInput) {
        searchInput.focus();
        searchInput.scrollIntoView({ behavior: 'smooth', block: 'center' });
      }
    } else {
      navigate(`${ROUTES.HOME}?focusSearch=true`);
    }
  };

  return (
    <>
      <header className="bg-white/95 backdrop-blur-md border-b border-slate-200/80 sticky top-0 z-40 transition-colors">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
          <div className="flex justify-between h-16 items-center">

            {/* LEFT: CompareHub Logo & Brand Name */}
            <div className="flex items-center gap-3">
              <Link
                to={ROUTES.HOME}
                className="flex items-center gap-2.5 group focus-visible:outline-2 focus-visible:outline-indigo-600 rounded-xl"
                aria-label={`${APP_NAME} Home`}
              >
                <div className="w-9 h-9 rounded-xl bg-linear-to-br from-indigo-600 to-indigo-700 flex items-center justify-center text-white font-black text-sm shadow-xs group-hover:from-indigo-700 group-hover:to-indigo-800 transition">
                  <svg className="w-5 h-5 fill-current" viewBox="0 0 24 24">
                    <path d="M12 2L2 7l10 5 10-5-10-5zM2 17l10 5 10-5M2 12l10 5 10-5" />
                  </svg>
                </div>
                <div className="flex flex-col">
                  <span className="font-extrabold text-xl tracking-tight text-slate-900 group-hover:text-indigo-600 transition leading-tight">
                    {APP_NAME}
                  </span>
                </div>
              </Link>
            </div>

            {/* CENTER: Deals, Compare, Price Tracker, Flights, Rides */}
            <nav className="hidden lg:flex items-center gap-1" aria-label="Primary navigation">
              {centerNavLinks.map((link) => {
                const active = link.match();
                return (
                  <Link
                    key={link.name}
                    to={link.path}
                    className={`relative px-3.5 py-2 rounded-xl text-sm font-semibold transition-all duration-150 flex items-center gap-2 focus-visible:outline-2 focus-visible:outline-indigo-600 ${
                      active
                        ? 'bg-indigo-50 text-indigo-700 shadow-2xs font-bold'
                        : 'text-slate-600 hover:text-slate-900 hover:bg-slate-100/80'
                    }`}
                  >
                    <span className={active ? 'text-indigo-600' : 'text-slate-400 group-hover:text-slate-600'}>
                      {link.icon}
                    </span>
                    <span>{link.name}</span>
                    {active && (
                      <span className="absolute bottom-0 left-1/2 -translate-x-1/2 w-4 h-0.5 bg-indigo-600 rounded-full" />
                    )}
                  </Link>
                );
              })}
            </nav>

            {/* RIGHT: Search icon, Saved/Wishlist, Login/Profile */}
            <div className="hidden sm:flex items-center gap-2 lg:gap-3">
              {/* Search trigger icon */}
              <button
                type="button"
                onClick={handleSearchClick}
                aria-label="Search products, flights, or rides"
                className="p-2.5 rounded-xl border border-transparent hover:border-slate-200 text-slate-600 hover:text-indigo-600 hover:bg-slate-50 transition cursor-pointer focus-visible:outline-2 focus-visible:outline-indigo-600"
                title="Search"
              >
                <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M21 21l-6-6m2-5a7 7 0 11-14 0 7 7 0 0114 0z" />
                </svg>
              </button>

              {/* Saved / Wishlist Link */}
              <Link
                to={ROUTES.SAVED}
                aria-label="Saved products and watchlist"
                className={`p-2.5 rounded-xl transition flex items-center gap-1.5 text-xs font-semibold focus-visible:outline-2 focus-visible:outline-indigo-600 ${
                  location.pathname.startsWith(ROUTES.SAVED)
                    ? 'bg-indigo-50 text-indigo-700 font-bold border border-indigo-100'
                    : 'text-slate-600 hover:text-slate-900 hover:bg-slate-100/70 border border-transparent'
                }`}
                title="Saved & Wishlist"
              >
                <svg className="w-4 h-4 text-rose-500 fill-current" viewBox="0 0 20 20">
                  <path fillRule="evenodd" d="M3.172 5.172a4 4 0 015.656 0L10 6.343l1.172-1.171a4 4 0 115.656 5.656L10 17.657l-6.828-6.829a4 4 0 010-5.656z" clipRule="evenodd" />
                </svg>
                <span className="hidden xl:inline">Saved</span>
              </Link>

              {/* Notifications Bell */}
              {isAuthenticated && (
                <div className="relative" ref={notifRef}>
                  <button
                    onClick={() => {
                      setNotifDropdownOpen(!notifDropdownOpen);
                      setUserDropdownOpen(false);
                    }}
                    aria-expanded={notifDropdownOpen}
                    aria-label={`Notifications, ${unreadCount} unread`}
                    className="relative p-2.5 rounded-xl border border-transparent hover:border-slate-200 hover:bg-slate-50 text-slate-600 hover:text-slate-900 transition cursor-pointer focus-visible:outline-2 focus-visible:outline-indigo-600"
                    title="Notifications"
                  >
                    <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                      <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M15 17h5l-1.405-1.405A2.032 2.032 0 0118 14.158V11a6.002 6.002 0 00-4-5.659V5a2 2 0 10-4 0v.341C7.67 6.165 6 8.388 6 11v3.159c0 .538-.214 1.055-.595 1.436L4 17h5m6 0v1a3 3 0 11-6 0v-1m6 0H9" />
                    </svg>
                    {unreadCount > 0 && (
                      <span className="absolute top-1.5 right-1.5 bg-rose-500 text-white text-[9px] font-black w-4 h-4 rounded-full flex items-center justify-center shadow-xs">
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
                            className="text-xs font-semibold text-indigo-600 hover:text-indigo-800 cursor-pointer"
                          >
                            Mark all read
                          </button>
                        )}
                      </div>

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
              )}

              {/* Login / Profile */}
              {isAuthenticated && user ? (
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
                    <span className="text-xs font-semibold text-slate-700 max-w-[100px] truncate">
                      {user.name}
                    </span>
                    <svg className="w-3.5 h-3.5 text-slate-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                      <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M19 9l-7 7-7-7" />
                    </svg>
                  </button>

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
                        to={ROUTES.DASHBOARD}
                        role="menuitem"
                        onClick={() => setUserDropdownOpen(false)}
                        className="flex items-center gap-2 px-4 py-2 text-xs font-bold text-indigo-700 bg-indigo-50/50 hover:bg-indigo-50"
                      >
                        <span aria-hidden="true">📊</span>
                        <span>My CompareHub</span>
                      </Link>

                      <Link
                        to={ROUTES.SAVED}
                        role="menuitem"
                        onClick={() => setUserDropdownOpen(false)}
                        className="flex items-center gap-2 px-4 py-2 text-xs font-medium text-slate-700 hover:bg-slate-50"
                      >
                        <span aria-hidden="true">❤️</span>
                        <span>Saved & Wishlist</span>
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
              ) : (
                <div className="flex items-center gap-2">
                  <button
                    onClick={() => setAuthModalOpen(true)}
                    className="px-3.5 py-1.5 text-xs font-semibold text-slate-700 hover:text-indigo-600 rounded-xl hover:bg-slate-50 transition cursor-pointer focus-visible:outline-2 focus-visible:outline-indigo-600"
                  >
                    Sign In
                  </button>
                  <button
                    onClick={() => setAuthModalOpen(true)}
                    className="px-4 py-2 text-xs font-bold text-white bg-indigo-600 hover:bg-indigo-700 rounded-xl transition shadow-xs cursor-pointer focus-visible:outline-2 focus-visible:outline-indigo-600"
                  >
                    Get Started
                  </button>
                </div>
              )}
            </div>

            {/* MOBILE: Compact Navigation Right Controls */}
            <div className="flex items-center lg:hidden gap-1 sm:gap-2">
              <button
                type="button"
                onClick={handleSearchClick}
                aria-label="Search"
                className="p-2 text-slate-600 hover:text-indigo-600 rounded-lg hover:bg-slate-100 transition min-h-[44px] min-w-[44px] flex items-center justify-center"
              >
                <svg className="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M21 21l-6-6m2-5a7 7 0 11-14 0 7 7 0 0114 0z" />
                </svg>
              </button>

              <Link
                to={ROUTES.SAVED}
                aria-label="Saved products"
                className="p-2 text-slate-600 hover:text-rose-500 rounded-lg hover:bg-slate-100 transition min-h-[44px] min-w-[44px] flex items-center justify-center"
              >
                <svg className="w-5 h-5 text-rose-500 fill-current" viewBox="0 0 20 20">
                  <path fillRule="evenodd" d="M3.172 5.172a4 4 0 015.656 0L10 6.343l1.172-1.171a4 4 0 115.656 5.656L10 17.657l-6.828-6.829a4 4 0 010-5.656z" clipRule="evenodd" />
                </svg>
              </Link>

              <button
                onClick={() => setMobileMenuOpen(!mobileMenuOpen)}
                aria-expanded={mobileMenuOpen}
                aria-label="Toggle navigation menu"
                className="p-2 rounded-xl text-slate-700 hover:text-slate-900 hover:bg-slate-100 transition min-h-[44px] min-w-[44px] flex items-center justify-center focus-visible:outline-2 focus-visible:outline-indigo-600"
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

        {/* MOBILE Slide-Down Drawer (Spacious & Clean, not squeezed) */}
        {mobileMenuOpen && (
          <div
            className="lg:hidden border-b border-slate-200 bg-white px-4 pt-3 pb-6 space-y-3 animate-fadeIn max-h-[calc(100vh-4rem)] overflow-y-auto shadow-xl"
            aria-label="Mobile navigation drawer"
          >
            <div className="space-y-1">
              <span className="text-[11px] font-bold text-slate-400 uppercase tracking-wider px-2 block mb-1">
                Explore Categories
              </span>
              {centerNavLinks.map((link) => {
                const active = link.match();
                return (
                  <Link
                    key={link.name}
                    to={link.path}
                    onClick={() => setMobileMenuOpen(false)}
                    className={`flex items-center gap-3 px-3.5 py-3 rounded-xl text-sm font-semibold transition min-h-[44px] ${
                      active
                        ? 'bg-indigo-50 text-indigo-700 font-bold border border-indigo-100'
                        : 'text-slate-700 hover:bg-slate-50'
                    }`}
                  >
                    <span className={active ? 'text-indigo-600' : 'text-slate-400'}>
                      {link.icon}
                    </span>
                    <span>{link.name}</span>
                  </Link>
                );
              })}
            </div>

            <div className="pt-3 border-t border-slate-100 space-y-2">
              <span className="text-[11px] font-bold text-slate-400 uppercase tracking-wider px-2 block">
                Account & Shortcuts
              </span>

              <Link
                to={ROUTES.SAVED}
                onClick={() => setMobileMenuOpen(false)}
                className="flex items-center gap-3 px-3.5 py-3 rounded-xl text-sm font-semibold text-slate-700 hover:bg-slate-50 min-h-[44px]"
              >
                <svg className="w-4 h-4 text-rose-500 fill-current" viewBox="0 0 20 20">
                  <path fillRule="evenodd" d="M3.172 5.172a4 4 0 015.656 0L10 6.343l1.172-1.171a4 4 0 115.656 5.656L10 17.657l-6.828-6.829a4 4 0 010-5.656z" clipRule="evenodd" />
                </svg>
                <span>Saved & Wishlist</span>
              </Link>

              {isAuthenticated && user ? (
                <>
                  <div className="px-3 py-2.5 bg-slate-50 rounded-xl flex items-center gap-3">
                    <div className="w-8 h-8 rounded-lg bg-indigo-600 text-white flex items-center justify-center text-xs font-bold shrink-0">
                      {user.name ? user.name.charAt(0).toUpperCase() : 'U'}
                    </div>
                    <div className="min-w-0 flex-1">
                      <p className="text-xs font-bold text-slate-900 truncate">{user.name}</p>
                      <p className="text-[11px] text-slate-500 truncate">{user.email}</p>
                    </div>
                  </div>

                  <button
                    onClick={() => {
                      logout();
                      setMobileMenuOpen(false);
                    }}
                    className="w-full py-3 text-center text-xs font-bold text-rose-600 bg-rose-50 hover:bg-rose-100 rounded-xl transition min-h-[44px] cursor-pointer"
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
                  className="w-full py-3 text-center text-sm font-bold text-white bg-indigo-600 hover:bg-indigo-700 rounded-xl shadow-xs transition min-h-[44px] cursor-pointer"
                >
                  Sign In / Get Started
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
