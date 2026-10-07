-- Ejecutar en el SQL Editor de la base de datos Supabase usada por el backend.
-- Alinea el esquema existente con el campo opcional documentado en docs/eventos.md.
BEGIN;

ALTER TABLE public.eventos
    ALTER COLUMN cliente DROP NOT NULL;

COMMIT;

-- Debe devolver is_nullable = YES.
SELECT table_schema, table_name, column_name, is_nullable
FROM information_schema.columns
WHERE table_schema = 'public'
  AND table_name = 'eventos'
  AND column_name = 'cliente';
