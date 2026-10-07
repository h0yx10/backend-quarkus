/** Contratos HTTP de events-api.
 * Fechas/UUID en texto; importables por Angular, React u otro cliente TypeScript.
 * Cada respuesta de éxito es ApiResponse<T>. DELETE responde ApiResponse<null>.
 * Rutas, permisos y ejemplos: contratos-frontend.md.
 */

// Respuesta exitosa (2xx)
export interface ApiResponse<T> {
  success: true;
  message: string;
  data: T;            // null en DELETE
  timestamp: string;  // ISO-8601 UTC
}

// Respuesta de error (4xx / 5xx)
export interface ApiError {
  success: false;
  message: string;    // mensaje listo para mostrar al usuario
  timestamp: string;
}

// 409 por sobrecarga de capacidad (PATCH /api/subtasks/{id})
export interface CapacityConflictError extends ApiError {
  plannedHours: number;
  limitHours: number;
  exceedsBy: number;
}

export type UUID = string;
export type DateISO = string;      // "2026-05-01"
export type DateTimeISO = string;  // "2026-05-20T18:00:00"

export type Rol = 'ORGANIZADOR' | 'ADMIN';
export type EstadoSubtarea = 'PENDING' | 'DONE' | 'POSTPONED';

// ---------- Auth ----------
export interface RegisterRequest {
  nombre: string;    // requerido, max 120
  correo: string;    // requerido, email valido, max 180
  password: string;  // requerido, mínimo 8 caracteres Unicode y máximo 72 bytes UTF-8
}

export interface LoginRequest {
  correo: string;
  password: string;
}

// Cuenta y perfil tienen UUID independientes.
export interface UsuarioResponse {
  id: UUID;            // usuarios.id = sub del JWT
  organizadorId: UUID | null; // organizadores.id, independiente del usuario
  nombre: string;
  correo: string;
  roles: Rol[];    // de usuario_roles
  activo: boolean;     // organizadores.activo; true si no tiene perfil
  createdAt: DateTimeISO;
}

export interface AuthResponse {
  accessToken: string;
  tokenType: 'Bearer';
  expiresIn: number;   // segundos
  usuario: UsuarioResponse;
}

export interface CreateUsuarioRequest extends RegisterRequest {
  roles?: Rol[]; // no vacío; por defecto ORGANIZADOR; solo POST administrativo
}
export interface UpdatePerfilRequest {
  nombre?: string;
  correo?: string;
  password?: string;
  passwordActual?: string; // obligatoria al cambiar contraseña propia
}
export interface UpdateUsuarioRequest extends UpdatePerfilRequest {
  roles?: Rol[]; // reemplaza la lista, solo ADMIN
  activo?: boolean; // solo ADMIN, requiere perfil
}

// ---------- Eventos ----------
export interface SubtareaInicialRequest {
  description?: string;   // opcional, máximo 255 caracteres; alias de entrada: descripcion
  name: string;          // requerido
  targetDate: DateISO;  // requerido
  estimatedHours: number;  // requerido, > 0
}

export interface CreateEventoRequest {
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
export interface UpdateEventoRequest {
  nombre?: string;
  tipo?: string;
  cliente?: string;
  contactoCliente?: string;
  fechaHora?: DateTimeISO;
  lugar?: string;
  plazoLimite?: DateTimeISO;
}

export interface EventoResponse {
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

export interface ProgressResponse {
  done: number;
  total: number;
  percentage: number;          // 0..100
}

// ---------- Subtareas ----------
export interface CreateSubtareaRequest {
  description?: string;   // opcional, máximo 255 caracteres; alias de entrada: descripcion
  name: string;              // requerido
  targetDate: DateISO;      // requerido
  estimatedHours: number;      // requerido, > 0
}

export interface UpdateSubtareaRequest {   // PATCH parcial
  description?: string; // maximo 255; omitido/null conserva el valor; "" lo vacía
  name?: string;
  targetDate?: DateISO;          // si cambia targetDate o estimatedHours se valida sobrecarga (409)
  estimatedHours?: number;          // > 0
}

export interface ChangeSubtareaStatusRequest {
  status: EstadoSubtarea;           // requerido
  note?: string;                    // opcional (tipicamente al posponer)
}

export interface SubtareaResponse {
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
export interface OverloadCheckRequest {
  targetDate?: DateISO;    // si se omite, usa la actual de la subtarea
  estimatedHours?: number;    // si se omite, usa las actuales
}

export interface OverloadCheckResponse {
  conflict: boolean;
  plannedHours: number;
  limitHours: number;
  exceedsBy: number;          // 0 si no hay conflicto
}

// ---------- Capacidad ----------
export interface CapacidadRequest {
  limiteHoras: number;        // requerido, 1..16
}

export interface CapacidadResponse {
  limiteHoras: number;
  porDefecto: boolean;        // true si nunca se configuro (6h)
  fecha: DateISO | null;
}

// ---------- Hoy ----------
export interface TodayResponse {
  vencidas: SubtareaResponse[];
  paraHoy: SubtareaResponse[];
  proximas: SubtareaResponse[];
  regla: string;              // texto explicativo del orden
}

export interface TodayQuery {
  eventId?: UUID;
  status?: EstadoSubtarea;
}

/** POST /api/auth/logout: Bearer obligatorio, sin body. */
export type LogoutResponse = ApiResponse<null>;

/** Payload en inglés aceptado por POST /api/events/{eventId}/subtasks.
 * La respuesta usa eventId/name/description/targetDate/estimatedHours/status/note/doneAt/createdAt/id.
 */
export interface SubtaskPayload {
  name: string;
  description?: string;
  targetDate: DateISO;
  estimatedHours: number;
}

/** Campos opcionales en PATCH /api/subtasks/{id}; acepta los mismos alias que CREATE. */
export type UpdateSubtaskPayload = Partial<SubtaskPayload>;
