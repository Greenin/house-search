import { Component, OnInit, inject, signal } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatSelectModule } from '@angular/material/select';
import { MatSnackBar } from '@angular/material/snack-bar';
import { MatTableModule } from '@angular/material/table';
import { MatTooltipModule } from '@angular/material/tooltip';
import { SelectedHouseApi } from '../../core/services/selected-house-api';
import { ESTADOS_GESTION, EstadoGestionCasa, SelectedHouse } from '../../core/models/selected-house.model';

const COLUMNAS = [
  'titulo',
  'ubicacion',
  'precio',
  'tamano',
  'habitaciones',
  'banos',
  'puntuacion',
  'fuente',
  'fechaSeleccionCasa',
  'estadoGestion',
  'acciones',
];

@Component({
  selector: 'app-casas-seleccionadas',
  imports: [MatButtonModule, MatIconModule, MatSelectModule, MatTableModule, MatTooltipModule],
  templateUrl: './casas-seleccionadas.html',
  styleUrl: './casas-seleccionadas.scss',
})
export class CasasSeleccionadas implements OnInit {
  private readonly selectedHouseApi = inject(SelectedHouseApi);
  private readonly snackBar = inject(MatSnackBar);

  protected readonly columnas = COLUMNAS;
  protected readonly estadosGestion = ESTADOS_GESTION;

  protected readonly casas = signal<SelectedHouse[]>([]);
  protected readonly cargando = signal(false);

  ngOnInit(): void {
    this.cargar();
  }

  protected cargar(): void {
    this.cargando.set(true);
    this.selectedHouseApi.listar().subscribe({
      next: (casas) => {
        this.casas.set(casas);
        this.cargando.set(false);
      },
      error: () => {
        this.cargando.set(false);
        this.snackBar.open('No se pudieron cargar las casas seleccionadas.', 'Cerrar', { duration: 5000 });
      },
    });
  }

  protected cambiarEstado(casa: SelectedHouse, nuevoEstado: EstadoGestionCasa): void {
    this.selectedHouseApi.cambiarEstado(casa.idCasaSeleccionada, nuevoEstado).subscribe({
      next: (actualizada) => {
        this.casas.update((lista) =>
          lista.map((c) => (c.idCasaSeleccionada === actualizada.idCasaSeleccionada ? actualizada : c)),
        );
        this.snackBar.open('Estado actualizado.', 'Cerrar', { duration: 3000 });
      },
      error: () => this.snackBar.open('No se pudo cambiar el estado.', 'Cerrar', { duration: 5000 }),
    });
  }

  protected borrar(casa: SelectedHouse): void {
    if (!confirm(`¿Borrar "${casa.titulo}" de las casas seleccionadas?`)) {
      return;
    }
    this.selectedHouseApi.borrar(casa.idCasaSeleccionada).subscribe({
      next: () => {
        this.casas.update((lista) => lista.filter((c) => c.idCasaSeleccionada !== casa.idCasaSeleccionada));
        this.snackBar.open('Casa eliminada de seleccionadas.', 'Cerrar', { duration: 4000 });
      },
      error: () => this.snackBar.open('No se pudo borrar la casa.', 'Cerrar', { duration: 5000 }),
    });
  }
}
