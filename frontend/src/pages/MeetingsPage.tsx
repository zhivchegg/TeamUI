import { useEffect, useState, type FormEvent } from 'react';
import { Link } from 'react-router-dom';
import { ApiError } from '../api/client';
import { createMeeting, listLeadMeetings, listMeetingParticipants } from '../api/endpoints';
import type { Meeting, MeetingLevel, MeetingParticipant } from '../api/types';
import { formatDateTime, localInputToIso, meetingLevelLabel, meetingStatusLabel } from '../format';

/** Группы участников в выпадающем списке, в порядке уровней. */
const PARTICIPANT_GROUPS: { level: MeetingLevel; label: string }[] = [
  { level: 'TEAM', label: 'Члены команды' },
  { level: 'CLUSTER', label: 'Лиды команд' },
  { level: 'STREAM', label: 'Лиды кластеров' },
];

export function MeetingsPage() {
  const [meetings, setMeetings] = useState<Meeting[] | null>(null);
  const [participants, setParticipants] = useState<MeetingParticipant[] | null>(null);
  const [error, setError] = useState<string | null>(null);

  const [participantId, setParticipantId] = useState('');
  const [when, setWhen] = useState('');
  const [busy, setBusy] = useState(false);

  function reload() {
    listLeadMeetings()
      .then((page) => setMeetings(page.content))
      .catch(() => setError('Не удалось загрузить встречи'));
  }

  useEffect(() => {
    reload();
    listMeetingParticipants()
      .then(setParticipants)
      .catch(() => setParticipants([]));
  }, []);

  async function onCreate(e: FormEvent) {
    e.preventDefault();
    setError(null);
    setBusy(true);
    try {
      await createMeeting({ participantId, scheduledDate: localInputToIso(when), status: 'PLANNED' });
      setWhen('');
      reload();
    } catch (err) {
      setError(err instanceof ApiError ? err.message : 'Не удалось создать встречу');
    } finally {
      setBusy(false);
    }
  }

  return (
    <div className="stack wide">
      <section className="card">
        <h1>Встречи 1:1</h1>
        {error && <p role="alert" className="error">{error}</p>}
        {meetings === null ? (
          <p className="muted">Загрузка…</p>
        ) : meetings.length === 0 ? (
          <p className="muted">Встреч пока нет.</p>
        ) : (
          <table>
            <thead>
              <tr>
                <th>Дата</th>
                <th>Участник</th>
                <th>Уровень</th>
                <th>Статус</th>
              </tr>
            </thead>
            <tbody>
              {meetings.map((m) => (
                <tr key={m.id}>
                  <td>
                    <Link to={`/meetings/${m.id}`}>{formatDateTime(m.scheduledDate)}</Link>
                  </td>
                  <td>{m.participantName}</td>
                  <td>{meetingLevelLabel(m.level)}</td>
                  <td>{meetingStatusLabel(m.status)}</td>
                </tr>
              ))}
            </tbody>
          </table>
        )}
      </section>

      <section className="card">
        <h2>Новая встреча</h2>
        {participants === null ? (
          <p className="muted">Загрузка…</p>
        ) : participants.length === 0 ? (
          <p className="muted">Нет людей, с которыми можно назначить встречу.</p>
        ) : (
          <form onSubmit={onCreate} className="stack">
            <label>
              Участник
              <select required value={participantId} onChange={(e) => setParticipantId(e.target.value)}>
                <option value="" disabled>
                  Выберите участника
                </option>
                {PARTICIPANT_GROUPS.map((group) => {
                  const items = participants.filter((p) => p.level === group.level);
                  if (items.length === 0) {
                    return null;
                  }
                  return (
                    <optgroup key={group.level} label={group.label}>
                      {items.map((p) => (
                        <option key={p.id} value={p.id}>
                          {p.fullName} — {p.scopeName}
                        </option>
                      ))}
                    </optgroup>
                  );
                })}
              </select>
            </label>
            <label>
              Дата и время
              <input type="datetime-local" required value={when} onChange={(e) => setWhen(e.target.value)} />
            </label>
            <button type="submit" disabled={busy || !participantId || !when}>
              Создать
            </button>
          </form>
        )}
      </section>
    </div>
  );
}
