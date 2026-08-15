# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Estado del proyecto

`house-search` es un clon arquitectónico de un proyecto hermano (`job-search`, no incluido en este repo) adaptado al dominio de búsqueda de casas en venta/alquiler. Corre **100% en local**, sin desplegar nada en la nube. Monorepo con tres partes independientes (`backend/`, `frontend/`, `scraper/`) más infraestructura en la raíz. Fase 1 (modelo de datos + backend CRUD + frontend navegable + scraper con fuentes en stub) ya está implementada; el scoring real con Claude y el scraping real de Idealista quedan documentados como próximas fases más abajo.

Un solo usuario, sin sistema de autenticación de usuarios (solo un filtro de API key para dos endpoints sensibles).

## Comandos

Orden de arranque completo (cuatro terminales):

```bash
docker compose up -d                       # Postgres, desde la raíz (host :5433)

cd backend && ./mvnw spring-boot:run        # backend en :8081

cd frontend && npm install && ng serve      # frontend en :4201 (proxy /api -> :8081)
                                             # copia src/environments/environment.example.ts
                                             # a environment.ts si no existe (ya viene creado en este repo)

cd scraper && npm install                   # el scraper NO se lanza a mano normalmente:
                                             # lo dispara el botón "Ejecutar búsqueda" del frontend
                                             # a través de POST /api/search/run
```

**Puertos no estándar, a propósito**: esta máquina también tiene corriendo el proyecto hermano `job-search` (Postgres en `5432`, backend en `8080`, `ng serve` en `4200`). Para no interferir con esos procesos, `house-search` usa `5433` / `8081` / `4201` — ver `docker-compose.yml`, `backend/src/main/resources/application.properties` (`server.port`, `spring.datasource.url`, `app.cors.allowed-origin`) y `frontend/angular.json` (`architect.serve.options.port`) / `frontend/proxy.conf.json`. Si en tu máquina esos puertos están libres, puedes volver a los estándar sin más cambios.

### Backend (`backend/`)

```bash
./mvnw spring-boot:run
./mvnw test
./mvnw test -Dtest=NombreClase
./mvnw test -Dtest=NombreClase#metodoTest
./mvnw compile                              # compilación rápida sin tests
./mvnw clean verify
```

### Frontend (`frontend/`)

```bash
ng serve                                    # dev server en :4201 con proxy a :8081
ng build --configuration development        # build rápido para detectar errores de TS
ng test                                     # Karma/Jasmine
```

### Scraper (`scraper/`)

```bash
node index.js                               # ejecución manual para depurar (normalmente la dispara el backend)
```

## Arquitectura

### Backend — Spring Boot 4.1.0, Java 21, paquete `dev.oscar2ia.housesearch`

Paquetes por capa técnica (no por feature), calcados del patrón de `job-search`:

- `model` / `model.enums` — entidades JPA (Lombok `@Getter @Setter`, sin `@Data`) y enums de dominio.
- `repository` — `JpaRepository` + `@Query` JPQL con filtros opcionales `(:param IS NULL OR campo = :param)`.
- `service` — lógica de negocio, inyección por constructor.
- `web` — `@RestController`, `WebConfig` (CORS) y `ApiExceptionHandler`.
- `dto` — records inmutables con método estático `from(entidad)`.
- `security` — `ApiKeyFilter` + `SecurityFilterConfig` (protección por API key, **no** Spring Security).
- `config` — `AnthropicClientConfig` (bean `AnthropicClient`).

**Modelo de datos** (todas las tablas en snake_case): `house`, `house_match_score` (1:1 con `house`, PK compartida vía `@MapsId`), `selected_house`, `selected_house_status_change` (log de auditoría de cambios de estado de gestión), `profile_config` (+ `profile_config_palabra_clave` y `profile_config_filtro_negativo` como `@ElementCollection`), `search_execution` (fila única con `id` fijo = 1).

Decisiones de diseño no obvias:
- **`SelectedHouse` no tiene una entidad 1:1 separada para el estado de gestión** (a diferencia del patrón `Offer`/`OfferApplication` del proyecto original): el campo `estadoGestion` vive directamente en `SelectedHouse`, porque aquí no hay dos conceptos de negocio distintos que separar.
- **`Orientacion`, `Calefaccion`, `TipoCalefaccion` y `Climatizacion` son `String` libres**, no enums: vienen de HTML scrapeado de portales inmobiliarios con formato inconsistente. Los enums (`EstadoCasa`, `Fuente`, `Prioridad`, `EstadoGestionCasa`, `EstadoBusqueda`) sí son `@Enumerated(STRING)` porque los controla la propia aplicación.
- **`puntuacion` no existe como columna en `House`**: vive solo en `HouseMatchScore` (relación 1:1); el listado hace el join en `HouseService.listarCasas`.
- **Deduplicación de casas**: hash SHA-256 de título+ubicación normalizados (sin acentos, minúsculas, espacios colapsados), columna única `house.id_titulo_ubicacion` — ver `HouseService.calcularIdTituloUbicacion`.
- **`SearchRunnerService`** lanza el scraper Node.js como `ProcessBuilder` en un hilo daemon (no bloquea el request HTTP), con lock de instancia, límite de una ejecución al día persistido en `search_execution.fecha_inicio` (sobrevive a reinicios), timeout de 30 minutos y captura de `Throwable` como red de seguridad. No hay variante "sin Playwright": todas las fuentes de casas la requieren por igual.
- **`HouseScoringService`** puntúa cada casa nueva con la API de Claude (`claude-opus-5`, salida estructurada vía `outputConfig(HouseScoreResult.class)`) en un hilo daemon lanzado desde `HouseService.insertarCasas` tras el insert masivo. Si falla el scoring de una casa concreta, esa casa simplemente se queda sin fila en `house_match_score` (puntuación "—" en el listado) — nunca aborta el lote.
- **Seguridad**: filtro de servlet puro (`ApiKeyFilter extends HttpFilter`), registrado vía `FilterRegistrationBean` **solo** en `POST /api/house/insert` y `POST /api/search/run` (header `X-API-Key`). El resto de endpoints están abiertos (app de un único usuario en local).

