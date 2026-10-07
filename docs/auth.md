# Autenticación y gestión de usuarios

La cuenta de login vive en `usuarios`; sus roles viven en `usuario_roles`. El perfil opcional
`organizadores` tiene un UUID independiente y `usuario_id` único hacia la cuenta. El JWT usa
`sub = usuarios.id`, nunca el ID del organizador.

## Autenticación

| Método | Ruta | Acceso | Resultado |
|---|---|---|---|
| POST | `/api/auth/register` | Público | 201: cuenta ORGANIZADOR, perfil activo y token |
| POST | `/api/auth/login` | Público | 200: token y usuario |
| POST | `/api/auth/logout` | Autenticado | 200: token revocado, `data: null` |
| GET | `/api/auth/me` | Autenticado | 200: usuario actual |
| PATCH | `/api/auth/me` | Autenticado | 200: perfil actualizado |
| DELETE | `/api/auth/me` | Autenticado | 200: `data: null` |

Registro: `{ nombre, correo, password }`. Nombre obligatorio hasta 120 caracteres; correo
válido hasta 180, guardado con trim y minúsculas; password con al menos 8 caracteres Unicode
y hasta **72 bytes UTF-8**. Contraseñas sin trim, almacenadas con BCrypt. Un correo duplicado,
incluido un registro concurrente, devuelve 409. Usuario, perfil y roles se guardan en una
transacción; el token se emite tras su confirmación.

Login: `{ correo, password }`. Devuelve el mismo 401 si la cuenta no existe, la contraseña
es incorrecta o el perfil está inactivo. JWT HS256: `accessToken`, `tokenType: "Bearer"`,
`expiresIn` en segundos y `usuario`; expiración predeterminada de 120 minutos.

Enviar `Authorization: Bearer <accessToken>` a rutas protegidas. Después de validar firma,
emisor y expiración, cada petición consulta la cuenta y sus roles actuales en BD. Una cuenta
eliminada o inactiva devuelve 401; retirar un permiso devuelve 403 en la ruta correspondiente.
Los roles del JWT son una instantánea informativa: las autorizaciones usan los roles de BD.
No hay refresh token. Para cerrar sesión, llamar POST /api/auth/logout con el Bearer
actual y después borrar el token guardado en el cliente.
Cambiar contraseña no revoca otros tokens ya emitidos.

PATCH propio admite `{ nombre?, correo?, password?, passwordActual? }`. Para cambiar password
es obligatorio comprobar `passwordActual`; contraseña actual incorrecta o ausente devuelve
401. Se rechazan propiedades desconocidas, incluidos `roles` y `activo`, con 400. Los campos
opcionales omitidos o null no cambian el valor. `{}` conserva el perfil actual.

## CRUD administrativo

Todas las rutas siguientes exigen ADMIN:

| Método | Ruta | Resultado |
|---|---|---|
| GET | `/api/admin/users` | 200: `UsuarioResponse[]` |
| GET | `/api/admin/users/{id}` | 200: `UsuarioResponse` |
| POST | `/api/admin/users` | 201: `UsuarioResponse`, sin token |
| PATCH | `/api/admin/users/{id}` | 200: `UsuarioResponse` |
| DELETE | `/api/admin/users/{id}` | 200: `data: null` |

POST admite `{ nombre, correo, password, roles? }`; por defecto asigna ORGANIZADOR. Los roles
válidos son ADMIN y ORGANIZADOR; la lista debe tener al menos uno y no admite null. Solo se
crea perfil al asignar ORGANIZADOR; una cuenta solo ADMIN puede autenticarse sin perfil.

PATCH admite `{ nombre?, correo?, password?, passwordActual?, roles?, activo? }`. `roles`
reemplaza la lista completa. Asignar ORGANIZADOR crea el perfil si falta. Retirar ORGANIZADOR
conserva perfil y datos, pero impide usar las rutas de negocio. `activo` solo aplica a un
perfil existente, de lo contrario devuelve 400. Si se asigna ORGANIZADOR y `activo` en el
mismo PATCH, se crea primero el perfil y se aplica su actividad. ADMIN puede restablecer
contraseñas ajenas; para cambiar la suya debe aportar `passwordActual`.

