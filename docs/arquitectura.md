# Arquitectura - events-api

API REST en Java 21 + Quarkus 3.40.1 JVM organizada como **arquitectura hexagonal (puertos y
adaptadores)**. El nucleo (dominio + aplicacion) no conoce Spring, HTTP, JWT ni la base de
datos: todo eso entra y sale por puertos. Las reglas se verifican automaticamente con ArchUnit
(`src/test/java/com/events/architecture/CleanArchitectureTest.java`).

## Vista general

```
                 ┌───────────────────────── infrastructure ─────────────────────────┐
  HTTP  ──►      │ adapter.in.rest  (controllers, DTOs, mappers, exception handler) │
  (front)        │        │ usa solo puertos de entrada (application.port.in)        │
                 │        ▼                                                          │
                 │   ┌──────────────── application ────────────────┐                │
                 │   │ port.in  (interfaces de casos de uso)        │                │
                 │   │ usecase  (implementaciones, Java puro)       │                │
                 │   │ port.out (interfaces que el caso de uso pide)│                │
                 │   └───────────────┬─────────────────────────────┘                │
                 │                   │ depende de                                    │
                 │             ┌─────▼─────┐                                         │
                 │             │  domain   │ entidades, enums, excepciones            │
                 │             └───────────┘                                         │
                 │        ▲ implementan port.out                                     │
                 │ adapter.out.persistence (JPA)   security (JWT, BCrypt, contexto)  │
                 │ config (wiring de beans, CORS, OpenAPI)                           │
                 └──────────────────────────────────────────────────────────────────┘
                        │                                   │
                   PostgreSQL (Supabase)            SmallRye JWT / SecurityIdentity
```

Las dependencias siempre apuntan hacia adentro: `infrastructure → application → domain`.

## Paquetes

| Paquete | Responsabilidad | Puede depender de |
|---|---|---|
| `com.events.domain.entity` | Entidades (`Usuario`, `Organizador`, `Rol`, `Evento`, `Subtarea`, `CapacidadDiaria`) y enums (`EstadoSubtarea`, `NombreRol`). Contienen las reglas de negocio (ej. horas > 0, limite 1..16, transiciones de estado). | Java estándar |
| `com.events.domain.exception` | Excepciones de negocio (`EventoNotFoundException`, `CapacityConflictException`, `CorreoYaRegistradoException`, `CredencialesInvalidasException`, ...). | nada |
| `com.events.application.port.in` | Un puerto (interfaz) por caso de uso: `CreateEventoPort`, `LoginPort`, `RegisterPort`, ... y records de resultado (`AuthResult`, `TodayGroups`, ...). | domain |
| `com.events.application.usecase` | Implementacion de cada caso de uso. Java puro, sin anotaciones de frameworks. | domain, ports |
| `com.events.application.port.out` | Lo que los casos de uso necesitan del exterior: repositorios, `CurrentUsuarioPort`, `CurrentOrganizadorPort`, `TransactionPort`, `PasswordHasherPort`, `TokenProviderPort`. | domain |
| `com.events.infrastructure.adapter.in.rest` | Adaptador de entrada HTTP: controllers, DTOs (request/response), mappers, `GlobalExceptionHandler`. | application.port.in, domain |
| `com.events.infrastructure.adapter.out.persistence` | Adaptadores de salida JPA (`*PersistenceAdapter`) con `EntityManager`, entidades JPA propias y `PersistenceMapper`. | application.port.out, domain |
| `com.events.infrastructure.security` | SmallRye JWT: permisos HTTP en `application.properties`, `JwtTokenProviderAdapter`, `BCryptPasswordHasherAdapter`, `SecurityContextCurrentUsuarioAdapter`, `SecurityContextCurrentOrganizadorAdapter`, `DatabaseJwtAuthenticationConverter`, `RestAuthenticationErrorHandler`. | application.port.out, domain |
| `com.events.infrastructure.config` | Wiring: `UseCaseConfig` crea los beans de casos de uso; `CorsConfig`; `OpenApiConfig`. | todo |

