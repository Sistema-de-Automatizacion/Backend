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
