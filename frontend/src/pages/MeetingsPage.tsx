import { useEffect, useState, type FormEvent } from 'react';
import { Link } from 'react-router-dom';
import { errorText } from '../api/errors';
import { createMeeting, listLeadMeetings, listMeetingParticipants, listParticipantMeetings } from '../api/endpoints';
import type { Meeting, MeetingLevel, MeetingParticipant } from '../api/types';
import { formatDateTime, localInputToIso, meetingLevelLabel, meetingStatusLabel } from '../format';

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
    setBusy(true);
    try {
      await createMeeting({ participantId, scheduledDate: localInputToIso(when), status: 'PLANNED' });
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
                  <input type="datetime-local" required value={when} onChange={(e) => setWhen(e.target.value)} />
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
        {meetings.map((m) => (
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
