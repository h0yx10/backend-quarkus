# Contratos API para el cliente front - events-api

Referencia completa para que el front se autentique y consuma la API.
Tipos exportados listos para copiar al frontend: [api-contracts.ts](./api-contracts.ts). Para el detalle por
recurso ver los demas archivos de `docs/`; Swagger en `/swagger-ui.html` (boton **Authorize**
para pegar el token).

- **Base URL:** `http://localhost:8080` (local) o la URL del backend desplegado.
- **Content-Type:** `application/json` en todos los requests con body.
- **Fechas:** `date` = `"yyyy-MM-dd"`; `datetime` = `"yyyy-MM-ddTHH:mm:ss"` **sin zona horaria**
  (hora local America/Bogota). `timestamp` del sobre de respuesta = Instant ISO-8601 en UTC.
- **IDs:** UUID en texto.
- **CORS:** origenes permitidos por `CORS_ORIGIN` (por defecto `http://localhost:4200` y
  `https://frontend-pi-olive-30.vercel.app`). El header `Authorization` esta permitido.

---

## 1. Flujo de autenticacion

1. El usuario se registra (`POST /api/auth/register`) o inicia sesion (`POST /api/auth/login`).
2. La respuesta trae `data.accessToken` y `data.expiresIn` (segundos). Guarda el token
   (ej. `localStorage`/`sessionStorage`) y calcula `expiresAt = Date.now() + expiresIn * 1000`.
3. En **cada** request a `/api/**` (excepto register/login) envia:
   ```
   Authorization: Bearer <accessToken>
   ```
4. Al recargar la app: si hay token no expirado, llama a `GET /api/auth/me` para restaurar el
   usuario. Si responde `401`, borra el token y manda a login.
5. Un `401` normalmente indica sesion invalida. Al cambiar la contraseña propia, también
   puede indicar `passwordActual` incorrecta: consulta `GET /api/auth/me`; si devuelve 200,
   conserva la sesion y muestra el error del formulario; si devuelve 401, borra el token
   y redirige a login. Un `403` indica falta de permisos y no debe cerrar la sesion.
6. Logout: llamar `POST /api/auth/logout` con el Bearer actual, sin body. Tras 200 o 401,
   borrar el token, expiresAt y el usuario local. Un fallo de red o 500 no confirma la
   revocación: mostrar el error y permitir reintentar. El token revocado devuelve 401;
   otros tokens de la cuenta permanecen válidos.
7. No hay refresh token: al expirar (por defecto 2 h) el usuario vuelve a iniciar sesion.

Cada usuario solo ve sus propios eventos/subtareas/capacidad. Pedir un recurso de otro
usuario devuelve `404`.

### Modelo de datos e identidad

- `usuarios`: cuenta de login; el `sub` del JWT y `UsuarioResponse.id` identifican esta tabla.
- `organizadores`: `id` independiente, `usuario_id` único como FK a usuarios y `activo`.
- `usuario_roles`: roles asociados directamente a usuarios.
- `UsuarioResponse.organizadorId` identifica el perfil o es null. `EventoResponse.organizadorId`
  identifica ese perfil, no la cuenta.
- El backend consulta roles y actividad en BD con cada petición; un cambio aplica incluso
  a JWT emitidos antes. Consultar `/me` para actualizar la información que muestra el cliente.
- Negocio exige ORGANIZADOR y perfil activo; ADMIN solo no obtiene acceso a eventos.
- PATCH rechaza propiedades desconocidas. Las bajas devuelven 409 si hay datos asociados
  o se intenta dejar el sistema sin ADMIN habilitado.

---

## 2. Sobre de respuesta

```ts
// Respuesta exitosa (2xx)
interface ApiResponse<T> {
  success: true;
  message: string;
  data: T;            // null en DELETE
  timestamp: string;  // ISO-8601 UTC
}

// Respuesta de error (4xx / 5xx)
interface ApiError {
  success: false;
  message: string;    // mensaje listo para mostrar al usuario
  timestamp: string;
}

// 409 por sobrecarga de capacidad (PATCH /api/subtasks/{id})
interface CapacityConflictError extends ApiError {
  plannedHours: number;
  limitHours: number;
  exceedsBy: number;
}
```

