import { EstadoCasa, Fuente, Prioridad } from './house.model';

export type EstadoGestionCasa =
  | 'PARA_INVESTIGAR'
  | 'POR_CONTACTAR'
  | 'CONTACTADA'
  | 'RESPUESTA_RECIBIDA'
  | 'CITA'
  | 'OFERTA'
  | 'RECHAZADA'
  | 'EN_PAUSA';

export const ESTADOS_GESTION: { value: EstadoGestionCasa; label: string }[] = [
  { value: 'PARA_INVESTIGAR', label: 'Para investigar' },
  { value: 'POR_CONTACTAR', label: 'Por contactar' },
  { value: 'CONTACTADA', label: 'Contactada' },
  { value: 'RESPUESTA_RECIBIDA', label: 'Respuesta recibida' },
  { value: 'CITA', label: 'Cita' },
  { value: 'OFERTA', label: 'Oferta' },
  { value: 'RECHAZADA', label: 'Rechazada' },
  { value: 'EN_PAUSA', label: 'En pausa' },
];

export interface SelectedHouse {
  idCasaSeleccionada: number;
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
  fuente: Fuente | null;
  prioridad: Prioridad | null;
  emailContacto: string | null;
  fechaLocalizacionCasa: string | null;
  enlaceCasa: string | null;
  fechaSeleccionCasa: string | null;
  puntuacion: number | null;
  razonesAFavor: string | null;
  razonesEnContra: string | null;
  fechaEvaluacion: string | null;
  modeloUsado: string | null;
  estadoGestion: EstadoGestionCasa | null;
}
