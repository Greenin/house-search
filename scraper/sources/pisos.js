import * as cheerio from 'cheerio';
import { pausaEntrePaginas } from '../lib/rate-limit.js';
import { descargarHtml, guardarHtmlError } from '../lib/http.js';

// Segunda fuente real, anadida tras comprobar que Habitaclia esta protegida
// por Imperva (bloquea especificamente al cliente fetch del scraper, ver
// CLAUDE.md) -> pisos.com se reviso con el mismo criterio que Habitaclia:
// robots.txt permisivo, condiciones de uso sin prohibicion expresa de
// scraping, y -lección aprendida- probado con el propio fetch() del scraper
// (no solo curl) para confirmar que no hay reto anti-bot.
//
// La ficha organiza cada caracteristica en un <div class="features__feature">
// con un icono estable (icon-ascensor, icon-terraza, icon-orientacion...) en
// vez de texto libre sin estructura como en Habitaclia -> parseo mas robusto
// por icono en vez de buscar palabras clave en frases.
const BASE_URL = 'https://www.pisos.com';
const MAX_RESULTADOS_POR_EJECUCION = 20;

const CAMPOS_FICHA_POR_DEFECTO = {
  planta: null,
  terraza: null,
  orientacion: null,
  ascensor: null,
  climatizacion: null,
  calefaccion: null,
  tipoCalefaccion: null,
  caracteristicasBasicas: null,
  consumoEnergetico: null,
  prioridad: null,
  emailContacto: null,
};

function construirUrlBusqueda(config) {
  const ubicacion = config.pisosComUbicacion ?? 'madrid';
  const prefijo = config.pisosComOperacion === 'alquiler' ? 'alquiler' : 'venta';
  return `${BASE_URL}/${prefijo}/pisos-${ubicacion}/`;
}

function parseNumero(texto) {
  const match = texto?.replace(/\s/g, '').match(/[\d.]+/);
  return match ? Number(match[0].replace(/\./g, '')) : null;
}

function parseCaracteristicas(items) {
  let tamano = null;
  let habitaciones = null;
  let banos = null;
  let planta = null;
  for (const texto of items) {
    if (tamano === null && /m2|m²/i.test(texto)) tamano = parseNumero(texto);
    else if (habitaciones === null && /hab/i.test(texto)) habitaciones = parseNumero(texto);
    else if (banos === null && /ba(n|ñ)o/i.test(texto)) banos = parseNumero(texto);
    else if (planta === null && /planta/i.test(texto)) planta = texto.replace(/planta/i, '').trim() || texto.trim();
  }
  return { tamano, habitaciones, banos, planta };
}

function extraerTarjetas(html) {
  const $ = cheerio.load(html);
  const tarjetas = [];
  $('div.ad-preview').each((_, el) => {
    const nodo = $(el);
    const items = nodo
      .find('p.ad-preview__char')
      .map((_, p) => $(p).text().trim())
      .get()
      .filter(Boolean);
    tarjetas.push({
      href: nodo.attr('data-lnk-href') ?? nodo.find('a.ad-preview__title').first().attr('href') ?? null,
      titulo: nodo.find('a.ad-preview__title').first().text().trim() || null,
      ubicacion: nodo.find('p.ad-preview__subtitle').first().text().trim() || null,
      precioTexto: nodo.find('span.ad-preview__price').first().text().trim() || null,
      descripcionCorta: nodo.find('p.ad-preview__description').first().text().trim() || null,
      ...parseCaracteristicas(items),
    });
  });
  return tarjetas;
}

/**
 * La ficha organiza cada caracteristica como
 * <div class="features__feature"><span class="features__icon icon-X"></span>
 * <div><span class="features__label">Label</span>
 * <span class="features__value">Valor</span></div></div>. El icono (icon-X)
 * es estable entre anuncios, a diferencia del texto libre de Habitaclia.
 */
function extraerFicha(html) {
  const $ = cheerio.load(html);
  const descripcion = $('.description__content').first().text().replace(/\s+/g, ' ').trim() || null;
  const consumoEnergetico = $('.energy-certificate__tag').first().text().trim().toUpperCase() || null;

  const features = [];
  $('.features__feature').each((_, el) => {
    const nodo = $(el);
    const icono = (nodo.find('.features__icon').first().attr('class') ?? '').match(/icon-([a-z0-9]+)/)?.[1] ?? null;
    const label = nodo.find('.features__label').first().text().replace(/:\s*$/, '').trim();
    const valor = nodo.find('.features__value').first().text().trim() || null;
    if (icono) features.push({ icono, label, valor });
  });

  return { descripcion, consumoEnergetico, features };
}

