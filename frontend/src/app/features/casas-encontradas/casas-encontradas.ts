import { DatePipe } from '@angular/common';
import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSnackBar } from '@angular/material/snack-bar';
import { MatTableModule } from '@angular/material/table';
import { MatTooltipModule } from '@angular/material/tooltip';
import { HouseApi } from '../../core/services/house-api';
import { SearchApi } from '../../core/services/search-api';
import { House } from '../../core/models/house.model';
import { SearchStatus } from '../../core/models/search-status.model';

const COLUMNAS_VISIBLES = [
  'titulo',
  'ubicacion',
  'precio',
  'tamano',
  'habitaciones',
  'banos',
  'planta',
  'estado',
  'puntuacion',
  'enlace',
  'acciones',
];

@Component({
  selector: 'app-casas-encontradas',
  imports: [DatePipe, MatButtonModule, MatIconModule, MatProgressSpinnerModule, MatTableModule, MatTooltipModule],
  templateUrl: './casas-encontradas.html',
  styleUrl: './casas-encontradas.scss',
})
export class CasasEncontradas implements OnInit {
  private readonly houseApi = inject(HouseApi);
  private readonly searchApi = inject(SearchApi);
  private readonly snackBar = inject(MatSnackBar);

  protected readonly columnasVisibles = COLUMNAS_VISIBLES;

  protected readonly casas = signal<House[]>([]);
  protected readonly cargando = signal(false);
  protected readonly ejecutandoBusqueda = signal(false);
  protected readonly estadoBusqueda = signal<SearchStatus | null>(null);
  protected readonly idCasaExpandida = signal<number | null>(null);

  protected readonly puedeBuscarHoy = computed(() => {
    const estado = this.estadoBusqueda();
    if (!estado) return true;
    if (estado.estado === 'EN_EJECUCION') return false;
    if (!estado.fechaInicio) return true;
    const inicio = new Date(estado.fechaInicio);
    const hoy = new Date();
    return !(
      inicio.getFullYear() === hoy.getFullYear() &&
      inicio.getMonth() === hoy.getMonth() &&
      inicio.getDate() === hoy.getDate()
    );
  });

  ngOnInit(): void {
    this.cargarCasas();
    this.cargarEstadoBusqueda();
  }

  protected cargarCasas(): void {
    this.cargando.set(true);
    this.houseApi.listar().subscribe({
      next: (casas) => {
        this.casas.set(casas);
        this.cargando.set(false);
      },
      error: () => {
        this.cargando.set(false);
        this.snackBar.open('No se pudieron cargar las casas encontradas.', 'Cerrar', { duration: 5000 });
      },
    });
  }

  private cargarEstadoBusqueda(): void {
    this.searchApi.estado().subscribe({
      next: (estado) => this.estadoBusqueda.set(estado),
      error: () => {
        // silencioso: la vista sigue siendo utilizable sin el estado de busqueda
      },
    });
  }

  protected limpiar(): void {
    if (!confirm('¿Seguro que quieres borrar todas las casas encontradas?')) {
      return;
    }
    this.houseApi.limpiar().subscribe({
      next: () => {
        this.casas.set([]);
        this.snackBar.open('Casas encontradas eliminadas.', 'Cerrar', { duration: 4000 });
      },
      error: () => this.snackBar.open('No se pudieron borrar las casas encontradas.', 'Cerrar', { duration: 5000 }),
    });
  }

  protected seleccionar(casa: House): void {
    this.houseApi.seleccionar(casa.idCasa).subscribe({
      next: () => {
        this.casas.update((lista) => lista.filter((c) => c.idCasa !== casa.idCasa));
        this.snackBar.open(`"${casa.titulo}" movida a Casas seleccionadas.`, 'Cerrar', { duration: 4000 });
      },
      error: () => this.snackBar.open('No se pudo seleccionar la casa.', 'Cerrar', { duration: 5000 }),
    });
  }

  protected alternarDetalle(idCasa: number): void {
    this.idCasaExpandida.update((actual) => (actual === idCasa ? null : idCasa));
  }

  protected textoPlano(html: string | null): string {
    if (!html) return '';
    return html.replace(/<[^>]*>/g, ' ').replace(/\s+/g, ' ').trim();
  }

  protected ejecutarBusqueda(): void {
    this.ejecutandoBusqueda.set(true);
    this.searchApi.ejecutar().subscribe({
      next: (estado) => {
        this.estadoBusqueda.set(estado);
        this.esperarFinDeBusqueda();
      },
      error: (err) => {
        this.ejecutandoBusqueda.set(false);
        if (err.status === 409) {
          this.snackBar.open('La búsqueda ya se está ejecutando.', 'Cerrar', { duration: 5000 });
        } else if (err.status === 429) {
          this.snackBar.open(
            'Ya se ejecutó hoy. Solo se permite una vez al día para no bloquear tu IP.',
            'Cerrar',
            { duration: 6000 },
          );
        } else {
          this.snackBar.open('No se pudo lanzar la búsqueda.', 'Cerrar', { duration: 5000 });
        }
        this.cargarEstadoBusqueda();
      },
    });
  }

  private esperarFinDeBusqueda(): void {
    this.searchApi.estado().subscribe({
      next: (estado) => {
        this.estadoBusqueda.set(estado);
        if (estado.estado === 'EN_EJECUCION') {
          setTimeout(() => this.esperarFinDeBusqueda(), 1500);
          return;
        }
        this.ejecutandoBusqueda.set(false);
        this.cargarCasas();
        if (estado.estado === 'COMPLETADA') {
          this.snackBar.open('Búsqueda completada.', 'Cerrar', { duration: 4000 });
        } else if (estado.estado === 'FALLIDA') {
          this.snackBar.open('La búsqueda ha fallado.', 'Cerrar', { duration: 5000 });
        }
      },
      error: () => {
        this.ejecutandoBusqueda.set(false);
      },
    });
  }
}
