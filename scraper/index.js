import 'dotenv/config';
import { configEstatico } from './config.js';
import { obtenerConfiguracionBusqueda } from './lib/profile-config.js';
import { enviarCasas } from './lib/insert.js';
import { cumpleCriterios, claveDeduplicado } from './lib/normalize.js';
import * as idealista from './sources/idealista.js';
import * as fotocasa from './sources/fotocasa.js';
import * as habitaclia from './sources/habitaclia.js';

// Idealista y Fotocasa prohiben expresamente el scraping en sus condiciones
// de uso y lo vigilan activamente con anti-bot (DataDome) -> se dejan como
// stubs, deliberadamente fuera de este array (ver sources/idealista.js y
// sources/fotocasa.js). Habitaclia es la primera fuente implementada de
// verdad: robots.txt permisivo y sin prohibicion expresa de acceso
// automatizado en sus condiciones (ver sources/habitaclia.js).
//
// requierePlaywright distingue las dos formas en que el backend puede lanzar
// esta ejecucion (ver SearchRunnerService): el boton "Ejecutar busqueda"
// (todas las fuentes, limitado a una vez al dia para proteger el rate-limit
// de las fuentes que si necesitan Playwright) y "Busqueda sin Playwright"
// (MODO_SCRAPER=SIN_PLAYWRIGHT, solo fuentes con requierePlaywright:false,
// sin limite diario).
const FUENTES = [{ nombre: 'Habitaclia', buscar: habitaclia.buscar, requierePlaywright: false }];

async function main() {
  const soloSinPlaywright = process.env.MODO_SCRAPER === 'SIN_PLAYWRIGHT';
  const fuentesAEjecutar = soloSinPlaywright ? FUENTES.filter((f) => !f.requierePlaywright) : FUENTES;

  const configDinamica = await obtenerConfiguracionBusqueda().catch((err) => {
    console.warn(`No se pudo leer la configuracion de busqueda: ${err.message}`);
    return {};
  });
  const config = { ...configEstatico, ...configDinamica };

  const todas = [];
  const vistas = new Set();

  for (const fuente of fuentesAEjecutar) {
    try {
      console.log(`Buscando en ${fuente.nombre}...`);
      const resultados = await fuente.buscar(config);
      for (const casa of resultados) {
        const clave = claveDeduplicado(casa.titulo, casa.ubicacion);
        if (vistas.has(clave)) continue;
        if (!cumpleCriterios(casa, config)) continue;
        vistas.add(clave);
        todas.push(casa);
      }
    } catch (err) {
      console.error(`Fallo en la fuente ${fuente.nombre}: ${err.message}`);
    }
  }

  if (fuentesAEjecutar.length === 0) {
    console.warn(
      soloSinPlaywright
        ? 'No hay ninguna fuente sin Playwright activa todavia.'
        : 'No hay ninguna fuente activa todavia (Idealista/Fotocasa siguen en stub).',
    );
  }

  if (todas.length === 0) {
    console.error('No se ha encontrado ninguna casa en esta ejecucion.');
    process.exitCode = 1;
    return;
  }

  const resultado = await enviarCasas(todas);
  console.log(
    `Casas recibidas: ${resultado.recibidas}, insertadas: ${resultado.insertadas}, duplicadas: ${resultado.duplicadas}`,
  );
}

main().catch((err) => {
  console.error(`Error inesperado en el scraper: ${err.message}`);
  process.exitCode = 1;
});
