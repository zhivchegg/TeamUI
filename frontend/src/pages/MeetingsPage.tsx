import { useEffect, useState, type FormEvent } from 'react';
import { Link } from 'react-router-dom';
import { errorText } from '../api/errors';
import { createMeeting, listLeadMeetings, listMeetingParticipants, listParticipantMeetings } from '../api/endpoints';
import type { Meeting, MeetingLevel, MeetingParticipant } from '../api/types';
import {
  formatDateTime,
  isoToLocalInput,
  localInputToIso,
  meetingLevelLabel,
  meetingStatusLabel,
} from '../format';

/** Группы участников в выпадающем списке, в порядке уровней. */
const PARTICIPANT_GROUPS: { level: MeetingLevel; label: string }[] = [
  { level: 'TEAM', label: 'Члены команды' },
  { level: 'CLUSTER', label: 'Лиды команд' },
  { level: 'STREAM', label: 'Лиды кластеров' },
];

export function MeetingsPage() {
  const [asParticipant, setAsParticipant] = useState<Meeting[] | null>(null);
  const [asLead, setAsLead] = useState<Meeting[] | null>(null);
  const [participants, setParticipants] = useState<MeetingParticipant[] | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [notice, setNotice] = useState<string | null>(null);

  const [participantId, setParticipantId] = useState('');
  const [when, setWhen] = useState('');
  const [busy, setBusy] = useState(false);

  function reloadLead() {
    listLeadMeetings()
      .then((page) => setAsLead(page.content))
      .catch(() => setError('Не удалось загрузить встречи, которые вы ведёте'));
  }

  useEffect(() => {
    listParticipantMeetings()
      .then((page) => setAsParticipant(page.content))
      .catch(() => setError('Не удалось загрузить ваши встречи'));
    reloadLead();
    listMeetingParticipants()
      .then(setParticipants)
      .catch(() => setParticipants([]));
  }, []);

  // Создавать встречи и вести их могут только пользователи, у которых есть с кем встречаться.
  const canLead = participants !== null && participants.length > 0;

  async function onCreate(e: FormEvent) {
    e.preventDefault();
    setError(null);
    setNotice(null);
    const iso = localInputToIso(when);
    if (new Date(iso) < new Date()) {
      setError('Дата встречи уже прошла. Выберите время в будущем.');
      return;
    }
    setBusy(true);
    try {
      await createMeeting({ participantId, scheduledDate: iso, status: 'PLANNED' });
      const picked = participants?.find((p) => p.id === participantId);
      setNotice(`Встреча с ${picked?.fullName ?? 'участником'} назначена на ${formatDateTime(iso)}.`);
      setParticipantId('');
      setWhen('');
      reloadLead();
    } catch (err) {
      setError(errorText(err, 'Не удалось создать встречу.'));
    } finally {
      setBusy(false);
    }
  }

  return (
    <div className="stack wide">
      <section className="card">
        <h1>Встречи 1:1</h1>
        {error && <p role="alert" className="error">{error}</p>}
        {notice && <p role="status" className="notice">{notice}</p>}
      </section>

      <section className="card">
        <h2>Мои встречи</h2>
        <MeetingTable meetings={asParticipant} view="participant" />
      </section>

      {canLead && (
        <>
          <section className="card">
            <h2>Встречи, которые я веду</h2>
            <MeetingTable meetings={asLead} view="lead" />
          </section>

          <section className="card">
            <h2>Новая встреча</h2>
            {participants === null ? null : (
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
                  <input
                    type="datetime-local"
                    required
                    min={isoToLocalInput(new Date().toISOString())}
                    value={when}
                    onChange={(e) => setWhen(e.target.value)}
                  />
                </label>
                <button type="submit" disabled={busy || !participantId || !when}>
                  Создать
                </button>
              </form>
            )}
          </section>
        </>
      )}
    </div>
  );
}

/**
 * Сначала запланированные встречи в будущем, ближайшие первыми, затем остальные,
 * последние по дате первыми.
 */
function sortMeetings(meetings: Meeting[]): Meeting[] {
  const now = Date.now();
  const upcoming = meetings
    .filter((m) => m.status === 'PLANNED' && new Date(m.scheduledDate).getTime() > now)
    .sort((a, b) => a.scheduledDate.localeCompare(b.scheduledDate));
  const rest = meetings
    .filter((m) => !(m.status === 'PLANNED' && new Date(m.scheduledDate).getTime() > now))
    .sort((a, b) => b.scheduledDate.localeCompare(a.scheduledDate));
  return [...upcoming, ...rest];
}

/** Таблица встреч: участнику показываем ведущего, лиду показываем участника. */
function MeetingTable({ meetings, view }: { meetings: Meeting[] | null; view: 'participant' | 'lead' }) {
  if (meetings === null) {
    return <p className="muted">Загрузка…</p>;
  }
  if (meetings.length === 0) {
    return <p className="muted">Встреч пока нет.</p>;
  }
  return (
    <table>
      <thead>
        <tr>
          <th>Дата</th>
          <th>{view === 'participant' ? 'Ведущий' : 'Участник'}</th>
          <th>Уровень</th>
          <th>Статус</th>
        </tr>
      </thead>
      <tbody>
        {sortMeetings(meetings).map((m) => (
          <tr key={m.id}>
            <td>
              <Link to={`/meetings/${m.id}`}>{formatDateTime(m.scheduledDate)}</Link>
            </td>
            <td>{view === 'participant' ? m.leadName : m.participantName}</td>
            <td>{meetingLevelLabel(m.level)}</td>
            <td>{meetingStatusLabel(m.status)}</td>
          </tr>
        ))}
      </tbody>
    </table>
  );
}
