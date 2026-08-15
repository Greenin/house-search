import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { EstadoGestionCasa, SelectedHouse } from '../models/selected-house.model';

@Injectable({ providedIn: 'root' })
export class SelectedHouseApi {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = '/api/selected_house';

  listar(): Observable<SelectedHouse[]> {
    return this.http.get<SelectedHouse[]>(this.baseUrl);
  }

  cambiarEstado(id: number, estado: EstadoGestionCasa): Observable<SelectedHouse> {
    return this.http.patch<SelectedHouse>(`${this.baseUrl}/${id}/status`, { estado });
  }

  borrar(id: number): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/${id}/delete`);
  }
}
