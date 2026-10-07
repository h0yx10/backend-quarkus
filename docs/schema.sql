-- events-api: esquema final para una BASE NUEVA PostgreSQL/Supabase.
-- No migra ni borra tablas existentes. Ejecutar antes de arrancar con ddl-auto=validate.
BEGIN;
CREATE TABLE usuarios (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    nombre VARCHAR(120) NOT NULL,
    correo VARCHAR(180) NOT NULL UNIQUE,
    password_hash VARCHAR(100) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT now()
);
CREATE UNIQUE INDEX ux_usuarios_correo_lower ON usuarios (lower(correo));
CREATE TABLE roles (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    nombre VARCHAR(30) NOT NULL UNIQUE,
    CONSTRAINT ck_roles_nombre CHECK (nombre IN ('ORGANIZADOR', 'ADMIN'))
);
INSERT INTO roles (nombre) VALUES ('ORGANIZADOR'), ('ADMIN');
CREATE TABLE usuario_roles (
    usuario_id UUID NOT NULL REFERENCES usuarios(id) ON DELETE CASCADE,
    rol_id UUID NOT NULL REFERENCES roles(id) ON DELETE RESTRICT,
    PRIMARY KEY (usuario_id, rol_id)
);
CREATE TABLE organizadores (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    usuario_id UUID NOT NULL UNIQUE REFERENCES usuarios(id) ON DELETE CASCADE,
    activo BOOLEAN NOT NULL DEFAULT TRUE
);
CREATE TABLE eventos (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    nombre VARCHAR(180) NOT NULL,
    tipo VARCHAR(100) NOT NULL,
    cliente VARCHAR(180),
    contacto_cliente VARCHAR(180),
    fecha_hora TIMESTAMP NOT NULL,
    lugar VARCHAR(240),
    plazo_limite TIMESTAMP,
    organizador_id UUID NOT NULL REFERENCES organizadores(id) ON DELETE RESTRICT
);
CREATE TABLE subtareas (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    nombre VARCHAR(180) NOT NULL,
    descripcion VARCHAR(255),
    fecha_objetivo DATE NOT NULL,
    horas_estimadas NUMERIC(8, 2) NOT NULL CHECK (horas_estimadas > 0),
    estado VARCHAR(20) NOT NULL CHECK (estado IN ('PENDING', 'DONE', 'POSTPONED')),
    nota VARCHAR(1000),
    done_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT now(),
    evento_id UUID NOT NULL REFERENCES eventos(id) ON DELETE CASCADE
);
CREATE TABLE capacidades_diarias (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    organizador_id UUID NOT NULL REFERENCES organizadores(id) ON DELETE RESTRICT,
    fecha DATE NOT NULL,
    limite_horas NUMERIC(8, 2) NOT NULL CHECK (limite_horas BETWEEN 1 AND 16),
    CONSTRAINT uk_capacidad_organizador_fecha UNIQUE (organizador_id, fecha)
);
CREATE INDEX ix_eventos_organizador ON eventos(organizador_id);
CREATE INDEX ix_subtareas_evento ON subtareas(evento_id);
CREATE INDEX ix_subtareas_evento_fecha_estado ON subtareas(evento_id, fecha_objetivo, estado);
CREATE INDEX ix_capacidades_organizador_fecha ON capacidades_diarias(organizador_id, fecha DESC);
CREATE TABLE tokens_revocados (
    token_hash VARCHAR(64) PRIMARY KEY,
    expires_at TIMESTAMP WITH TIME ZONE NOT NULL
);
CREATE INDEX ix_tokens_revocados_expiracion ON tokens_revocados(expires_at);
COMMIT;

-- Primer ADMIN: registrar primero una cuenta por POST /api/auth/register.
-- Ejecutar manualmente este bloque reemplazando el correo y volver a iniciar sesion:
-- INSERT INTO usuario_roles (usuario_id, rol_id)
-- SELECT u.id, r.id FROM usuarios u CROSS JOIN roles r
-- WHERE lower(u.correo) = lower('admin@dominio.com') AND r.nombre = 'ADMIN'
-- ON CONFLICT DO NOTHING;
