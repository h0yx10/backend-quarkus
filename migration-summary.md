# events-api -> Quarkus Migration Summary

**Migration Date**: 2026-10-07
**Source**: Spring Boot 3.2.5 (Java 21)
**Target**: Quarkus 3.40.1 (Java 21)
**Strategy**: full-quarkus
**Overall Status**: 🚧 **88% Complete** (Backend: 100%, Frontend: not applicable; container verification pending)

## 2. Executive Summary

Migración implementada en `backend-quarkus`. El proyecto original permanece intacto.
Se conserva el comportamiento y los contratos del backend. Docker está preparado y falta
construir y ejecutar la imagen en un equipo con runtime de contenedores.

**Key Achievements**

- Dominio y aplicación Java puro; puertos y casos de uso conservados.
- Persistencia EntityManager, transacciones Narayana y aislamiento entre usuarios.
- JWT HS256 compatible, roles consultados en BD, BCrypt y logout persistente.
- 127 pruebas sin fallos; PostgreSQL desechable y arranque JVM verificados.
- Cobertura aplicación: 98.62% líneas y 86.73% ramas; umbrales 60%/50% conservados.
- 29/29 contratos HTTP coinciden con fixtures equivalentes, ignorando UUID, token y timestamps dinámicos: `migration-metadata/contract-parity.json`.

**Remaining Work**

- Ejecutar `docker compose up --build -d` y repetir pruebas HTTP contra el contenedor.

## 3. Migration Modules

| Module | Name | Status | Details |
|---|---|---|---|
| prerequisite | prerequisite | Complete | JDK 21 and Maven Wrapper 3.9.16 available; Docker/Podman unavailable |
| planning | planning | Complete | User-selected JVM, separate target, pure core, EntityManager, external Supabase and schema validation |
| build | build | Complete | BOM/plugin 3.40.1, compiler 3.15.0, Surefire/Failsafe 3.5.6; no Spring/Lombok dependencies |
| code | code | Complete | Six JPA entities moved to infrastructure; all ports and application sources preserved; CDI, Jakarta REST, Narayana, dynamic JWT authorization |
| testing | testing | Complete | verify passed: {'tests': 127, 'failures': 0, 'errors': 0, 'skipped': 0}; coverage {'BRANCH': 86.73, 'LINE': 98.62}; PostgreSQL 17.5 disposable tests; HTTP parity artifact |
| cleanup | cleanup | Complete | Source hashes unchanged; docs and configuration updated; obsolete Spring repositories replaced, no unmigrated code deleted |
| docker | docker | Pending | Multi-stage non-root JVM Dockerfile and app-only Compose prepared; execution pending runtime |
| reporting | reporting | Complete | Generated summary, execution metadata and verification evidence; token usage/cost unavailable |

## 4. Component Migration Status

### JPA Entities (6/6 - 100%)

| Entity | Table | Status | Notes |
|---|---|---|---|
| UsuarioEntity | usuarios | Complete | Roles y perfil opcional |
| OrganizadorEntity | organizadores | Complete | UUID independiente |
| RolEntity | roles | Complete | Bloqueo ADMIN |
| EventoEntity | eventos | Complete | Cascada subtareas |
| SubtareaEntity | subtareas | Complete | description preservada |
| CapacidadDiariaEntity | capacidades_diarias | Complete | Propietario y fecha |

**Migration Pattern**: entidades JPA de infraestructura y mapper; fábricas de reconstrucción Java puro. RevokedTokenEntity sigue en infraestructura.

### Repositories (6/6 - 100%)

| Repository | Entity | Status | Notes |
|---|---|---|---|
| UsuarioPersistenceAdapter | Usuario | Complete | Fetch, unique/FK, bloqueos |
| OrganizadorPersistenceAdapter | Organizador | Complete | Buscar por usuario |
| RolPersistenceAdapter | Rol | Complete | Pesimista ADMIN |
| EventoPersistenceAdapter | Evento | Complete | Filtrar propietario |
| SubtareaPersistenceAdapter | Subtarea | Complete | Sumas, orden, aislamiento |
| CapacidadDiariaPersistenceAdapter | CapacidadDiaria | Complete | Límite más reciente |

