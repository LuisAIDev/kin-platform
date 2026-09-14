-- Flag para cuentas fundador/VIP: sin limite de triajes ni paywall de exportacion
ALTER TABLE users ADD COLUMN IF NOT EXISTS unlimited_access BOOLEAN NOT NULL DEFAULT FALSE;

-- Indice parcial para busquedas rapidas (pocos usuarios tendran el flag)
CREATE INDEX IF NOT EXISTS idx_users_unlimited_access
  ON users(unlimited_access)
  WHERE unlimited_access = TRUE;

-- Otorgar al owner
UPDATE users SET unlimited_access = TRUE WHERE email = 'luisgue.11@hotmail.com';