import { Component, inject } from '@angular/core';
import { ToastService } from '../../../../services/toast.service';

@Component({
  selector: 'app-toast-host',
  standalone: true,
  template: `
    <section class="toast-stack" aria-live="polite" aria-label="Notificaciones">
      @for (toast of service.toasts(); track toast.id) {
        <article class="toast-card" [class.toast-success]="toast.kind === 'success'" [class.toast-error]="toast.kind === 'error'"
          [attr.role]="toast.kind === 'error' ? 'alert' : 'status'">
          <span class="toast-icon" aria-hidden="true">{{ toast.kind === 'success' ? '✓' : '!' }}</span>
          <div class="toast-copy"><strong>{{ toast.title }}</strong><span>{{ toast.message }}</span></div>
          <button type="button" class="toast-close" aria-label="Cerrar notificación" (click)="service.dismiss(toast.id)">×</button>
        </article>
      }
    </section>
  `,
  styles: [`
    .toast-stack { position: fixed; z-index: 3000; top: 1.25rem; right: 1.25rem; display: grid; gap: .75rem; width: min(390px, calc(100vw - 2rem)); pointer-events: none; }
    .toast-card { display: flex; align-items: flex-start; gap: .75rem; padding: 1rem; border: 1px solid #e5e7eb; border-left: 4px solid; border-radius: 12px; background: #fff; box-shadow: 0 12px 35px #17203326; color: #263248; pointer-events: auto; animation: toast-in .2s ease-out; }
    .toast-success { border-left-color: #16a36a; }.toast-error { border-left-color: #dc3545; }
    .toast-icon { display: grid; place-items: center; flex: 0 0 1.65rem; height: 1.65rem; border-radius: 50%; font-weight: 800; color: #fff; background: #16a36a; }.toast-error .toast-icon { background: #dc3545; }
    .toast-copy { display: grid; gap: .2rem; min-width: 0; }.toast-copy strong { font-size: .92rem; }.toast-copy span { font-size: .86rem; line-height: 1.4; overflow-wrap: anywhere; color: #5c6678; }
    .toast-close { border: 0; background: transparent; color: #758094; font-size: 1.3rem; line-height: 1; margin-left: auto; cursor: pointer; }
    @keyframes toast-in { from { opacity: 0; transform: translateY(-8px); } to { opacity: 1; transform: translateY(0); } }
    @media (prefers-reduced-motion: reduce) { .toast-card { animation: none; } }
  `]
})
export class ToastHostComponent {
  readonly service = inject(ToastService);
}