## Eliminación y último ADMIN

DELETE propio o administrativo elimina físicamente la cuenta, perfil y asignaciones de
roles cuando no existen eventos ni capacidades asociadas. Si existen, devuelve **409** sin
borrar nada. Las claves foráneas también impiden el borrado con dependencias. Las subtareas
pertenecen a los eventos y quedan protegidas por esta regla.

No se permite eliminar, desactivar o retirar el rol al último ADMIN habilitado. Un ADMIN
habilitado tiene credenciales y no tiene perfil inactivo. Los cambios se serializan con un
bloqueo transaccional sobre el rol ADMIN para cubrir peticiones concurrentes. Un cambio
rechazado revierte también el resto de campos del PATCH.

## Respuesta del usuario

`UsuarioResponse`: `{ id, organizadorId, nombre, correo, roles, activo, createdAt }`.
`id` identifica `usuarios`; `organizadorId` es el UUID del perfil o null. `activo` viene del
perfil y vale true cuando no hay perfil. `createdAt` es obligatorio en nuevas cuentas.
Nunca se devuelven password ni passwordHash.

Éxitos usan `{ success, message, data, timestamp }`; errores
`{ success: false, message, timestamp }`. Códigos: 400 entrada inválida, 401 autenticación
fallida, 403 permisos insuficientes, 404 usuario inexistente, 409 correo duplicado,
dependencias o protección del último ADMIN.

## Ejemplos JSON de respuestas exitosas

Los valores de UUID, fechas y token son ilustrativos. Los mensajes se reproducen exactamente
como los devuelve el backend, incluidos los textos sin tildes. El marcador de accessToken
se reemplaza por un JWT real; expiresIn está expresado en segundos. El código HTTP está en
la respuesta HTTP y no es un campo del JSON.

| Método y ruta | HTTP | message exacto |
|---|---|---|
| `POST /api/auth/register` | 201 | `La cuenta se creo correctamente.` |
| `POST /api/auth/login` | 200 | `Iniciaste sesion correctamente.` |
| `POST /api/auth/logout` | 200 | `Cerraste sesion correctamente.` |
| `GET /api/auth/me` | 200 | `El usuario se consulto correctamente.` |
| `PATCH /api/auth/me` | 200 | `Perfil actualizado correctamente.` |
| `DELETE /api/auth/me` | 200 | `Cuenta eliminada correctamente.` |
| `GET /api/admin/users` | 200 | `Usuarios consultados correctamente.` |
| `GET /api/admin/users/{id}` | 200 | `Usuario consultado correctamente.` |
| `POST /api/admin/users` | 201 | `Usuario creado correctamente.` |
| `PATCH /api/admin/users/{id}` | 200 | `Usuario actualizado correctamente.` |
| `DELETE /api/admin/users/{id}` | 200 | `Usuario eliminado correctamente.` |

### POST /api/auth/register — 201

Registro público con rol ORGANIZADOR y perfil activo.

```json
{
  "success": true,
  "message": "La cuenta se creo correctamente.",
  "data": {
    "accessToken": "<JWT emitido por el backend>",
    "tokenType": "Bearer",
    "expiresIn": 7200,
    "usuario": {
      "id": "6f1c1f5e-7d0a-4c55-9a43-2b9f0f1e9c11",
      "organizadorId": "2b8639f5-d7d4-4805-a1c5-4fb72b018084",
      "nombre": "Camila Restrepo",
      "correo": "camila@correo.com",
      "roles": [
        "ORGANIZADOR"
      ],
      "activo": true,
      "createdAt": "2026-10-02T10:15:30"
    }
  },
  "timestamp": "2026-10-02T15:15:30Z"
}
```

### POST /api/auth/login — 200

Login correcto. El token y expiresIn dependen de la configuración.

