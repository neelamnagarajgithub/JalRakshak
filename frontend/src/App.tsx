import React, { useCallback, useEffect, useMemo, useState } from 'react';
import {
  ApiError,
  acknowledgeAlert,
  createStation,
  getActiveAlerts,
  getRecentMeasurements,
  getStations,
  resolveAlert,
  triggerScenario,
  type ScenarioName,
} from './api';
import type { Alert, Measurement, Station } from './types';
import './App.css';

const POLL_INTERVAL_MS = 10000;

const SCENARIOS: { name: ScenarioName; label: string }[] = [
  { name: 'normal', label: 'Normal readings' },
  { name: 'rising-level', label: 'Rising level' },
  { name: 'rainfall', label: 'Heavy rainfall' },
  { name: 'spike', label: 'Sensor spike' },
  { name: 'offline', label: 'Station offline' },
  { name: 'duplicate', label: 'Duplicate reading' },
  { name: 'delayed', label: 'Delayed reading' },
];

function formatTime(iso: string | null | undefined): string {
  if (!iso) return '—';
  try {
    return new Date(iso).toLocaleString();
  } catch {
    return iso;
  }
}

function statusBadgeClass(status: string): string {
  switch (status) {
    case 'ONLINE':
    case 'ACTIVE':
      return 'badge badge-active';
    case 'STALE':
    case 'ACKNOWLEDGED':
      return 'badge badge-warning';
    case 'OFFLINE':
      return 'badge badge-offline';
    case 'RESOLVED':
      return 'badge badge-resolved';
    default:
      return 'badge';
  }
}

function severityBadgeClass(severity: string): string {
  switch (severity) {
    case 'CRITICAL':
      return 'badge badge-critical';
    case 'HIGH':
      return 'badge badge-offline';
    case 'WARNING':
      return 'badge badge-warning';
    default:
      return 'badge';
  }
}

