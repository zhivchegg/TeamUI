import { useEffect, useState, type FormEvent } from 'react';
import { useParams } from 'react-router-dom';
import { useAuth } from '../auth/AuthContext';
import { errorText } from '../api/errors';
import { createAction, getMeeting, updateAction, updateMeeting, updateNotes } from '../api/endpoints';
import type { ActionItem, ActionStatus, Meeting, MeetingStatus } from '../api/types';
import { actionStatusLabel, formatDateTime, meetingStatusLabel } from '../format';

const ACTION_STATUSES: ActionStatus[] = ['OPEN', 'IN_PROGRESS', 'DONE', 'CANCELLED'];
const MEETING_STATUSES: MeetingStatus[] = ['PLANNED', 'COMPLETED', 'NO_SHOW', 'CANCELLED'];

export function MeetingDetailPage() {
  const { id = '' } = useParams();
  const { user } = useAuth();
  const viewerId = user?.userId ?? '';

  const [meeting, setMeeting] = useState<Meeting | null>(null);
  const [error, setError] = useState<string | null>(null);

  const [sharedNotes, setSharedNotes] = useState('');
  const [privateNotes, setPrivateNotes] = useState('');
  const [notesSaved, setNotesSaved] = useState(false);

  const [meetingStatus, setMeetingStatus] = useState<MeetingStatus>('PLANNED');

  const [actionText, setActionText] = useState('');
  const [actionOwnerId, setActionOwnerId] = useState('');
  const [actionDue, setActionDue] = useState('');

  function applyMeeting(m: Meeting) {
    setMeeting(m);
    setSharedNotes(m.sharedNotes ?? '');
    setPrivateNotes(m.privateNotes ?? '');
    setMeetingStatus(m.status);
    setActionOwnerId((current) => current || m.participantId);
  }

  useEffect(() => {
    getMeeting(id)
      .then(applyMeeting)
      .catch((err) => setError(errorText(err, 'Не удалось открыть встречу.')));
  }, [id]);

  if (!meeting) {
    return error ? <p role="alert" className="error">{error}</p> : <p className="muted">Загрузка…</p>;
  }

  // Права по ролям в этой встрече. Серверные проверки те же: участник не может менять заметки
  // и добавлять действия, а лид, не являющийся автором, не меняет личные заметки и статус встречи.
  const isParticipant = meeting.participantId === viewerId;
  const isAuthor = meeting.leadId === viewerId;
  const isLead = !isParticipant;

  async function onSaveNotes(e: FormEvent) {
    e.preventDefault();
    if (!meeting) return;
    setError(null);
    setNotesSaved(false);
    try {
      // Сервер отклоняет любое значение, кроме null, если у пользователя нет права на поле.
      const updated = await updateNotes(meeting.id, {
        sharedNotes: isLead ? sharedNotes || null : null,
        privateNotes: isAuthor ? privateNotes || null : null,
        version: meeting.version,
      });
      applyMeeting(updated);
      setNotesSaved(true);
    } catch (err) {
      setError(errorText(err, 'Не удалось сохранить заметки.'));
    }
  }

  async function onSaveMeetingStatus(e: FormEvent) {
    e.preventDefault();
    if (!meeting) return;
    setError(null);
    try {
      applyMeeting(
        await updateMeeting(meeting.id, {
          scheduledDate: meeting.scheduledDate,
          status: meetingStatus,
          energyScore: meeting.energyScore,
          energyNote: meeting.energyNote,
          version: meeting.version,
        }),
      );
    } catch (err) {
      setError(errorText(err, 'Не удалось изменить статус встречи.'));
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
      setError(errorText(err, 'Не удалось добавить действие.'));
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
      setError(errorText(err, 'Не удалось изменить действие.'));
    }
  }

  return (
    <div className="stack wide">
      <section className="card">
        <h1>Встреча с {isLead ? meeting.participantName : meeting.leadName}</h1>
        <p className="muted">
          {formatDateTime(meeting.scheduledDate)} · {meetingStatusLabel(meeting.status)}
        </p>
        {error && <p role="alert" className="error">{error}</p>}
      </section>

      {isAuthor && (
        <section className="card">
          <h2>Статус встречи</h2>
          <form onSubmit={onSaveMeetingStatus} className="row">
            <label>
              Статус
              <select value={meetingStatus} onChange={(e) => setMeetingStatus(e.target.value as MeetingStatus)}>
                {MEETING_STATUSES.map((s) => (
                  <option key={s} value={s}>
                    {meetingStatusLabel(s)}
                  </option>
                ))}
              </select>
            </label>
            <button type="submit" disabled={meetingStatus === meeting.status}>
              Сохранить статус
            </button>
          </form>
        </section>
      )}

      <section className="card">
        <h2>Заметки</h2>
        {isLead ? (
          <form onSubmit={onSaveNotes} className="stack">
            <label>
              Общие заметки (видят оба лида)
              <textarea rows={5} value={sharedNotes} onChange={(e) => setSharedNotes(e.target.value)} />
            </label>
            {isAuthor && (
              <label>
                Личные заметки (видите только вы)
                <textarea rows={5} value={privateNotes} onChange={(e) => setPrivateNotes(e.target.value)} />
              </label>
            )}
            <div className="row">
              <button type="submit">Сохранить заметки</button>
              {notesSaved && <span className="muted">Сохранено</span>}
            </div>
          </form>
        ) : (
          <p className="muted">Заметки к встрече ведут лиды. Вам они не показываются.</p>
        )}
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
                {isLead || a.ownerId === viewerId ? (
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
                ) : (
                  <span className="muted"> · {actionStatusLabel(a.status)}</span>
                )}
              </li>
            ))}
          </ul>
        )}

        {isLead && (
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
        )}
      </section>
    </div>
  );
}