```json
{
  "success": true,
  "message": "Iniciaste sesion correctamente.",
  "data": {
    "accessToken": "<JWT emitido por el backend>",
    "tokenType": "Bearer",
    "expiresIn": 7200,
    "usuario": {
      "id": "6f1c1f5e-7d0a-4c55-9a43-2b9f0f1e9c11",
      "organizadorId": "2b8639f5-d7d4-4805-a1c5-4fb72b018084",
      "nombre": "Camila Restrepo",
      "correo": "camila@correo.com",
      "roles": [
        "ORGANIZADOR"
      ],
      "activo": true,
      "createdAt": "2026-10-02T10:15:30"
    }
  },
  "timestamp": "2026-10-02T15:15:30Z"
}
```

### POST /api/auth/logout — 200

Enviar el token de la sesión a cerrar; no lleva body:

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

Después del 200, ese mismo JWT responde 401 en cualquier ruta protegida, incluso si se
repite logout. Otros tokens de la cuenta siguen funcionando; iniciar sesión de nuevo emite
un token diferente. Logout no elimina la cuenta ni sus datos ni cambia sus roles.

La revocación se guarda en PostgreSQL usando solo el SHA-256 del token y su expiración.
Se comparte entre instancias y permanece tras reinicios. Al ejecutar logout se limpian las
revocaciones expiradas, conservando el margen de 60 segundos aceptado al validar JWT.
Una petición que ya estaba autenticada antes de confirmar logout puede terminar su ejecución.

El frontend debe borrar accessToken, expiresAt y el usuario que conserva en memoria después
del 200, o del 401 si la sesión ya no está disponible. Si hay un fallo de red o un 500, no se
ha confirmado la revocación: mostrar el error y permitir reintentar. El backend no puede
eliminar directamente localStorage/sessionStorage del navegador.

401 sin token:

```json
{
  "success": false,
  "message": "Debes iniciar sesion para acceder a este recurso.",
  "timestamp": "2026-10-03T15:15:30Z"
}
```

401 token inválido, expirado o revocado:

```json
{
  "success": false,
  "message": "Tu sesion expiro o el token no es valido. Inicia sesion nuevamente.",
  "timestamp": "2026-10-03T15:15:30Z"
}
```

500 al fallar el cierre de sesión:

```json
{
  "success": false,
  "message": "Ocurrio un inconveniente. Intentalo nuevamente mas tarde.",
  "timestamp": "2026-10-03T15:15:30Z"
}
```

### GET /api/auth/me — 200

Consulta del usuario autenticado.

```json
{
  "success": true,
  "message": "El usuario se consulto correctamente.",
  "data": {
    "id": "6f1c1f5e-7d0a-4c55-9a43-2b9f0f1e9c11",
    "organizadorId": "2b8639f5-d7d4-4805-a1c5-4fb72b018084",
    "nombre": "Camila Restrepo",
    "correo": "camila@correo.com",
    "roles": [
      "ORGANIZADOR"
    ],
    "activo": true,
    "createdAt": "2026-10-02T10:15:30"
  },
  "timestamp": "2026-10-02T15:15:30Z"
}
```

### PATCH /api/auth/me — 200

Ejemplo tras actualizar nombre. Cambiar contraseña devuelve esta misma estructura, sin contraseñas ni hashes.

```json
{
  "success": true,
  "message": "Perfil actualizado correctamente.",
  "data": {
    "id": "6f1c1f5e-7d0a-4c55-9a43-2b9f0f1e9c11",
    "organizadorId": "2b8639f5-d7d4-4805-a1c5-4fb72b018084",
    "nombre": "Camila Actualizada",
    "correo": "camila@correo.com",
    "roles": [
      "ORGANIZADOR"
    ],
    "activo": true,
    "createdAt": "2026-10-02T10:15:30"
  },
  "timestamp": "2026-10-02T15:15:30Z"
}
```

### DELETE /api/auth/me — 200

Cuenta sin datos asociados; después del éxito el frontend elimina el token.

```json
{
  "success": true,
  "message": "Cuenta eliminada correctamente.",
  "data": null,
  "timestamp": "2026-10-02T15:15:30Z"
}
```

### GET /api/admin/users — 200

Listado administrativo. Si no hay resultados, data es [].

