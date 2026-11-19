import { Injectable, signal } from '@angular/core';

export type ToastKind = 'success' | 'error';

export interface ToastMessage {
  id: number;
  kind: ToastKind;
  title: string;
  message: string;
}

@Injectable({ providedIn: 'root' })
export class ToastService {
  private sequence = 0;
  private readonly items = signal<ToastMessage[]>([]);
  readonly toasts = this.items.asReadonly();

  success(message: string, title = 'Operación exitosa'): void {
    this.show('success', title, message);
  }

  error(message: string, title = 'No se pudo completar'): void {
    this.show('error', title, message);
  }

  dismiss(id: number): void {
    this.items.update((items) => items.filter((item) => item.id !== id));
  }

  private show(kind: ToastKind, title: string, message: string): void {
    const id = ++this.sequence;
    this.items.update((items) => [...items, { id, kind, title, message }]);
    window.setTimeout(() => this.dismiss(id), kind === 'error' ? 8000 : 4500);
  }
}
