export async function readHealth(signal: AbortSignal): Promise<void> {
  const response = await fetch('/actuator/health/readiness', {
    signal,
    credentials: 'same-origin',
    headers: { Accept: 'application/json' },
  });
  if (!response.ok) throw new Error('Health request failed');
  const payload: unknown = await response.json();
  if (
    !payload ||
    typeof payload !== 'object' ||
    !('status' in payload) ||
    payload.status !== 'UP'
  ) {
    throw new Error('Service is not ready');
  }
}
