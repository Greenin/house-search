import { Component } from '@angular/core';
import { MatIconModule } from '@angular/material/icon';
import { MatToolbarModule } from '@angular/material/toolbar';
import { RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';

@Component({
  selector: 'app-root',
  imports: [RouterOutlet, RouterLink, RouterLinkActive, MatToolbarModule, MatIconModule],
  templateUrl: './app.html',
  styleUrl: './app.scss',
})
export class App {
  protected readonly navLinks = [
    { path: 'casas-encontradas', label: 'Casas encontradas', icon: 'home_work' },
    { path: 'casas-seleccionadas', label: 'Casas seleccionadas', icon: 'check_circle' },
    { path: 'configuracion-busqueda', label: 'Configuración Búsqueda', icon: 'tune' },
    { path: 'ejecucion-busqueda', label: 'Ejecución búsqueda', icon: 'history' },
  ];
}