| Status | Significado | Que hacer en el front |
|---|---|---|
| 200 / 201 | OK | usar `data` |
| 400 | validacion o JSON mal formado | mostrar `message` en el formulario |
| 401 | token invalido/expirado, login fallido o contraseña actual incorrecta | login: mostrar error; cambio de contraseña: confirmar sesion con GET `/api/auth/me`; otras rutas: cerrar sesion |
| 403 | rol insuficiente | mostrar "sin permisos" |
| 404 | no existe o no es del usuario | mostrar "no encontrado" |
| 409 | correo ya registrado / sobrecarga / baja con dependencias / último ADMIN | mostrar `message` (y datos extra si es capacidad) |
| 500 | error inesperado | mensaje generico |

---

## 3. Tipos (TypeScript)

```ts
type UUID = string;
type DateISO = string;      // "2026-05-01"
type DateTimeISO = string;  // "2026-05-20T18:00:00"

type Rol = 'ORGANIZADOR' | 'ADMIN';
type EstadoSubtarea = 'PENDING' | 'DONE' | 'POSTPONED';

// ---------- Auth ----------
interface RegisterRequest {
  nombre: string;    // requerido, max 120
  correo: string;    // requerido, email valido, max 180
  password: string;  // requerido, mínimo 8 caracteres Unicode y máximo 72 bytes UTF-8
}

interface LoginRequest {
  correo: string;
  password: string;
}

// Cuenta y perfil tienen UUID independientes.
interface UsuarioResponse {
  id: UUID;            // usuarios.id = sub del JWT
  organizadorId: UUID | null; // organizadores.id, independiente del usuario
  nombre: string;
  correo: string;
  roles: Rol[];    // de usuario_roles
  activo: boolean;     // organizadores.activo; true si no tiene perfil
  createdAt: DateTimeISO;
}

interface AuthResponse {
  accessToken: string;
  tokenType: 'Bearer';
  expiresIn: number;   // segundos
  usuario: UsuarioResponse;
}

interface CreateUsuarioRequest extends RegisterRequest {
  roles?: Rol[]; // no vacío; por defecto ORGANIZADOR; solo POST administrativo
}
interface UpdatePerfilRequest {
  nombre?: string;
  correo?: string;
  password?: string;
  passwordActual?: string; // obligatoria al cambiar contraseña propia
}
interface UpdateUsuarioRequest extends UpdatePerfilRequest {
  roles?: Rol[]; // reemplaza la lista, solo ADMIN
  activo?: boolean; // solo ADMIN, requiere perfil
}

// ---------- Eventos ----------
interface SubtareaInicialRequest {
  description?: string;   // opcional, máximo 255 caracteres; alias de entrada: descripcion
  name: string;          // requerido
  targetDate: DateISO;  // requerido
  estimatedHours: number;  // requerido, > 0
}

interface CreateEventoRequest {
  nombre: string;              // requerido, max 180
  tipo: string;                // requerido
  cliente?: string;
  contactoCliente?: string;
  fechaHora: DateTimeISO;      // requerido
  lugar?: string;
  plazoLimite?: DateTimeISO;
  subtareas?: SubtareaInicialRequest[];
}

// PATCH: solo se aplican los campos enviados (no se puede "borrar" un campo enviando null)
interface UpdateEventoRequest {
  nombre?: string;
  tipo?: string;
  cliente?: string;
  contactoCliente?: string;
  fechaHora?: DateTimeISO;
  lugar?: string;
  plazoLimite?: DateTimeISO;
}

interface EventoResponse {
  id: UUID;
  nombre: string;
  tipo: string;
  cliente: string | null;
  contactoCliente: string | null;
  fechaHora: DateTimeISO;
  lugar: string | null;
  plazoLimite: DateTimeISO | null;
  organizadorId: UUID;         // perfil del usuario autenticado (= UsuarioResponse.organizadorId)
  subtareas: SubtareaResponse[];
}

interface ProgressResponse {
  done: number;
  total: number;
  percentage: number;          // 0..100
}

// ---------- Subtareas ----------
interface CreateSubtareaRequest {
  description?: string;   // opcional, máximo 255 caracteres; alias de entrada: descripcion
  name: string;              // requerido
  targetDate: DateISO;      // requerido
  estimatedHours: number;      // requerido, > 0
}

interface UpdateSubtareaRequest {   // PATCH parcial
  description?: string; // maximo 255; omitido/null conserva el valor; "" lo vacía
  name?: string;
  targetDate?: DateISO;          // si cambia targetDate o estimatedHours se valida sobrecarga (409)
  estimatedHours?: number;          // > 0
}

interface ChangeSubtareaStatusRequest {
  status: EstadoSubtarea;           // requerido
  note?: string;                    // opcional (tipicamente al posponer)
}

interface SubtareaResponse {
  id: UUID;
  eventId: UUID;
  description: string | null;
  name: string;
  targetDate: DateISO;
  estimatedHours: number;
  status: EstadoSubtarea;
  note: string | null;
  doneAt: DateTimeISO | null;
  createdAt: DateTimeISO;
}

// ---------- Conflictos ----------
interface OverloadCheckRequest {
  targetDate?: DateISO;    // si se omite, usa la actual de la subtarea
  estimatedHours?: number;    // si se omite, usa las actuales
}

interface OverloadCheckResponse {
  conflict: boolean;
  plannedHours: number;
  limitHours: number;
  exceedsBy: number;          // 0 si no hay conflicto
}

// ---------- Capacidad ----------
interface CapacidadRequest {
  limiteHoras: number;        // requerido, 1..16
}

interface CapacidadResponse {
  limiteHoras: number;
  porDefecto: boolean;        // true si nunca se configuro (6h)
  fecha: DateISO | null;
}

// ---------- Hoy ----------
interface TodayResponse {
  vencidas: SubtareaResponse[];
  paraHoy: SubtareaResponse[];
  proximas: SubtareaResponse[];
  regla: string;              // texto explicativo del orden
}
```

