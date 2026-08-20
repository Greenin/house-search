# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Estado del proyecto

`house-search` es un clon arquitectónico de un proyecto hermano (`job-search`, no incluido en este repo) adaptado al dominio de búsqueda de casas en venta/alquiler. Corre **100% en local**, sin desplegar nada en la nube. Monorepo con tres partes independientes (`backend/`, `frontend/`, `scraper/`) más infraestructura en la raíz. Fase 1 (modelo de datos + backend CRUD + frontend navegable) ya está implementada; Fase 2 (scraping real) está en marcha con **Habitaclia** como primera fuente funcional — ver más abajo por qué no es Idealista.

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
- **`Orientacion`, `Calefaccion`, `TipoCalefaccion` y `Climatizacion` son `String` libres**, no enums: vienen de HTML scrapeado de portales inmobiliarios con formato inconsistente. Los enums (`EstadoCasa`, `Fuente`, `Prioridad`, `EstadoGestionCasa`, `EstadoBusqueda`) sí son `@Enumerated(STRING)` porque los controla la propia aplicación. `Fuente` tiene `IDEALISTA`/`FOTOCASA` (fuentes no implementadas, ver scraper) además de `HABITACLIA` (la única activa) y `OTRA`.
- **`puntuacion` no existe como columna en `House`**: vive solo en `HouseMatchScore` (relación 1:1); el listado hace el join en `HouseService.listarCasas`.
- **Deduplicación de casas**: hash SHA-256 de título+ubicación normalizados (sin acentos, minúsculas, espacios colapsados), columna única `house.id_titulo_ubicacion` — ver `HouseService.calcularIdTituloUbicacion`.
- **`SearchRunnerService`** lanza el scraper Node.js como `ProcessBuilder` en un hilo daemon (no bloquea el request HTTP), timeout de 30 minutos y captura de `Throwable` como red de seguridad. Fuerza `HEADLESS=true` en el entorno del proceso hijo (el backend no tiene pantalla asociada). No todas las fuentes usan Playwright por igual: cada fuente elige Playwright o HTTP simple según si el portal necesita JS real (ver sección Scraper).
- **Dos modos de búsqueda, cada uno con su propia fila fija en `search_execution`** (`SearchExecution.ID_COMPLETA=1`/`ID_SIN_PLAYWRIGHT=2`, en vez de la fila única original): botón **"Ejecutar búsqueda"** (`POST /api/search/run`) lanza *todas* las fuentes activas y está limitado a una vez al día (protege el rate-limit de las fuentes que sí requieren Playwright); botón **"Búsqueda sin Playwright"** (`POST /api/search/run-sin-playwright`, `MODO_SCRAPER=SIN_PLAYWRIGHT` en el entorno del proceso hijo) lanza solo las fuentes con `requierePlaywright:false` (hoy, Habitaclia) y **no tiene límite diario**. Ambos modos comparten el mismo lock de instancia en `SearchRunnerService` — no pueden correr a la vez, para no golpear la misma fuente por duplicado — pero el límite de "una vez al día" de un modo no afecta al otro, porque cada uno lee/escribe su propia fila.
- **`HouseScoringService`** puntúa cada casa nueva con la API de Claude (`claude-opus-5`, salida estructurada vía `outputConfig(HouseScoreResult.class)`) en un hilo daemon lanzado desde `HouseService.insertarCasas` tras el insert masivo. Si falla el scoring de una casa concreta, esa casa simplemente se queda sin fila en `house_match_score` (puntuación "—" en el listado) — nunca aborta el lote.
- **Seguridad**: filtro de servlet puro (`ApiKeyFilter extends HttpFilter`), registrado vía `FilterRegistrationBean` **solo** en `POST /api/house/insert` y `POST /api/search/run` (header `X-API-Key`). El resto de endpoints están abiertos (app de un único usuario en local).
- **`spring.jpa.hibernate.ddl-auto=update` no amplía los `CHECK` de los enums en una BD ya existente**: Hibernate genera un `CHECK (columna IN (...))` para cada `@Enumerated(STRING)` la primera vez que crea la tabla, con los valores del enum de Java *en ese momento*, pero `ddl-auto=update` nunca lo recrea aunque el enum gane valores nuevos después (se vio al añadir `Fuente.HABITACLIA`: compilaba bien, pero el insert daba 500 por violar `house_fuente_check` en Postgres). Al añadir un valor a `Fuente`/`EstadoCasa`/`Prioridad`/etc. hay que `ALTER TABLE ... DROP CONSTRAINT ...` + `ADD CONSTRAINT ...` a mano contra la BD local (no hay Flyway/Liquibase en este proyecto). Una BD creada desde cero (volumen nuevo) no tiene este problema, porque el `CHECK` se genera ya con el enum completo.

