# Capacidad

Base path: `/api/capacity`

Configuracion del limite diario de horas del organizador (US-12). Este limite es el que usan
`PATCH /api/subtasks/{id}` (US-07) y `POST /api/subtasks/{id}/conflicts/overload` (US-08) para
detectar sobrecarga.

---

## GET /api/capacity

**Descripcion:** devuelve el limite configurado, o 6 horas por defecto si no existe configuracion.

**Request:** sin parametros.

**Response 200** - `data`: `CapacidadResponse`.

---

## PUT /api/capacity

**Descripcion:** actualiza el limite diario de horas.

**Request body** (`CapacidadRequest`)

| Campo | Tipo | Requerido | Validacion |
|---|---|---|---|
| `limiteHoras` | number (decimal) | si | rango permitido `1..16` |

**Response 200** - `data`: `CapacidadResponse`.

**Response 400** si el valor esta fuera de rango o falta.

**Response 409** si el nuevo limite es **menor** que el actual y algun dia, de hoy en adelante,
ya tiene mas horas planificadas (subtareas no `DONE`) que ese limite. No se guarda el cambio: el
organizador debe reprogramar o reducir esas gestiones primero. Subir el limite siempre se permite
y los dias pasados no cuentan.

```json
{
  "success": false,
  "code": "capacity_below_planned",
  "message": "No puedes bajar el limite a 5.5 h: hay dias con mas horas planificadas (18/10/2026: 6 h). Reprograma o reduce esas gestiones primero.",
  "limitHours": 5.5,
  "overloadedDays": [ { "date": "2026-10-18", "plannedHours": 6.00 } ],
  "timestamp": "2026-10-17T21:20:11.512Z"
}
```

---

## Contrato `CapacidadResponse`

| Campo | Tipo | Descripcion |
|---|---|---|
| `limiteHoras` | number (decimal) | limite diario vigente |
| `porDefecto` | boolean | `true` si no hay configuracion guardada y se devuelve el default (6h) |
| `fecha` | date ISO \| null | fecha desde la que aplica la configuracion guardada; `null` cuando `porDefecto` es `true` |