---

## 4. Endpoints

🔓 = publico · 🔒 = cuenta autenticada · 🛡️ = ADMIN · 📅 = ORGANIZADOR y perfil activo.
Todas las rutas protegidas requieren `Authorization: Bearer <token>`.

### Autenticacion

| | Metodo | Ruta | Body | Respuesta `data` | Errores |
|---|---|---|---|---|---|
| 🔓 | POST | `/api/auth/register` | `RegisterRequest` | **201** `AuthResponse` | 400, 409 |
| 🔓 | POST | `/api/auth/login` | `LoginRequest` | 200 `AuthResponse` | 400, 401 |
| 🔒 | POST | `/api/auth/logout` | - | 200 `null` | 401, 500 |
| 🔒 | GET | `/api/auth/me` | - | 200 `UsuarioResponse` | 401 |
| 🛡️ | GET | `/api/admin/users` | - | 200 `UsuarioResponse[]` | 401, 403 |
| 🛡️ | GET | `/api/admin/users/{id}` | - | 200 `UsuarioResponse` | 401, 403, 404 |
| 🛡️ | POST | `/api/admin/users` | `CreateUsuarioRequest` | 201 `UsuarioResponse` | 400, 401, 403, 409 |
| 🛡️ | PATCH | `/api/admin/users/{id}` | `UpdateUsuarioRequest` | 200 `UsuarioResponse` | 400, 401, 403, 404, 409 |
| 🛡️ | DELETE | `/api/admin/users/{id}` | - | 200 `null` | 401, 403, 404, 409 |
| 🔒 | PATCH | `/api/auth/me` | `UpdatePerfilRequest` | 200 `UsuarioResponse` | 400, 401, 409 |
| 🔒 | DELETE | `/api/auth/me` | - | 200 `null` | 401, 409 |