## Reglas verificadas por ArchUnit

1. El dominio no depende de `application` ni de `infrastructure`.
2. El dominio no depende de Spring, Quarkus, CDI, JPA, Bean Validation ni Lombok.
3. La aplicacion no depende de `infrastructure`.
4. **La aplicacion no depende de Spring, Quarkus, Jakarta ni Nimbus/JWT** (la seguridad entra por puertos).
5. Los adaptadores de entrada no dependen de los de salida, ni viceversa.
6. Los adaptadores de entrada usan puertos (`port.in`), nunca las clases `usecase` directamente.
7. Los puertos de entrada y de salida son interfaces (salvo los records de resultado listados).
8. Los adaptadores de persistencia viven en `adapter.out.persistence`; los recursos `@Path` en `adapter.in.rest.controller`.

Las seis entidades de dominio son Java puro. Las entidades JPA independientes y el mapper
viven en infraestructura. El mapper reconstruye agregados dentro de la transacción y no
expone proxies lazy. `reconstituir` restaura IDs y fechas sin ejecutar reglas de creación.

## Flujo de una peticion protegida

Ejemplo: `GET /api/events/{id}` con `Authorization: Bearer <jwt>`.

1. **SmallRye JWT** (`Hs256PrincipalFactory`, `ApiJwtAuthenticationMechanism`): el resource server valida firma HS256, `iss` y `exp`
   del JWT. Si falta o es invalido → `RestAuthenticationErrorHandler` responde `401` JSON.
   Si la ruta es `/api/admin/**` y la cuenta no tiene ADMIN actualmente → `403`.
   Las rutas de negocio requieren ORGANIZADOR y perfil activo.
2. `DatabaseJwtAuthenticationConverter` consulta la cuenta, rechaza cuentas eliminadas/inactivas
   y reconstruye los roles de `SecurityIdentity` desde los roles actuales en BD. El `JsonWebToken` queda en el contexto; las consultas de BD se ejecutan con `runBlocking`.
3. **`EventoController`** (adaptador de entrada) llama a `GetEventoPort.execute(id)`.
4. **`GetEventoUseCase`** pide el organizador actual a `CurrentOrganizadorPort` (implementado por
   `SecurityContextCurrentOrganizadorAdapter`, que busca `organizadores.usuario_id`
   usando el `sub` del JWT) y busca con
   `EventoRepositoryPort.findByIdAndOrganizadorId(id, organizadorId)`.
5. **`EventoPersistenceAdapter`** ejecuta la consulta JPA filtrando por `organizador_id`.
   Si el evento es de otro usuario no se encuentra → `EventoNotFoundException` → `404`.
6. El controller mapea la entidad a `EventoResponse` y la envuelve en `ApiResponse`.

## Flujo de registro / login

```
POST /api/auth/register ─► AuthController ─► RegisterPort (RegisterUseCase)
                                               ├─ UsuarioRepositoryPort.existsByCorreo      (409 si existe)
                                               ├─ RolRepositoryPort.findByNombre(ORGANIZADOR)
                                               ├─ PasswordHasherPort.hash      ◄── BCryptPasswordHasherAdapter
                                               ├─ TransactionPort: save de usuario + perfil + usuario_roles
                                               │  (Narayana en infraestructura; commit antes del JWT)
                                               └─ TokenProviderPort.generate   ◄── JwtTokenProviderAdapter (SmallRye JWT)

POST /api/auth/login ─► AuthController ─► LoginPort (LoginUseCase)
                                            ├─ findByCorreo + organizador activo + password_hash != null
                                            ├─ PasswordHasherPort.matches   (401 si falla)
                                            └─ TokenProviderPort.generate
```

El caso de uso nunca ve BCrypt ni JWT: solo puertos. Cambiar a otro algoritmo o a tokens
opacos = escribir otro adaptador, sin tocar `application`.

## Propiedad de los datos (multiusuario)

