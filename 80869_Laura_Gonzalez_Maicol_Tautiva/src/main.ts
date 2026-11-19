/// <reference types="@angular/localize" />

import { enableProdMode, importProvidersFrom } from '@angular/core';

import { environment } from './environments/environment';
import { BrowserModule, bootstrapApplication } from '@angular/platform-browser';
import { provideHttpClient, withInterceptors } from '@angular/common/http';

import { AppRoutingModule } from './app/app-routing.module';
import { AppComponent } from './app/app.component';
import { responseToastInterceptor } from './app/services/response-toast.interceptor';
import { perfilHttpInterceptor } from './app/services/perfil-http.interceptor';

if (environment.production) {
  enableProdMode();
}

bootstrapApplication(AppComponent, {
  providers: [importProvidersFrom(BrowserModule, AppRoutingModule), provideHttpClient(withInterceptors([perfilHttpInterceptor, responseToastInterceptor]))]
}).catch((err) => console.error(err));
