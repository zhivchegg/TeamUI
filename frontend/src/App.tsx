import { BrowserRouter, Link, Navigate, Route, Routes } from 'react-router-dom';
import { useAuth } from './auth/AuthContext';
import { LoginPage } from './pages/LoginPage';
import { DashboardPage } from './pages/DashboardPage';
import { MeetingsPage } from './pages/MeetingsPage';
import { MeetingDetailPage } from './pages/MeetingDetailPage';

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
  return (
    <>
      <header className="topbar">
        <strong>TeamUI</strong>
        {user && (
          <nav>
            <Link to="/">Дашборд</Link>
            <Link to="/meetings">Встречи</Link>
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
