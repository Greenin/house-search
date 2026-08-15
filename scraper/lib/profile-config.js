export async function obtenerConfiguracionBusqueda() {
  const baseUrl = process.env.BACKEND_URL || 'http://localhost:8080';

  const respuesta = await fetch(`${baseUrl}/api/profile-config`);
  if (!respuesta.ok) {
    throw new Error(`No se pudo leer la configuracion de busqueda (${respuesta.status})`);
  }

  return respuesta.json();
}