### Eventos

| | Metodo | Ruta | Body | Respuesta `data` | Errores |
|---|---|---|---|---|---|
| 📅 | GET | `/api/events` | - | 200 `EventoResponse[]` | 400, 401, 403 |
| 📅 | GET | `/api/events/{id}` | - | 200 `EventoResponse` | 400, 401, 403, 404 |
| 📅 | POST | `/api/events` | `CreateEventoRequest` | **201** `EventoResponse` | 400, 401, 403 |
| 📅 | PATCH | `/api/events/{id}` | `UpdateEventoRequest` | 200 `EventoResponse` | 400, 401, 403, 404 |
| 📅 | DELETE | `/api/events/{id}` | - | 200 `null` (borra sus subtareas) | 400, 401, 403, 404 |
| 📅 | GET | `/api/events/{id}/progress` | - | 200 `ProgressResponse` | 400, 401, 403, 404 |

### Subtareas

| | Metodo | Ruta | Body | Respuesta `data` | Errores |
|---|---|---|---|---|---|
| 📅 | POST | `/api/events/{eventId}/subtasks` | `CreateSubtareaRequest` | **201** `SubtareaResponse` | 400, 401, 403, 404 |
| 📅 | GET | `/api/events/{eventId}/subtasks` | - | 200 `SubtareaResponse[]` | 400, 401, 403, 404 |
| 📅 | PATCH | `/api/subtasks/{id}` | `UpdateSubtareaRequest` | 200 `SubtareaResponse` | 400, 401, 403, 404, **409** `CapacityConflictError` |
| 📅 | PATCH | `/api/subtasks/{id}/status` | `ChangeSubtareaStatusRequest` | 200 `SubtareaResponse` | 400, 401, 403, 404 |
| 📅 | DELETE | `/api/subtasks/{id}` | - | 200 `null` | 400, 401, 403, 404 |
| 📅 | POST | `/api/subtasks/{id}/conflicts/overload` | `OverloadCheckRequest` | 200 `OverloadCheckResponse` (no guarda) | 400, 401, 403, 404 |

### Capacidad diaria

| | Metodo | Ruta | Body | Respuesta `data` | Errores |
|---|---|---|---|---|---|
| 📅 | GET | `/api/capacity` | - | 200 `CapacidadResponse` | 400, 401, 403 |
| 📅 | PUT | `/api/capacity` | `CapacidadRequest` | 200 `CapacidadResponse` | 400, 401, 403 |

### Vista Hoy

| | Metodo | Ruta | Query | Respuesta `data` | Errores |
|---|---|---|---|---|---|
| 📅 | GET | `/api/today` | `eventId?: UUID`, `status?: EstadoSubtarea` | 200 `TodayResponse` | 400, 401, 403 |

---

## 5. Ejemplos

### Registro

```http
POST /api/auth/register
Content-Type: application/json

{ "nombre": "Camila Restrepo", "correo": "camila@correo.com", "password": "Secreta123" }
```

```json
{
  "success": true,
  "message": "La cuenta se creo correctamente.",
  "data": {
    "accessToken": "eyJhbGciOiJIUzI1NiJ9...",
    "tokenType": "Bearer",
    "expiresIn": 7200,
    "usuario": {
      "id": "6f1c1f5e-7d0a-4c55-9a43-2b9f0f1e9c11",
      "organizadorId": "2b8639f5-d7d4-4805-a1c5-4fb72b018084",
      "nombre": "Camila Restrepo",
      "correo": "camila@correo.com",
      "roles": ["ORGANIZADOR"],
      "activo": true,
      "createdAt": "2026-10-02T10:15:30"
    }
  },
  "timestamp": "2026-10-02T15:15:30.123Z"
}
```

### Logout

```http
POST /api/auth/logout
Authorization: Bearer <accessToken>
```

