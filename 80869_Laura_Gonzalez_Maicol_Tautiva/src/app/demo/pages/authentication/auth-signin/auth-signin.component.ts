// angular import
import { Component, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router, RouterModule } from '@angular/router';
import { form, FormField, minLength, required } from '@angular/forms/signals';

// project import
import { SharedModule } from 'src/app/theme/shared/shared.module';
import { AuthService } from 'src/app/services/auth.service';

@Component({
  selector: 'app-auth-signin',
  imports: [CommonModule, RouterModule, SharedModule, FormField],
  templateUrl: './auth-signin.component.html',
  styleUrls: ['./auth-signin.component.scss']
})
export class AuthSigninComponent {
  private auth = inject(AuthService);
  private router = inject(Router);

  submitted = signal(false);
  error = signal('');
  showPassword = signal(false);

  loginModal = signal<{ username: string; password: string }>({
    username: '',
    password: ''
  });

  loginForm = form(this.loginModal, (schemaPath) => {
    required(schemaPath.username, { message: 'El username es obligatorio' });
    minLength(schemaPath.username, 3, { message: 'El username debe tener al menos 3 caracteres' });
    required(schemaPath.password, { message: 'Password is required' });
    minLength(schemaPath.password, 8, { message: 'Password must be at least 8 characters' });
  });

  onSubmit(event: Event) {
    this.submitted.set(true);
    this.error.set('');
    event.preventDefault();
    const credentials = this.loginModal();
    if (this.loginForm.username().invalid() || this.loginForm.password().invalid()) return;
    this.auth.login(credentials.username, credentials.password).subscribe({
      next: response => {
        if (!response.login || !response.token) { this.error.set('Usuario o contraseña incorrectos.'); return; }
        this.auth.guardarSesion(response);
        const path = response.rol === 'DESPACHO_MEDICO' ? '/despacho-medico'
          : response.rol === 'VETERINARIO' ? '/calendario-medico' : '/citas';
        void this.router.navigateByUrl(path);
      },
      error: err => this.error.set(err?.error?.mensaje || 'No se pudo iniciar sesión. Verifica el usuario, la contraseña y el estado del backend.')
    });
  }

  togglePasswordVisibility() {
    this.showPassword.set(!this.showPassword());
  }
}
