import { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { errorText } from '../api/errors';
import { listNotifications, markAllNotificationsRead, markNotificationRead } from '../api/endpoints';
import type { NotificationItem } from '../api/types';
import { formatDateTime } from '../format';

export function NotificationsPage() {
  const navigate = useNavigate();
  const [items, setItems] = useState<NotificationItem[] | null>(null);
  const [error, setError] = useState<string | null>(null);

  function load() {
    listNotifications(0, 50)
      .then((page) => setItems(page.content))
      .catch((err) => setError(errorText(err, 'Не удалось загрузить уведомления.')));
  }

  useEffect(load, []);

  async function open(n: NotificationItem) {
    setError(null);
    try {
      if (!n.read) {
        await markNotificationRead(n.id);
      }
    } catch (err) {
      setError(errorText(err, 'Не удалось отметить уведомление.'));
      return;
    }
    if (n.meetingId) {
      navigate(`/meetings/${n.meetingId}`);
    } else {
      load();
    }
  }

  async function markAll() {
    setError(null);
    try {
      await markAllNotificationsRead();
      load();
    } catch (err) {
      setError(errorText(err, 'Не удалось отметить уведомления.'));
    }
  }

  const hasUnread = items?.some((n) => !n.read) ?? false;

  return (
    <div className="stack wide">
      <section className="card">
        <div className="row">
          <h1>Уведомления</h1>
          {hasUnread && (
            <button type="button" onClick={() => void markAll()}>
              Отметить все прочитанными
            </button>
          )}
        </div>
        {error && <p role="alert" className="error">{error}</p>}
        {items === null ? (
          <p className="muted">Загрузка…</p>
        ) : items.length === 0 ? (
          <p className="muted">Уведомлений пока нет.</p>
        ) : (
          <ul className="notifications">
            {items.map((n) => (
              <li key={n.id} className={n.read ? 'muted' : 'unread'}>
                <button type="button" className="link" onClick={() => void open(n)}>
                  {n.text}
                </button>{' '}
                <span className="muted">{formatDateTime(n.createdAt)}</span>
              </li>
            ))}
          </ul>
        )}
      </section>
    </div>
  );
}