const App: React.FC = () => {
  const [stations, setStations] = useState<Station[] | null>(null);
  const [measurements, setMeasurements] = useState<Measurement[] | null>(null);
  const [alerts, setAlerts] = useState<Alert[] | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [lastUpdated, setLastUpdated] = useState<Date | null>(null);
  const [scenarioBusy, setScenarioBusy] = useState<ScenarioName | null>(null);
  const [scenarioMessage, setScenarioMessage] = useState<string | null>(null);
  const [alertActionBusyId, setAlertActionBusyId] = useState<string | null>(null);
  const [showAddStation, setShowAddStation] = useState(false);

  const loadAll = useCallback(async () => {
    try {
      const [stationsResult, measurementsResult, alertsResult] = await Promise.all([
        getStations(),
        getRecentMeasurements(),
        getActiveAlerts(),
      ]);
      setStations(stationsResult);
      setMeasurements(measurementsResult);
      setAlerts(alertsResult);
      setError(null);
      setLastUpdated(new Date());
    } catch (err) {
      const message = err instanceof ApiError ? err.message : 'Unexpected error loading dashboard data.';
      setError(message);
    }
  }, []);

  useEffect(() => {
    loadAll();
    const interval = setInterval(loadAll, POLL_INTERVAL_MS);
    return () => clearInterval(interval);
  }, [loadAll]);

  const latestByStation = useMemo(() => {
    const map = new Map<string, Measurement>();
    (measurements ?? []).forEach((m) => {
      const existing = map.get(m.stationId);
      if (!existing || new Date(m.observedAt) > new Date(existing.observedAt)) {
        map.set(m.stationId, m);
      }
    });
    return map;
  }, [measurements]);

  const stationName = useCallback(
    (stationId: string) => stations?.find((s) => s.id === stationId)?.name ?? stationId,
    [stations],
  );

  const handleScenario = async (name: ScenarioName) => {
    setScenarioBusy(name);
    setScenarioMessage(null);
    try {
      const message = await triggerScenario(name);
      setScenarioMessage(typeof message === 'string' ? message : 'Scenario started');
      await loadAll();
    } catch (err) {
      setScenarioMessage(err instanceof ApiError ? err.message : 'Failed to start scenario.');
    } finally {
      setScenarioBusy(null);
    }
  };

  const handleAlertAction = async (id: string, action: 'acknowledge' | 'resolve') => {
    setAlertActionBusyId(id);
    try {
      if (action === 'acknowledge') {
        await acknowledgeAlert(id);
      } else {
        await resolveAlert(id);
      }
      await loadAll();
    } catch (err) {
      setError(err instanceof ApiError ? err.message : 'Failed to update alert.');
    } finally {
      setAlertActionBusyId(null);
    }
  };

  const isLoading = stations === null && measurements === null && alerts === null && !error;

  return (
    <div className="app">
      <header className="app-header">
        <div>
          <h1>JalRakshak</h1>
          <p className="subtitle">River-level monitoring &amp; alerting — prototype, simulated data only</p>
        </div>
        <div className="header-meta">
          {lastUpdated && <span className="last-updated">Updated {lastUpdated.toLocaleTimeString()}</span>}
          <button className="btn btn-secondary" onClick={loadAll}>Refresh</button>
        </div>
      </header>

      {error && (
        <div className="banner banner-error">
          <strong>Could not load dashboard data:</strong> {error}
        </div>
      )}

      {isLoading && <div className="banner banner-info">Loading dashboard…</div>}

      <section className="panel">
        <div className="panel-header">
          <h2>Demo scenarios</h2>
          <span className="demo-tag">DEMO DATA — not real sensor readings</span>
        </div>
        <div className="scenario-buttons">
          {SCENARIOS.map((scenario) => (
            <button
              key={scenario.name}
              className="btn btn-scenario"
              disabled={scenarioBusy !== null}
              onClick={() => handleScenario(scenario.name)}
            >
              {scenarioBusy === scenario.name ? 'Starting…' : scenario.label}
            </button>
          ))}
        </div>
        {scenarioMessage && <p className="scenario-message">{scenarioMessage}</p>}
      </section>

      <div className="grid">
        <section className="panel">
          <div className="panel-header">
            <h2>Stations {stations ? `(${stations.length})` : ''}</h2>
            <button className="btn btn-secondary" onClick={() => setShowAddStation((v) => !v)}>
              {showAddStation ? 'Cancel' : '+ Add station'}
            </button>
          </div>

          {showAddStation && (
            <AddStationForm
              onCreated={async () => {
                setShowAddStation(false);
                await loadAll();
              }}
            />
          )}

          {stations && stations.length === 0 && (
            <p className="empty-state">No stations registered yet. Add one above.</p>
          )}

          {stations && stations.length > 0 && (
            <ul className="station-list">
              {stations.map((station) => {
                const latest = latestByStation.get(station.id);
                return (
                  <li key={station.id} className="station-card">
                    <div className="station-card-top">
                      <strong>{station.name}</strong>
                      <span className={statusBadgeClass(station.status)}>{station.status}</span>
                    </div>
                    <div className="station-meta">
                      {station.stationCode}{station.riverName ? ` · ${station.riverName}` : ''}
                    </div>
                    <div className="station-reading">
                      {latest ? (
                        <>
                          <span>{latest.waterLevelM != null ? latest.waterLevelM.toFixed(2) : '—'} m water level</span>
                          <span>{latest.rainfallMmPerHour != null ? latest.rainfallMmPerHour.toFixed(1) : '—'} mm/h rainfall</span>
                          <span className="station-reading-time">as of {formatTime(latest.observedAt)}</span>
                        </>
                      ) : (
                        <span className="empty-state">No readings yet</span>
                      )}
                    </div>
                  </li>
                );
              })}
            </ul>
          )}
        </section>

        <section className="panel">
          <h2>Active alerts {alerts ? `(${alerts.length})` : ''}</h2>
          {alerts && alerts.length === 0 && <p className="empty-state">No active alerts. All quiet.</p>}
          {alerts && alerts.length > 0 && (
            <ul className="alert-list">
              {alerts.map((alert) => (
                <li key={alert.id} className="alert-card">
                  <div className="alert-card-top">
                    <span className={severityBadgeClass(alert.severity)}>{alert.severity}</span>
                    <span className={statusBadgeClass(alert.status)}>{alert.status}</span>
                    <span className="alert-rule">{alert.ruleCode}</span>
                  </div>
                  <div className="alert-title">{alert.title}</div>
                  <div className="alert-station">{stationName(alert.stationId)}</div>
                  {alert.reason && <div className="alert-reason">{alert.reason}</div>}
                  <div className="alert-meta">
                    Triggered {formatTime(alert.triggeredAt)}
                    {alert.repeatCount && alert.repeatCount > 1 ? ` · seen ${alert.repeatCount} times` : ''}
                    {alert.lastOccurredAt ? ` · last at ${formatTime(alert.lastOccurredAt)}` : ''}
                  </div>
                  {alert.status === 'ACTIVE' && (
                    <div className="alert-actions">
                      <button
                        className="btn btn-secondary"
                        disabled={alertActionBusyId === alert.id}
                        onClick={() => handleAlertAction(alert.id, 'acknowledge')}
                      >
                        Acknowledge
                      </button>
                      <button
                        className="btn btn-secondary"
                        disabled={alertActionBusyId === alert.id}
                        onClick={() => handleAlertAction(alert.id, 'resolve')}
                      >
                        Resolve
                      </button>
                    </div>
                  )}
                  {alert.status === 'ACKNOWLEDGED' && (
                    <div className="alert-actions">
                      <button
                        className="btn btn-secondary"
                        disabled={alertActionBusyId === alert.id}
                        onClick={() => handleAlertAction(alert.id, 'resolve')}
                      >
                        Resolve
                      </button>
                    </div>
                  )}
                </li>
              ))}
            </ul>
          )}
        </section>

        <section className="panel panel-wide">
          <h2>Recent measurements {measurements ? `(${measurements.length})` : ''}</h2>
          {measurements && measurements.length === 0 && (
            <p className="empty-state">No measurements yet. Run a demo scenario to generate some.</p>
          )}
          {measurements && measurements.length > 0 && (
            <div className="table-scroll">
              <table className="measurement-table">
                <thead>
                  <tr>
                    <th>Station</th>
                    <th>Observed at</th>
                    <th>Water level (m)</th>
                    <th>Rainfall (mm/h)</th>
                    <th>Flow rate (m³/s)</th>
                    <th>Event ID</th>
                  </tr>
                </thead>
                <tbody>
                  {measurements.slice(0, 30).map((m) => (
                    <tr key={m.eventId}>
                      <td>{stationName(m.stationId)}</td>
                      <td>{formatTime(m.observedAt)}</td>
                      <td>{m.waterLevelM != null ? m.waterLevelM.toFixed(2) : '—'}</td>
                      <td>{m.rainfallMmPerHour != null ? m.rainfallMmPerHour.toFixed(1) : '—'}</td>
                      <td>{m.flowRateM3S != null ? m.flowRateM3S.toFixed(1) : '—'}</td>
                      <td className="event-id">{m.eventId}</td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          )}
        </section>
      </div>

      <footer className="app-footer">
        Prototype using simulated data and illustrative thresholds — not a certified flood-warning system.
      </footer>
    </div>
  );
};

const AddStationForm: React.FC<{ onCreated: () => void }> = ({ onCreated }) => {
  const [stationCode, setStationCode] = useState('');
  const [name, setName] = useState('');
  const [riverName, setRiverName] = useState('');
  const [latitude, setLatitude] = useState('');
  const [longitude, setLongitude] = useState('');
  const [submitting, setSubmitting] = useState(false);
  const [formError, setFormError] = useState<string | null>(null);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setFormError(null);

    if (!stationCode.trim() || !name.trim()) {
      setFormError('Station code and name are required.');
      return;
    }

    setSubmitting(true);
    try {
      await createStation({
        stationCode: stationCode.trim(),
        name: name.trim(),
        riverName: riverName.trim() || undefined,
        latitude: latitude ? Number(latitude) : undefined,
        longitude: longitude ? Number(longitude) : undefined,
      });
      onCreated();
    } catch (err) {
      setFormError(err instanceof ApiError ? err.message : 'Failed to create station.');
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <form className="add-station-form" onSubmit={handleSubmit}>
      {formError && <div className="banner banner-error">{formError}</div>}
      <div className="form-row">
        <label>
          Station code *
          <input value={stationCode} onChange={(e) => setStationCode(e.target.value)} placeholder="station-003" />
        </label>
        <label>
          Name *
          <input value={name} onChange={(e) => setName(e.target.value)} placeholder="East River Station" />
        </label>
      </div>
      <div className="form-row">
        <label>
          River name
          <input value={riverName} onChange={(e) => setRiverName(e.target.value)} placeholder="Eastern River" />
        </label>
        <label>
          Latitude
          <input value={latitude} onChange={(e) => setLatitude(e.target.value)} placeholder="40.71" type="number" step="any" />
        </label>
        <label>
          Longitude
          <input value={longitude} onChange={(e) => setLongitude(e.target.value)} placeholder="-74.00" type="number" step="any" />
        </label>
      </div>
      <button className="btn btn-primary" type="submit" disabled={submitting}>
        {submitting ? 'Creating…' : 'Create station'}
      </button>
    </form>
  );
};

export default App;
