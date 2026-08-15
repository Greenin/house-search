export const ORIENTACIONES: string[] = [
  'Norte',
  'Sur',
  'Este',
  'Oeste',
  'Noreste',
  'Noroeste',
  'Sureste',
  'Suroeste',
];

export interface ProfileConfig {
  id?: number;
  contextoCasaBuscada: string | null;
  precioMaximo: number | null;
  tamanoMinimo: number | null;
  numeroHabitacionesMinimo: number | null;
  numeroBanosMinimo: number | null;
  terrazaRequerida: boolean | null;
  orientacion: string | null;
  ascensorRequerido: boolean | null;
  climatizacionRequerida: boolean | null;
  palabrasClave: string[];
  filtrosNegativos: string[];
}
