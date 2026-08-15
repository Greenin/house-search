import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { SearchStatus } from '../models/search-status.model';

@Injectable({ providedIn: 'root' })
export class SearchApi {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = '/api/search';

  ejecutar(): Observable<SearchStatus> {
    return this.http.post<SearchStatus>(
      `${this.baseUrl}/run`,
      {},
      { headers: { 'X-API-Key': environment.apiKey } },
    );
  }

  estado(): Observable<SearchStatus> {
    return this.http.get<SearchStatus>(`${this.baseUrl}/status`);
  }
}
