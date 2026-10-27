import { useEffect, useState, type FormEvent } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import { useAuth } from '../auth/AuthContext';
import { errorText } from '../api/errors';
import {
  createAction,
  deleteMeeting,
  getMeeting,
  updateAction,
  updateAgenda,
  updateMeeting,
  updateNotes,
} from '../api/endpoints';
import type { ActionItem, ActionStatus, Meeting, MeetingStatus } from '../api/types';
import {
  actionStatusLabel,
  formatDateTime,
  isoToLocalInput,
  localInputToIso,
  meetingStatusLabel,
} from '../format';

const ACTION_STATUSES: ActionStatus[] = ['OPEN', 'IN_PROGRESS', 'DONE', 'CANCELLED'];
const MEETING_STATUSES: MeetingStatus[] = ['PLANNED', 'COMPLETED', 'NO_SHOW', 'CANCELLED'];

export function MeetingDetailPage() {
  const { id = '' } = useParams();
  const navigate = useNavigate();
  const { user } = useAuth();
  const viewerId = user?.userId ?? '';

  const [meeting, setMeeting] = useState<Meeting | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [confirmingDelete, setConfirmingDelete] = useState(false);

  const [scheduledLocal, setScheduledLocal] = useState('');
  const [meetingStatus, setMeetingStatus] = useState<MeetingStatus>('PLANNED');
  const [energy, setEnergy] = useState('');
  const [paramsSaved, setParamsSaved] = useState(false);

  const [agendaText, setAgendaText] = useState('');
  const [agendaSaved, setAgendaSaved] = useState(false);

  const [sharedNotes, setSharedNotes] = useState('');
  const [privateNotes, setPrivateNotes] = useState('');
  const [notesSaved, setNotesSaved] = useState(false);

  const [actionText, setActionText] = useState('');
  const [actionOwnerId, setActionOwnerId] = useState('');
  const [actionDue, setActionDue] = useState('');

  function applyMeeting(m: Meeting) {
    setMeeting(m);
    setScheduledLocal(isoToLocalInput(m.scheduledDate));
    setMeetingStatus(m.status);
    setEnergy(m.energyScore === null ? '' : String(m.energyScore));
    setAgendaText(m.participantAgenda ?? '');
    setSharedNotes(m.sharedNotes ?? '');
    setPrivateNotes(m.privateNotes ?? '');
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

  // Права по ролям в этой встрече. Серверные проверки те же: участник не меняет заметки, действия
  // и параметры встречи, а лид, не являющийся автором, не меняет личные заметки и параметры.
  const isParticipant = meeting.participantId === viewerId;
  const isAuthor = meeting.leadId === viewerId;
  const isLead = !isParticipant;

  async function onSaveParams(e: FormEvent) {
    e.preventDefault();
    if (!meeting) return;
    setError(null);
    setParamsSaved(false);
    const iso = localInputToIso(scheduledLocal);
    if (meetingStatus === 'PLANNED' && new Date(iso) < new Date()) {
      setError('Запланированную встречу нельзя перенести на прошедшую дату.');
      return;
    }
    try {
      applyMeeting(
        await updateMeeting(meeting.id, {
          scheduledDate: iso,
          status: meetingStatus,
          energyScore: energy ? Number(energy) : null,
          energyNote: meeting.energyNote,
          version: meeting.version,
        }),
      );
      setParamsSaved(true);
    } catch (err) {
      setError(errorText(err, 'Не удалось сохранить параметры встречи.'));
    }
  }

  async function onDelete() {
    if (!meeting) return;
    setError(null);
    try {
      await deleteMeeting(meeting.id);
      navigate('/meetings');
    } catch (err) {
      setError(errorText(err, 'Не удалось удалить встречу.'));
    }
  }

  async function onSaveAgenda(e: FormEvent) {
    e.preventDefault();
    if (!meeting) return;
    setError(null);
    setAgendaSaved(false);
    try {
      applyMeeting(await updateAgenda(meeting.id, { agenda: agendaText || null, version: meeting.version }));
      setAgendaSaved(true);
    } catch (err) {
      setError(errorText(err, 'Не удалось сохранить повестку.'));
    }
  }

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
          <h2>Параметры встречи</h2>
          <form onSubmit={onSaveParams} className="stack">
            <div className="row">
              <label>
                Дата и время
                <input
                  type="datetime-local"
                  required
                  value={scheduledLocal}
                  onChange={(e) => setScheduledLocal(e.target.value)}
                />
              </label>
              <label>
                Статус встречи
                <select value={meetingStatus} onChange={(e) => setMeetingStatus(e.target.value as MeetingStatus)}>
                  {MEETING_STATUSES.map((s) => (
                    <option key={s} value={s}>
                      {meetingStatusLabel(s)}
                    </option>
                  ))}
                </select>
              </label>
              <label>
                Самочувствие участника (1–5)
                <select value={energy} onChange={(e) => setEnergy(e.target.value)}>
                  <option value="">Не указано</option>
                  {[1, 2, 3, 4, 5].map((n) => (
                    <option key={n} value={String(n)}>
                      {n}
                    </option>
                  ))}
                </select>
              </label>
            </div>
            <div className="row">
              <button type="submit">Сохранить встречу</button>
              {paramsSaved && <span className="muted">Сохранено</span>}
            </div>
          </form>
        </section>
      )}

      <section className="card">
        <h2>Повестка участника</h2>
        {isParticipant ? (
          <form onSubmit={onSaveAgenda} className="stack">
            <label>
              Вопросы и темы, которые вы хотите обсудить
              <textarea rows={4} value={agendaText} onChange={(e) => setAgendaText(e.target.value)} />
            </label>
            <div className="row">
              <button type="submit">Сохранить повестку</button>
              {agendaSaved && <span className="muted">Сохранено</span>}
            </div>
          </form>
        ) : meeting.participantAgenda ? (
          <p className="pre-wrap">{meeting.participantAgenda}</p>
        ) : (
          <p className="muted">Участник ещё не добавил вопросы.</p>
        )}
      </section>

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

      {isAuthor && (
        <section className="card">
          <h2>Удаление встречи</h2>
          <p className="muted">Встреча, её заметки и действия будут удалены без возможности восстановления.</p>
          {confirmingDelete ? (
            <div className="row">
              <button type="button" className="danger" onClick={() => void onDelete()}>
                Да, удалить встречу
              </button>
              <button type="button" onClick={() => setConfirmingDelete(false)}>
                Отмена
              </button>
            </div>
          ) : (
            <button type="button" className="danger" onClick={() => setConfirmingDelete(true)}>
              Удалить встречу
            </button>
          )}
        </section>
      )}
    </div>
  );
}
