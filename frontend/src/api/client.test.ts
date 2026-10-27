import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { ApiError, apiFetch, setAccessToken, setRefresher } from './client';

/** Ответ в общем конверте бэкенда: {status, message, data}. */
function envelope(data: unknown, status = '0', message = ''): unknown {
  return { status, message, data };
}

function jsonResponse(httpStatus: number, body: unknown): Response {
  return new Response(body === undefined ? null : JSON.stringify(body), {
    status: httpStatus,
    headers: { 'Content-Type': 'application/json' },
  });
}

describe('apiFetch', () => {
  let fetchMock: ReturnType<typeof vi.fn>;

  beforeEach(() => {
    fetchMock = vi.fn();
    vi.stubGlobal('fetch', fetchMock);
    setAccessToken(null);
    setRefresher(null);
  });

  afterEach(() => {
    vi.unstubAllGlobals();
  });

  it('sends the access token as a Bearer header and includes cookies', async () => {
    setAccessToken('token-1');
    fetchMock.mockResolvedValueOnce(jsonResponse(200, envelope({ ok: true })));

    await apiFetch('/api/dashboard');

    const [url, init] = fetchMock.mock.calls[0];
    expect(url).toBe('/api/dashboard');
    expect(init.credentials).toBe('include');
    expect(new Headers(init.headers).get('Authorization')).toBe('Bearer token-1');
  });

  it('unwraps data from the envelope', async () => {
    fetchMock.mockResolvedValueOnce(jsonResponse(200, envelope({ value: 42 })));

    await expect(apiFetch<{ value: number }>('/api/meetings/1')).resolves.toEqual({ value: 42 });
  });

  it('unwraps a page into its pagination block', async () => {
    const page = { content: [{ id: 'a' }], page: 0, size: 20, totalElements: 1, totalPages: 1 };
    fetchMock.mockResolvedValueOnce(jsonResponse(200, envelope(page)));

    await expect(apiFetch('/api/meetings?asLead=true')).resolves.toEqual(page);
  });

  it('refreshes the session on 401 and retries the request once', async () => {
    setAccessToken('expired');
    const refresher = vi.fn(async () => {
      setAccessToken('fresh');
      return true;
    });
    setRefresher(refresher);
    fetchMock
      .mockResolvedValueOnce(jsonResponse(401, envelope(null, '401', 'expired')))
      .mockResolvedValueOnce(jsonResponse(200, envelope({ value: 42 })));

    const result = await apiFetch<{ value: number }>('/api/meetings');

    expect(result).toEqual({ value: 42 });
    expect(refresher).toHaveBeenCalledTimes(1);
    expect(fetchMock).toHaveBeenCalledTimes(2);
    expect(new Headers(fetchMock.mock.calls[1][1].headers).get('Authorization')).toBe('Bearer fresh');
  });

  it('does not retry forever when the retried request is still 401', async () => {
    const refresher = vi.fn(async () => true);
    setRefresher(refresher);
    fetchMock.mockResolvedValue(jsonResponse(401, envelope(null, '401', 'nope')));

    await expect(apiFetch('/api/meetings')).rejects.toBeInstanceOf(ApiError);
    expect(refresher).toHaveBeenCalledTimes(1);
    expect(fetchMock).toHaveBeenCalledTimes(2);
  });

  it('does not try to refresh on /api/auth endpoints', async () => {
    const refresher = vi.fn(async () => true);
    setRefresher(refresher);
    fetchMock.mockResolvedValueOnce(jsonResponse(401, envelope(null, '401', 'Invalid credentials')));

    await expect(apiFetch('/api/auth/login', { method: 'POST' })).rejects.toMatchObject({ status: 401 });
    expect(refresher).not.toHaveBeenCalled();
  });

  it('throws ApiError with the envelope message on failure', async () => {
    fetchMock.mockResolvedValueOnce(jsonResponse(409, envelope(null, '409', 'Version conflict')));

    await expect(apiFetch('/api/meetings/1/notes', { method: 'PUT' })).rejects.toMatchObject({
      status: 409,
      message: 'Version conflict',
    });
  });

  it('throws when a 200 response carries a non-zero envelope status', async () => {
    fetchMock.mockResolvedValueOnce(jsonResponse(200, envelope(null, '400', 'Bad input')));

    await expect(apiFetch('/api/meetings')).rejects.toMatchObject({ status: 400, message: 'Bad input' });
  });

  it('throws when the body is not in the envelope', async () => {
    fetchMock.mockResolvedValueOnce(jsonResponse(200, { value: 42 }));

    await expect(apiFetch('/api/meetings')).rejects.toMatchObject({
      status: 200,
      message: 'Неожиданный формат ответа сервера',
    });
  });

  it('returns undefined for 204 No Content', async () => {
    fetchMock.mockResolvedValueOnce(new Response(null, { status: 204 }));

    await expect(apiFetch('/api/meetings/1', { method: 'DELETE' })).resolves.toBeUndefined();
  });
});
