import { useEffect, useState } from 'react';
import { BrowserRouter, Link, Navigate, Route, Routes, useLocation } from 'react-router-dom';
import { getUnreadCount } from './api/endpoints';
import { useAuth } from './auth/AuthContext';
import { LoginPage } from './pages/LoginPage';
import { DashboardPage } from './pages/DashboardPage';
import { MeetingsPage } from './pages/MeetingsPage';
import { MeetingDetailPage } from './pages/MeetingDetailPage';
import { NotificationsPage } from './pages/NotificationsPage';

function RequireAuth({ children }: { children: JSX.Element }) {
  const { user, loading } = useAuth();
  if (loading) {
    return <p className="muted">Загрузка…</p>;
  }
  if (!user) {
    return <Navigate to="/login" replace />;
  }
  return children;
}

function Shell() {
  const { user, signOut } = useAuth();
  const location = useLocation();
  const [unread, setUnread] = useState(0);

  // Счётчик обновляем при каждом переходе: уведомления читают на отдельной странице.
  useEffect(() => {
    if (!user) return;
    getUnreadCount()
      .then((r) => setUnread(r.count))
      .catch(() => setUnread(0));
  }, [user, location.pathname]);

  return (
    <>
      <header className="topbar">
        <strong>TeamUI</strong>
        {user && (
          <nav>
            <Link to="/">Дашборд</Link>
            <Link to="/meetings">Встречи</Link>
            <Link to="/notifications">Уведомления{unread > 0 ? ` (${unread})` : ''}</Link>
            <button type="button" className="link" onClick={() => void signOut()}>
              Выйти
            </button>
          </nav>
        )}
      </header>
      <main>
        <Routes>
          <Route path="/login" element={<LoginPage />} />
          <Route
            path="/"
            element={
              <RequireAuth>
                <DashboardPage />
              </RequireAuth>
            }
          />
          <Route
            path="/meetings"
            element={
              <RequireAuth>
                <MeetingsPage />
              </RequireAuth>
            }
          />
          <Route
            path="/notifications"
            element={
              <RequireAuth>
                <NotificationsPage />
              </RequireAuth>
            }
          />
          <Route
            path="/meetings/:id"
            element={
              <RequireAuth>
                <MeetingDetailPage />
              </RequireAuth>
            }
          />
        </Routes>
      </main>
    </>
  );
}

export function App() {
  return (
    <BrowserRouter>
      <Shell />
    </BrowserRouter>
  );
}
