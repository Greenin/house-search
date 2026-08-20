export type EstadoCasa = 'NUEVA' | 'SEGUNDA_MANO';
export type Fuente = 'IDEALISTA' | 'FOTOCASA' | 'HABITACLIA' | 'OTRA';
export type Prioridad = 'ALTA' | 'MEDIA' | 'BAJA';

export interface House {
  idCasa: number;
  titulo: string;
  ubicacion: string | null;
  precio: number | null;
  tamano: number | null;
  habitaciones: number | null;
  banos: number | null;
  planta: string | null;
  estado: EstadoCasa | null;
  terraza: boolean | null;
  orientacion: string | null;
  ascensor: boolean | null;
  descripcion: string | null;
  climatizacion: string | null;
  calefaccion: string | null;
  tipoCalefaccion: string | null;
  caracteristicasBasicas: string | null;
  consumoEnergetico: string | null;
  fuente: Fuente;
  prioridad: Prioridad | null;
  emailContacto: string | null;
  fechaLocalizacionCasa: string | null;
  enlaceCasa: string | null;
  puntuacion: number | null;
  razonesAFavor: string | null;
  razonesEnContra: string | null;
  fechaEvaluacion: string | null;
  modeloUsado: string | null;
}
