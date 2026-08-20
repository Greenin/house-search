import * as cheerio from 'cheerio';
import { pausaEntrePaginas } from '../lib/rate-limit.js';
import { descargarHtml, guardarHtmlError } from '../lib/http.js';

// Habitaclia es la primera fuente scrapeada de verdad (ver CLAUDE.md): se
// eligio en vez de Idealista/Fotocasa porque su robots.txt es mucho mas
// permisivo con las paginas de busqueda/ficha y sus condiciones de uso no
// prohiben expresamente el acceso automatizado (a diferencia de Idealista,
// que ademas vigila activamente con DataDome). Aun asi hay que respetar las
// reglas que SI tiene: nada de paginacion (Disallow: /*pag=, /*/l/2*...) ni
// de query params de orden/filtro (?f=, ?geo=, ?from=, ?lo=, ?ordenar=...),
// de ahi que limpiarUrl() elimine siempre el query string de los enlaces de
// ficha antes de navegar.
//
// Habitaclia sirve HTML ya renderizado en el servidor (sin JS necesario), asi
// que aqui se usa fetch+cheerio en vez de Playwright: mas ligero y rapido que
// arrancar un Chromium para una fuente que no lo necesita. Playwright
// (lib/browser.js) sigue disponible para futuras fuentes que si requieran un
// navegador real (JS pesado, anti-bot).
const BASE_URL = 'https://www.habitaclia.com';
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
  const ubicacion = config.habitacliaUbicacion ?? 'madrid';
  const prefijo = config.habitacliaOperacion === 'alquiler' ? 'alquiler' : 'viviendas';
  return `${BASE_URL}/${prefijo}-${ubicacion}.htm`;
}

/** Quita el query string: varios parametros habituales de listado (f=, geo=, from=, lo=...) estan en Disallow. */
function limpiarUrl(href) {
  if (!href) return null;
  const url = new URL(href, BASE_URL);
  url.search = '';
  return url.toString();
}

function parsePrecio(texto) {
  const match = texto?.replace(/\s/g, '').match(/[\d.]+/);
  return match ? Number(match[0].replace(/\./g, '')) : null;
}

function parseFeatureLine(texto) {
  if (!texto) return { tamano: null, habitaciones: null, banos: null };
  const tamano = texto.match(/(\d+)\s*m2/i) ?? texto.match(/(\d+)\s*m\b/i);
  const habitaciones = texto.match(/(\d+)\s*hab/i);
  const banos = texto.match(/(\d+)\s*ba/i);
  return {
    tamano: tamano ? Number(tamano[1]) : null,
    habitaciones: habitaciones ? Number(habitaciones[1]) : null,
    banos: banos ? Number(banos[1]) : null,
  };
}

function extraerTarjetas(html) {
  const $ = cheerio.load(html);
  const tarjetas = [];
  $('article.list-item-container').each((_, el) => {
    const nodo = $(el);
    const enlace = nodo.find('h3.list-item-title a').first();
    tarjetas.push({
      href: enlace.attr('href') ?? null,
      titulo: enlace.text().trim() || null,
      ubicacion: nodo.find('p.list-item-location span').first().text().trim() || null,
      featureLine: nodo.find('p.list-item-feature').first().text().trim() || null,
      precioTexto: nodo.find('article.list-item-price [itemprop="price"]').first().text().trim() || null,
      descripcionCorta: nodo.find('p.list-item-description').first().text().trim() || null,
      selltype: nodo.attr('data-selltype'),
    });
  });
  return tarjetas;
}

/**
 * La ficha de Habitaclia organiza las caracteristicas en varios
 * <article class="has-aside"><h3>titulo</h3><ul><li>...</li></ul></article>
 * (Distribucion, Caracteristicas generales, Equipamiento comunitario...) con
 * texto libre e inconsistente entre anuncios -> de ahi que en el modelo de
 * datos orientacion/calefaccion/climatizacion sean String y no enum (ver
 * CLAUDE.md).
 */
function extraerFicha(html) {
  const $ = cheerio.load(html);
  const descripcion = $('#js-detail-description').first().text().trim() || null;
  const secciones = [];
  $('article.has-aside').each((_, el) => {
    const art = $(el);
    if (art.attr('id') === 'js-translate') return;
    const items = art
      .find('li')
      .map((_, li) => $(li).text().replace(/\s+/g, ' ').trim())
      .get()
      .filter(Boolean);
    secciones.push({ items });
  });
  return { descripcion, secciones };
}

