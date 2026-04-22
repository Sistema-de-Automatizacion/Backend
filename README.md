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
- Maven 3.9+ (o usar `./mvnw` que ya viene con el proyecto)
- Acceso a la BD espejo de producción (pedir credenciales a Juan). Las queries esperan el schema `db_packgps` con las vistas `vw_sv_all_motos_semanal` y `vw_gd_recaudo_bruto`.

## Setup local (onboarding)

Si recién clonaste el repo y querés levantar todo en tu máquina:

```bash
git clone https://github.com/Sistema-de-Automatizacion/Backend.git
cd Backend
cp .env.example .env
# abrir .env y reemplazar los CHANGE_ME con los valores reales (pedirlos a Juan)
./mvnw spring-boot:run
```

Verificación rápida:

```bash
curl http://localhost:8080/actuator/health      # debe responder {"status":"UP"}
curl -H "X-API-Key: <la-key-del-.env>" http://localhost:8080/contracts/next-to-pay
```

> 📌 La BD contra la que se conecta localmente es la **espejo** de producción (no la fuente original), por eso es seguro ejecutar SELECT sin miedo a afectar datos reales. Las credenciales no se comitean al repo (el `.env` está en `.gitignore`).

## Configuración

Las credenciales y la configuración sensible se leen desde variables de entorno. Copia `.env.example` como `.env` y completa los valores:

```bash
cp .env.example .env
```

Variables soportadas:

| Variable                    | Requerida | Descripción                                                            |
|-----------------------------|-----------|------------------------------------------------------------------------|
| `DB_URL`                    | ✅        | JDBC URL de MySQL                                                      |
| `DB_USERNAME`               | ✅        | Usuario de la base de datos                                            |
| `DB_PASSWORD`               | ✅        | Contraseña de la base de datos                                         |
| `app.api-key`               | ✅        | Token compartido (≥32 chars) que protege todos los endpoints           |
| `app.cors.allowed-origins`  | ⚠️        | Orígenes permitidos separados por coma. Default `*`. En prod: el dominio exacto del frontend (p. ej. `https://fronted-2rrf.onrender.com`) |

Spring Boot carga el `.env` automáticamente gracias a `spring.config.import=optional:file:.env[.properties]`, así que no hace falta exportar nada en la shell: basta con tener el archivo en la raíz del proyecto.

## Ejecución local

```bash
./mvnw spring-boot:run
```

La aplicación arranca en `http://localhost:8080`. Todas las peticiones (salvo `/actuator/health` y `/actuator/info`) requieren el header `X-API-Key`.

## Docker

```bash
docker build -t comunications-backend .
docker run --rm -p 8080:8080 --env-file .env comunications-backend
```

## Endpoints principales

Todos los endpoints requieren el header `X-API-Key: <token>` excepto los marcados como públicos.

| Método | Ruta                             | Auth | Descripción                                                     |
|--------|----------------------------------|------|-----------------------------------------------------------------|
| GET    | `/actuator/health`               | ❌   | Health check (público para probes de Azure)                     |
| GET    | `/actuator/info`                 | ❌   | Metadata de la aplicación                                       |
| POST   | `/save/notification`             | ✅   | Registra una notificación enviada                               |
| POST   | `/save/error-notification`       | ✅   | Registra una notificación que no pudo enviarse                  |
| GET    | `/contracts/next-to-pay`         | ✅   | Contratos con deuda pendiente (mora o recordatorio) + mensaje   |
| GET    | `/contracts/paid-today`          | ✅   | Clientes con pago semanal registrado hoy + mensaje de confirmación |
| GET    | `/get/notifications?id=`         | ✅   | Notificaciones por número de contrato                           |
| GET    | `/notifications/all?page=&size=` | ✅   | Historial completo paginado de notificaciones enviadas          |
| GET    | `/notifications/errors/all?page=&size=` | ✅ | Historial completo paginado de notificaciones fallidas    |

### Ejemplo de request autenticado

