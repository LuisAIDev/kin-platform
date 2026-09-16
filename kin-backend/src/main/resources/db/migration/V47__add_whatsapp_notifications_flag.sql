-- Opt-in del médico para recibir avisos por WhatsApp
-- Reutiliza el campo phone existente como número de WhatsApp
ALTER TABLE users ADD COLUMN IF NOT EXISTS whatsapp_notifications_enabled BOOLEAN DEFAULT FALSE NOT NULL;

CREATE INDEX IF NOT EXISTS idx_users_whatsapp_notifications 
  ON users(whatsapp_notifications_enabled) 
  WHERE whatsapp_notifications_enabled = TRUE;