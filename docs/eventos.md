# Eventos

Base path: `/api/events`

Gestion de eventos y su plan de trabajo logistico (US-01, US-03).

---

## GET /api/events

**Descripcion:** lista los eventos del usuario autenticado.

**Request:** sin parametros.

**Response 200** - `data`: array de `EventoResponse` (ver contrato abajo).

---

## GET /api/events/{id}

**Descripcion:** consulta un evento con sus subtareas por su identificador.

**Path params**

| Nombre | Tipo | Descripcion |
|---|---|---|
| `id` | UUID | identificador del evento |

**Response 200** - `data`: `EventoResponse`.

**Response 404** si el evento no existe.

---

## POST /api/events

**Descripcion:** crea un evento y, opcionalmente, su plan inicial de subtareas (US-01, US-02).

**Request body** (`CreateEventoRequest`)

| Campo | Tipo | Requerido | Validacion |
|---|---|---|---|
| `nombre` | string | si | no vacio, max 180 caracteres |
| `tipo` | string | si | no vacio |
| `cliente` | string | no | - |
| `contactoCliente` | string | no | - |
| `fechaHora` | datetime ISO `yyyy-MM-dd'T'HH:mm:ss` | si | - |
| `lugar` | string | no | - |
| `plazoLimite` | datetime ISO `yyyy-MM-dd'T'HH:mm:ss` | no | - |
| `subtareas` | array de `SubtareaInicialRequest` | no | ver abajo |

`SubtareaInicialRequest` (elementos del array `subtareas`):

| Campo | Tipo | Requerido | Validacion |
|---|---|---|---|
| `name` | string | si | no vacio |
| `description` | string | no | maximo 255 caracteres |
| `targetDate` | date ISO `yyyy-MM-dd` | si | - |
| `estimatedHours` | number (decimal) | si | > 0 |

**Response 201** - `data`: `EventoResponse`.

**Response 400** si falla alguna validacion.

---

## PATCH /api/events/{id}

**Descripcion:** actualiza unicamente los campos enviados de un evento existente (US-03). Los campos
omitidos o `null` no se modifican.

**Path params**

| Nombre | Tipo |
|---|---|
| `id` | UUID |

**Request body** (`UpdateEventoRequest`, todos los campos opcionales)

| Campo | Tipo |
|---|---|
| `nombre` | string |
| `tipo` | string |
| `cliente` | string |
| `contactoCliente` | string |
| `fechaHora` | datetime ISO |
| `lugar` | string |
| `plazoLimite` | datetime ISO |

**Response 200** - `data`: `EventoResponse`.

**Response 404** si el evento no existe.

---

## DELETE /api/events/{id}

**Descripcion:** elimina un evento y sus subtareas asociadas (US-03).

**Path params**

| Nombre | Tipo |
|---|---|
| `id` | UUID |

**Response 200** - `data: null`.

**Response 404** si el evento no existe.

---

## GET /api/events/{id}/progress

**Descripcion:** devuelve el avance de preparacion del evento (US-10): subtareas completadas vs. total.

**Path params**

| Nombre | Tipo |
|---|---|
| `id` | UUID |

**Response 200** - `data`: `ProgressResponse`.

| Campo | Tipo | Descripcion |
|---|---|---|
| `done` | integer | subtareas en estado `DONE` |
| `total` | integer | total de subtareas del evento |
| `percentage` | number (decimal) | `done / total * 100` |

**Response 404** si el evento no existe.

---

## Contrato `EventoResponse`

Usado como `data` en las respuestas de crear, consultar, listar y actualizar evento.

| Campo | Tipo | Descripcion |
|---|---|---|
| `id` | UUID | identificador del evento |
| `nombre` | string | |
| `tipo` | string | |
| `cliente` | string \| null | |
| `contactoCliente` | string \| null | |
| `fechaHora` | datetime ISO | |
| `lugar` | string \| null | |
| `plazoLimite` | datetime ISO \| null | |
| `organizadorId` | UUID | organizador propietario del evento |
| `subtareas` | array de `SubtareaResponse` | ver contrato en [subtareas.md](./subtareas.md) |