- `usuarios` guarda credenciales y tiene sus roles mediante `usuario_roles`. El perfil opcional
  `organizadores` tiene `id` propio, `usuario_id` único y `activo`. Eventos y capacidades
  referencian `organizadores.id`. `CurrentUsuarioPort` devuelve el `sub` del JWT;
  `CurrentOrganizadorPort` busca el perfil activo del usuario y devuelve su ID independiente.
- `UsuariosUseCase` implementa el CRUD administrativo y perfil propio. Usa `TransactionPort`
  para cambios atómicos, con bloqueo del rol ADMIN y del usuario a editar/eliminar.
  El último ADMIN habilitado no puede perder acceso y las cuentas con datos no pueden borrarse.
- **Todos** los casos de uso que reciben un id (evento o subtarea) filtran por el
  organizador del token: `findByIdAndOrganizadorId`, `existsByIdAndOrganizadorId`.
- Los listados (`/api/events`, `/api/today`, `/api/capacity`) ya filtraban por
  `CurrentOrganizadorPort`, que ahora resuelve al usuario autenticado en vez del antiguo
  organizador demo.

Modelo de datos completo en [schema.sql](./schema.sql):

```
usuarios 1───0..1 organizadores 1───* eventos 1───* subtareas
   │                         └───* capacidades_diarias
   └───* usuario_roles *───1 roles
```

## Como agregar un caso de uso nuevo

1. Puerto de entrada en `application/port/in` (interfaz `XxxPort`).
2. Implementacion en `application/usecase` (Java puro; si necesita el usuario actual, inyecta
   `CurrentOrganizadorPort` y filtra por el).
3. Si necesita algo externo, un puerto en `application/port/out` y su adaptador en
   `infrastructure/adapter/out/...` (o `infrastructure/security`).
4. Bean en `UseCaseConfig`.
5. Endpoint en un controller de `adapter/in/rest/controller` usando solo el puerto.
6. Si la ruta debe ser publica o restringida por rol, ajustala en los permisos HTTP de `application.properties`.
7. Test unitario del caso de uso con mocks de los puertos.

## Configuracion de seguridad

| Propiedad | Variable | Default |
|---|---|---|
| `app.security.jwt.secret` | `JWT_SECRET` | obligatoria, >= 32 bytes UTF-8 |
| `app.security.jwt.expiration-minutes` | `JWT_EXPIRATION_MINUTES` | `120` |
| `app.security.jwt.issuer` | - | `events-api` |

## Esquema y transacciones

`docs/schema.sql` crea el modelo para una base nueva; no realiza migraciones. Hibernate usa
`quarkus.hibernate-orm.schema-management.strategy=validate`. Las FK de eventos y capacidades restringen el borrado del organizador;
las FK de perfil y asignaciones de roles permiten eliminar una cuenta sin datos de negocio.
`NarayanaTransactionAdapter` implementa `TransactionPort` mediante `QuarkusTransaction`;
las capas de aplicación y dominio no dependen de Spring.

## Cierre de sesión

`POST /api/auth/logout` invoca `LogoutPort`/`LogoutUseCase`. `CurrentTokenPort` obtiene el
JWT autenticado y su expiración del contexto de seguridad; `TokenRevocationPort` persiste
su hash SHA-256 y expiración en `tokens_revocados`. Los puertos y el caso de uso no conocen
Spring ni JWT. El adaptador JDBC usa una transacción y un INSERT idempotente.

`DatabaseJwtAuthenticationConverter` consulta las revocaciones antes de aceptar la cuenta
y sus roles. JWT revocado devuelve 401. Cada emisión incluye un `jti` UUID distinto para
que dos sesiones creadas en el mismo segundo puedan cerrarse independientemente. La
revocación por hash también cubre tokens previos sin `jti`. No hay sesiones HTTP del servidor;
las revocaciones se comparten en PostgreSQL. Se eliminan expiradas al realizar logout,
respetando los 60 segundos de desfase del validador.
