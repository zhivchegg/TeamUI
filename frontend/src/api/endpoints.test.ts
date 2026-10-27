import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { setAccessToken } from './client';
import { refreshSession } from './endpoints';

const session = {
  token: 'access-1',
  tokenType: 'Bearer',
  userId: 'u-1',
  email: 'anna@teamui.test',
  role: 'TEAM_MEMBER',
  expiresIn: 900,
};

describe('refreshSession', () => {
  let fetchMock: ReturnType<typeof vi.fn>;

  beforeEach(() => {
    fetchMock = vi.fn(
      async () =>
        new Response(JSON.stringify({ status: '0', message: '', data: session }), {
          status: 200,
          headers: { 'Content-Type': 'application/json' },
        }),
    );
    vi.stubGlobal('fetch', fetchMock);
    setAccessToken(null);
  });

  afterEach(() => {
    vi.unstubAllGlobals();
  });

  it('shares one request between concurrent callers, because the refresh token is single-use', async () => {
    const [first, second] = await Promise.all([refreshSession(), refreshSession()]);

    expect(fetchMock).toHaveBeenCalledTimes(1);
    expect(first).toEqual(second);
  });

  it('sends a new request once the previous one has finished', async () => {
    await refreshSession();
    await refreshSession();

    expect(fetchMock).toHaveBeenCalledTimes(2);
  });
});
