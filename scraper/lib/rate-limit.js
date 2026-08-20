/**
 * Pausa aleatoria entre paginas para no golpear el sitio de forma mecanica.
 * Compartido entre lib/browser.js (fuentes con Playwright) y lib/http.js
 * (fuentes por HTTP simple, como Habitaclia) para no duplicar el ritmo.
 */
export async function pausaEntrePaginas(minMs = 2000, maxMs = 3000) {
  const espera = minMs + Math.random() * (maxMs - minMs);
  await new Promise((resolve) => setTimeout(resolve, espera));
}