**Migration Pattern**: Spring Data sustituido por EntityManager detrás de los mismos puertos. TokenRevocationPersistenceAdapter usa JDBC e INSERT ON CONFLICT.

### Services (100%)

| Service | Status | Notes |
|---|---|---|
| Casos de uso de aplicación | Complete | Sin cambios funcionales ni anotaciones framework |
| TransactionPort | Complete | NarayanaTransactionAdapter |
| Puertos de identidad y JWT | Complete | SecurityIdentity y SmallRye JWT |

**Migration Pattern**: productores CDI en infraestructura; núcleo independiente.

### Controllers (7/7 - 100%)

| Controller | Endpoints | Status | Notes |
|---|---|---|---|
| AuthController | /api/auth | Complete | Registro/login/me/logout |
| AdminController | /api/admin/users | Complete | CRUD |
| EventoController | /api/events | Complete | CRUD/progreso |
| SubtareaController + EventSubtasksController | /api/subtasks; /api/events/{eventId}/subtasks | Complete | Recurso separado para resolver rutas anidadas |
| ConflictController | /api/subtasks/{id}/conflicts/overload | Complete | Previsualización |
| CapacidadController | /api/capacity | Complete | GET/PUT |
| TodayController | /api/today | Complete | Filtros y agrupación |

**Migration Pattern**: Jakarta REST, @Blocking, ApiResponse, mappers de errores explícitos.

### Configuration (100%)

| Configuration | Status | Notes |
|---|---|---|
| application.properties | Complete | Env externas, validate, perfiles |
| CDI/Jackson/OpenAPI | Complete | Productores, campos desconocidos, ISO |
| Seguridad | Complete | HS256 bytes UTF-8, roles actuales, JSON 401/403 |
| Health y Swagger | Complete | /q/health; compatibilidad actuator/swagger-ui.html |

**Migration Pattern**: configuración Quarkus nativa. No frontend ni templates.

## 5. Technology Mapping

### Framework

- Spring Boot → Quarkus JVM.

### Persistence

- Spring Data JPA → Hibernate ORM + EntityManager.
- TransactionTemplate → QuarkusTransaction/Narayana.
- JdbcTemplate → DataSource/JDBC Agroal.

### Web Layer

- Spring MVC/MockMvc → Jakarta REST/REST Assured.
- Springdoc → SmallRye OpenAPI.

### Dependency Injection

- @Bean y constructores Lombok → @Produces e inyección con constructores explícitos.

### Configuration

- YAML Spring → application.properties y perfiles Quarkus.

### Security

- Spring Security → SmallRye JWT + SecurityIdentityAugmentor.
- BCryptPasswordEncoder → Elytron BcryptUtil.
- Validación HS256 mantiene exactamente los bytes UTF-8, issuer y 60 s de margen.
- Los roles del token no autorizan: se reconstruyen desde BD en cada petición.

## 6. Build & Deployment

`./mvnw -Ppostgres-it verify` completó correctamente con DB desechable y JDK 21.
El perfil normal `./mvnw verify` usa H2 para las pruebas de framework.

```bash
./mvnw quarkus:dev
./mvnw package
java -Duser.timezone=America/Bogota -jar target/quarkus-app/quarkus-run.jar
# En un equipo con Docker:
docker compose up --build -d
```

BOM y plugins contrastados con la referencia oficial `maven-3.40.1`; Java 21 se conserva por elección del usuario. Evidencia: `migration-metadata/build-reference-comparison.json`.

Distribuir toda `target/quarkus-app/`. Dockerfile multi-stage JDK/JRE21, usuario10001,
healthcheck HTTP. Compose conecta a Supabase externa; no aprovisiona ni modifica esa BD.
Docker build/run no ejecutados: Docker y Podman no están instalados.

## 7. Database

SQL explícito `docs/schema.sql`; ejecutar solamente en base nueva. No se migran datos.
Se comprobó PostgreSQL17.5 local desechable, incluidas concurrencia, bloqueos, FK y rollback.
Ninguna prueba conectó a Supabase. El esquema incluye tokens_revocados.

