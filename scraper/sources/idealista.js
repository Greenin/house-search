// Idealista no tiene API publica: requeriria un scraper con Playwright.
// Es la fuente con mas volumen de listados en Espana, pero tambien la mas
// fragil (fuertes protecciones anti-bot). Se deja como stub explicitamente
// excluido del array FUENTES en index.js hasta implementarla de verdad
// (Fase 3), igual que hizo InfoJobs en el proyecto job-search.
export async function buscar() {
  throw new Error('Idealista (Playwright) todavia no esta implementado.');
}
