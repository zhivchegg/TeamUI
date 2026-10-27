// HTTP-клиент для бэкенда.
//
// Access-токен хранится только в памяти модуля (не в localStorage): его не видит чужой скрипт,
// и он теряется при перезагрузке, что и нужно. Refresh-токен живёт в httpOnly-cookie,
// которую ставит бэкенд, поэтому JavaScript до него не добирается.

import type { ApiResponse } from './types';

export class ApiError extends Error {
  readonly status: number;

  constructor(status: number, message: string) {
    super(message);
    this.name = 'ApiError';
    this.status = status;
  }
}

let accessToken: string | null = null;

/** Функция обновления сессии. Возвращает true, если новый токен получен. */
let refresher: (() => Promise<boolean>) | null = null;

export function setAccessToken(token: string | null): void {
  accessToken = token;
}

export function setRefresher(fn: (() => Promise<boolean>) | null): void {
  refresher = fn;
}

async function readErrorMessage(res: Response): Promise<string> {
  try {
    const body = (await res.json()) as { message?: string };
    if (body.message) {
      return body.message;
    }
  } catch {
    // Тело не JSON, используем текст статуса ниже.
  }
  return res.statusText || `Ошибка ${res.status}`;
}

/**
 * Выполняет запрос к API. При 401 один раз пробует обновить сессию и повторяет запрос.
 * Запросы к /api/auth/* не обновляются, чтобы не зациклиться на неудачном входе.
 */
export async function apiFetch<T>(path: string, init: RequestInit = {}, retried = false): Promise<T> {
  const headers = new Headers(init.headers);
  if (init.body !== undefined && !headers.has('Content-Type')) {
    headers.set('Content-Type', 'application/json');
  }
  if (accessToken) {
    headers.set('Authorization', `Bearer ${accessToken}`);
  }

  const res = await fetch(path, { ...init, headers, credentials: 'include' });

  if (res.status === 401 && !retried && refresher && !path.startsWith('/api/auth/')) {
    const refreshed = await refresher();
    if (refreshed) {
      return apiFetch<T>(path, init, true);
    }
  }

  if (!res.ok) {
    throw new ApiError(res.status, await readErrorMessage(res));
  }
  if (res.status === 204) {
    return undefined as T;
  }
  return unwrapEnvelope<T>(res, await res.json());
}

/** Достаёт полезные данные из конверта `{status, message, data}`. Иначе ответ не соответствует контракту. */
function unwrapEnvelope<T>(res: Response, body: unknown): T {
  if (!isEnvelope(body)) {
    throw new ApiError(res.status, 'Неожиданный формат ответа сервера');
  }
  if (body.status !== '0') {
    throw new ApiError(Number(body.status) || res.status, body.message || `Ошибка ${body.status}`);
  }
  return body.data as T;
}

function isEnvelope(body: unknown): body is ApiResponse<unknown> {
  return typeof body === 'object' && body !== null && 'status' in body && 'data' in body;
}
