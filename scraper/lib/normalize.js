export function normalizarTexto(texto) {
  if (!texto) return '';
  return texto
    .normalize('NFD')
    .replace(/\p{Diacritic}/gu, '')
    .toLowerCase()
    .trim()
    .replace(/\s+/g, ' ');
}

export function claveDeduplicado(titulo, ubicacion) {
  return `${normalizarTexto(titulo)}|${normalizarTexto(ubicacion)}`;
}

/**
 * Filtra una casa ya normalizada contra la configuracion de busqueda dinamica
 * traida del backend (GET /api/profile-config). Los filtros negativos
 * comparan contra titulo+descripcion; el resto de campos son comparaciones
 * numericas/booleanas directas.
 */
export function cumpleCriterios(casa, config) {
  if (config.precioMaximo != null && casa.precio != null && casa.precio > config.precioMaximo) {
    return false;
  }
  if (config.tamanoMinimo != null && casa.tamano != null && casa.tamano < config.tamanoMinimo) {
    return false;
  }
  if (
    config.numeroHabitacionesMinimo != null &&
    casa.habitaciones != null &&
    casa.habitaciones < config.numeroHabitacionesMinimo
  ) {
    return false;
  }
  if (config.numeroBanosMinimo != null && casa.banos != null && casa.banos < config.numeroBanosMinimo) {
    return false;
  }
  if (config.terrazaRequerida && casa.terraza === false) {
    return false;
  }
  if (config.ascensorRequerido && casa.ascensor === false) {
    return false;
  }

  const textoCasa = normalizarTexto(`${casa.titulo ?? ''} ${casa.descripcion ?? ''}`);
  const filtrosNegativos = config.filtrosNegativos ?? [];
  if (filtrosNegativos.some((filtro) => textoCasa.includes(normalizarTexto(filtro)))) {
    return false;
  }

  return true;
}
