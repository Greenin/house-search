import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { House } from '../models/house.model';
import { SelectedHouse } from '../models/selected-house.model';

@Injectable({ providedIn: 'root' })
export class HouseApi {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = '/api/house';

  listar(): Observable<House[]> {
    return this.http.get<House[]>(this.baseUrl);
  }

  limpiar(): Observable<void> {
    return this.http.post<void>(`${this.baseUrl}/clear`, {});
  }

  seleccionar(idCasa: number): Observable<SelectedHouse> {
    return this.http.post<SelectedHouse>(`/api/selected_house/${idCasa}/copy`, {});
  }
}
