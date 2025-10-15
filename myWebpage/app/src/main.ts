import { enableProdMode } from '@angular/core';
import { bootstrapApplication } from '@angular/platform-browser';
import { importProvidersFrom } from '@angular/core';
import { HttpClientModule } from '@angular/common/http';
import { MatTableModule } from '@angular/material/table';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';

import { CatalogComponent } from './app/catalog/catalog.component';
import { environment } from './app/environments/environment';
import { provideAnimationsAsync } from '@angular/platform-browser/animations/async'; 

if (environment.production) {
  enableProdMode();
}

bootstrapApplication(CatalogComponent, {
  providers: [
    importProvidersFrom(HttpClientModule, MatTableModule, MatProgressSpinnerModule), provideAnimationsAsync('noop')
  ]
}).catch(err => console.error(err));