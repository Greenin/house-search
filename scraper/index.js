import 'dotenv/config';
import { configEstatico } from './config.js';
import { obtenerConfiguracionBusqueda } from './lib/profile-config.js';
import { enviarCasas } from './lib/insert.js';
import { cumpleCriterios, claveDeduplicado } from './lib/normalize.js';
import * as idealista from './sources/idealista.js';
import * as fotocasa from './sources/fotocasa.js';

// Idealista y Fotocasa estan implementadas como stubs (Playwright, fuente mas
// fragil por las protecciones anti-bot de los portales inmobiliarios) y
// deliberadamente NO estan en este array hasta que se implementen de verdad
// (ver scraper/sources/idealista.js). Cuando una fuente este lista se anade
// aqui, ej.: { nombre: 'Idealista', buscar: idealista.buscar, requierePlaywright: true }.
const FUENTES = [];

async function main() {
  const configDinamica = await obtenerConfiguracionBusqueda().catch((err) => {
    console.warn(`No se pudo leer la configuracion de busqueda: ${err.message}`);
    return {};
  });
  const config = { ...configEstatico, ...configDinamica };

  const todas = [];
  const vistas = new Set();

  for (const fuente of FUENTES) {
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

  if (FUENTES.length === 0) {
    console.warn('No hay ninguna fuente activa todavia (Idealista/Fotocasa siguen en stub).');
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
