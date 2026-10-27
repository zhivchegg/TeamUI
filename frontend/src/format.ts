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
