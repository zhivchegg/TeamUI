import { apiFetch, setAccessToken } from './client';
import type {
  ActionBody,
  ActionItem,
  CreateMeetingBody,
  Dashboard,
  LoginResult,
  Meeting,
  MeetingParticipant,
  Page,
  UpdateMeetingBody,
  UpdateNotesBody,
} from './types';

// ----- Авторизация -----

export async function login(email: string, password: string): Promise<LoginResult> {
  const result = await apiFetch<LoginResult>('/api/auth/login', {
    method: 'POST',
    body: JSON.stringify({ email, password }),
  });
  setAccessToken(result.token);
  return result;
}

/**
 * Текущая попытка обновления. Refresh-токен одноразовый: второй параллельный запрос с той же
 * cookie считается повторным использованием и отзывает все сессии пользователя. Поэтому
 * параллельные вызовы делят один запрос.
 */
let refreshing: Promise<LoginResult | null> | null = null;

/** Обновляет сессию по refresh-cookie. Возвращает null, если cookie нет или истекла. */
export function refreshSession(): Promise<LoginResult | null> {
  if (!refreshing) {
    refreshing = doRefresh().finally(() => {
      refreshing = null;
    });
  }
  return refreshing;
}

async function doRefresh(): Promise<LoginResult | null> {
  try {
    const result = await apiFetch<LoginResult>('/api/auth/refresh', { method: 'POST' });
    setAccessToken(result.token);
    return result;
  } catch {
    setAccessToken(null);
    return null;
  }
}

export async function logout(): Promise<void> {
  try {
    await apiFetch<void>('/api/auth/logout', { method: 'POST' });
  } finally {
    setAccessToken(null);
  }
}

// ----- Дашборд -----

export function getDashboard(): Promise<Dashboard> {
  return apiFetch<Dashboard>('/api/dashboard');
}

// ----- Встречи -----

export function listLeadMeetings(page = 0, size = 20): Promise<Page<Meeting>> {
  return apiFetch<Page<Meeting>>(`/api/meetings?asLead=true&page=${page}&size=${size}`);
}

/** Встречи, где пользователь участник (его 1:1 с лидом). */
export function listParticipantMeetings(page = 0, size = 20): Promise<Page<Meeting>> {
  return apiFetch<Page<Meeting>>(`/api/meetings?asLead=false&page=${page}&size=${size}`);
}

export function updateMeeting(id: string, body: UpdateMeetingBody): Promise<Meeting> {
  return apiFetch<Meeting>(`/api/meetings/${id}`, { method: 'PUT', body: JSON.stringify(body) });
}

export function listMeetingParticipants(): Promise<MeetingParticipant[]> {
  return apiFetch<MeetingParticipant[]>('/api/meetings/participants');
}

export function getMeeting(id: string): Promise<Meeting> {
  return apiFetch<Meeting>(`/api/meetings/${id}`);
}

export function createMeeting(body: CreateMeetingBody): Promise<Meeting> {
  return apiFetch<Meeting>('/api/meetings', { method: 'POST', body: JSON.stringify(body) });
}

export function updateNotes(id: string, body: UpdateNotesBody): Promise<Meeting> {
  return apiFetch<Meeting>(`/api/meetings/${id}/notes`, { method: 'PUT', body: JSON.stringify(body) });
}

// ----- Действия встречи -----

export function createAction(meetingId: string, body: ActionBody): Promise<ActionItem> {
  return apiFetch<ActionItem>(`/api/meetings/${meetingId}/actions`, {
    method: 'POST',
    body: JSON.stringify(body),
  });
}

export function updateAction(meetingId: string, actionId: string, body: ActionBody): Promise<ActionItem> {
  return apiFetch<ActionItem>(`/api/meetings/${meetingId}/actions/${actionId}`, {
    method: 'PUT',
    body: JSON.stringify(body),
  });
}