function buscarItem(secciones, palabraClave) {
  for (const seccion of secciones) {
    const item = seccion.items.find((texto) => texto.toLowerCase().includes(palabraClave));
    if (item) return item;
  }
  return null;
}

function derivarCaracteristicas(secciones) {
  const usados = new Set();
  const usar = (item) => {
    if (item) usados.add(item);
    return item;
  };

  const itemAscensor = usar(buscarItem(secciones, 'ascensor'));
  const itemTerraza = usar(buscarItem(secciones, 'terraza'));
  const calefaccion = usar(buscarItem(secciones, 'calefacci'));
  const climatizacion = usar(buscarItem(secciones, 'aire acondicionado'));
  const orientacion = usar(buscarItem(secciones, 'orientaci'));
  const planta = usar(buscarItem(secciones, 'planta'));
  const itemEnergia = usar(buscarItem(secciones, 'consumo:'));

  const todosLosItems = secciones.flatMap((s) => s.items);
  const caracteristicasBasicas = todosLosItems.filter((item) => !usados.has(item)).join('; ') || null;

  return {
    ascensor: itemAscensor ? !itemAscensor.toLowerCase().startsWith('sin ') : null,
    terraza: itemTerraza ? !itemTerraza.toLowerCase().startsWith('sin ') : null,
    calefaccion,
    climatizacion,
    orientacion,
    planta,
    consumoEnergetico: itemEnergia ? (itemEnergia.match(/Consumo:\s*([A-G])/i)?.[1] ?? itemEnergia) : null,
    caracteristicasBasicas,
  };
}

function casaBaseDesdeTarjeta(tarjeta, enlace) {
  const { tamano, habitaciones, banos } = parseFeatureLine(tarjeta.featureLine);
  return {
    titulo: tarjeta.titulo,
    ubicacion: tarjeta.ubicacion,
    precio: parsePrecio(tarjeta.precioTexto),
    tamano,
    habitaciones,
    banos,
    estado: tarjeta.selltype === 'SECOND_HAND' ? 'SEGUNDA_MANO' : 'NUEVA',
    descripcion: tarjeta.descripcionCorta,
    fuente: 'HABITACLIA',
    fechaLocalizacionCasa: new Date().toISOString().slice(0, 10),
    enlaceCasa: enlace,
    ...CAMPOS_FICHA_POR_DEFECTO,
  };
}

export async function buscar(config) {
  const urlBusqueda = construirUrlBusqueda(config);
  console.log(`Habitaclia: descargando listado ${urlBusqueda}`);

  let tarjetas;
  let htmlListado = '';
  try {
    htmlListado = await descargarHtml(urlBusqueda);
    tarjetas = extraerTarjetas(htmlListado);
  } catch (err) {
    guardarHtmlError(htmlListado, 'habitaclia-listado');
    throw err;
  }

  const limite = config.habitacliaMaxResultados ?? MAX_RESULTADOS_POR_EJECUCION;
  tarjetas = tarjetas.slice(0, limite);
  console.log(`Habitaclia: ${tarjetas.length} anuncios encontrados en el listado, visitando fichas...`);

  const casas = [];
  for (const tarjeta of tarjetas) {
    const enlace = limpiarUrl(tarjeta.href);
    if (!enlace || !tarjeta.titulo) continue;

    const casa = casaBaseDesdeTarjeta(tarjeta, enlace);
    let htmlFicha = '';

    try {
      await pausaEntrePaginas();
      htmlFicha = await descargarHtml(enlace);
      const ficha = extraerFicha(htmlFicha);
      Object.assign(casa, derivarCaracteristicas(ficha.secciones));
      if (ficha.descripcion) casa.descripcion = ficha.descripcion;
    } catch (err) {
      console.error(`Habitaclia: no se pudo leer la ficha de "${tarjeta.titulo}": ${err.message}`);
      guardarHtmlError(htmlFicha, 'habitaclia-ficha');
      // se conserva la casa con los datos ya extraidos del listado
    }

    casas.push(casa);
  }

  return casas;
}