```json
{
  "success": true,
  "message": "Usuarios consultados correctamente.",
  "data": [
    {
      "id": "6f1c1f5e-7d0a-4c55-9a43-2b9f0f1e9c11",
      "organizadorId": "2b8639f5-d7d4-4805-a1c5-4fb72b018084",
      "nombre": "Camila Restrepo",
      "correo": "camila@correo.com",
      "roles": [
        "ORGANIZADOR"
      ],
      "activo": true,
      "createdAt": "2026-10-02T10:15:30"
    },
    {
      "id": "a16a2a59-d6eb-4a98-8c12-ddbaf466d6f0",
      "organizadorId": null,
      "nombre": "Administrador",
      "correo": "admin@correo.com",
      "roles": [
        "ADMIN"
      ],
      "activo": true,
      "createdAt": "2026-10-02T10:16:00"
    }
  ],
  "timestamp": "2026-10-02T15:15:30Z"
}
```

### GET /api/admin/users/{id} — 200

Consulta administrativa de una cuenta existente.

```json
{
  "success": true,
  "message": "Usuario consultado correctamente.",
  "data": {
    "id": "6f1c1f5e-7d0a-4c55-9a43-2b9f0f1e9c11",
    "organizadorId": "2b8639f5-d7d4-4805-a1c5-4fb72b018084",
    "nombre": "Camila Restrepo",
    "correo": "camila@correo.com",
    "roles": [
      "ORGANIZADOR"
    ],
    "activo": true,
    "createdAt": "2026-10-02T10:15:30"
  },
  "timestamp": "2026-10-02T15:15:30Z"
}
```

### POST /api/admin/users — 201

Ejemplo de creación con roles: ["ADMIN"]. No emite token; organizadorId es null porque no se creó perfil.

```json
{
  "success": true,
  "message": "Usuario creado correctamente.",
  "data": {
    "id": "a16a2a59-d6eb-4a98-8c12-ddbaf466d6f0",
    "organizadorId": null,
    "nombre": "Administrador",
    "correo": "admin@correo.com",
    "roles": [
      "ADMIN"
    ],
    "activo": true,
    "createdAt": "2026-10-02T10:16:00"
  },
  "timestamp": "2026-10-02T15:15:30Z"
}
```

### PATCH /api/admin/users/{id} — 200

Ejemplo tras asignar ADMIN y ORGANIZADOR. roles contiene la lista final completa.

```json
{
  "success": true,
  "message": "Usuario actualizado correctamente.",
  "data": {
    "id": "6f1c1f5e-7d0a-4c55-9a43-2b9f0f1e9c11",
    "organizadorId": "2b8639f5-d7d4-4805-a1c5-4fb72b018084",
    "nombre": "Camila Restrepo",
    "correo": "camila@correo.com",
    "roles": [
      "ADMIN",
      "ORGANIZADOR"
    ],
    "activo": true,
    "createdAt": "2026-10-02T10:15:30"
  },
  "timestamp": "2026-10-02T15:15:30Z"
}
```

### DELETE /api/admin/users/{id} — 200

Eliminación administrativa permitida, sin datos de negocio y sin eliminar al último ADMIN habilitado.

```json
{
  "success": true,
  "message": "Usuario eliminado correctamente.",
  "data": null,
  "timestamp": "2026-10-02T15:15:30Z"
}
```

## Errores esperados por endpoint

Las rutas protegidas pueden responder 401 por sesión inválida. Las rutas administrativas
pueden responder 403 por falta de ADMIN. Cualquier operación puede responder 500 ante un
fallo inesperado. Las respuestas de error no incluyen data, token ni un código HTTP en el JSON.