**Endpoints** (rutas en singular, a diferencia del proyecto original):
```
POST   /api/search/run                (API key) — todas las fuentes, 1/día
GET    /api/search/status
POST   /api/search/run-sin-playwright (API key) — solo fuentes sin Playwright, sin límite
GET    /api/search/status-sin-playwright
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

Sin NgModules, sin NgRx. Cuatro rutas lazy (`loadComponent`) bajo `src/app/features/`: `casas-encontradas` (vista principal, tabla con columnas de ancho fijo + fila de detalle expandible + botones Ejecutar búsqueda/Actualizar/Limpiar/Búsqueda sin Playwright + acciones Seleccionar/Ver detalles), `casas-seleccionadas` (tabla + cambio de estado de gestión inline + borrar por fila), `configuracion-busqueda` (formulario reactivo con chips para palabras clave y filtros negativos), `ejecucion-busqueda` (solo lectura).

- `core/models/` — interfaces TypeScript que reflejan los DTOs del backend.
- `core/services/` — un servicio por recurso (`HouseApi`, `SelectedHouseApi`, `ProfileConfigApi`, `SearchApi`), todos `providedIn: 'root'`. La API key solo se envía a mano en `SearchApi.ejecutar()`/`ejecutarSinPlaywright()` (header `X-API-Key`, leído de `environments/environment.ts`, **gitignorado** — la plantilla versionada es `environment.example.ts`).
- Proxy de desarrollo: `frontend/proxy.conf.json` (`/api` → `localhost:8081`), configurado en `angular.json` → `architect.serve.options.proxyConfig` (junto con `options.port: 4201`).
- **Paleta de color propia: rojo / dorado / hueso** (`#8c2b2b` / `#6b1f1f` texto y nav, `#d4af37`/`#c9a227` bordes y acentos, `#faf6ee` fondo) — definida en `src/styles.scss` (tema Material 3 `mat.$red-palette`/`mat.$yellow-palette`) y `src/app/app.scss` (toolbar). Tipografía Baloo 2 (título de marca) + Roboto (cuerpo), cargadas en `src/index.html`.
- El logo es un icono de Material Symbols (`villa`) como placeholder en el toolbar (`src/app/app.html`) — pendiente de sustituir por un `public/icono.png` propio.
- `casas-encontradas` hace polling cada 1.5s tras lanzar cada tipo de búsqueda: `GET /api/search/status` para "Ejecutar búsqueda", `GET /api/search/status-sin-playwright` para "Búsqueda sin Playwright" (señales independientes: `ejecutandoBusqueda`/`ejecutandoBusquedaSinPlaywright`). "Ejecutar búsqueda" se deshabilita si ya se ejecutó hoy o si cualquiera de las dos búsquedas está en curso; "Búsqueda sin Playwright" solo se deshabilita si alguna está en curso (sin límite diario propio).

### Scraper — Node.js (ESM), Playwright + fetch/cheerio según la fuente (`scraper/`)

Orquestador `index.js` con un array `FUENTES` que hoy solo contiene `{ nombre: 'Habitaclia', buscar: habitaclia.buscar, requierePlaywright: false }`. Cada fuente se ejecuta en su propio `try/catch` dentro del bucle de `main()`: si una fuente falla, las demás siguen (aislamiento por fuente). Si la variable de entorno `MODO_SCRAPER=SIN_PLAYWRIGHT` está definida (la fija `SearchRunnerService` al lanzar el proceso hijo desde el botón "Búsqueda sin Playwright"), `main()` filtra `FUENTES` a solo las que tengan `requierePlaywright: false` antes de ejecutar nada; sin esa variable (botón "Ejecutar búsqueda", o `node index.js` a mano) se ejecutan todas. `lib/insert.js` hace `POST /api/house/insert` con `X-API-Key`; `lib/profile-config.js` hace `GET /api/profile-config` (sin API key); `lib/normalize.js` tiene los helpers de normalización/deduplicación/filtrado compartidos; `lib/rate-limit.js` tiene `pausaEntrePaginas()` (2-3s aleatorios, compartido por todas las fuentes). `scraper/.env` es un symlink a `../.env`.

