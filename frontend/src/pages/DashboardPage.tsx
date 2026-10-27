import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { getDashboard } from '../api/endpoints';
import type { Dashboard, MeetingPreview } from '../api/types';
import { formatDateTime, meetingStatusLabel } from '../format';

export function DashboardPage() {
  const [data, setData] = useState<Dashboard | null>(null);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    getDashboard().then(setData).catch(() => setError('Не удалось загрузить дашборд'));
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
  // Лиды остаются в составе команды, здесь они вынесены отдельно, чтобы было видно, кто ведёт команду.
  const leads = team?.memberCards.filter((m) => m.role === 'TEAM_LEAD' || m.role === 'IT_LEAD') ?? [];

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

      {team && (
        <>
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
            <h2>Ближайшие встречи</h2>
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
        </>
      )}

      {cluster && (
        <section className="card">
          <h2>Команды кластера</h2>
          <p className="muted">
            Команд: {cluster.teamCount}, участников: {cluster.memberCount}, команд в зоне риска:{' '}
            {cluster.teamSummaries.filter((t) => t.atRisk).length}
          </p>
          {cluster.teamSummaries.length === 0 ? (
            <p className="muted">В кластере пока нет команд.</p>
          ) : (
            <table>
              <thead>
                <tr>
                  <th>Команда</th>
                  <th>Участников</th>
                  <th>Зона риска</th>
                </tr>
              </thead>
              <tbody>
                {cluster.teamSummaries.map((t) => (
                  <tr key={t.id}>
                    <td>{t.name}</td>
                    <td>{t.memberCount}</td>
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
                  <th>Команд в зоне риска</th>
                </tr>
              </thead>
              <tbody>
                {stream.clusterSummaries.map((c) => (
                  <tr key={c.id}>
                    <td>{c.name}</td>
                    <td>{c.teamCount}</td>
                    <td>{c.memberCount}</td>
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
