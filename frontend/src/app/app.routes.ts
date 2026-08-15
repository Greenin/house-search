import { Routes } from '@angular/router';

export const routes: Routes = [
  { path: '', redirectTo: 'casas-encontradas', pathMatch: 'full' },
  {
    path: 'casas-encontradas',
    loadComponent: () =>
      import('./features/casas-encontradas/casas-encontradas').then((m) => m.CasasEncontradas),
  },
  {
    path: 'casas-seleccionadas',
    loadComponent: () =>
      import('./features/casas-seleccionadas/casas-seleccionadas').then((m) => m.CasasSeleccionadas),
  },
  {
    path: 'configuracion-busqueda',
    loadComponent: () =>
      import('./features/configuracion-busqueda/configuracion-busqueda').then(
        (m) => m.ConfiguracionBusqueda,
      ),
  },
  {
    path: 'ejecucion-busqueda',
    loadComponent: () =>
      import('./features/ejecucion-busqueda/ejecucion-busqueda').then((m) => m.EjecucionBusqueda),
  },
];