```json
{
  "success": true,
  "message": "Cerraste sesion correctamente.",
  "data": null,
  "timestamp": "2026-10-03T15:15:30Z"
}
```

Se revoca el JWT utilizado. No enviar el token en un JSON ni en la URL. Después del cierre,
el cliente borra su copia del token. Reutilizarlo, incluso en logout, devuelve 401.
Ejemplos de errores en [auth.md](./auth.md).

### Login fallido

```json
{ "success": false, "message": "Correo o contrasena incorrectos.", "timestamp": "2026-10-02T15:16:00Z" }
```

### Ruta protegida sin token / token expirado

```json
{ "success": false, "message": "Debes iniciar sesion para acceder a este recurso.", "timestamp": "..." }
{ "success": false, "message": "Tu sesion expiro o el token no es valido. Inicia sesion nuevamente.", "timestamp": "..." }
```

### Crear evento autenticado

```http
POST /api/events
Authorization: Bearer eyJhbGciOiJIUzI1NiJ9...
Content-Type: application/json

{
  "nombre": "Boda Camila y Andres",
  "tipo": "Social",
  "fechaHora": "2026-12-20T18:00:00",
  "lugar": "Club Campestre",
  "subtareas": [
    { "name": "Reservar salon", "targetDate": "2026-11-10", "estimatedHours": 3 }
  ]
}
```

---

## 6. Implementacion sugerida en el cliente

### Angular (interceptor funcional, Angular 15+)

```ts
// auth.interceptor.ts
import { HttpInterceptorFn, HttpErrorResponse } from '@angular/common/http';
import { inject } from '@angular/core';
import { Router } from '@angular/router';
import { catchError, throwError } from 'rxjs';

const PUBLIC = ['/api/auth/login', '/api/auth/register'];

export const authInterceptor: HttpInterceptorFn = (req, next) => {
  const router = inject(Router);
  const token = localStorage.getItem('accessToken');
  const isPublic = PUBLIC.some(p => req.url.includes(p));

  const authReq = token && !isPublic
    ? req.clone({ setHeaders: { Authorization: `Bearer ${token}` } })
    : req;

  return next(authReq).pipe(
    catchError((err: HttpErrorResponse) => {
      const path = new URL(req.url, window.location.origin).pathname;
      const passwordUpdate = req.method === 'PATCH' &&
        (path === '/api/auth/me' || path.startsWith('/api/admin/users/'));
      // En passwordUpdate, el servicio consulta GET /api/auth/me para distinguir
      // contraseña actual incorrecta (200) de sesión inválida (401).
      if (err.status === 401 && !isPublic && !passwordUpdate) {
        localStorage.removeItem('accessToken');
        router.navigate(['/login']);
      }
      return throwError(() => err);
    })
  );
};

// app.config.ts -> provideHttpClient(withInterceptors([authInterceptor]))
```

```ts
// auth.service.ts (resumen)
login(body: LoginRequest) {
  return this.http.post<ApiResponse<AuthResponse>>(`${API}/api/auth/login`, body).pipe(
    tap(r => {
      localStorage.setItem('accessToken', r.data.accessToken);
      localStorage.setItem('expiresAt', String(Date.now() + r.data.expiresIn * 1000));
    })
  );
}
isLoggedIn() { return Number(localStorage.getItem('expiresAt') ?? 0) > Date.now(); }
hasRole(user: UsuarioResponse, rol: Rol) { return user.roles.includes(rol); }
```

### fetch (cualquier framework)