```properties
quarkus.datasource.jdbc.url=${DB_URL}
quarkus.datasource.username=${DB_USERNAME}
quarkus.datasource.password=${DB_PASSWORD}
quarkus.hibernate-orm.schema-management.strategy=validate
quarkus.hibernate-orm.sql-load-script=no-file
```

Primer ADMIN: registrar cuenta y aplicar SQL documentado en docs/auth.md.

## 8. Migration Reports

1. [Reporte prerequisite](migration-reports/prerequisite-report.json): 1/1 comprobaciones.
2. [Reporte planning](migration-reports/planning-report.json): 1/1 comprobaciones.
3. [Reporte build](migration-reports/build-report.json): 1/1 comprobaciones.
4. [Reporte code](migration-reports/code-report.json): 1/1 comprobaciones.
5. [Reporte testing](migration-reports/testing-report.json): 1/1 comprobaciones.
6. [Reporte cleanup](migration-reports/cleanup-report.json): 1/1 comprobaciones.
7. [Reporte docker](migration-reports/docker-report.json): 0/1; ejecución pendiente.
8. [Reporte reporting](migration-reports/reporting-report.json): 1/1 comprobaciones.

Evidencias: migration-metadata/verify.log, verification.json, dependencies.txt,
contract-parity.json, jvm-startup.log y target/site/jacoco/index.html.

## 9. Next Steps

### Immediate

1. Configurar variables externas y verificar el esquema de destino antes de arrancar.
2. Construir y ejecutar Docker; comprobar health y contratos HTTP.

### Short Term

1. Integrar `verify` y PostgreSQL desechable en CI.

### Long Term

1. Mantener pruebas de contratos al evolucionar endpoints.

## 10. Key Migration Patterns

Entidad anterior:

```java
@Entity class Usuario { @Id UUID id; }
```

Entidad y núcleo actuales:

```java
// infrastructure
@Entity class UsuarioEntity { @Id UUID id; }
// domain: sin JPA
class Usuario { private UUID id; /* reconstituir(...) */ }
```

Repositorio anterior:

```java
interface JpaUsuarioRepository extends JpaRepository<Usuario, UUID> { }
```

Adaptador actual:

```java
@ApplicationScoped @Transactional
class UsuarioPersistenceAdapter implements UsuarioRepositoryPort {
    @Inject EntityManager em;
    // consultas y mapeo dentro de la transacción
}
```

REST anterior/actual:

```java
// Antes: @RestController @GetMapping
// Ahora: @Path, @GET, @Blocking; mismo puerto y ApiResponse
```

## 11. Lessons Learned

### What Went Well

1. Puertos y casos de uso independientes permitieron conservar reglas y pruebas.
2. PostgreSQL desechable comprobó SQL real y concurrencia sin runtime Docker.
3. Fixtures HTTP detectaron diferencias que las pruebas unitarias no cubrían.

### Challenges

1. Rutas anidadas requieren recursos Jakarta REST separados.
2. SmallRye requiere clave HS256 explícita para conservar bytes de secretos previos.
3. Authentication challenge, validación y UUID necesitan mappers para mantener mensajes.
4. Evitar proxies lazy fuera de la transacción mediante mapeo y unproxy explícitos.

## 12. Resources

- [Quarkus guides](https://quarkus.io/guides/)
- [REST](https://quarkus.io/guides/rest/)
- [JWT](https://quarkus.io/guides/security-jwt/)
- [Security customization](https://quarkus.io/guides/security-customization/)
- [Hibernate ORM](https://quarkus.io/guides/hibernate-orm/)
- [Transactions](https://quarkus.io/guides/transaction/)
- [Coverage](https://quarkus.io/guides/tests-with-coverage/)
- [OpenAPI](https://quarkus.io/guides/openapi-swaggerui/)

## 13. Conclusion

Backend migrado y verificado en JVM conservando arquitectura, datos y contratos.
Los archivos Docker están listos; la aceptación de contenedor queda pendiente de un runtime.
No se modificó el proyecto fuente ni se migró ninguna base existente.

---
**Generated by**: Codex (GPT-6)
**Date**: 2026-10-07
**Version**: 1.0
**Token usage**: unavailable / unavailable (cost unavailable)
