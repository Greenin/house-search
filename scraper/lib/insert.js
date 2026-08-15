export async function enviarCasas(casas) {
  const baseUrl = process.env.BACKEND_URL || 'http://localhost:8080';
  const apiKey = process.env.INSERT_API_KEY;

  const respuesta = await fetch(`${baseUrl}/api/house/insert`, {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      'X-API-Key': apiKey,
    },
    body: JSON.stringify(casas),
  });

  if (!respuesta.ok) {
    const texto = await respuesta.text().catch(() => '');
    throw new Error(`Fallo al insertar casas (${respuesta.status}): ${texto}`);
  }

  return respuesta.json();
}
