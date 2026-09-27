// Mirrors the backend DTOs exactly (see backend/api-service/.../dto/*.java).
// Do not invent fields here that the API does not actually return.

export type StationStatus = 'ONLINE' | 'STALE' | 'OFFLINE';

export interface Station {
  id: string;
  stationCode: string;
  name: string;
  riverName: string | null;
  latitude: number | null;
  longitude: number | null;
  status: StationStatus;
  lastSeenAt: string | null;
  createdAt: string | null;
}

export interface Measurement {
  eventId: string;
  stationId: string;
  observedAt: string;
  waterLevelM: number | null;
  rainfallMmPerHour: number | null;
  flowRateM3S: number | null;
  schemaVersion: number;
}

export type AlertStatus = 'ACTIVE' | 'ACKNOWLEDGED' | 'RESOLVED';
export type AlertSeverity = 'INFO' | 'WARNING' | 'HIGH' | 'CRITICAL';

export interface Alert {
  id: string;
  stationId: string;
  ruleCode: string;
  severity: AlertSeverity;
  status: AlertStatus;
  title: string;
  reason: string | null;
  evidence: Record<string, unknown> | null;
  triggeredAt: string;
  acknowledgedAt: string | null;
  resolvedAt: string | null;
  lastOccurredAt: string | null;
  repeatCount: number | null;
}

export interface NewStationInput {
  stationCode: string;
  name: string;
  riverName?: string;
  latitude?: number;
  longitude?: number;
}
