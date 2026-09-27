-- Adds repeat-occurrence tracking to alerts, used by the alert-suppression
-- policy (see stream-processor docs, "Repeated alert suppression"):
-- repeated triggers of the same station+rule condition while suppressed
-- update the existing alert row's evidence/last-seen/repeat-count instead
-- of creating a new row or being silently dropped.

ALTER TABLE alerts
    ADD COLUMN last_occurred_at TIMESTAMPTZ,
    ADD COLUMN repeat_count INTEGER NOT NULL DEFAULT 1;

UPDATE alerts SET last_occurred_at = triggered_at WHERE last_occurred_at IS NULL;
