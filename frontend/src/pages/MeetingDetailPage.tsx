import { useEffect, useState, type FormEvent } from 'react';
import { useParams } from 'react-router-dom';
import { ApiError } from '../api/client';
import { createAction, getMeeting, updateAction, updateNotes } from '../api/endpoints';
import type { ActionItem, ActionStatus, Meeting } from '../api/types';
import { actionStatusLabel, formatDateTime, meetingStatusLabel } from '../format';

const ACTION_STATUSES: ActionStatus[] = ['OPEN', 'IN_PROGRESS', 'DONE', 'CANCELLED'];

function errorText(err: unknown): string {
  if (err instanceof ApiError && err.status === 409) {
    return 'Встреча уже изменена в другом окне. Обновите страницу и повторите.';
  }
  return err instanceof ApiError ? err.message : 'Произошла ошибка';
}

export function MeetingDetailPage() {
  const { id = '' } = useParams();
  const [meeting, setMeeting] = useState<Meeting | null>(null);
  const [error, setError] = useState<string | null>(null);

  const [sharedNotes, setSharedNotes] = useState('');
  const [privateNotes, setPrivateNotes] = useState('');
  const [notesSaved, setNotesSaved] = useState(false);

  const [actionText, setActionText] = useState('');
  const [actionOwnerId, setActionOwnerId] = useState('');
  const [actionDue, setActionDue] = useState('');

  function applyMeeting(m: Meeting) {
    setMeeting(m);
    setSharedNotes(m.sharedNotes ?? '');
    setPrivateNotes(m.privateNotes ?? '');
    setActionOwnerId((current) => current || m.participantId);
  }

  useEffect(() => {
    getMeeting(id)
      .then(applyMeeting)
      .catch((err) => setError(errorText(err)));
  }, [id]);

  async function onSaveNotes(e: FormEvent) {
    e.preventDefault();
    if (!meeting) return;
    setError(null);
    setNotesSaved(false);
    try {
      const updated = await updateNotes(meeting.id, {
        sharedNotes: sharedNotes || null,
        privateNotes: privateNotes || null,
        version: meeting.version,
      });
      applyMeeting(updated);
      setNotesSaved(true);
    } catch (err) {
      setError(errorText(err));
    }
  }

  async function onAddAction(e: FormEvent) {
    e.preventDefault();
    if (!meeting) return;
    setError(null);
    try {
      await createAction(meeting.id, {
        text: actionText,
        ownerId: actionOwnerId,
        dueDate: actionDue || null,
        status: 'OPEN',
      });
      setActionText('');
      setActionDue('');
      applyMeeting(await getMeeting(meeting.id));
    } catch (err) {
      setError(errorText(err));
    }
  }

  async function onChangeActionStatus(action: ActionItem, status: ActionStatus) {
    if (!meeting) return;
    setError(null);
    try {
      await updateAction(meeting.id, action.id, {
        text: action.text,
        ownerId: action.ownerId,
        dueDate: action.dueDate,
        status,
      });
      applyMeeting(await getMeeting(meeting.id));
    } catch (err) {
      setError(errorText(err));
    }
  }

  if (!meeting) {
    return error ? <p role="alert" className="error">{error}</p> : <p className="muted">Загрузка…</p>;
  }

  return (
    <div className="stack wide">
      <section className="card">
        <h1>Встреча с {meeting.participantName}</h1>
        <p className="muted">
          {formatDateTime(meeting.scheduledDate)} · {meetingStatusLabel(meeting.status)}
        </p>
        {error && <p role="alert" className="error">{error}</p>}
      </section>

      <section className="card">
        <h2>Заметки</h2>
        <form onSubmit={onSaveNotes} className="stack">
          <label>
            Общие заметки (видят оба лида)
            <textarea rows={5} value={sharedNotes} onChange={(e) => setSharedNotes(e.target.value)} />
          </label>
          <label>
            Личные заметки (видите только вы)
            <textarea rows={5} value={privateNotes} onChange={(e) => setPrivateNotes(e.target.value)} />
          </label>
          <div className="row">
            <button type="submit">Сохранить заметки</button>
            {notesSaved && <span className="muted">Сохранено</span>}
          </div>
        </form>
      </section>

      <section className="card">
        <h2>Действия</h2>
        {meeting.actions.length === 0 ? (
          <p className="muted">Действий пока нет.</p>
        ) : (
          <ul className="actions">
            {meeting.actions.map((a) => (
              <li key={a.id}>
                <span>{a.text}</span>{' '}
                <span className="muted">
                  {a.ownerName}
                  {a.dueDate ? `, до ${a.dueDate}` : ''}
                </span>
                <select
                  aria-label="Статус действия"
                  value={a.status}
                  onChange={(e) => void onChangeActionStatus(a, e.target.value as ActionStatus)}
                >
                  {ACTION_STATUSES.map((s) => (
                    <option key={s} value={s}>
                      {actionStatusLabel(s)}
                    </option>
                  ))}
                </select>
              </li>
            ))}
          </ul>
        )}

        <form onSubmit={onAddAction} className="stack">
          <h3>Новое действие</h3>
          <label>
            Текст
            <input required value={actionText} onChange={(e) => setActionText(e.target.value)} />
          </label>
          <label>
            Ответственный
            <select value={actionOwnerId} onChange={(e) => setActionOwnerId(e.target.value)}>
              <option value={meeting.participantId}>{meeting.participantName}</option>
              <option value={meeting.leadId}>{meeting.leadName}</option>
            </select>
          </label>
          <label>
            Срок
            <input type="date" value={actionDue} onChange={(e) => setActionDue(e.target.value)} />
          </label>
          <button type="submit" disabled={!actionText || !actionOwnerId}>
            Добавить
          </button>
        </form>
      </section>
    </div>
  );
}