| Endpoint | Otros errores esperados |
|---|---|
| `POST /api/auth/register` | 400 por validación/JSON/campos desconocidos; 409 por correo duplicado |
| `POST /api/auth/login` | 400 por credenciales vacías/JSON inválido; 401 por credenciales incorrectas o perfil inactivo |
| `POST /api/auth/logout` | 401 sin token o con token inválido/expirado/revocado; 500 si falla la persistencia de la revocación |
| `GET /api/auth/me` | 401 por cuenta/token no disponibles |
| `PATCH /api/auth/me` | 400 por validación o campos no permitidos; 401 por passwordActual incorrecta/ausente; 409 por correo duplicado |
| `DELETE /api/auth/me` | 409 por datos asociados o último ADMIN habilitado |
| `GET /api/admin/users` | 401 sin sesión; 403 sin ADMIN |
| `GET /api/admin/users/{id}` | 400 por UUID inválido; 404 por usuario inexistente |
| `POST /api/admin/users` | 400 por validación/roles/JSON; 409 por correo duplicado |
| `PATCH /api/admin/users/{id}` | 400 por UUID/validación/roles/actividad sin perfil; 401 por passwordActual incorrecta al cambiar la propia contraseña; 404 por usuario inexistente; 409 por correo duplicado o último ADMIN |
| `DELETE /api/admin/users/{id}` | 400 por UUID inválido; 404 por usuario inexistente; 409 por datos asociados o último ADMIN |

## Ejemplos JSON de errores

### 400 — correo inválido en registro

Ejemplo enviando correo: "correo-invalido" y los demás campos válidos.

```json
{
  "success": false,
  "message": "Escribe un correo valido.",
  "timestamp": "2026-10-02T15:15:30Z"
}
```

### 400 — contraseña fuera del límite

Registro, creación o actualización con menos de 8 caracteres Unicode o más de 72 bytes UTF-8.

```json
{
  "success": false,
  "message": "La contrasena debe tener al menos 8 caracteres y un maximo de 72 bytes UTF-8.",
  "timestamp": "2026-10-02T15:15:30Z"
}
```

### 400 — JSON inválido o campo no permitido

Incluye JSON mal formado, roles desconocidos como ROOT y campos desconocidos. Por ejemplo, enviar roles o activo al PATCH /api/auth/me.

```json
{
  "success": false,
  "message": "Revisa los datos ingresados e intentalo nuevamente.",
  "timestamp": "2026-10-02T15:15:30Z"
}
```

### 400 — actividad sin perfil

PATCH administrativo con activo sobre una cuenta sin perfil, sin asignar ORGANIZADOR en la misma petición.

```json
{
  "success": false,
  "message": "El usuario no tiene perfil de organizador.",
  "timestamp": "2026-10-02T15:15:30Z"
}
```

### 401 — login fallido

El mensaje es idéntico si no existe el correo, falla la contraseña o el perfil está inactivo.

```json
{
  "success": false,
  "message": "Correo o contrasena incorrectos.",
  "timestamp": "2026-10-02T15:15:30Z"
}
```

### 401 — contraseña actual incorrecta

Cambiar contraseña propia sin passwordActual o con un valor incorrecto, también al usar la ruta administrativa sobre la misma cuenta. Este error no invalida la sesión: consultar GET /api/auth/me antes de descartar el token.

```json
{
  "success": false,
  "message": "La contrasena actual es incorrecta.",
  "timestamp": "2026-10-02T15:15:30Z"
}
```

### 401 — falta el token

Petición protegida sin Authorization: Bearer. Las respuestas 401 de seguridad también incluyen el header WWW-Authenticate: Bearer.

```json
{
  "success": false,
  "message": "Debes iniciar sesion para acceder a este recurso.",
  "timestamp": "2026-10-02T15:15:30Z"
}
```

### 401 — token o cuenta no disponibles

JWT inválido/expirado/revocado, sujeto inválido, cuenta eliminada o perfil inactivo, detectados durante la autenticación de una petición protegida.

```json
{
  "success": false,
  "message": "Tu sesion expiro o el token no es valido. Inicia sesion nuevamente.",
  "timestamp": "2026-10-02T15:15:30Z"
}
```

### 403 — permisos insuficientes

Por ejemplo, una cuenta ORGANIZADOR intenta acceder al CRUD administrativo. Conservar la sesión.

```json
{
  "success": false,
  "message": "No tienes permisos para acceder a este recurso.",
  "timestamp": "2026-10-02T15:15:30Z"
}
```

### 404 — usuario inexistente