function buscarFeature(features, icono) {
  return features.find((f) => f.icono === icono) ?? null;
}

function derivarCaracteristicas(features) {
  const usados = new Set();
  const usar = (feature) => {
    if (feature) usados.add(feature);
    return feature;
  };

  const ascensor = usar(buscarFeature(features, 'ascensor'));
  const terraza = usar(buscarFeature(features, 'terraza'));
  const calefaccion = usar(buscarFeature(features, 'calefaccion'));
  const climatizacion = usar(buscarFeature(features, 'aireacondicionado'));
  const orientacion = usar(buscarFeature(features, 'orientacion'));
  const planta = usar(buscarFeature(features, 'planta'));
  const estadoConservacion = usar(buscarFeature(features, 'estadoconservacion'));

  const restantes = features
    .filter((f) => !usados.has(f))
    .map((f) => (f.valor ? `${f.label}: ${f.valor}` : f.label))
    .filter(Boolean);

  return {
    ascensor: ascensor ? true : null,
    terraza: terraza ? true : null,
    calefaccion: calefaccion?.label ?? null,
    climatizacion: climatizacion?.label ?? null,
    orientacion: orientacion?.valor ?? null,
    planta: planta?.valor ?? null,
    estado: /estrenar|obra nueva/i.test(estadoConservacion?.valor ?? '') ? 'NUEVA' : 'SEGUNDA_MANO',
    caracteristicasBasicas: restantes.join('; ') || null,
  };
}

function casaBaseDesdeTarjeta(tarjeta, enlace) {
  return {
    titulo: tarjeta.titulo,
    ubicacion: tarjeta.ubicacion,
    precio: parseNumero(tarjeta.precioTexto),
    tamano: tarjeta.tamano,
    habitaciones: tarjeta.habitaciones,
    banos: tarjeta.banos,
    estado: 'SEGUNDA_MANO',
    descripcion: tarjeta.descripcionCorta,
    fuente: 'PISOS_COM',
    fechaLocalizacionCasa: new Date().toISOString().slice(0, 10),
    enlaceCasa: enlace,
    ...CAMPOS_FICHA_POR_DEFECTO,
    planta: tarjeta.planta,
  };
}

export async function buscar(config) {
  const urlBusqueda = construirUrlBusqueda(config);
  console.log(`Pisos.com: descargando listado ${urlBusqueda}`);

  let tarjetas;
  let htmlListado = '';
  try {
    htmlListado = await descargarHtml(urlBusqueda);
    tarjetas = extraerTarjetas(htmlListado);
  } catch (err) {
    guardarHtmlError(htmlListado, 'pisos-listado');
    throw err;
  }

  const limite = config.pisosComMaxResultados ?? MAX_RESULTADOS_POR_EJECUCION;
  tarjetas = tarjetas.slice(0, limite);
  console.log(`Pisos.com: ${tarjetas.length} anuncios encontrados en el listado, visitando fichas...`);

  const casas = [];
  for (const tarjeta of tarjetas) {
    const enlace = tarjeta.href ? new URL(tarjeta.href, BASE_URL).toString() : null;
    if (!enlace || !tarjeta.titulo) continue;

    const casa = casaBaseDesdeTarjeta(tarjeta, enlace);
    let htmlFicha = '';

    try {
      await pausaEntrePaginas();
      htmlFicha = await descargarHtml(enlace);
      const ficha = extraerFicha(htmlFicha);
      const extra = derivarCaracteristicas(ficha.features);
      Object.assign(casa, extra);
      if (ficha.descripcion) casa.descripcion = ficha.descripcion;
      if (ficha.consumoEnergetico) casa.consumoEnergetico = ficha.consumoEnergetico;
    } catch (err) {
      console.error(`Pisos.com: no se pudo leer la ficha de "${tarjeta.titulo}": ${err.message}`);
      guardarHtmlError(htmlFicha, 'pisos-ficha');
      // se conserva la casa con los datos ya extraidos del listado
    }

    casas.push(casa);
  }

  return casas;
}
