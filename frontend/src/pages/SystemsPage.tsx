import { useEffect, useState, type FormEvent } from 'react';
import { ApiError } from '../api/client';
import { assignExpertise, createSystem, getDashboard, listTeamSystems } from '../api/endpoints';
import type { Criticality, Dashboard, ExpertiseLevel, SystemWithPeople, TeamMemberCard } from '../api/types';
import { criticalityLabel } from '../format';

const CRITICALITIES: Criticality[] = ['CRITICAL', 'HIGH', 'MEDIUM', 'LOW'];
const LEVELS: { value: ExpertiseLevel; label: string }[] = [
  { value: 'EXPERT', label: 'Эксперт' },
  { value: 'ADVANCED', label: 'Продвинутый' },
  { value: 'BASIC', label: 'Базовый' },
];

/**
 * Системы команды и кто в них эксперт. Лид команды добавляет системы и назначает экспертизу.
 */
export function SystemsPage() {
  const [dashboard, setDashboard] = useState<Dashboard | null>(null);
  const [systems, setSystems] = useState<SystemWithPeople[] | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [notice, setNotice] = useState<string | null>(null);

  const teamId = dashboard?.teamId ?? null;
  const members: TeamMemberCard[] = dashboard?.teamHealth?.memberCards ?? [];

  function reload(id: string) {
    listTeamSystems(id)
      .then(setSystems)
      .catch(() => setError('Не удалось загрузить системы команды.'));
  }

  useEffect(() => {
    getDashboard()
      .then((d) => {
        setDashboard(d);
        if (d.teamId) {
          reload(d.teamId);
        } else {
          setSystems([]);
        }
      })
      .catch(() => setError('Не удалось загрузить данные.'));
  }, []);

  if (error) {
    return <p role="alert" className="error">{error}</p>;
  }
  if (!dashboard || systems === null) {
    return <p className="muted">Загрузка…</p>;
  }
  if (!teamId) {
    return <p className="muted">Раздел доступен лидам команды.</p>;
  }

  return (
    <div className="stack wide">
      <section className="card">
        <h1>Системы команды: {dashboard.teamName}</h1>
        {notice && <p role="status" className="notice">{notice}</p>}
        {systems.length === 0 ? (
          <p className="muted">Систем пока нет. Добавьте первую ниже.</p>
        ) : (
          <table>
            <thead>
              <tr>
                <th>Система</th>
                <th>Критичность</th>
                <th>Эксперты</th>
                <th>Продвинутые</th>
                <th>Базовые</th>
              </tr>
            </thead>
            <tbody>
              {systems.map((s) => (
                <tr key={s.id}>
                  <td>{s.name}</td>
                  <td>{criticalityLabel(s.criticality)}</td>
                  <td>{s.experts.join(', ') || '—'}</td>
                  <td>{s.advanced.join(', ') || '—'}</td>
                  <td>{s.basic.join(', ') || '—'}</td>
                </tr>
              ))}
            </tbody>
          </table>
        )}
      </section>

      <SystemForm
        teamId={teamId}
        onCreated={() => {
          setNotice('Система добавлена.');
          reload(teamId);
        }}
      />

      <ExpertiseForm
        systems={systems}
        members={members}
        onAssigned={() => {
          setNotice('Экспертиза назначена.');
          reload(teamId);
        }}
      />
    </div>
  );
}

function SystemForm({ teamId, onCreated }: { teamId: string; onCreated: () => void }) {
  const [name, setName] = useState('');
  const [description, setDescription] = useState('');
  const [criticality, setCriticality] = useState<Criticality>('MEDIUM');
  const [error, setError] = useState<string | null>(null);

  async function onSubmit(e: FormEvent) {
    e.preventDefault();
    setError(null);
    try {
      await createSystem({ name: name.trim(), description: description.trim() || null, criticality, teamId });
      setName('');
      setDescription('');
      onCreated();
    } catch (err) {
      setError(err instanceof ApiError && err.status === 403 ? 'Недостаточно прав.' : 'Не удалось добавить систему.');
    }
  }

  return (
    <section className="card">
      <h2>Новая система</h2>
      <form onSubmit={onSubmit} className="stack">
        <label>
          Название
          <input required value={name} onChange={(e) => setName(e.target.value)} />
        </label>
        <label>
          Описание (необязательно)
          <input value={description} onChange={(e) => setDescription(e.target.value)} />
        </label>
        <label>
          Критичность
          <select value={criticality} onChange={(e) => setCriticality(e.target.value as Criticality)}>
            {CRITICALITIES.map((c) => (
              <option key={c} value={c}>
                {criticalityLabel(c)}
              </option>
            ))}
          </select>
        </label>
        <div className="row">
          <button type="submit" disabled={!name.trim()}>
            Добавить систему
          </button>
          {error && <span className="error">{error}</span>}
        </div>
      </form>
    </section>
  );
}

function ExpertiseForm({
  systems,
  members,
  onAssigned,
}: {
  systems: SystemWithPeople[];
  members: TeamMemberCard[];
  onAssigned: () => void;
}) {
  const [systemId, setSystemId] = useState('');
  const [userId, setUserId] = useState('');
  const [level, setLevel] = useState<ExpertiseLevel>('EXPERT');
  const [error, setError] = useState<string | null>(null);

  async function onSubmit(e: FormEvent) {
    e.preventDefault();
    setError(null);
    try {
      await assignExpertise(systemId, { userId, level });
      onAssigned();
    } catch (err) {
      setError(err instanceof ApiError && err.status === 403 ? 'Недостаточно прав.' : 'Не удалось назначить экспертизу.');
    }
  }

  return (
    <section className="card">
      <h2>Назначить экспертизу</h2>
      {systems.length === 0 || members.length === 0 ? (
        <p className="muted">Сначала добавьте систему. Экспертизу можно назначить участникам команды.</p>
      ) : (
        <form onSubmit={onSubmit} className="stack">
          <label>
            Система
            <select required value={systemId} onChange={(e) => setSystemId(e.target.value)}>
              <option value="" disabled>
                Выберите систему
              </option>
              {systems.map((s) => (
                <option key={s.id} value={s.id}>
                  {s.name}
                </option>
              ))}
            </select>
          </label>
          <label>
            Участник
            <select required value={userId} onChange={(e) => setUserId(e.target.value)}>
              <option value="" disabled>
                Выберите участника
              </option>
              {members.map((m) => (
                <option key={m.id} value={m.id}>
                  {m.fullName}
                </option>
              ))}
            </select>
          </label>
          <label>
            Уровень
            <select value={level} onChange={(e) => setLevel(e.target.value as ExpertiseLevel)}>
              {LEVELS.map((l) => (
                <option key={l.value} value={l.value}>
                  {l.label}
                </option>
              ))}
            </select>
          </label>
          <div className="row">
            <button type="submit" disabled={!systemId || !userId}>
              Назначить
            </button>
            {error && <span className="error">{error}</span>}
          </div>
        </form>
      )}
    </section>
  );
}
