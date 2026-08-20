import { Component, OnInit, inject, signal } from '@angular/core';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSnackBar } from '@angular/material/snack-bar';
import { SearchApi } from '../../core/services/search-api';
import { SearchStatus } from '../../core/models/search-status.model';

@Component({
  selector: 'app-ejecucion-busqueda',
  imports: [MatFormFieldModule, MatInputModule],
  templateUrl: './ejecucion-busqueda.html',
  styleUrl: './ejecucion-busqueda.scss',
})
export class EjecucionBusqueda implements OnInit {
  private readonly searchApi = inject(SearchApi);
  private readonly snackBar = inject(MatSnackBar);

  protected readonly estadoCompleta = signal<SearchStatus | null>(null);
  protected readonly estadoSinPlaywright = signal<SearchStatus | null>(null);

  ngOnInit(): void {
    this.searchApi.estado().subscribe({
      next: (estado) => this.estadoCompleta.set(estado),
      error: () =>
        this.snackBar.open('No se pudo cargar el estado de "Ejecutar búsqueda".', 'Cerrar', { duration: 5000 }),
    });

    this.searchApi.estadoSinPlaywright().subscribe({
      next: (estado) => this.estadoSinPlaywright.set(estado),
      error: () =>
        this.snackBar.open('No se pudo cargar el estado de "Búsqueda sin Playwright".', 'Cerrar', {
          duration: 5000,
        }),
    });
  }
}
