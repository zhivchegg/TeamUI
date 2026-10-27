import { useEffect, useState, type FormEvent } from 'react';
import { Link } from 'react-router-dom';
import { ApiError } from '../api/client';
import { createEvent, getDashboard, listLeadMeetings, submitPulse } from '../api/endpoints';
import type { Dashboard, EventType, ImpactLevel, Meeting, MeetingPreview, TeamMemberCard } from '../api/types';
import { criticalityLabel, eventTypeLabel, formatDateTime, impactLabel, meetingStatusLabel } from '../format';
import { PersonSelect } from '../components/PersonSelect';

const EVENT_TYPES: EventType[] = ['ACHIEVEMENT', 'NOTE', 'TRAINING', 'INCIDENT', 'LATENESS', 'TASK_MISS'];
const IMPACT_LEVELS: ImpactLevel[] = ['POSITIVE_LOW', 'POSITIVE_HIGH', 'NEGATIVE_LOW', 'NEGATIVE_HIGH'];

export function DashboardPage() {
  const [data, setData] = useState<Dashboard | null>(null);
  const [leadMeetings, setLeadMeetings] = useState<Meeting[] | null>(null);
  const [error, setError] = useState<string | null>(null);

  function loadDashboard() {
    getDashboard().then(setData).catch(() => setError('Не удалось загрузить дашборд'));
  }

  useEffect(() => {
    loadDashboard();
    listLeadMeetings(0, 50)
      .then((page) => setLeadMeetings(page.content))
      .catch(() => setLeadMeetings([]));
  }, []);

  if (error) {
    return <p role="alert" className="error">{error}</p>;
  }
  if (!data) {
    return <p className="muted">Загрузка…</p>;
  }

  const team = data.teamHealth;
  const cluster = data.clusterOverview;
  const stream = data.streamOverview;
  const member = data.role === 'TEAM_MEMBER' ? data.memberMetrics : null;
  const isLead = data.role !== null && data.role !== 'TEAM_MEMBER';
  // Лиды остаются в составе команды, здесь они вынесены отдельно, чтобы было видно, кто ведёт команду.
  const leads = team?.memberCards.filter((m) => m.role === 'TEAM_LEAD' || m.role === 'IT_LEAD') ?? [];
  const namesById = new Map((team?.memberCards ?? []).map((m) => [m.id, m.fullName]));
  const upcomingAsLead = (leadMeetings ?? [])
    .filter((m) => m.status === 'PLANNED' && new Date(m.scheduledDate) > new Date())
    .sort((a, b) => a.scheduledDate.localeCompare(b.scheduledDate))
    .slice(0, 5);

  let scope = 'Роль не назначена';
  if (data.teamName) {
    scope = `Команда: ${data.teamName}`;
  } else if (cluster) {
    scope = `Кластер: ${cluster.name}`;
  } else if (stream) {
    scope = `Стрим: ${stream.streamName}`;
  } else if (data.role) {
    scope = 'Команда не назначена';
  }

  return (
    <div className="stack wide">
      <section className="card">
        <h1>
          Здравствуйте, {data.userProfile.firstName} {data.userProfile.lastName}
        </h1>
        <p className="muted">{scope}</p>
        {data.role === null && <p className="muted">Обратитесь к администратору, чтобы вам назначили роль.</p>}
      </section>

      {member && (
        <>
          <PulseForm />

          <section className="card">
            <h2>Моя следующая встреча</h2>
            {member.upcomingMeeting ? (
              <MeetingLine meeting={member.upcomingMeeting} />
            ) : (
              <p className="muted">
                Нет запланированных встреч. <Link to="/meetings">Открыть встречи</Link>
              </p>
            )}
          </section>

          <section className="card">
            <h2>Мои открытые действия</h2>
            {member.openActions.length === 0 ? (
              <p className="muted">Открытых действий нет.</p>
            ) : (
              <ul>
                {member.openActions.map((a) => (
                  <li key={a.id}>
                    {a.text}
                    {a.dueDate && (
                      <span className={a.overdue ? 'error' : 'muted'}>
                        {' '}
                        — до {a.dueDate}
                        {a.overdue ? ', просрочено' : ''}
                      </span>
                    )}
                  </li>
                ))}
              </ul>
            )}
          </section>

          <section className="card">
            <h2>Последние встречи</h2>
            {member.lastMeetings.length === 0 ? (
              <p className="muted">Проведённых встреч пока нет.</p>
            ) : (
              <ul>
                {member.lastMeetings.map((m) => (
                  <li key={m.id}>
                    <MeetingLine meeting={m} />
                  </li>
                ))}
              </ul>
            )}
          </section>
        </>
      )}

      {isLead && !team && (
        <section className="card">
          <h2>Мои ближайшие встречи</h2>
          {leadMeetings === null ? (
            <p className="muted">Загрузка…</p>
          ) : upcomingAsLead.length === 0 ? (
            <p className="muted">
              Нет запланированных встреч. <Link to="/meetings">Открыть встречи</Link>
            </p>
          ) : (
            <ul>
              {upcomingAsLead.map((m) => (
                <li key={m.id}>
                  <Link to={`/meetings/${m.id}`}>
                    {formatDateTime(m.scheduledDate)}, {m.participantName}
                  </Link>
                </li>
              ))}
            </ul>
          )}
        </section>
      )}

      {team && (
        <>
          <section className="card">
            <h2>Сводка команды</h2>
            <p>
              Энергия команды:{' '}
              {team.teamEnergyAverage === null ? (
                <span className="muted">
                  мало данных: {team.energyResponses} из {team.energyMinResponses} ответов за последние 4 недели
                </span>
              ) : (
                <strong>{team.teamEnergyAverage.toFixed(1)} из 5</strong>
              )}{' '}
              <span
                className="muted"
                title="Среднее всех опросов самочувствия участников команды. Участник отправляет опрос сам, на своём дашборде."
              >
                ⓘ
              </span>{' '}
              <Link to="/manual#energy">Откуда берётся энергия</Link>
            </p>
            <p>
              Просроченных действий: <strong>{team.overdueActions.length}</strong>
            </p>
            <p>
              <Link to="/systems">Системы и экспертиза команды</Link>
            </p>
          </section>

          {data.teamId && (
            <EventForm teamId={data.teamId} members={team.memberCards} onCreated={loadDashboard} />
          )}

          {team.overdueActions.length > 0 && (
            <section className="card">
              <h2>Просроченные действия</h2>
              <ul>
                {team.overdueActions.map((a) => (
                  <li key={a.id}>
                    {a.text} <span className="muted">— {namesById.get(a.ownerId) ?? 'участник'}, до {a.dueDate}</span>
                  </li>
                ))}
              </ul>
            </section>
          )}

          <section className="card">
            <h2>Риски потери экспертизы</h2>
            {team.busFactorAlerts.length === 0 ? (
              <p className="muted">Рисков нет.</p>
            ) : (
              <ul>
                {team.busFactorAlerts.map((s) => (
                  <li key={s.systemId}>
                    <strong>{s.systemName}</strong>{' '}
                    <span className="muted">
                      ({criticalityLabel(s.criticality)}), экспертов: {s.expertCount}, продвинутых: {s.advancedCount}
                      {s.expertNames.length > 0 ? `: ${s.expertNames.join(', ')}` : ''}
                    </span>
                  </li>
                ))}
              </ul>
            )}
          </section>

          <section className="card">
            <h2>Лиды команды</h2>
            {leads.length === 0 ? (
              <p className="muted">Лиды не назначены.</p>
            ) : (
              <ul>
                {leads.map((m) => (
                  <li key={m.id}>
                    <strong>{m.fullName}</strong> <span className="muted">— {roleLabel(m.role)}</span>
                  </li>
                ))}
              </ul>
            )}
          </section>

          <section className="card">
            <h2>Участники команды</h2>
            {team.memberCards.length === 0 ? (
              <p className="muted">В команде пока нет участников.</p>
            ) : (
              <table>
                <thead>
                  <tr>
                    <th>Имя</th>
                    <th>Роль</th>
                    <th>Открытых действий</th>
                    <th>Последняя 1:1</th>
                  </tr>
                </thead>
                <tbody>
                  {team.memberCards.map((m) => (
                    <tr key={m.id}>
                      <td>{m.fullName}</td>
                      <td>{roleLabel(m.role)}</td>
                      <td>{m.openActionCount}</td>
                      <td>{m.lastOneOnOneDate ? formatDateTime(m.lastOneOnOneDate) : '—'}</td>
                    </tr>
                  ))}
                </tbody>
              </table>
            )}
          </section>

          <section className="card">
            <h2>Ближайшие встречи команды</h2>
            {team.upcomingMeetings.length === 0 ? (
              <p className="muted">
                Нет запланированных встреч. <Link to="/meetings">Открыть встречи</Link>
              </p>
            ) : (
              <ul>
                {team.upcomingMeetings.map((m) => (
                  <li key={m.id}>
                    <Link to={`/meetings/${m.id}`}>
                      {m.participantName}, {formatDateTime(m.scheduledDate)}
                    </Link>{' '}
                    <span className="muted">({meetingStatusLabel(m.status)})</span>
                  </li>
                ))}
              </ul>
            )}
          </section>

          <section className="card">
            <h2>Последние события</h2>
            {team.unacknowledgedEvents.length === 0 ? (
              <p className="muted">Событий пока нет.</p>
            ) : (
              <ul>
                {team.unacknowledgedEvents.map((e) => (
                  <li key={e.id}>
                    {e.title} <span className="muted">— {formatDateTime(e.createdAt)}</span>
                  </li>
                ))}
              </ul>
            )}
          </section>
        </>
      )}

      {cluster && (
        <section className="card">
          <h2>Команды кластера</h2>
          <p className="muted">
            Команд: {cluster.teamCount}, участников: {cluster.memberCount}, команд в зоне риска:{' '}
            {cluster.teamSummaries.filter((t) => t.atRisk).length}, просроченных действий: {cluster.overdueActions}
          </p>
          {cluster.teamSummaries.length === 0 ? (
            <p className="muted">В кластере пока нет команд.</p>
          ) : (
            <table>
              <thead>
                <tr>
                  <th>Команда</th>
                  <th>Участников</th>
                  <th>Просрочено</th>
                  <th>Зона риска</th>
                </tr>
              </thead>
              <tbody>
                {cluster.teamSummaries.map((t) => (
                  <tr key={t.id}>
                    <td>{t.name}</td>
                    <td>{t.memberCount}</td>
                    <td>{t.overdueActions}</td>
                    <td>{t.atRisk ? 'Да' : 'Нет'}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          )}
        </section>
      )}

      {stream && (
        <section className="card">
          <h2>Кластеры стрима</h2>
          <p className="muted">
            Команд: {stream.totalTeams}, участников: {stream.totalMembers}, команд в зоне риска:{' '}
            {stream.teamsAtRisk}
          </p>
          {stream.clusterSummaries.length === 0 ? (
            <p className="muted">В стриме пока нет кластеров.</p>
          ) : (
            <table>
              <thead>
                <tr>
                  <th>Кластер</th>
                  <th>Команд</th>
                  <th>Участников</th>
                  <th>Просрочено</th>
                  <th>Команд в зоне риска</th>
                </tr>
              </thead>
              <tbody>
                {stream.clusterSummaries.map((c) => (
                  <tr key={c.id}>
                    <td>{c.name}</td>
                    <td>{c.teamCount}</td>
                    <td>{c.memberCount}</td>
                    <td>{c.overdueActions}</td>
                    <td>{c.teamSummaries.filter((t) => t.atRisk).length}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          )}
        </section>
      )}
    </div>
  );
}

/** Опрос самочувствия: участник отмечает энергию и, если хочет, что мешает. */
function PulseForm() {
  const [energy, setEnergy] = useState('');
  const [burden, setBurden] = useState('');
  const [topics, setTopics] = useState('');
  const [status, setStatus] = useState<string | null>(null);
  const [error, setError] = useState<string | null>(null);

  async function onSubmit(e: FormEvent) {
    e.preventDefault();
    setError(null);
    setStatus(null);
    try {
      await submitPulse({
        energyScore: Number(energy),
        burdenNote: burden.trim() || null,
        topics: topics.trim() || null,
      });
      setStatus('Спасибо, запись сохранена.');
      setBurden('');
      setTopics('');
    } catch (err) {
      setError(err instanceof ApiError && err.status === 403 ? 'Недостаточно прав.' : 'Не удалось сохранить.');
    }
  }

  return (
    <section className="card">
      <h2>Как вы себя чувствуете?</h2>
      <form onSubmit={onSubmit} className="stack">
        <label>
          Энергия (1–5)
          <select required value={energy} onChange={(e) => setEnergy(e.target.value)}>
            <option value="" disabled>
              Выберите
            </option>
            {[1, 2, 3, 4, 5].map((n) => (
              <option key={n} value={String(n)}>
                {n}
              </option>
            ))}
          </select>
        </label>
        <label>
          Что мешает работать (необязательно)
          <textarea rows={2} value={burden} onChange={(e) => setBurden(e.target.value)} />
        </label>
        <label>
          О чём хотите поговорить (необязательно)
          <textarea rows={2} value={topics} onChange={(e) => setTopics(e.target.value)} />
        </label>
        <div className="row">
          <button type="submit" disabled={!energy}>
            Отправить
          </button>
          {status && <span className="muted">{status}</span>}
          {error && <span className="error">{error}</span>}
        </div>
      </form>
    </section>
  );
}

/** Событие в таймлайне участника, которое добавляет лид команды. */
function EventForm({
  teamId,
  members,
  onCreated,
}: {
  teamId: string;
  members: TeamMemberCard[];
  onCreated: () => void;
}) {
  const [userId, setUserId] = useState('');
  const [eventType, setEventType] = useState<EventType>('ACHIEVEMENT');
  const [title, setTitle] = useState('');
  const [category, setCategory] = useState('');
  const [impact, setImpact] = useState('');
  const [visible, setVisible] = useState(true);
  const [status, setStatus] = useState<string | null>(null);
  const [error, setError] = useState<string | null>(null);

  async function onSubmit(e: FormEvent) {
    e.preventDefault();
    setError(null);
    setStatus(null);
    try {
      await createEvent({
        userId,
        teamId,
        eventType,
        category: category.trim() || null,
        title: title.trim(),
        impactLevel: impact ? (impact as ImpactLevel) : null,
        visibleToEmployee: visible,
      });
      setStatus('Событие добавлено.');
      setTitle('');
      setCategory('');
      setImpact('');
      onCreated();
    } catch (err) {
      setError(err instanceof ApiError && err.status === 403 ? 'Недостаточно прав.' : 'Не удалось добавить событие.');
    }
  }

  return (
    <section className="card">
      <h2>Добавить событие</h2>
      <form onSubmit={onSubmit} className="stack">
        <PersonSelect
          label="Участник"
          required
          value={userId}
          onChange={setUserId}
          options={members.map((m) => ({ id: m.id, label: m.fullName }))}
        />
        <label>
          Тип
          <select value={eventType} onChange={(e) => setEventType(e.target.value as EventType)}>
            {EVENT_TYPES.map((t) => (
              <option key={t} value={t}>
                {eventTypeLabel(t)}
              </option>
            ))}
          </select>
        </label>
        <label>
          Название
          <input required value={title} onChange={(e) => setTitle(e.target.value)} />
        </label>
        <label>
          Категория (необязательно)
          <input value={category} onChange={(e) => setCategory(e.target.value)} />
        </label>
        <label>
          Влияние (необязательно)
          <select value={impact} onChange={(e) => setImpact(e.target.value)}>
            <option value="">Не указано</option>
            {IMPACT_LEVELS.map((i) => (
              <option key={i} value={i}>
                {impactLabel(i)}
              </option>
            ))}
          </select>
        </label>
        <label className="row">
          <input type="checkbox" checked={visible} onChange={(e) => setVisible(e.target.checked)} />
          Видно участнику
        </label>
        <div className="row">
          <button type="submit" disabled={!userId || !title.trim()}>
            Добавить
          </button>
          {status && <span className="muted">{status}</span>}
          {error && <span className="error">{error}</span>}
        </div>
      </form>
    </section>
  );
}

/** Русское название роли в команде. Неизвестные роли выводим как есть. */
function roleLabel(role: string): string {
  const labels: Record<string, string> = {
    TEAM_LEAD: 'Лид команды',
    IT_LEAD: 'IT-лид команды',
    TEAM_MEMBER: 'Участник',
  };
  return labels[role] ?? role;
}

/** Строка встречи со ссылкой на страницу встречи, ведущий и статус. */
function MeetingLine({ meeting }: { meeting: MeetingPreview }) {
  return (
    <>
      <Link to={`/meetings/${meeting.id}`}>
        {formatDateTime(meeting.scheduledDate)}, с {meeting.leadName}
      </Link>{' '}
      <span className="muted">({meetingStatusLabel(meeting.status)})</span>
    </>
  );
}
