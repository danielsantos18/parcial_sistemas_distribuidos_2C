import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { HttpClient } from '@angular/common/http';
import { FormsModule } from '@angular/forms';
import { NavbarComponent } from '../../shared/components/navbar/navbar';
import { Router } from '@angular/router';
import { Observable, of } from 'rxjs';
import { catchError, delay } from 'rxjs/operators';

@Component({
  selector: 'app-home',
  standalone: true,
  imports: [CommonModule, FormsModule, NavbarComponent],
  templateUrl: './home.html',
  styleUrls: ['./home.scss']
})
export class HomeComponent implements OnInit {
  services: any[] = [];
  selectedService: any = null;
  traceId: string | null = null;
  status: string = '';
  message: string = '';
  loading: boolean = false;
  polling = false;
  user: any = null;

  // Si tienes API real pon aquí la base. Si no, se usará el catálogo simulado.
  private apiUrl = 'https://YOUR_API_GATEWAY_URL.amazonaws.com';

  // catálogo local (simulación)
  private localCatalog = [
    { id: 1, categoria: 'Energía', proveedor: 'Empresa Eléctrica Nacional', servicio: 'Luz Residencial', plan: 'Básico', precio_mensual: 45000, detalles: '150 kWh incluidos', estado: 'Activo' },
    { id: 2, categoria: 'Energía', proveedor: 'Empresa Eléctrica Nacional', servicio: 'Luz Residencial', plan: 'Premium', precio_mensual: 75000, detalles: '300 kWh incluidos', estado: 'Activo' },
    { id: 3, categoria: 'Agua', proveedor: 'Acuacol', servicio: 'Agua Residencial', plan: 'Básico', precio_mensual: 30000, detalles: 'Consumo básico', estado: 'Activo' },
  ];

  constructor(private http: HttpClient, private router: Router) {}

  ngOnInit() {
    const userData = localStorage.getItem('user');
    if (userData) {
      this.user = JSON.parse(userData);
    } else {
      this.router.navigate(['/auth']);
      return;
    }
    this.getCatalog();
  }

  getCatalog() {
    // intenta pedir al backend; si falla, usa la lista local
    this.fetchCatalogFromApi().pipe(
      catchError(err => {
        console.warn('Catalog API failed, using local catalog', err);
        return of(this.localCatalog);
      })
    ).subscribe((res: any) => {
      this.services = res || [];
    });
  }

  private fetchCatalogFromApi(): Observable<any> {
    // GET /catalog
    return this.http.get<any[]>(`${this.apiUrl}/catalog`).pipe(
      // si quieres simular latencia del backend en desarrollo, descomenta:
      // delay(300)
    );
  }

  selectService(s: any) {
    this.selectedService = s;
  }

  startPayment() {
    if (!this.selectedService) { this.message = 'Select a service first'; return; }

    this.loading = true;
    this.message = 'Starting payment...';
    this.status = 'INITIAL';

    const paymentBody = {
      cardId: this.getDefaultCardId(),
      service: this.selectedService
    };

    // Si quieres probar sin backend, simular la respuesta con traceId
    if (this.apiUrl.includes('YOUR_API_GATEWAY_URL')) {
      // simulación local
      const simulatedTrace = this.simulateStartPayment(paymentBody);
      this.onPaymentStarted(simulatedTrace);
      return;
    }

    // llamada real
    this.http.post<{ traceId: string }>(`${this.apiUrl}/payment`, paymentBody).subscribe({
      next: (res) => this.onPaymentStarted(res.traceId),
      error: (err) => {
        console.error(err);
        this.loading = false;
        this.message = 'Failed to start payment';
        this.status = 'FAILED';
      }
    });
  }

  private onPaymentStarted(traceId: string) {
    this.traceId = traceId;
    this.status = 'IN_PROGRESS';
    this.message = `Trace ID: ${traceId}`;
    this.loading = true;
    this.polling = true;
    this.pollStatus(); // inicia polling
  }

  pollStatus() {
    if (!this.traceId) return;

    // Si estás con backend real llama /payment/status/{traceId}
    // Aquí simulamos estados secuenciales si no hay backend
    if (this.apiUrl.includes('YOUR_API_GATEWAY_URL')) {
      this.simulatePolling().subscribe(statusObj => {
        this.status = statusObj.status;
        this.message = statusObj.message;
        this.loading = false;
        this.polling = false;
      });
      return;
    }

    // Polling real cada 3s
    const intervalId = setInterval(() => {
      this.http.get<any>(`${this.apiUrl}/payment/status/${this.traceId}`).subscribe({
        next: (res) => {
          this.status = res.status;
          if (res.status === 'FINISH' || res.status === 'FAILED') {
            clearInterval(intervalId);
            this.loading = false;
            this.message = res.status === 'FINISH' ? 'Payment completed' : `Payment failed: ${res.error || 'Unknown'}`;
            this.polling = false;
          }
        },
        error: (err) => {
          clearInterval(intervalId);
          console.error(err);
          this.loading = false;
          this.status = 'FAILED';
          this.message = 'Error checking payment status';
          this.polling = false;
        }
      });
    }, 3000);
  }

  // Helpers para simular
  private simulateStartPayment(_body: any): string {
    // genera pseudo-uuid simple
    return 'trace-' + Math.random().toString(36).substring(2, 10);
  }

  private simulatePolling(): Observable<{ status: string, message: string }> {
    // simulamos flujo: IN_PROGRESS (3s) -> FINISH (3s)
    // retornamos un observable que emite FINISH después de 6s
    return of({ status: 'FINISH', message: '✅ Payment completed (simulated)' }).pipe(delay(2000));
  }

  private getDefaultCardId(): string {
    // usar tu cardId de pruebas del parcial
    return '39fe6315-2dd5-4f2d-9160-22f1c96a05c8';
  }

  goToProfile() {
    this.router.navigate(['/profile']);
  }

  
}
