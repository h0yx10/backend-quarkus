# Conflictos

Deteccion de sobrecarga diaria antes de confirmar una reprogramacion (US-07, US-08).

---

## POST /api/subtasks/{id}/conflicts/overload

**Descripcion:** calcula si reprogramar la subtarea a la fecha/horas indicadas superaria el limite
diario configurado (ver [capacidad.md](./capacidad.md)), **sin guardar el cambio**. Pensado para que
el cliente muestre una advertencia antes de confirmar un `PATCH /api/subtasks/{id}`.

**Path params**

| Nombre | Tipo |
|---|---|
| `id` | UUID de la subtarea |

**Request body** (`OverloadCheckRequest`)

| Campo | Tipo |
|---|---|
| `targetDate` | date ISO `yyyy-MM-dd`; si se omite, usa la actual |
| `estimatedHours` | number (decimal) > 0; si se omite, usa las actuales |

El body es obligatorio; para previsualizar sin cambios envia `{}`.

**Response 200** - `data`: `OverloadCheckResponse`. Si la subtarea esta `DONE`, `conflict` es
`false`: una subtarea hecha no suma horas al dia.

**Response 400** si falta el body o `estimatedHours` es `<= 0`.

**Response 404** si la subtarea no existe.

---

## Contrato `OverloadCheckResponse`

| Campo | Tipo | Descripcion |
|---|---|---|
| `conflict` | boolean | `true` si la reprogramacion superaria el limite diario |
| `plannedHours` | number (decimal) | horas totales planeadas para ese dia si se confirma el cambio |
| `limitHours` | number (decimal) | limite diario configurado |
| `exceedsBy` | number (decimal) | horas que exceden el limite (`0` si `conflict` es `false`) |
