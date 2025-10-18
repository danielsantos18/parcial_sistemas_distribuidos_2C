import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { environment } from '../environments/environment';
import { Observable } from 'rxjs';

@Injectable({ providedIn: 'root' })
export class CardService {
  constructor(private http: HttpClient) {}

  createCard(userId: string, type: 'CREDIT' | 'DEBIT'): Observable<any> {
    return this.http.post(environment.apiCardRequest, { userId, request: type });
  }

  activateCard(userId: string): Observable<any> {
    return this.http.post(environment.apiCardActivate, { userId });
  }

  getCard(cardId: string): Observable<any> {
    return this.http.get(`${environment.apiCardGet}/${cardId}`);
  }

  deposit(cardId: string, amount: number): Observable<any> {
    return this.http.post(`${environment.apiTransactionSave}/${cardId}`, {
      merchant: 'SAVING',
      amount,
    });
  }

  payCredit(cardId: string, amount: number): Observable<any> {
    return this.http.post(`${environment.apiCardPaid}/${cardId}`, {
      merchant: 'PSE',
      amount,
    });
  }

  purchase(cardId: string, merchant: string, amount: number): Observable<any> {
    return this.http.post(environment.apiTransactionPurchase, {
      cardId,
      merchant,
      amount,
    });
  }
}
