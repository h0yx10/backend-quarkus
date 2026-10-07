# Subtareas

Plan de trabajo logistico, reprogramacion y ejecucion (US-02, US-03, US-06, US-09).

---

## POST /api/events/{eventId}/subtasks

**Descripcion:** crea una subtarea logistica para un evento (US-02).

**Path params**

| Nombre | Tipo |
|---|---|
| `eventId` | UUID |

**Request body** (`CreateSubtareaRequest`)

| Campo | Tipo | Requerido | Validacion |
|---|---|---|---|
| `name` | string | si | no vacio |
| `description` | string | no | maximo 255 caracteres; admite null |
| `targetDate` | date ISO `yyyy-MM-dd` | si | - |
| `estimatedHours` | number (decimal) | si | > 0 |

Los nombres públicos de los campos son en inglés, igual que las respuestas:

```json
{
  "name": "Confirmar catering",
  "description": "Confirmar menu vegetariano con el proveedor",
  "targetDate": "2026-10-10",
  "estimatedHours": 2
}
```

La descripción se guarda en `subtareas.descripcion`. Omitirla o enviarla como null conserva
la compatibilidad con los clientes anteriores. La columna ya está en `docs/schema.sql`.

**Response 201** - `data`: `SubtareaResponse`.

```json
{
  "success": true,
  "message": "La subtarea se creo correctamente.",
  "data": {
    "id": "2b8639f5-d7d4-4805-a1c5-4fb72b018084",
    "eventId": "6f1c1f5e-7d0a-4c55-9a43-2b9f0f1e9c11",
    "name": "Confirmar catering",
    "description": "Confirmar menu vegetariano con el proveedor",
    "targetDate": "2026-10-10",
    "estimatedHours": 2,
    "status": "PENDING",
    "note": null,
    "doneAt": null,
    "createdAt": "2026-10-06T10:15:30"
  },
  "timestamp": "2026-10-06T15:15:30Z"
}
```

Todos los campos de la respuesta son en inglés. Sin descripción,
`data.description` es null. Más de 255 caracteres responde 400 con message
`La descripcion puede tener maximo 255 caracteres.`.

**Response 400** si falla alguna validacion. **Response 404** si el evento no existe.

---

## GET /api/events/{eventId}/subtasks

**Descripcion:** lista las subtareas logisticas de un evento.

**Path params**

| Nombre | Tipo |
|---|---|
| `eventId` | UUID |

**Response 200** - `data`: array de `SubtareaResponse`.

---

## PATCH /api/subtasks/{id}

**Descripcion:** actualiza los campos enviados de una subtarea. Enviar `targetDate` y/o
`estimatedHours` reprograma la subtarea (US-06) y valida sobrecarga diaria contra el limite
configurado en [capacidad.md](./capacidad.md) (US-07); si se supera, la operacion se rechaza con
**409** y no se guarda el cambio.

**Path params**

| Nombre | Tipo |
|---|---|
| `id` | UUID de la subtarea |

**Request body** (`UpdateSubtareaRequest`, todos los campos opcionales)

| Campo | Tipo | Validacion |
|---|---|---|
| `name` | string | - |
| `description` | string | maximo 255; omitido/null conserva el valor |
| `targetDate` | date ISO `yyyy-MM-dd` | - |
| `estimatedHours` | number (decimal) | > 0 si se envia |

Ejemplo para editar solamente la descripción:

```json
{
  "description": "Confirmar menu y horario actualizado con el proveedor"
}
```

Omitir description o enviar null conserva el valor anterior. Enviar "" lo vacía. Solo cambiar
la descripción no recalcula la sobrecarga; si también cambian fecha/horas, se mantiene la
validación habitual y un 409 no guarda ningún campo. Más de 255 caracteres devuelve 400 con
message `La descripcion puede tener maximo 255 caracteres.`.

**Response 200** - `data`: `SubtareaResponse`, con description actualizado.

**Response 409** (contrato especial de conflicto, ver [README.md](./README.md#sobre-de-respuesta-estandar))
si la reprogramacion supera el limite diario. **Response 404** si la subtarea no existe.

---

## PATCH /api/subtasks/{id}/status

**Descripcion:** marca una subtarea como hecha o pospuesta, con nota opcional (US-09).

**Path params**

| Nombre | Tipo |
|---|---|
| `id` | UUID de la subtarea |

**Request body** (`ChangeSubtareaStatusRequest`)

| Campo | Tipo | Requerido | Notas |
|---|---|---|---|
| `status` | enum: `PENDING`, `DONE`, `POSTPONED` | si | - |
| `note` | string | no | usada sobre todo al posponer |

**Response 200** - `data`: `SubtareaResponse`.

**Response 404** si la subtarea no existe.

---

## DELETE /api/subtasks/{id}

**Descripcion:** elimina una subtarea logistica (US-03).

**Path params**

| Nombre | Tipo |
|---|---|
| `id` | UUID de la subtarea |

**Response 200** - `data: null`.

**Response 404** si la subtarea no existe.

---

## Contrato `SubtareaResponse`

Usado como `data` (o como elemento de un array) en las respuestas de crear, listar, actualizar y
cambiar estado de subtarea, y dentro de `EventoResponse.subtareas` y la vista Hoy.
description se guarda tanto al crear como al editar. PATCH conserva el valor si se omite
o se envía null; enviar "" lo vacía.

| Campo | Tipo | Descripcion |
|---|---|---|
| `id` | UUID | identificador de la subtarea |
| `eventId` | UUID | evento al que pertenece |
| `name` | string | |
| `description` | string \| null | descripcion guardada, maximo 255 caracteres |
| `targetDate` | date ISO `yyyy-MM-dd` | fecha planeada de ejecucion |
| `estimatedHours` | number (decimal) | |
| `status` | enum: `PENDING`, `DONE`, `POSTPONED` | |
| `note` | string \| null | |
| `doneAt` | datetime ISO \| null | momento en que se marco `DONE` |
| `createdAt` | datetime ISO | momento de creacion |

## Compatibilidad de entrada

Las respuestas y Swagger usan exclusivamente campos en inglés. Para clientes anteriores,
las entradas todavía admiten los alias nombre→name, descripcion→description,
fechaObjetivo→targetDate, horasEstimadas→estimatedHours, estado→status y nota→note.
Usar siempre los campos en inglés en integraciones nuevas y no mezclar alias del mismo campo.
Los mensajes de éxito/error siguen siendo los textos en español del backend.

Ejemplo de cambio de estado:

```json
{
  "status": "POSTPONED",
  "note": "Esperando confirmacion del proveedor"
}
```