GET, PATCH o DELETE administrativo con un UUID válido que no identifica una cuenta existente.

```json
{
  "success": false,
  "message": "No encontramos el usuario.",
  "timestamp": "2026-10-02T15:15:30Z"
}
```

### 409 — correo ya registrado

Registro, creación administrativa o modificación del correo; también cubre registros concurrentes.

```json
{
  "success": false,
  "message": "Ya existe una cuenta con ese correo.",
  "timestamp": "2026-10-02T15:15:30Z"
}
```

### 409 — eliminación con datos de negocio

DELETE propio o administrativo cuando existen eventos o capacidades del perfil. Se conserva la cuenta y sus datos.

```json
{
  "success": false,
  "message": "No se puede eliminar un usuario con eventos o capacidades asociados.",
  "timestamp": "2026-10-02T15:15:30Z"
}
```

### 409 — dependencia detectada al borrar

Mensaje alternativo si la clave foránea detecta la dependencia durante el borrado, por ejemplo ante una creación concurrente de datos de negocio.

```json
{
  "success": false,
  "message": "No se puede eliminar un usuario con datos asociados.",
  "timestamp": "2026-10-02T15:15:30Z"
}
```

### 409 — último ADMIN habilitado

DELETE propio/administrativo o PATCH que desactiva el perfil o retira ADMIN cuando no queda otro ADMIN habilitado. Se revierte toda la operación.

```json
{
  "success": false,
  "message": "No se puede eliminar, desactivar ni retirar el rol del ultimo ADMIN habilitado.",
  "timestamp": "2026-10-02T15:15:30Z"
}
```

### 500 — fallo inesperado

Respuesta genérica para fallos no contemplados. No devuelve detalles internos del error.

```json
{
  "success": false,
  "message": "Ocurrio un inconveniente. Intentalo nuevamente mas tarde.",
  "timestamp": "2026-10-02T15:15:30Z"
}
```

### Mensajes de validación y casos variables

Para registro/login, los mensajes personalizados de validación incluyen:

| Condición | message |
|---|---|
| Nombre vacío en registro | `Escribe un nombre.` |
| Nombre de registro mayor a 120 caracteres | `El nombre puede tener maximo 120 caracteres.` |
| Correo vacío en registro/login | `Escribe tu correo.` |
| Correo de registro inválido o mayor a 180 caracteres | `Escribe un correo valido.` |
| Contraseña vacía en registro/login | `Escribe tu contrasena.` |
| Contraseña de registro demasiado corta o con más de 72 bytes UTF-8 | `La contrasena debe tener al menos 8 caracteres y un maximo de 72 bytes UTF-8.` |

El backend devuelve un solo message de validación por respuesta. Si hay varios campos
inválidos o varias reglas incumplidas, no garantiza cuál se devuelve primero.

Los DTO administrativos y de edición usan mensajes predeterminados de Bean Validation
para algunas reglas (correo inválido, nombre vacío, roles vacíos/null, tamaño). Esos textos
pueden variar según la configuración regional; conservar el sobre de error y mostrar message.
En UUID inválidos, el message proviene de la conversión del parámetro y contiene el valor
recibido; no hay un texto fijo para todos los casos.

Si la cuenta desaparece entre la validación del JWT y la consulta GET /api/auth/me, puede
responder 401 con `No encontramos el usuario autenticado.`. El caso habitual de cuenta
eliminada se rechaza antes con el mensaje de token o cuenta no disponibles.

El frontend debe usar el status HTTP para decidir el flujo y message para mostrar el motivo;
no debe depender de comparar textos ni cerrar sesión ante un 400, 403 o 409.

## Rutas y puesta en marcha

Registro y login, Swagger y health son públicos. `/api/admin/**` exige ADMIN. Eventos,
subtareas, capacidad y Hoy exigen ORGANIZADOR y perfil activo; recursos ajenos devuelven 404.
Las demás rutas protegidas exigen cuenta válida.



El permiso aplica inmediatamente en el backend. Volver a iniciar sesión o consultar `/me`
actualiza el usuario que muestra el frontend.