**Por qué Habitaclia y no Idealista/Fotocasa**: antes de implementar nada se leyó el `robots.txt` real de los tres portales y sus condiciones de uso. Idealista prohíbe expresamente "robot, spider, scraper o cualquier proceso automático" sin autorización escrita y lo vigila activamente con **DataDome** (confirmado); Fotocasa prohíbe la reproducción del contenido sin autorización y su `robots.txt` bloquea explícitamente `/search/`, `/buscar/` y las páginas de listado de las grandes ciudades. Ambas quedan como **stubs** (`sources/idealista.js`, `sources/fotocasa.js`, lanzan error si se invocan) deliberadamente fuera de `FUENTES`. Habitaclia, en cambio, tiene un `robots.txt` permisivo (solo bloquea rutas privadas/ajax y parámetros de orden/paginación por query string) y sus condiciones de uso no prohíben expresamente el acceso automatizado — es la única fuente con implementación real por ahora.

**Dos mecanismos de acceso, elegidos por fuente**:
- **`lib/http.js`** (`descargarHtml`, `guardarHtmlError`) — para portales que sirven HTML ya renderizado en el servidor, como Habitaclia: `fetch` simple con user-agent de navegador, sin arrancar Chromium. Más ligero y rápido.
- **`lib/browser.js`** (`abrirNavegador`, `capturarError`, `guardarSesion`) — Playwright, para futuras fuentes que sí necesiten JS real o esquivar cargas dinámicas. `headless` se decide por la variable de entorno `HEADLESS`: **visible por defecto** (para depurar selectores a mano con `node index.js`), solo pasa a `headless:true` si `HEADLESS=true` está definida explícitamente (`SearchRunnerService` la fuerza al lanzar el proceso hijo). Sesión persistente opcional vía `context.storageState({ path: 'auth.json' })` — `scraper/auth.json` gitignorado; hoy ninguna fuente requiere login, queda listo para cuando haga falta.

Ambos mecanismos guardan evidencia cuando algo falla dentro de su propio `catch` (equivalente a un screenshot): `capturarError` guarda un `.png` de Playwright, `guardarHtmlError` guarda el `.html` recibido — ambos en `scraper/screenshots/` (gitignorado).

**`sources/habitaclia.js`**: pide `https://www.habitaclia.com/{viviendas|alquiler}-<ubicacion>.htm` (sin paginar — el `robots.txt` de Habitaclia prohíbe `/*pag=` y variantes de `/l/2*`), extrae las tarjetas del listado (título, ubicación, precio, m², habitaciones, baños, estado) y pide cada ficha individual para completar terraza/ascensor/orientación/calefacción/climatización/planta/consumo energético, parseando con `cheerio` los `<article class="has-aside">` de la ficha (texto libre e inconsistente entre anuncios, de ahí que esos campos sean `String` y no enum — ver más arriba). Los enlaces de ficha se limpian de query string antes de pedirlos (`f=`, `geo=`, `from=`, `lo=`... están en `Disallow`). Cada ficha va en su propio `try/catch`: si falla, la casa se conserva con los datos ya extraídos del listado y los campos de ficha a `null`, en vez de descartarla. Tope de `MAX_RESULTADOS_POR_EJECUCION` (20) fichas por ejecución, una sola página de resultados, sin repetir.

**Próxima fase**: revisar Fotocasa/otros portales si Habitaclia no da suficiente volumen de resultados; Idealista queda descartado salvo que aparezca una vía de acceso autorizada (p. ej. su API de partners).

## Infraestructura y variables de entorno

- `docker-compose.yml` (raíz): un único servicio `postgres:16-alpine` (`house-search-db`), expuesto en el host en el puerto **5433** (no 5432: ese puerto lo ocupa el Postgres del proyecto hermano `job-search`, que corre en la misma máquina — el contenedor interno sigue escuchando en 5432, solo cambia el mapeo de host). `spring.datasource.url` en `application.properties` ya apunta a `localhost:5433`. Volumen `pgdata`.
- `.env` / `.env.example` (raíz, **no** en subcarpetas): `POSTGRES_DB`, `POSTGRES_USER`, `POSTGRES_PASSWORD`, `ANTHROPIC_API_KEY` (vacío por defecto — rellenar para que el scoring funcione), `INSERT_API_KEY` (ya generado, debe coincidir con `frontend/src/environments/environment.ts`), `BACKEND_URL` (`http://localhost:8081` — el scraper lo usa en `lib/insert.js`/`lib/profile-config.js`; sin esta variable el default del código es `:8080`, el puerto de `job-search`, así que **es obligatoria** en este proyecto). `HEADLESS` no vive en `.env` (lo fuerza `SearchRunnerService` al lanzar el proceso hijo); déjala sin definir para desarrollo manual (`node index.js`) y ver el navegador.
- `.gitignore` (raíz): `.env`, `target/`, `node_modules/`, `dist/`, `.angular/`, `*.log`, `scraper/screenshots/`, `scraper/auth.json`.
