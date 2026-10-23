# Microservicios y gateway

El backend se divide en dos bounded contexts (DDD), cada uno como un servicio Quarkus con su
propia arquitectura hexagonal, detras de **Traefik** como API Gateway. No hay Eureka: el
descubrimiento lo resuelve la red de Docker (cada servicio se alcanza por su nombre) y Traefik
detecta los contenedores por sus *labels*.

```text
                      Internet (HTTPS)
                             │
                    ┌────────▼────────┐  80/443 (unico puerto publicado)
                    │     Traefik     │  CORS · borra X-User-Id/X-Organizador-Id/X-Roles
                    └──┬──────────┬───┘
     /api/auth/**      │          │  /api/events/** /api/subtasks/** /api/capacity /api/today
     /api/admin/**     │          │  1) forwardAuth ──► identity /api/auth/verify
                       │          │  2) reenvia con los headers de identidad
              ┌────────▼───┐   ┌──▼───────────────┐
              │  identity  │──►│     planning     │   red privada "backend", HTTP plano
              │  service   │   │     service      │   (identity → planning solo para
              └─────┬──────┘   └────────┬─────────┘    GET /internal/organizadores/{id}/datos)
                    │                   │
                    └──────► Postgres ◄─┘   (cada servicio mapea solo sus tablas)
```

## Por que dos servicios y no mas

| Servicio | Agregados | Responsabilidad |
|---|---|---|
| `identity-service` | `Usuario` (con `Rol` y perfil `Organizador`) | Quien eres, que roles tienes, si tu cuenta esta activa. Unico que emite y valida JWT. |
| `planning-service` | `Evento` + `Subtarea`, `CapacidadDiaria` | Planificar el trabajo de cada organizador y proteger su limite diario de horas. |

La regla central del negocio —*la suma de horas no DONE de un organizador en un dia no puede
superar su limite*— lee subtareas y capacidad en la misma transaccion (crear evento con
subtareas, crear/editar subtarea, previsualizar sobrecarga). Separar eventos, subtareas o
capacidad en servicios distintos convertiria esa validacion en llamadas remotas con
condiciones de carrera. "Hoy" y "Progreso" son consultas sobre subtareas, no contextos propios.

Entre contextos solo viaja el **id del organizador** (`organizador_id`): planning no tiene
relaciones JPA hacia usuarios ni organizadores.

## Contratos entre servicios

### 1. forwardAuth (Traefik → identity)

Para cada peticion a planning, Traefik llama `GET /api/auth/verify` en identity con el mismo
`Authorization`. identity valida firma, expiracion, revocacion (logout) y que la cuenta siga
activa, consultando la BD:

| Respuesta de identity | Efecto |
|---|---|
| `401` + JSON de error | Traefik devuelve ese 401 al cliente; planning no se entera. |
| `204` + `X-User-Id`, `X-Roles`, `X-Organizador-Id` (si tiene perfil) | Traefik copia esos headers a la peticion y la reenvia a planning. |

planning construye la identidad con esos headers (`GatewayHeadersAuthenticationMechanism`):
exige rol `ORGANIZADOR` en sus rutas (403 si no) y usa `X-Organizador-Id` como dueno de los datos.

**Por que no se pueden falsificar:** el middleware `strip-identity` borra esos tres headers de
toda peticion entrante antes del forwardAuth, y planning no publica puertos: solo Traefik lo
alcanza.

### 2. Consulta interna (identity → planning)

Antes de borrar una cuenta, identity pregunta a planning si el organizador tiene datos:

```
GET http://planning-service:8080/internal/organizadores/{organizadorId}/datos
→ {"tieneDatos": true|false}
```

`/internal` no tiene router en Traefik, asi que no es accesible desde Internet. Si planning no
responde, identity devuelve `503` y **no** borra la cuenta.

## Base de datos

Ambos servicios usan la misma base Postgres con el esquema de [schema.sql](schema.sql), pero
cada uno mapea solo sus tablas:

