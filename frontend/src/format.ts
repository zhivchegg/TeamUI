const MEETING_STATUS_LABELS: Record<string, string> = {
  PLANNED: 'Запланирована',
  COMPLETED: 'Проведена',
  CANCELLED: 'Отменена',
  NO_SHOW: 'Не явился',
};

const MEETING_LEVEL_LABELS: Record<string, string> = {
  TEAM: 'Команда',
  CLUSTER: 'Кластер',
  STREAM: 'Стрим',
};

const ACTION_STATUS_LABELS: Record<string, string> = {
  OPEN: 'Открыто',
  IN_PROGRESS: 'В работе',
  DONE: 'Готово',
  CANCELLED: 'Отменено',
};

export function meetingStatusLabel(status: string): string {
  return MEETING_STATUS_LABELS[status] ?? status;
}

export function meetingLevelLabel(level: string): string {
  return MEETING_LEVEL_LABELS[level] ?? level;
}

const CRITICALITY_LABELS: Record<string, string> = {
  CRITICAL: 'Критичная',
  HIGH: 'Высокая',
  MEDIUM: 'Средняя',
  LOW: 'Низкая',
};

export function criticalityLabel(value: string): string {
  return CRITICALITY_LABELS[value] ?? value;
}

export function actionStatusLabel(status: string): string {
  return ACTION_STATUS_LABELS[status] ?? status;
}

export function formatDateTime(iso: string): string {
  return new Date(iso).toLocaleString('ru-RU', { dateStyle: 'medium', timeStyle: 'short' });
}

/** Переводит значение `datetime-local` (без часового пояса) в ISO-строку с временем браузера. */
export function localInputToIso(value: string): string {
  return new Date(value).toISOString();
}

/** Обратное преобразование: ISO-строка в значение `datetime-local` по времени браузера. */
export function isoToLocalInput(iso: string): string {
  const d = new Date(iso);
  const pad = (n: number) => String(n).padStart(2, '0');
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}T${pad(d.getHours())}:${pad(d.getMinutes())}`;
}
