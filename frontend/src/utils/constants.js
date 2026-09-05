export const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080/api';

export const APP_NAME = 'CompareHub';
export const APP_TAGLINE = 'Universal Comparison Platform';

export const ROUTES = {
  HOME: '/',
  SHOPPING: '/shopping',
  SMART_CART: '/smart-cart',
  SMART_JOURNEY: '/smart-journey',
  BUDGET: '/budget',
  DECISION_ENGINE: '/decision-engine',
  FLIGHTS: '/flights',
  RIDES: '/rides',
  ALERTS: '/alerts',
  SAVED: '/saved',
  SAVINGS: '/savings',
  HISTORY: '/history',
  NOTIFICATIONS: '/notifications',
};

export const PRODUCT_CATEGORIES = [
  'All Categories',
  'Smartphones',
  'Laptops & Computers',
  'Audio & Headphones',
  'Smart Watches & Wearables',
  'Gaming & Consoles',
  'Cameras',
  'Home Appliances',
];

export const CABIN_CLASSES = [
  { value: 'ECONOMY', label: 'Economy' },
  { value: 'PREMIUM_ECONOMY', label: 'Premium Economy' },
  { value: 'BUSINESS', label: 'Business' },
  { value: 'FIRST', label: 'First Class' },
];

export const RIDE_TYPES = [
  { id: 'all', name: 'All Rides', icon: 'car' },
  { id: 'cab', name: 'Cabs & Taxis', icon: 'taxi' },
  { id: 'auto', name: 'Auto Rickshaw', icon: 'auto' },
  { id: 'bike', name: 'Bike Taxi', icon: 'bike' },
];

export default ROUTES;
