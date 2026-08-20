import { chromium } from 'playwright';
import { existsSync, mkdirSync } from 'node:fs';
import { fileURLToPath } from 'node:url';
import path from 'node:path';
import { pausaEntrePaginas } from './rate-limit.js';

export { pausaEntrePaginas };

const DIRNAME = path.dirname(fileURLToPath(import.meta.url));
const SCREENSHOTS_DIR = path.resolve(DIRNAME, '../screenshots');

// Sesion persistente (cookies, localStorage) para no tener que autenticarse
// en cada ejecucion. Solo se usa si el fichero ya existe: hoy ninguna fuente
// requiere login para buscar/leer fichas, asi que esto queda listo para el
// dia que haga falta (ver guardarSesion). Gitignorado por contener cookies.
export const AUTH_FILE = path.resolve(DIRNAME, '../auth.json');

/**
 * headless:false por defecto en desarrollo para poder ver el navegador y
 * depurar selectores; headless:true solo cuando se fija explicitamente
 * HEADLESS=true en el entorno (imprescindible al lanzarlo desde el backend,
 * que no tiene pantalla asociada -> SearchRunnerService debe fijar esta var).
 */
function esHeadless() {
  return process.env.HEADLESS === 'true';
}

export async function abrirNavegador() {
  const browser = await chromium.launch({ headless: esHeadless() });
  const context = await browser.newContext({
    storageState: existsSync(AUTH_FILE) ? AUTH_FILE : undefined,
    userAgent:
      'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0 Safari/537.36',
    locale: 'es-ES',
  });
  return { browser, context };
}

/** Guarda cookies/localStorage tras un login manual, para reutilizar en la siguiente ejecucion. */
export async function guardarSesion(context) {
  await context.storageState({ path: AUTH_FILE });
}

/** Captura de pantalla en el momento del fallo, para poder ver que paso sin relanzar en modo visible. */
export async function capturarError(page, prefijo) {
  mkdirSync(SCREENSHOTS_DIR, { recursive: true });
  const ruta = path.join(SCREENSHOTS_DIR, `${prefijo}-${Date.now()}.png`);
  try {
    await page.screenshot({ path: ruta });
    console.error(`Captura guardada en ${ruta}`);
  } catch (err) {
    console.error(`No se pudo guardar la captura de error: ${err.message}`);
  }
}
