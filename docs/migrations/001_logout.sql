-- Ejecutar una vez sobre una base existente ANTES de desplegar el servicio logout.
-- Solo agrega el almacenamiento de revocaciones; no cambia ni borra datos existentes.
BEGIN;
CREATE TABLE IF NOT EXISTS tokens_revocados (
    token_hash VARCHAR(64) PRIMARY KEY,
    expires_at TIMESTAMP WITH TIME ZONE NOT NULL
);
CREATE INDEX IF NOT EXISTS ix_tokens_revocados_expiracion ON tokens_revocados(expires_at);
COMMIT;
