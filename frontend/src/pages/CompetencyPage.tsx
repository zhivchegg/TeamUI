import { useEffect, useState, type FormEvent } from 'react';
import { errorText } from '../api/errors';
import {
  createCompetency,
  deleteCompetency,
  getDashboard,
  listCompetencyMatrix,
  listCompetencyScores,
  submitLeadScore,
  submitSelfScore,
  updateCompetency,
} from '../api/endpoints';
import type { CompetencyDefinition, CompetencyDefinitionBody, CompetencyScore, Dashboard } from '../api/types';
import { useAuth } from '../auth/AuthContext';
import { PersonSelect } from '../components/PersonSelect';

const LEVELS = [1, 2, 3, 4];
const LEVEL_NAMES: Record<number, string> = { 1: 'Уровень 1', 2: 'Уровень 2', 3: 'Уровень 3', 4: 'Уровень 4' };

/** Текст описания уровня, если он задан. */
function levelText(c: CompetencyDefinition, level: number): string {
  const text = [c.level1, c.level2, c.level3, c.level4][level - 1];
  return text ?? 'Описание уровня не задано.';
}

/**
 * Матрица компетенций: самооценка сотрудника, оценки лида и редактирование списка.
 * Список общий для компании и отдельный для каждой команды.
 */
export function CompetencyPage() {
  const { user } = useAuth();
  const [matrix, setMatrix] = useState<CompetencyDefinition[] | null>(null);
  const [myScores, setMyScores] = useState<CompetencyScore[]>([]);
  const [dashboard, setDashboard] = useState<Dashboard | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [notice, setNotice] = useState<string | null>(null);

  function reload() {
    listCompetencyMatrix()
      .then(setMatrix)
      .catch((err) => setError(errorText(err, 'Не удалось загрузить матрицу.')));
    if (user) {
      listCompetencyScores(user.userId)
        .then((page) => setMyScores(page.content))
        .catch(() => setMyScores([]));
    }
  }

  useEffect(() => {
    reload();
    getDashboard().then(setDashboard).catch(() => setDashboard(null));
  }, []);

  if (error) {
    return <p role="alert" className="error">{error}</p>;
  }
  if (!matrix || !user) {
    return <p className="muted">Загрузка…</p>;
  }

  const isTeamLead = dashboard?.role === 'TEAM_LEAD' || dashboard?.role === 'IT_LEAD';
  const teamId = dashboard?.teamId ?? null;
  const members = dashboard?.teamHealth?.memberCards ?? [];
  const canCreateCompany = matrix.some((c) => c.teamId === null && c.canEdit);

  return (
    <div className="stack wide">
      <section className="card">
        <h1>Матрица компетенций</h1>
        <p className="muted">
          Оцените себя по каждой компетенции. Уровень выбирайте по описанию: оно говорит, что нужно уметь. Лид
          оценивает вас отдельно, расхождение обсуждайте на 1:1. Оценки не используют для решений о зарплате и
          повышении.
        </p>
        {notice && <p role="status" className="notice">{notice}</p>}
      </section>

      <section className="card">
        <h2>Моя самооценка</h2>
        <table>
          <thead>
            <tr>
              <th>Компетенция</th>
              <th>Моя оценка</th>
              <th>Оценка лида</th>
            </tr>
          </thead>
          <tbody>
            {matrix.map((c) => (
              <SelfRow
                key={c.id}
                competency={c}
                score={myScores.find((s) => s.competencyId === c.id) ?? null}
                onSaved={() => {
                  setNotice('Самооценка сохранена.');
                  reload();
                }}
                onError={setError}
                userId={user.userId}
              />
            ))}
          </tbody>
        </table>
      </section>

      {isTeamLead && teamId && members.length > 0 && (
        <TeamRatings matrix={matrix} members={members} onSaved={() => setNotice('Оценка лида сохранена.')} />
      )}

      <section className="card">
        <h2>Список компетенций</h2>
        {matrix.filter((c) => c.canEdit).length === 0 && !(isTeamLead && teamId) && !canCreateCompany ? (
          <p className="muted">Список редактируют лиды направления и команд.</p>
        ) : null}
        {matrix
          .filter((c) => c.canEdit)
          .map((c) => (
            <EditRow
              key={c.id}
              competency={c}
              onSaved={() => {
                setNotice('Компетенция сохранена.');
                reload();
              }}
              onError={setError}
            />
          ))}
        {(canCreateCompany || (isTeamLead && teamId)) && (
          <CreateForm
            teamId={isTeamLead && teamId ? teamId : null}
            allowCompany={canCreateCompany}
            onCreated={() => {
              setNotice('Компетенция добавлена.');
              reload();
            }}
            onError={setError}
          />
        )}
      </section>
    </div>
  );
}

