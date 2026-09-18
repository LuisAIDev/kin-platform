-- ============================================================
-- V48: sala de videollamada Jitsi por cita (ADR-042)
-- ID impredecible (UUID) para que nadie fuera de los participantes
-- pueda unirse a la sala.
-- ============================================================

ALTER TABLE appointments ADD COLUMN IF NOT EXISTS video_room_id VARCHAR(100);
ALTER TABLE appointments ADD COLUMN IF NOT EXISTS video_room_created_at TIMESTAMPTZ;

CREATE INDEX IF NOT EXISTS idx_appointments_video_room
  ON appointments(video_room_id)
  WHERE video_room_id IS NOT NULL;
