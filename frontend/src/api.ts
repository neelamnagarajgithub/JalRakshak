import type { Alert, Measurement, NewStationInput, Station } from './types';

// Vite exposes env vars prefixed VITE_ via import.meta.env; see .env.example.
const API_BASE_URL: string =
  (import.meta as any).env?.VITE_API_BASE_URL ?? 'http://localhost:9000/api/v1';

class ApiError extends Error {
  status: number;
  constructor(status: number, message: string) {
    super(message);
    this.status = status;
  }
}

async function request<T>(path: string, init?: RequestInit): Promise<T> {
  let response: Response;
  try {
    response = await fetch(`${API_BASE_URL}${path}`, {
      headers: { 'Content-Type': 'application/json' },
      ...init,
    });
  } catch {
    throw new ApiError(0, 'Could not reach the API. Is the backend running on ' + API_BASE_URL + '?');
  }

  if (!response.ok) {
    let message = `Request failed (${response.status})`;
    try {
      const body = await response.json();
      message = body.message || body.reason || message;
    } catch {
      // response had no JSON body; keep the generic message
    }
    throw new ApiError(response.status, message);
  }

  if (response.status === 204) {
    return undefined as T;
  }

  const contentType = response.headers.get('content-type') ?? '';
  if (contentType.includes('application/json')) {
    return (await response.json()) as T;
  }
  // The simulator endpoint returns a plain-text confirmation, not JSON.
  return (await response.text()) as unknown as T;
}

export { ApiError };

// --- Stations ---
export const getStations = () => request<Station[]>('/stations');
export const getStation = (id: string) => request<Station>(`/stations/${id}`);
export const createStation = (input: NewStationInput) =>
  request<Station>('/stations', { method: 'POST', body: JSON.stringify(input) });

// --- Measurements ---
export const getRecentMeasurements = () => request<Measurement[]>('/measurements');
export const getMeasurementsForStation = (stationId: string) =>
  request<Measurement[]>(`/measurements/station/${stationId}`);

// --- Alerts ---
export const getActiveAlerts = () => request<Alert[]>('/alerts');
export const getAlertsForStation = (stationId: string) => request<Alert[]>(`/alerts/station/${stationId}`);
export const acknowledgeAlert = (id: string) => request<Alert>(`/alerts/${id}/acknowledge`, { method: 'POST' });
export const resolveAlert = (id: string) => request<Alert>(`/alerts/${id}/resolve`, { method: 'POST' });

// --- Simulator (demo data generator; see docs/demo-script.md) ---
export type ScenarioName =
  | 'normal' | 'rising-level' | 'rainfall' | 'spike' | 'offline' | 'duplicate' | 'delayed';
export const triggerScenario = (name: ScenarioName) =>
  request<string>(`/simulator/scenarios/${name}`, { method: 'POST' });
