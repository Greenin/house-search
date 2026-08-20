// Criterios estaticos de busqueda que no vienen de la configuracion dinamica
// del backend (GET /api/profile-config): ProfileConfig no tiene un campo de
// ubicacion, asi que la zona/operacion de busqueda se fija aqui a mano.
export const configEstatico = {
  // Slug de ubicacion tal y como lo usa Habitaclia en su URL
  // (https://www.habitaclia.com/viviendas-<slug>.htm), p.ej. "madrid",
  // "madrid-capital", "barcelona"...
  habitacliaUbicacion: 'madrid',
  // 'venta' o 'alquiler'.
  habitacliaOperacion: 'venta',
};
