export type EstadoBusqueda = 'INACTIVA' | 'EN_EJECUCION' | 'COMPLETADA' | 'FALLIDA';

export interface SearchStatus {
  estado: EstadoBusqueda;
  fechaInicio: string | null;
  fechaFin: string | null;
  codigoSalida: number | null;
  mensaje: string | null;
}
