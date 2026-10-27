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

/** Обновляет сессию по refresh-cookie. Возвращает null, если cookie нет или истекла. */
export async function refreshSession(): Promise<LoginResult | null> {
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