| Servicio | Tablas |
|---|---|
| identity | `usuarios`, `roles`, `usuario_roles`, `organizadores`, `tokens_revocados` |
| planning | `eventos`, `subtareas`, `capacidades_diarias` |

No hace falta migrar datos. Las FK `eventos.organizador_id` y `capacidades_diarias.organizador_id`
hacia `organizadores` siguen en la base como red de seguridad. Si mas adelante se separan las
bases (una por servicio), esas FK desaparecen y la consulta interna de arriba pasa a ser la
unica garantia al borrar usuarios.

## Despliegue en el VPS

Pensado para Ubuntu 24.04 con 1 GB de RAM o mas (cada servicio usa unos 150 MB en JVM).

1. **DNS:** crea un registro `A` de tu dominio de API (por ejemplo `api.tudominio.com`) apuntando
   a la IP del VPS.
2. **Docker:**
   ```bash
   curl -fsSL https://get.docker.com | sh
   sudo usermod -aG docker $USER   # cierra sesion y vuelve a entrar
   ```
3. **Firewall:** abre solo SSH, HTTP y HTTPS.
   ```bash
   sudo ufw allow OpenSSH && sudo ufw allow 80/tcp && sudo ufw allow 443/tcp && sudo ufw enable
   ```
4. **Codigo y variables:**
   ```bash
   git clone <repo> && cd backend-quarkus
   cp .env.example .env
   ```
   Completa en `.env`: `API_DOMAIN=api.tudominio.com`, `ACME_EMAIL`, `CORS_ORIGIN` con la URL del
   frontend, credenciales `DB_*` y un `JWT_SECRET` largo (`openssl rand -base64 48`).
   Si quieres que los tokens emitidos en Render sigan siendo validos, usa el mismo `JWT_SECRET`.
5. **Levantar:**
   ```bash
   docker compose -f compose.yaml -f compose.prod.yaml up -d --build
   docker compose ps          # los tres deben quedar "running"/"healthy"
   ```
6. **Comprobar:**
   ```bash
   curl -i https://api.tudominio.com/api/events          # 401 JSON: el gateway exige sesion
   curl -i -X POST https://api.tudominio.com/api/auth/login \
        -H 'Content-Type: application/json' -d '{"correo":"...","password":"..."}'
   ```
   Swagger: `https://api.tudominio.com/identity/swagger-ui` y `/planning/swagger-ui`.
7. **Frontend:** cambia la URL base de la API (hoy `https://backend-quarkus-aor3.onrender.com`)
   por `https://api.tudominio.com`. Las rutas `/api/...` no cambian.

Operacion diaria:

```bash
git pull && docker compose -f compose.yaml -f compose.prod.yaml up -d --build   # actualizar
docker compose logs -f traefik planning-service                                 # logs
docker compose up -d --scale planning-service=2                                 # escalar
```

Traefik usa `traefik:v3.6`: las versiones anteriores a 3.6 no pueden hablar con Docker
Engine 29 o posterior (la version que instala hoy `get.docker.com`).

## Que se verifico

- `./mvnw verify`: tests unitarios, de integracion con H2 y reglas de ArchUnit de ambos servicios.
- Prueba de punta a punta sin Docker: Postgres 16 real con `schema.sql`, ambos JAR y el binario
  de Traefik 3.6.1 con una configuracion en archivo equivalente a las *labels* de
  `compose.yaml`: login, 401 sin token, aislamiento entre organizadores, intento de suplantar
  `X-Organizador-Id` (rechazado), 409 de capacidad, preflight CORS, `/internal` no expuesto,
  borrado de cuenta con y sin datos, y logout revocando el acceso a planning.
- `compose.yaml` en si (labels de Docker, healthchecks, red) **no** se ejecuto: el equipo donde
  se hizo la migracion no tiene Docker. Es lo primero a comprobar en el VPS (paso 5).
