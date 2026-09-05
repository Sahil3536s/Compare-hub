import React from 'react';
import { BrowserRouter, Routes, Route } from 'react-router-dom';
import { AppProvider } from './context/AppContext';
import { AuthProvider } from './context/AuthContext';
import MainLayout from './layouts/MainLayout';
import HomePage from './pages/HomePage';
import ShoppingPage from './pages/ShoppingPage';
import SmartCartPage from './pages/SmartCartPage';
import FlightsPage from './pages/FlightsPage';
import RidesPage from './pages/RidesPage';
import AlertsPage from './pages/AlertsPage';
import SavedPage from './pages/SavedPage';
import HistoryPage from './pages/HistoryPage';
import NotificationsPage from './pages/NotificationsPage';
import SavingsDashboardPage from './pages/SavingsDashboardPage';
import SmartJourneyPage from './pages/SmartJourneyPage';
import BudgetAssistantPage from './pages/BudgetAssistantPage';
import DecisionEnginePage from './pages/DecisionEnginePage';
import NotFoundPage from './pages/NotFoundPage';
import { ROUTES } from './utils/constants';

function App() {
  return (
    <AuthProvider>
      <AppProvider>
        <BrowserRouter>
          <Routes>
            <Route path={ROUTES.HOME} element={<MainLayout />}>
              <Route index element={<HomePage />} />
              <Route path="shopping" element={<ShoppingPage />} />
              <Route path="smart-cart" element={<SmartCartPage />} />
              <Route path="smart-journey" element={<SmartJourneyPage />} />
              <Route path="budget" element={<BudgetAssistantPage />} />
              <Route path="decision-engine" element={<DecisionEnginePage />} />
              <Route path="flights" element={<FlightsPage />} />
              <Route path="rides" element={<RidesPage />} />
              <Route path="alerts" element={<AlertsPage />} />
              <Route path="saved" element={<SavedPage />} />
              <Route path="savings" element={<SavingsDashboardPage />} />
              <Route path="history" element={<HistoryPage />} />
              <Route path="notifications" element={<NotificationsPage />} />
              <Route path="*" element={<NotFoundPage />} />
            </Route>
          </Routes>
        </BrowserRouter>
      </AppProvider>
    </AuthProvider>
  );
}

export default App;
