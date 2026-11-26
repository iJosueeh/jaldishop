import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { PromoCode, UpsertPromoCodePayload } from '../models/promo-code.models';

@Injectable({
  providedIn: 'root',
})
export class PromoCodeService {
  private readonly http = inject(HttpClient);
  private readonly adminBaseUrl = `${environment.apiUrl}/admin/promo-codes`;
  private readonly merchantBaseUrl = `${environment.apiUrl}/merchants/stores`;

  listGlobal(): Observable<PromoCode[]> {
    return this.http.get<PromoCode[]>(this.adminBaseUrl);
  }

  createGlobal(payload: UpsertPromoCodePayload): Observable<PromoCode> {
    return this.http.post<PromoCode>(this.adminBaseUrl, payload);
  }

  updateGlobal(id: string, payload: UpsertPromoCodePayload): Observable<PromoCode> {
    return this.http.put<PromoCode>(`${this.adminBaseUrl}/${id}`, payload);
  }

  toggleGlobal(id: string, active: boolean): Observable<PromoCode> {
    return this.http.patch<PromoCode>(`${this.adminBaseUrl}/${id}/toggle`, { active });
  }

  deleteGlobal(id: string): Observable<void> {
    return this.http.delete<void>(`${this.adminBaseUrl}/${id}`);
  }

  listByStore(storeId: string): Observable<PromoCode[]> {
    return this.http.get<PromoCode[]>(`${this.merchantBaseUrl}/${storeId}/promo-codes`);
  }

  createForStore(storeId: string, payload: UpsertPromoCodePayload): Observable<PromoCode> {
    return this.http.post<PromoCode>(`${this.merchantBaseUrl}/${storeId}/promo-codes`, payload);
  }

  updateForStore(storeId: string, promoId: string, payload: UpsertPromoCodePayload): Observable<PromoCode> {
    return this.http.put<PromoCode>(`${this.merchantBaseUrl}/${storeId}/promo-codes/${promoId}`, payload);
  }

  toggleForStore(storeId: string, promoId: string, active: boolean): Observable<PromoCode> {
    return this.http.patch<PromoCode>(`${this.merchantBaseUrl}/${storeId}/promo-codes/${promoId}/toggle`, { active });
  }

  deleteForStore(storeId: string, promoId: string): Observable<void> {
    return this.http.delete<void>(`${this.merchantBaseUrl}/${storeId}/promo-codes/${promoId}`);
  }
}
