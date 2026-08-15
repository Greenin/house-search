import { COMMA, ENTER } from '@angular/cdk/keycodes';
import { Component, OnInit, inject, signal } from '@angular/core';
import { FormControl, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatChipInputEvent, MatChipsModule } from '@angular/material/chips';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatSlideToggleModule } from '@angular/material/slide-toggle';
import { MatSnackBar } from '@angular/material/snack-bar';
import { ORIENTACIONES, ProfileConfig } from '../../core/models/profile-config.model';
import { ProfileConfigApi } from '../../core/services/profile-config-api';

const LONGITUD_MAXIMA_PALABRA = 255;

interface ConfiguracionForm {
  contextoCasaBuscada: FormControl<string | null>;
  precioMaximo: FormControl<number | null>;
  tamanoMinimo: FormControl<number | null>;
  numeroHabitacionesMinimo: FormControl<number | null>;
  numeroBanosMinimo: FormControl<number | null>;
  terrazaRequerida: FormControl<boolean>;
  orientacion: FormControl<string>;
  ascensorRequerido: FormControl<boolean>;
  climatizacionRequerida: FormControl<boolean>;
}

@Component({
  selector: 'app-configuracion-busqueda',
  imports: [
    ReactiveFormsModule,
    MatButtonModule,
    MatChipsModule,
    MatFormFieldModule,
    MatIconModule,
    MatInputModule,
    MatSelectModule,
    MatSlideToggleModule,
  ],
  templateUrl: './configuracion-busqueda.html',
  styleUrl: './configuracion-busqueda.scss',
})
export class ConfiguracionBusqueda implements OnInit {
  private readonly profileConfigApi = inject(ProfileConfigApi);
  private readonly snackBar = inject(MatSnackBar);

  protected readonly orientaciones = ORIENTACIONES;
  protected readonly separadoresChip = [ENTER, COMMA];

  protected readonly cargando = signal(false);
  protected readonly guardando = signal(false);
  protected readonly palabrasClave = signal<string[]>([]);
  protected readonly filtrosNegativos = signal<string[]>([]);

  protected readonly form = new FormGroup<ConfiguracionForm>({
    contextoCasaBuscada: new FormControl(''),
    precioMaximo: new FormControl(null, { validators: Validators.min(0) }),
    tamanoMinimo: new FormControl(null, { validators: Validators.min(0) }),
    numeroHabitacionesMinimo: new FormControl(null, { validators: Validators.min(0) }),
    numeroBanosMinimo: new FormControl(null, { validators: Validators.min(0) }),
    terrazaRequerida: new FormControl(false, { nonNullable: true }),
    orientacion: new FormControl('', { nonNullable: true }),
    ascensorRequerido: new FormControl(false, { nonNullable: true }),
    climatizacionRequerida: new FormControl(false, { nonNullable: true }),
  });

  ngOnInit(): void {
    this.cargando.set(true);
    this.profileConfigApi.obtener().subscribe({
      next: (config) => {
        this.form.patchValue({
          contextoCasaBuscada: config.contextoCasaBuscada ?? '',
          precioMaximo: config.precioMaximo,
          tamanoMinimo: config.tamanoMinimo,
          numeroHabitacionesMinimo: config.numeroHabitacionesMinimo,
          numeroBanosMinimo: config.numeroBanosMinimo,
          terrazaRequerida: config.terrazaRequerida ?? false,
          orientacion: config.orientacion ?? '',
          ascensorRequerido: config.ascensorRequerido ?? false,
          climatizacionRequerida: config.climatizacionRequerida ?? false,
        });
        this.palabrasClave.set(config.palabrasClave ?? []);
        this.filtrosNegativos.set(config.filtrosNegativos ?? []);
        this.cargando.set(false);
      },
      error: () => {
        this.cargando.set(false);
        this.snackBar.open('No se pudo cargar la configuración de búsqueda.', 'Cerrar', { duration: 5000 });
      },
    });
  }

  protected anadirPalabraClave(event: MatChipInputEvent): void {
    this.anadirElemento(event, this.palabrasClave);
  }

  protected quitarPalabraClave(palabra: string): void {
    this.palabrasClave.update((lista) => lista.filter((p) => p !== palabra));
  }

  protected anadirFiltroNegativo(event: MatChipInputEvent): void {
    this.anadirElemento(event, this.filtrosNegativos);
  }

  protected quitarFiltroNegativo(filtro: string): void {
    this.filtrosNegativos.update((lista) => lista.filter((f) => f !== filtro));
  }

  private anadirElemento(event: MatChipInputEvent, destino: ReturnType<typeof signal<string[]>>): void {
    const valor = (event.value ?? '').trim();
    event.chipInput?.clear();
    if (!valor) {
      return;
    }
    if (valor.length > LONGITUD_MAXIMA_PALABRA) {
      this.snackBar.open(`Cada palabra debe tener como máximo ${LONGITUD_MAXIMA_PALABRA} caracteres.`, 'Cerrar', {
        duration: 4000,
      });
      return;
    }
    destino.update((lista) => (lista.includes(valor) ? lista : [...lista, valor]));
  }

  protected guardar(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      this.snackBar.open('Revisa los campos marcados en rojo antes de guardar.', 'Cerrar', { duration: 4000 });
      return;
    }

    const valores = this.form.getRawValue();
    const config: ProfileConfig = {
      contextoCasaBuscada: valores.contextoCasaBuscada,
      precioMaximo: valores.precioMaximo,
      tamanoMinimo: valores.tamanoMinimo,
      numeroHabitacionesMinimo: valores.numeroHabitacionesMinimo,
      numeroBanosMinimo: valores.numeroBanosMinimo,
      terrazaRequerida: valores.terrazaRequerida,
      orientacion: valores.orientacion === '' ? null : valores.orientacion,
      ascensorRequerido: valores.ascensorRequerido,
      climatizacionRequerida: valores.climatizacionRequerida,
      palabrasClave: this.palabrasClave(),
      filtrosNegativos: this.filtrosNegativos(),
    };

    this.guardando.set(true);
    this.profileConfigApi.guardar(config).subscribe({
      next: () => {
        this.guardando.set(false);
        this.snackBar.open('Configuración de búsqueda guardada.', 'Cerrar', { duration: 4000 });
      },
      error: () => {
        this.guardando.set(false);
        this.snackBar.open('No se pudo guardar la configuración.', 'Cerrar', { duration: 5000 });
      },
    });
  }
}
