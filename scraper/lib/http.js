import { mkdirSync, writeFileSync } from 'node:fs';
import { fileURLToPath } from 'node:url';
import path from 'node:path';

const DIRNAME = path.dirname(fileURLToPath(import.meta.url));
const SCREENSHOTS_DIR = path.resolve(DIRNAME, '../screenshots');

const USER_AGENT =
  'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0 Safari/537.36';

/**
 * Descarga HTML por peticion HTTP simple, para fuentes que sirven el
 * contenido ya renderizado en el servidor y no necesitan un navegador real
 * (ver sources/habitaclia.js). Las fuentes que si necesiten JS/anti-bot
 * deben usar lib/browser.js (Playwright) en su lugar.
 */
export async function descargarHtml(url) {
  const respuesta = await fetch(url, {
    headers: { 'User-Agent': USER_AGENT, 'Accept-Language': 'es-ES,es;q=0.9' },
  });
  if (!respuesta.ok) {
    throw new Error(`HTTP ${respuesta.status} al pedir ${url}`);
  }
  return respuesta.text();
}

/** Guarda el HTML recibido cuando algo falla: el equivalente sin navegador al screenshot de capturarError(). */
export function guardarHtmlError(html, prefijo) {
  mkdirSync(SCREENSHOTS_DIR, { recursive: true });
  const ruta = path.join(SCREENSHOTS_DIR, `${prefijo}-${Date.now()}.html`);
  try {
    writeFileSync(ruta, html ?? '');
    console.error(`HTML de depuracion guardado en ${ruta}`);
  } catch (err) {
    console.error(`No se pudo guardar el HTML de depuracion: ${err.message}`);
  }
}