**Endpoints** (rutas en singular, a diferencia del proyecto original):
```
POST   /api/search/run                (API key)
GET    /api/search/status
POST   /api/house/insert              (API key, bulk)
GET    /api/house?scoreMin=&tamanoMinimo=&fuente=&precioMaximo=
GET    /api/house/{id}
POST   /api/house/clear
DELETE /api/house/{id}
POST   /api/selected_house/{id}/copy
GET    /api/selected_house?estado=&scoreMin=&fuente=
PATCH  /api/selected_house/{id}/status
DELETE /api/selected_house/{id}/delete
GET    /api/profile-config
PUT    /api/profile-config
```

### Frontend — Angular 20 standalone + Angular Material 20 (Material 3)

Sin NgModules, sin NgRx. Cuatro rutas lazy (`loadComponent`) bajo `src/app/features/`: `casas-encontradas` (vista principal, tabla con columnas de ancho fijo + fila de detalle expandible + botones Ejecutar búsqueda/Actualizar/Limpiar + acciones Seleccionar/Ver detalles), `casas-seleccionadas` (tabla + cambio de estado de gestión inline + borrar por fila), `configuracion-busqueda` (formulario reactivo con chips para palabras clave y filtros negativos), `ejecucion-busqueda` (solo lectura).

- `core/models/` — interfaces TypeScript que reflejan los DTOs del backend.
- `core/services/` — un servicio por recurso (`HouseApi`, `SelectedHouseApi`, `ProfileConfigApi`, `SearchApi`), todos `providedIn: 'root'`. La API key solo se envía a mano en `SearchApi.ejecutar()` (header `X-API-Key`, leído de `environments/environment.ts`, **gitignorado** — la plantilla versionada es `environment.example.ts`).
- Proxy de desarrollo: `frontend/proxy.conf.json` (`/api` → `localhost:8081`), configurado en `angular.json` → `architect.serve.options.proxyConfig` (junto con `options.port: 4201`).
- **Paleta de color propia: rojo / dorado / hueso** (`#8c2b2b` / `#6b1f1f` texto y nav, `#d4af37`/`#c9a227` bordes y acentos, `#faf6ee` fondo) — definida en `src/styles.scss` (tema Material 3 `mat.$red-palette`/`mat.$yellow-palette`) y `src/app/app.scss` (toolbar). Tipografía Baloo 2 (título de marca) + Roboto (cuerpo), cargadas en `src/index.html`.
- El logo es un icono de Material Symbols (`villa`) como placeholder en el toolbar (`src/app/app.html`) — pendiente de sustituir por un `public/icono.png` propio.
- Tras lanzar una búsqueda, `casas-encontradas` hace polling a `GET /api/search/status` cada 1.5s hasta que deja de estar `EN_EJECUCION`. El botón "Ejecutar búsqueda" se deshabilita si ya se ejecutó hoy o si hay una ejecución en curso.

### Scraper — Node.js (ESM) + Playwright (`scraper/`)

Orquestador `index.js` con un array `FUENTES` — **actualmente vacío**: `sources/idealista.js` y `sources/fotocasa.js` son stubs que lanzan error si se invocan y están deliberadamente excluidos de `FUENTES` hasta implementarlos de verdad. `lib/insert.js` hace `POST /api/house/insert` con `X-API-Key`; `lib/profile-config.js` hace `GET /api/profile-config` (sin API key); `lib/normalize.js` tiene los helpers de normalización/deduplicación/filtrado compartidos. `scraper/.env` es un symlink a `../.env`.

**Próxima fase (no implementada todavía)**: scraping real de Idealista con Playwright (rate-limiting cuidadoso — el propio dominio del scraping inmobiliario español tiene fuerte protección anti-bot —, esperas aleatorias entre páginas, salida airosa ante bloqueo/CAPTCHA). Fotocasa se deja como stub para una fase posterior.

## Infraestructura y variables de entorno

- `docker-compose.yml` (raíz): un único servicio `postgres:16-alpine` (`house-search-db`), expuesto en el host en el puerto **5433** (no 5432: ese puerto lo ocupa el Postgres del proyecto hermano `job-search`, que corre en la misma máquina — el contenedor interno sigue escuchando en 5432, solo cambia el mapeo de host). `spring.datasource.url` en `application.properties` ya apunta a `localhost:5433`. Volumen `pgdata`.
- `.env` / `.env.example` (raíz, **no** en subcarpetas): `POSTGRES_DB`, `POSTGRES_USER`, `POSTGRES_PASSWORD`, `ANTHROPIC_API_KEY` (vacío por defecto — rellenar para que el scoring funcione), `INSERT_API_KEY` (ya generado, debe coincidir con `frontend/src/environments/environment.ts`).
- `.gitignore` (raíz): `.env`, `target/`, `node_modules/`, `dist/`, `.angular/`, `*.log`, `scraper/screenshots/`.
