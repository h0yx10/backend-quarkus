# Documentacion de la API - events-api

Base URL local: `http://localhost:8080`

> **Autenticacion:** todas las rutas `/api/**` exigen `Authorization: Bearer <accessToken>`,
> excepto `POST /api/auth/register` y `POST /api/auth/login`. Ver [auth.md](./auth.md).
> Negocio exige ORGANIZADOR y perfil activo. Los permisos se consultan en BD en cada petición.
> Contrato completo para el cliente front: [contratos-frontend.md](./contratos-frontend.md).
> Arquitectura: [arquitectura.md](./arquitectura.md). Esquema SQL: [schema.sql](./schema.sql).

## Sobre de respuesta estandar

Toda respuesta exitosa se envuelve en `ApiResponse<T>`:

| Campo | Tipo | Descripcion |
|---|---|---|
| `success` | boolean | `true` |
| `message` | string | mensaje descriptivo de la operacion |
| `data` | `T` | el contrato propio de cada endpoint (ver cada recurso) |
| `timestamp` | string (Instant ISO-8601) | momento de la respuesta |

Toda respuesta de error usa este contrato:

| Campo | Tipo | Descripcion |
|---|---|---|
| `success` | boolean | `false` |
| `message` | string | mensaje del error |
| `timestamp` | string (Instant ISO-8601) | momento del error |

Errores segun el caso:

| Status | Cuando ocurre |
|---|---|
| `401 Unauthorized` | sin token, token invalido/expirado, o credenciales de login incorrectas |
| `403 Forbidden` | el token es valido pero el rol no alcanza (ej. `/api/admin/**` sin ADMIN) |
| `400 Bad Request` | body invalido / no cumple validaciones (`@NotBlank`, `@NotNull`, `@Size`, `@DecimalMin`) o JSON mal formado |
| `404 Not Found` | usuario, evento, subtarea u organizador no encontrado, **o perteneciente a otro usuario** |
| `409 Conflict` | correo ya registrado, baja con dependencias, último ADMIN habilitado, o la reprogramacion de una subtarea supera el limite diario de horas (ver contrato especial abajo) |
| `500 Internal Server Error` | error inesperado |

Contrato especial del **409** (sobrecarga de capacidad), agrega tres campos al error estandar:

| Campo | Tipo | Descripcion |
|---|---|---|
| `plannedHours` | number (decimal) | horas totales planeadas para ese dia si se confirma el cambio |
| `limitHours` | number (decimal) | limite diario configurado |
| `exceedsBy` | number (decimal) | horas que exceden el limite |

## Indice de recursos

| Archivo | Recurso | Historias de usuario |
|---|---|---|
| [auth.md](./auth.md) | Registro, login, usuario actual, admin | US-11 |
| [eventos.md](./eventos.md) | Eventos | US-01, US-02, US-03, US-10 |
| [subtareas.md](./subtareas.md) | Subtareas logisticas | US-02, US-03, US-06, US-07, US-09 |
| [capacidad.md](./capacidad.md) | Limite diario de horas | US-12 |
| [conflictos.md](./conflictos.md) | Previsualizacion de sobrecarga | US-07, US-08 |
| [hoy.md](./hoy.md) | Vista "Hoy" | US-04, US-05 |

## Tabla resumen de endpoints

| Metodo | Ruta | Descripcion |
|---|---|---|
| POST | `/api/auth/register` | Registrarse (publica) |
| POST | `/api/auth/login` | Iniciar sesion (publica) |
| POST | `/api/auth/logout` | Cerrar sesión y revocar el JWT actual |
| GET | `/api/auth/me` | Usuario autenticado |
| GET | `/api/admin/users` | Listar usuarios (solo ADMIN) |
| GET | `/api/admin/users/{id}` | Consultar usuario (ADMIN) |
| POST | `/api/admin/users` | Crear usuario y roles (ADMIN), sin token |
| PATCH | `/api/admin/users/{id}` | Actualizar usuario, roles y actividad (ADMIN) |
| DELETE | `/api/admin/users/{id}` | Eliminar usuario sin datos asociados (ADMIN) |
| PATCH | `/api/auth/me` | Editar perfil propio; password exige passwordActual |
| DELETE | `/api/auth/me` | Eliminar cuenta propia sin datos asociados |
| GET | `/api/events` | Listar eventos del organizador |
| GET | `/api/events/{id}` | Consultar un evento con sus subtareas |
| POST | `/api/events` | Crear un evento (con plan inicial de subtareas opcional) |
| PATCH | `/api/events/{id}` | Actualizar campos de un evento |
| DELETE | `/api/events/{id}` | Eliminar un evento y sus subtareas |
| GET | `/api/events/{id}/progress` | Progreso de preparacion del evento |
| POST | `/api/events/{eventId}/subtasks` | Crear subtarea de un evento |
| GET | `/api/events/{eventId}/subtasks` | Listar subtareas de un evento |
| PATCH | `/api/subtasks/{id}` | Editar / reprogramar una subtarea |
| PATCH | `/api/subtasks/{id}/status` | Cambiar estado de ejecucion de una subtarea |
| DELETE | `/api/subtasks/{id}` | Eliminar una subtarea |
| POST | `/api/subtasks/{id}/conflicts/overload` | Previsualizar sobrecarga diaria sin guardar |
| GET | `/api/capacity` | Consultar limite diario de horas |
| PUT | `/api/capacity` | Actualizar limite diario de horas |
| GET | `/api/today` | Vista de gestiones del dia (vencidas / hoy / proximas) |

## Documentacion interactiva (Swagger)

Con la app corriendo:
- Swagger UI: `http://localhost:8080/swagger-ui.html`
- OpenAPI JSON: `http://localhost:8080/api-docs`