/** Строка самооценки: выбор уровня, описание выбранного уровня и оценка лида. */
function SelfRow({
  competency,
  score,
  userId,
  onSaved,
  onError,
}: {
  competency: CompetencyDefinition;
  score: CompetencyScore | null;
  userId: string;
  onSaved: () => void;
  onError: (message: string) => void;
}) {
  const [level, setLevel] = useState<string>(score?.selfScore ? String(score.selfScore) : '');
  const selected = level ? Number(level) : null;

  async function save() {
    if (!selected) return;
    try {
      await submitSelfScore(userId, { competencyId: competency.id, selfScore: selected, comment: null });
      onSaved();
    } catch (err) {
      onError(errorText(err, 'Не удалось сохранить самооценку.'));
    }
  }

  return (
    <tr>
      <td>
        <strong>{competency.name}</strong>
        {competency.description && <div className="muted">{competency.description}</div>}
        {selected && <div className="level-text">{LEVEL_NAMES[selected]}: {levelText(competency, selected)}</div>}
      </td>
      <td>
        <select value={level} onChange={(e) => setLevel(e.target.value)} aria-label={`Моя оценка: ${competency.name}`}>
          <option value="">—</option>
          {LEVELS.map((n) => (
            <option key={n} value={String(n)}>
              {n}
            </option>
          ))}
        </select>{' '}
        <button type="button" onClick={() => void save()} disabled={!selected}>
          Сохранить
        </button>
      </td>
      <td>{score?.leadScore ? `${score.leadScore}${score.scoredByName ? ` (${score.scoredByName})` : ''}` : '—'}</td>
    </tr>
  );
}

/** Оценки членов команды для лида команды. */
function TeamRatings({
  matrix,
  members,
  onSaved,
}: {
  matrix: CompetencyDefinition[];
  members: { id: string; fullName: string }[];
  onSaved: () => void;
}) {
  const [memberId, setMemberId] = useState('');
  const [scores, setScores] = useState<CompetencyScore[]>([]);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    if (!memberId) return;
    listCompetencyScores(memberId)
      .then((page) => setScores(page.content))
      .catch(() => setScores([]));
  }, [memberId]);

  return (
    <section className="card">
      <h2>Оценки команды</h2>
      <PersonSelect
        label="Сотрудник"
        value={memberId}
        onChange={setMemberId}
        options={members.map((m) => ({ id: m.id, label: m.fullName }))}
      />
      {error && <p role="alert" className="error">{error}</p>}
      {memberId && (
        <table>
          <thead>
            <tr>
              <th>Компетенция</th>
              <th>Самооценка</th>
              <th>Моя оценка</th>
            </tr>
          </thead>
          <tbody>
            {matrix.map((c) => (
              <LeadRow
                key={c.id}
                competency={c}
                memberId={memberId}
                score={scores.find((s) => s.competencyId === c.id) ?? null}
                onSaved={() => {
                  onSaved();
                  listCompetencyScores(memberId).then((page) => setScores(page.content));
                }}
                onError={setError}
              />
            ))}
          </tbody>
        </table>
      )}
    </section>
  );
}

function LeadRow({
  competency,
  memberId,
  score,
  onSaved,
  onError,
}: {
  competency: CompetencyDefinition;
  memberId: string;
  score: CompetencyScore | null;
  onSaved: () => void;
  onError: (message: string) => void;
}) {
  const [level, setLevel] = useState<string>(score?.leadScore ? String(score.leadScore) : '');

  async function save() {
    if (!level) return;
    try {
      await submitLeadScore(memberId, { competencyId: competency.id, leadScore: Number(level), comment: null });
      onSaved();
    } catch (err) {
      onError(errorText(err, 'Не удалось сохранить оценку.'));
    }
  }

  return (
    <tr>
      <td>
        <strong>{competency.name}</strong>
        {level && <div className="level-text">{LEVEL_NAMES[Number(level)]}: {levelText(competency, Number(level))}</div>}
      </td>
      <td>{score?.selfScore ?? '—'}</td>
      <td>
        <select value={level} onChange={(e) => setLevel(e.target.value)} aria-label={`Оценка лида: ${competency.name}`}>
          <option value="">—</option>
          {LEVELS.map((n) => (
            <option key={n} value={String(n)}>
              {n}
            </option>
          ))}
        </select>{' '}
        <button type="button" onClick={() => void save()} disabled={!level}>
          Сохранить
        </button>
      </td>
    </tr>
  );
}

