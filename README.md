# Comunications Backend

Servicio REST para la automatización de notificaciones de pago y contratos del sistema **Motos del Caribe**.

## Stack

- Java 21
- Spring Boot 4.0.3 (Web, Data JPA, Validation)
- MySQL 8 (Azure Database for MySQL)
- Maven
- Docker

## Requisitos

- JDK 21
- Maven 3.9+
- MySQL accesible (con las vistas `vw_sv_all_motos_semanal` y `vw_gd_recaudo_bruto`)

## Configuración

Las credenciales se leen desde variables de entorno. Copia `.env.example` como `.env` y completa los valores:

```bash
cp .env.example .env
```

Variables requeridas:

| Variable      | Descripción                         |
|---------------|-------------------------------------|
| `DB_URL`      | JDBC URL de MySQL                   |
| `DB_USERNAME` | Usuario de la base de datos         |
| `DB_PASSWORD` | Contraseña de la base de datos     |

## Ejecución local

```bash
./mvnw spring-boot:run
```

La aplicación arranca en `http://localhost:8080`.

## Docker

```bash
docker build -t comunications-backend .
docker run --rm -p 8080:8080 --env-file .env comunications-backend
```

## Endpoints principales

| Método | Ruta                       | Descripción                                       |
|--------|----------------------------|---------------------------------------------------|
| POST   | `/save/notification`       | Registra una notificación enviada                 |
| POST   | `/save/error-notification` | Registra una notificación que no pudo enviarse   |
| GET    | `/contracts/next-to-pay`   | Contratos próximos a pagar con mensaje de cobro   |
| GET    | `/get/notifications?id=`   | Notificaciones por número de contrato             |

### Documentación interactiva

- Swagger UI: `http://localhost:8080/swagger-ui.html`
- OpenAPI JSON: `http://localhost:8080/v3/api-docs`

## Estructura del proyecto

```
src/main/java/com/automatization/comunications
├── controller    # Endpoints REST
├── service       # Lógica de negocio
├── repository    # Spring Data JPA
├── model
│   ├── entity    # Entidades JPA (incluye vistas de MySQL)
│   └── dto       # DTOs / records de entrada y salida
└── exception     # Manejo global de errores
```

## Tests

```bash
./mvnw test
```

## Mejoras implementadas

Esta sección documenta las mejoras aplicadas al proyecto en el ciclo de *quick wins*. Cada una llegó en una rama independiente con su propio PR.

### 1. Credenciales externalizadas (`fix/externalize-db-credentials`)

- **Antes:** `application.properties` tenía hardcodeados `spring.datasource.username=Sebastian` y `spring.datasource.password=Good9251`, expuestos en el historial de git.
- **Después:** las credenciales se leen desde las variables de entorno `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`. Se agregó `.env.example` como plantilla y `.env` quedó ignorado en `.gitignore`.
- **Impacto:** elimina el secreto del código fuente y permite rotarlo sin recompilar.

### 2. Dependencias saneadas (`chore/remove-unused-deps`)

- **Antes:** `pom.xml` declaraba `spring-boot-starter-data-rest`, `spring-boot-starter-restclient`, `spring-boot-starter-data-rest-test` y `spring-boot-starter-restclient-test`, ninguno usado por el código, y no declaraba explícitamente `web`, `validation` ni `test`.
- **Después:** se reemplazaron por los starters que realmente respaldan el código: `spring-boot-starter-web`, `spring-boot-starter-validation` y `spring-boot-starter-test`. Se eliminó el record `ContractDto` que no tenía referencias.
- **Impacto:** build más pequeño, menos superficie de vulnerabilidades y dependencias alineadas con el uso real.

### 3. Manejo global de excepciones (`fix/exception-handling`)

- **Antes:** los endpoints POST atrapaban `Exception` y devolvían `201 CREATED` incluso en caso de error, sin loguear el stack trace. Los GET devolvían `500` genérico para cualquier problema.
- **Después:** se introdujo `GlobalExceptionHandler` (`@RestControllerAdvice`) con handlers tipados para `MethodArgumentNotValidException`, `ConstraintViolationException`, `MissingServletRequestParameterException`, `MethodArgumentTypeMismatchException`, `DataAccessException` y `Exception`. Los controladores quedaron libres de try/catch y los POST devuelven el `201` correcto en éxito.
- **Impacto:** respuestas JSON consistentes con `timestamp`, `status`, `error`, `message` y `details`; trazas guardadas en el log del servidor; clientes pueden distinguir éxito de fallo.

### 4. Validación de entrada (`feat/input-validation`)

- **Antes:** el query param `id` en `GET /get/notifications` aceptaba cualquier valor sin validación.
- **Después:** `NotificationController` está anotado con `@Validated` y el parámetro `id` lleva `@NotBlank` + `@Pattern("\\d+")`. Los DTOs ya tenían validaciones; ahora también se honran los query params.
- **Impacto:** entradas inválidas responden `400 Bad Request` con mensaje explicativo en lugar de llegar a la capa de datos.

### 5. Actuator y healthcheck (`feat/actuator-healthcheck`)

- **Antes:** no había endpoint de salud; el `HEALTHCHECK` del `Dockerfile` no existía.
- **Después:** se agregó `spring-boot-starter-actuator` exponiendo `/actuator/health` e `/actuator/info`, con probes de *liveness*/*readiness* habilitados. El `Dockerfile` incluye `HEALTHCHECK` apuntando a `/actuator/health`.
- **Impacto:** orquestadores (Docker, Kubernetes, Azure App Service) pueden detectar instancias caídas y reiniciarlas automáticamente.

### 6. Documentación del proyecto (`docs/readme`)

- **Antes:** no había `README.md`; la información de setup, endpoints y estructura solo existía en la cabeza del equipo.
- **Después:** este documento cubre stack, requisitos, configuración, ejecución local, Docker, endpoints, estructura y tests.
- **Impacto:** onboarding más rápido y menor dependencia de conocimiento tribal.

### Resumen de los cambios

| Área                    | Antes                                 | Después                                      |
|-------------------------|---------------------------------------|----------------------------------------------|
| Credenciales BD         | Hardcodeadas en `.properties`        | Variables de entorno + `.env.example`        |
| Dependencias Maven      | Starters no usados / faltantes        | Solo los necesarios, declarados explícitos  |
| Manejo de errores       | `try/catch` con status incorrectos    | `@RestControllerAdvice` global tipado        |
| Validación de query     | Sin validación                        | `@NotBlank` + `@Pattern` con mensajes claros |
| Healthcheck             | Inexistente                           | Actuator + `HEALTHCHECK` en Dockerfile       |
| Documentación           | Sin README                            | README con setup, endpoints y mejoras        |

### Próximos pasos sugeridos

Refactors mayores pendientes para un siguiente ciclo:

- **Spring Security** con autenticación/autorización en los endpoints.
- **Cobertura de tests** (unitarios, integración y end-to-end) — actualmente solo existe `contextLoads()`.
- **Reemplazar `Object[]`** de la query nativa por un DTO tipado.
- **Flyway/Liquibase** para versionar el esquema y las vistas de MySQL.
- **Paginación** en `/contracts/next-to-pay` para evitar cargar la tabla completa.
- **Internacionalización** de los mensajes de notificación (hoy hardcodeados en español).
