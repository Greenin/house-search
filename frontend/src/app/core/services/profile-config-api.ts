import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { ProfileConfig } from '../models/profile-config.model';

@Injectable({ providedIn: 'root' })
export class ProfileConfigApi {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = '/api/profile-config';

  obtener(): Observable<ProfileConfig> {
    return this.http.get<ProfileConfig>(this.baseUrl);
  }

  guardar(config: ProfileConfig): Observable<ProfileConfig> {
    return this.http.put<ProfileConfig>(this.baseUrl, config);
  }
}
