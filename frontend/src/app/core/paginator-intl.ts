import { Injectable } from '@angular/core';
import { MatPaginatorIntl } from '@angular/material/paginator';

/** Textos del paginador de Angular Material en español. */
@Injectable()
export class PaginatorIntlEs extends MatPaginatorIntl {
  override itemsPerPageLabel = 'Filas por página:';
  override nextPageLabel = 'Página siguiente';
  override previousPageLabel = 'Página anterior';
  override firstPageLabel = 'Primera página';
  override lastPageLabel = 'Última página';

  override getRangeLabel = (page: number, pageSize: number, length: number): string => {
    if (length === 0) {
      return 'Mostrando 0 de 0 · Total: 0 registros';
    }
    const inicio = page * pageSize + 1;
    const fin = Math.min((page + 1) * pageSize, length);
    return `Mostrando ${inicio} – ${fin} · Total: ${length} ${length === 1 ? 'registro' : 'registros'}`;
  };
}