/** Редактирование одной компетенции: название, описание и четыре уровня. */
function EditRow({
  competency,
  onSaved,
  onError,
}: {
  competency: CompetencyDefinition;
  onSaved: () => void;
  onError: (message: string) => void;
}) {
  const [draft, setDraft] = useState<CompetencyDefinitionBody>(toBody(competency));
  const [confirming, setConfirming] = useState(false);

  async function onSubmit(e: FormEvent) {
    e.preventDefault();
    try {
      await updateCompetency(competency.id, draft);
      onSaved();
    } catch (err) {
      onError(errorText(err, 'Не удалось сохранить компетенцию.'));
    }
  }

  async function onDelete() {
    try {
      await deleteCompetency(competency.id);
      onSaved();
    } catch (err) {
      onError(errorText(err, 'Не удалось удалить компетенцию. По ней могут быть оценки.'));
    }
  }

  return (
    <details className="competency-edit">
      <summary>
        {competency.name} {competency.teamId ? '(команда)' : '(компания)'}
      </summary>
      <form onSubmit={onSubmit} className="stack">
        <CompetencyFields draft={draft} setDraft={setDraft} />
        <div className="row">
          <button type="submit" disabled={!draft.name.trim()}>
            Сохранить
          </button>
          {confirming ? (
            <>
              <button type="button" className="danger" onClick={() => void onDelete()}>
                Да, удалить
              </button>
              <button type="button" onClick={() => setConfirming(false)}>
                Отмена
              </button>
            </>
          ) : (
            <button type="button" className="danger" onClick={() => setConfirming(true)}>
              Удалить
            </button>
          )}
        </div>
      </form>
    </details>
  );
}

function CreateForm({
  teamId,
  allowCompany,
  onCreated,
  onError,
}: {
  teamId: string | null;
  allowCompany: boolean;
  onCreated: () => void;
  onError: (message: string) => void;
}) {
  const [scope, setScope] = useState<'company' | 'team'>(allowCompany ? 'company' : 'team');
  const [draft, setDraft] = useState<CompetencyDefinitionBody>({
    name: '',
    description: null,
    teamId: null,
    level1: null,
    level2: null,
    level3: null,
    level4: null,
  });

  async function onSubmit(e: FormEvent) {
    e.preventDefault();
    try {
      await createCompetency({ ...draft, teamId: scope === 'team' ? teamId : null });
      setDraft({ ...draft, name: '', description: null, level1: null, level2: null, level3: null, level4: null });
      onCreated();
    } catch (err) {
      onError(errorText(err, 'Не удалось добавить компетенцию.'));
    }
  }

  return (
    <form onSubmit={onSubmit} className="stack">
      <h3>Новая компетенция</h3>
      <label>
        Где видна
        <select value={scope} onChange={(e) => setScope(e.target.value as 'company' | 'team')}>
          {allowCompany && <option value="company">Вся компания</option>}
          {teamId && <option value="team">Только моя команда</option>}
        </select>
      </label>
      <CompetencyFields draft={draft} setDraft={setDraft} />
      <div className="row">
        <button type="submit" disabled={!draft.name.trim()}>
          Добавить
        </button>
      </div>
    </form>
  );
}

function CompetencyFields({
  draft,
  setDraft,
}: {
  draft: CompetencyDefinitionBody;
  setDraft: (d: CompetencyDefinitionBody) => void;
}) {
  return (
    <>
      <label>
        Название
        <input required value={draft.name} onChange={(e) => setDraft({ ...draft, name: e.target.value })} />
      </label>
      <label>
        Кратко о компетенции
        <input value={draft.description ?? ''} onChange={(e) => setDraft({ ...draft, description: e.target.value || null })} />
      </label>
      {LEVELS.map((n) => {
        const key = `level${n}` as 'level1' | 'level2' | 'level3' | 'level4';
        return (
          <label key={n}>
            {LEVEL_NAMES[n]}: что нужно уметь
            <textarea
              rows={2}
              value={draft[key] ?? ''}
              onChange={(e) => setDraft({ ...draft, [key]: e.target.value || null })}
            />
          </label>
        );
      })}
    </>
  );
}

function toBody(c: CompetencyDefinition): CompetencyDefinitionBody {
  return {
    name: c.name,
    description: c.description,
    teamId: c.teamId,
    level1: c.level1,
    level2: c.level2,
    level3: c.level3,
    level4: c.level4,
  };
}
