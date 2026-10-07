# Hoy

Vista de gestiones urgentes del dia, con filtros (US-04, US-05).

---

## GET /api/today

**Descripcion:** devuelve las subtareas no `DONE`, agrupadas en Vencidas / Para hoy / Proximas,
con filtros opcionales por evento y estado.

**Query params**

| Nombre | Tipo | Requerido | Descripcion |
|---|---|---|---|
| `eventId` | UUID | no | filtra por evento |
| `status` | enum: `PENDING`, `DONE`, `POSTPONED` | no | filtra por estado |

**Response 200** - `data`: `TodayResponse`.

---

## Contrato `TodayResponse`

| Campo | Tipo | Descripcion |
|---|---|---|
| `vencidas` | array de `SubtareaResponse` | subtareas cuya `fechaObjetivo` ya paso |
| `paraHoy` | array de `SubtareaResponse` | subtareas cuya `fechaObjetivo` es hoy |
| `proximas` | array de `SubtareaResponse` | subtareas cuya `fechaObjetivo` es futura |
| `regla` | string | descripcion textual del criterio usado para clasificar cada grupo |

Ver el contrato de cada elemento de los arrays en [subtareas.md](./subtareas.md#contrato-subtarearesponse).