```ts
async function api<T>(path: string, init: RequestInit = {}): Promise<T> {
  const token = localStorage.getItem('accessToken');
  const res = await fetch(`${API}${path}`, {
    ...init,
    headers: {
      'Content-Type': 'application/json',
      ...(token ? { Authorization: `Bearer ${token}` } : {}),
      ...init.headers,
    },
  });
  const body = await res.json();
  if (!res.ok) {
    const pathname = path.split('?')[0];
    const isPublic = ['/api/auth/login', '/api/auth/register'].includes(pathname);
    const passwordUpdate = init.method?.toUpperCase() === 'PATCH' &&
      (pathname === '/api/auth/me' || pathname.startsWith('/api/admin/users/'));
    // En passwordUpdate, el servicio confirma la sesión con GET /api/auth/me.
    if (res.status === 401 && !isPublic && !passwordUpdate) {
      localStorage.removeItem('accessToken');
      location.href = '/login';
    }
    throw body as ApiError;
  }
  return (body as ApiResponse<T>).data;
}
```

### Guards de rutas en el front

- Rutas privadas: exigir `isLoggedIn()`; si no, redirigir a `/login`.
- Rutas de administracion: exigir `usuario.roles.includes('ADMIN')` (el backend igual devuelve
  403 si no lo tiene; el guard es solo para UX).

## Notas de edición y eliminación

Los campos opcionales omitidos o null conservan su valor. PATCH propio no admite `roles` ni
`activo`. Al cambiar la contraseña propia se exige `passwordActual`, también si ADMIN usa
la ruta administrativa sobre su misma cuenta. ADMIN puede restablecer contraseñas ajenas.
Cambiar password mantiene los JWT ya emitidos hasta su expiración.

DELETE elimina cuenta/perfil/asignaciones solo cuando no hay eventos ni capacidades.
Si devuelve 409, mostrar el mensaje y conservar la sesión. Tras DELETE propio exitoso,
eliminar el token y volver a login. El último ADMIN habilitado no puede ser eliminado,
desactivado ni perder su rol.

## Validaciones para formularios

- Cuenta: nombre obligatorio hasta 120; correo válido hasta 180; password mínimo 8
  caracteres Unicode y máximo 72 bytes UTF-8 (`new TextEncoder().encode(password).length`).
- Roles: ADMIN y ORGANIZADOR, lista no vacía. El registro público no admite roles.
- Evento: nombre, tipo y fechaHora obligatorios. Respetar los tamaños de almacenamiento:
  nombre 180, tipo 100, cliente/contactoCliente 180, lugar 240 caracteres.
- Subtarea: nombre, fechaObjetivo y horasEstimadas obligatorios; horas > 0. Respetar nombre
  hasta 180 y nota hasta 1000 caracteres. Horas y capacidad se almacenan con dos decimales.
- Capacidad: limiteHoras obligatorio entre 1 y 16 inclusive.
- Estado: PENDING, DONE o POSTPONED. Vista Hoy excluye DONE, incluso usando status=DONE.
- No enviar organizadorId ni usuarioId al crear eventos o capacidades: el backend
  resuelve la pertenencia desde la cuenta autenticada. No enviar campos desconocidos.

## Descripción de subtareas y payload del cliente

`POST /api/events/{eventId}/subtasks` usa `name`, `description`, `targetDate` y
`estimatedHours`. Los nombres españoles solo son alias de compatibilidad de entrada.
Usar una sola variante por campo. `description` es opcional, admite null y tiene un máximo
de 255 caracteres. Se guarda en `subtareas.descripcion` y se devuelve como `description`
en las respuestas de subtarea; todos sus campos son en inglés: eventId, name, description,
targetDate, estimatedHours, status, note, doneAt y createdAt, además de id.
Se admite igualmente description en las subtareas iniciales de POST /api/events.
PATCH /api/subtasks/{id} permite editar description (o descripcion). Omitirla o enviarla
como null conserva la descripción anterior; enviar "" la vacía. Usa name, targetDate y estimatedHours
para los otros campos editables, con los nombres españoles como alias de entrada.

```ts
interface SubtaskPayload {
  name: string;
  description?: string;
  targetDate: string;
  estimatedHours: number;
}
```

```json
{
  "name": "Confirmar catering",
  "description": "Confirmar menu vegetariano con el proveedor",
  "targetDate": "2026-10-10",
  "estimatedHours": 2
}
```