```bash
curl -H "X-API-Key: $APP_API_KEY" \
     https://motos-del-caribe-exfsh8ekghg9bba5.mexicocentral-01.azurewebsites.net/contracts/next-to-pay
```

Respuestas:
- `200` con JSON cuando la key es válida
- `401 Unauthorized` con `{"status":401,"error":"Unauthorized","message":"..."}` si falta o es incorrecta

### Documentación interactiva

- Swagger UI: `http://localhost:8080/swagger-ui.html` (también protegido por API key)
- OpenAPI JSON: `http://localhost:8080/v3/api-docs`

## Estructura del proyecto

```
src/main/java/com/automatization/comunications
├── controller    # Endpoints REST
├── service       # Lógica de negocio
├── repository    # Spring Data JPA
├── config        # CorsConfig, ApiKeyFilter, SecurityConfig
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

### 7. Carga automática de `.env` (`feat/spring-config-import-env`)

- **Antes:** correr `./mvnw spring-boot:run` exigía exportar `DB_URL`, `DB_USERNAME`, `DB_PASSWORD` en la shell o la app fallaba al arrancar con *Could not resolve placeholder*.
- **Después:** `application.properties` incluye `spring.config.import=optional:file:.env[.properties]`. Si existe un archivo `.env` en la raíz del proyecto, Spring lo carga como property source. El `optional:` evita que falle cuando no existe (ej. en producción donde vienen variables reales del entorno).
- **Impacto:** onboarding de un clone nuevo en un solo paso, sin fricción de variables de entorno manuales.

### 8. CORS + endpoints paginados para el dashboard (`feat/cors-and-list-endpoints`)

- **Antes:** el backend rechazaba llamadas de otro origen por CORS, y no había forma de listar todas las notificaciones (solo por contrato individual).
- **Después:** se agregó `CorsConfig` con un `CorsFilter` configurable via `app.cors.allowed-origins`, y dos nuevos endpoints `GET /notifications/all` y `GET /notifications/errors/all` con paginación `page`/`size` validada por `@Min`/`@Max` y ordenamiento descendente por ID.
- **Impacto:** habilita el frontend web (Render Static Site) para consumir la API desde otro dominio y mostrar el historial completo con paginación visual.

### 9. Autenticación con API Key (`feat/api-key-auth`)

- **Antes:** todos los endpoints eran públicos. Cualquiera con la URL del Azure App Service podía leer contratos, insertar notificaciones o borrar datos.
- **Después:** se agregó `ApiKeyFilter` (`OncePerRequestFilter`) registrado via `SecurityConfig` que valida el header `X-API-Key` en cada request usando comparación de tiempo constante contra la property `app.api-key`. `/actuator/health`, `/actuator/info` y los preflight CORS `OPTIONS` siguen públicos; todo lo demás responde `401 Unauthorized` con JSON explícito si falta la key o es inválida.
- **Impacto:** cierra el agujero crítico de seguridad #2 del code review inicial. Tanto n8n como el frontend de Render se autentican con la misma key compartida via el header `X-API-Key`.

### 10. CI del deploy (`fix/disable-context-test`)

- **Antes:** el workflow de GitHub Actions que Azure creó para el App Service fallaba en `mvn clean install` porque el único test existente era un `@SpringBootTest` que requería MySQL, y CI no tiene BD.
- **Después:** `ComunicationsApplicationTests` está marcado con `@Disabled` hasta que se escriban tests de unidad con Mockito que no necesiten datasource.
- **Impacto:** el pipeline de deploy a Azure vuelve a pasar verde en cada push a `main`.

### 11. Plantillas aprobadas por Meta + endpoint de pagos del día

- **Antes:** los mensajes de cobro se armaban por concatenación de strings y solo cubrían dos plantillas (recordatorio / mora). No había forma de confirmar al cliente un pago recién registrado.
- **Después:** los 3 mensajes (`notificacion_mora`, `recordatorio_cuota`, `pago_recibido`) están definidos como text blocks en constantes y se construyen con helpers dedicados (`buildMoraMessage`, `buildReminderMessage`, `buildPaymentReceivedMessage`), respetando exactamente las plantillas aprobadas por Meta para WhatsApp Business. Se agregó el endpoint `GET /contracts/paid-today` que lista los clientes con un pago registrado hoy.
- **Impacto:** mensajes legibles, trazables y fácilmente auditables contra lo aprobado; el dashboard puede mostrar en una sección aparte los pagos del día.

### 12. Unidades monetarias consistentes (todo en pesos en el DTO)

- **Antes:** `paymentContract` viajaba en "miles" sin multiplicar, mientras que `paymentPayout` y `accumulatedDebt` se multiplicaban ×1000 antes de salir al DTO. El frontend volvía a multiplicar por 1000 en `renderContracts`, inflando los montos 1000× en la UI y rompiendo el filtro "pagó exactamente la cuota" (restaba miles contra pesos).
- **Después:** todo campo monetario del `ContractAndPayoutDto` (`paymentContract`, `paymentPayout`, `accumulatedDebt`, `debt`) se almacena en pesos desde el service. Los helpers de mensaje reciben los valores en pesos sin re-multiplicar, y el frontend solo les aplica `Intl.NumberFormat("es-CO", {style:"currency"})`.
- **Impacto:** los mensajes y la UI muestran las cifras correctas; los filtros y comparaciones operan sobre la misma unidad.

### 13. Calificación de vistas con el schema `db_packgps`

- **Antes:** las queries nativas y las anotaciones `@Table` referenciaban `vw_sv_all_motos_semanal` y `vw_gd_recaudo_bruto` sin schema, lo que funcionaba solo si la BD default del `DB_URL` era donde vivían las vistas.
- **Después:** las vistas están calificadas como `db_packgps.vw_sv_all_motos_semanal` y `db_packgps.vw_gd_recaudo_bruto` en las queries de `IRepositoryContract` y en `@Table(name="...", schema="db_packgps")` de las entidades.
- **Impacto:** el backend sigue resolviendo las vistas aunque el `DB_URL` apunte a otra base (por ejemplo, la BD espejo de producción donde la default no es `db_packgps`).

### 14. Integración con la BD productiva: mensajes por caso real

- **Antes:** la lógica asumía un JOIN con `vw_gd_recaudo_bruto` para cada contrato, replicaba la fila por cada pago histórico del cliente (un contrato con 5 pagos producía 5 filas duplicadas), y la fecha de vencimiento del mensaje de recordatorio tomaba `fecha_semanal` tal cual — que es un identificador de semana, no la fecha efectiva de pago del cliente. Adicionalmente `paymentPayout` sumaba pagos diarios (`empresa='RENTAYA_D'`) que el sistema no proyecta en `vw_sv_all_motos_semanal` porque son de cobro diario, no semanal.
- **Después:** `findAllPayoutAndContract` trae **una sola fila por contrato**: la de la semana más reciente (`fecha_semanal <= CURRENT_DATE`) vía subquery con `MAX(fecha_semanal) GROUP BY contrato`. La deuda viene directo de `c.deuda_cli` (que ya es neto) y la mora arrastrada se calcula como `deuda_cli - cuota`. `findClientsPaidByDate` agrega `p.Recaudo > 0 AND (p.empresa IS NULL OR p.empresa <> 'RENTAYA_D')` para excluir pagos diarios y pagos cero. Y la fecha del mensaje de recordatorio se calcula dinámicamente en Java (`computeClientDueDate`): lunes de la semana actual + offset según `dia_canon` del cliente (`Mar→+1`, `Mier→+2`, `Jue→+3`, …).
- **Decisión de mensaje:** si `deuda_cli > cuota` (hay mora arrastrada) → MORA; si `deuda_cli == cuota` → RECORDATORIO; si `deuda_cli == 0` → el contrato no aparece. Se usan `if`/`else if` para que mora tenga prioridad sobre recordatorio cuando ambos aplican.
- **Filtro de contratos activos:** se descartan los que no tienen `estado_semana` vacío o nulo, eliminando `CANCELADO*`, `06-Alistamiento`, `40-Desistalación de GPS`, `60-taller por vender`, etc.
- **Impacto:** pasa de 10.800+ filas duplicadas a 1.331 contratos únicos notificables; los montos y fechas reflejan la realidad del cliente; n8n puede consumir el endpoint sin lógica extra de deduplicación.

### Resumen de los cambios

| Área                    | Antes                                 | Después                                             |
|-------------------------|---------------------------------------|-----------------------------------------------------|
| Credenciales BD         | Hardcodeadas en `.properties`        | Variables de entorno + `.env.example` + autoload    |
| Dependencias Maven      | Starters no usados / faltantes        | Solo los necesarios, declarados explícitos         |
| Manejo de errores       | `try/catch` con status incorrectos    | `@RestControllerAdvice` global tipado               |
| Validación de query     | Sin validación                        | `@NotBlank` + `@Pattern` con mensajes claros        |
| Healthcheck             | Inexistente                           | Actuator + `HEALTHCHECK` en Dockerfile              |
| CORS                    | No configurado                        | `CorsFilter` controlado por env var                 |
| Listado de notificaciones | Sólo filtrado por contrato          | Endpoints `/notifications/all` y `.../errors/all` paginados |
| Autenticación           | Endpoints 100% públicos              | `X-API-Key` filter con `app.api-key`                |
| CI de deploy            | Fallaba en el test por falta de BD    | Test deshabilitado hasta tener suite real          |
| Documentación           | Sin README                            | README con setup, endpoints, mejoras y seguridad    |
| Mensajes de cobro       | Strings concatenados, 2 casos         | 3 text blocks aprobados por Meta + builders aislados |
| Pagos del día           | No existía                            | `GET /contracts/paid-today` con mensaje de confirmación |
| Unidades monetarias     | Miles y pesos mezclados en DTO        | Todo en pesos, frontend sin multiplicar             |
| Schema de vistas        | Resolución implícita por `DB_URL`    | `db_packgps.vw_*` explícito en queries y `@Table`   |
| Duplicados por contrato | 5-8 filas por cliente (JOIN con pagos)| Una fila por contrato (última semana vigente)      |
| Fecha del recordatorio  | `c.fecha_semanal` (un lunes opaco)    | Calculada en Java según `dia_canon` del cliente     |
| Filtro de contratos     | Solo `estado_semana=JUSTIFICADO`      | Solo `estado_semana` vacío/null (vigentes)         |
| Filtro de pagos         | Cualquier recaudo                      | `Recaudo > 0` y `empresa <> 'RENTAYA_D'`            |

### Próximos pasos sugeridos

Refactors mayores pendientes para un siguiente ciclo:

- **Rotar las credenciales de MySQL** que aún están en el historial de git.
- **Cobertura de tests real** (unitarios con Mockito, integración con Testcontainers) para reactivar el `contextLoads()`.
- **Reemplazar `Object[]`** de la query nativa por un DTO tipado.
- **Flyway/Liquibase** para versionar el esquema y las vistas de MySQL.
- **Paginación** en `/contracts/next-to-pay` para evitar cargar la tabla completa.
- **Idempotencia** en los POST (`unique(num_contract, day_remember)` + `409 Conflict`) para cubrir reintentos de n8n.
- **Internacionalización** de los mensajes de notificación (hoy hardcodeados en español).

## Despliegue en producción

El backend está desplegado en **Azure App Service** (`motos-del-caribe`) en la región México Central, con CI/CD vía GitHub Actions (`.github/workflows/main_motos-del-caribe.yml`) que se dispara en cada push a `main`. Las variables de entorno de producción (`DB_URL`, `DB_USERNAME`, `DB_PASSWORD`, `app.cors.allowed-origins`, `app.api-key`) se configuran en el portal de Azure → App Service → Environment variables.

El frontend vive en un repo aparte ([`Sistema-de-Automatizacion/Fronted`](https://github.com/Sistema-de-Automatizacion/Fronted)) y se despliega como Static Site en Render.
