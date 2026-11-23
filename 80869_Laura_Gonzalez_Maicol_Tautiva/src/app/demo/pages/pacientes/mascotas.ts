import { CommonModule } from '@angular/common';
import { ChangeDetectorRef, Component, OnInit } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { of, switchMap, throwError } from 'rxjs';
import { SharedModule } from 'src/app/theme/shared/shared.module';
import { MascotaPaciente, MascotaRequest, MascotasService } from 'src/app/services/mascotas.service';

@Component({
  selector: 'app-mascotas', standalone: true, imports: [CommonModule, FormsModule, SharedModule],
  templateUrl: './mascotas.html', styleUrl: './mascotas.scss'
})
export class MascotasComponent implements OnInit {
  mascotas: MascotaPaciente[] = [];
  cargando = true; guardando = false; modal = false; error = ''; mensaje = '';
  editandoId: number | null = null;
  filtro = '';
  form = { nombre: '', especie: '', raza: '', sexo: '', edad: null as number | null, clienteNombre: '' };

  constructor(private service: MascotasService, private cdr: ChangeDetectorRef) {}
  ngOnInit(): void { this.cargar(); }
  get filtradas(): MascotaPaciente[] {
    const q = this.filtro.trim().toLocaleLowerCase();
    return this.mascotas.filter(m => !q || `${m.nombre} ${m.especie} ${m.raza ?? ''} ${m.sexo ?? ''} ${m.cliente?.nombre} ${m.cliente?.apellido}`.toLocaleLowerCase().includes(q));
  }
  cargar(): void {
    this.cargando = true;
    this.service.listar().subscribe({
      next: mascotas => { this.mascotas = mascotas; this.cargando = false; this.cdr.detectChanges(); },
      error: e => { this.error = this.mensajeError(e); this.cargando = false; this.cdr.detectChanges(); }
    });
  }
  nueva(): void { this.editandoId = null; this.form = { nombre: '', especie: '', raza: '', sexo: '', edad: null, clienteNombre: '' }; this.error = ''; this.mensaje = ''; this.modal = true; }
  editar(m: MascotaPaciente): void {
    this.editandoId = m.id;
    this.form = {
      nombre: m.nombre,
      especie: m.especie,
      raza: m.raza ?? '',
      sexo: m.sexo ?? '',
      edad: m.edad ?? null,
      clienteNombre: `${m.cliente?.nombre ?? ''} ${m.cliente?.apellido ?? ''}`.trim()
    };
    this.error = '';
    this.mensaje = '';
    this.modal = true;
  }
  guardar(): void {
    if (!this.form.nombre.trim() || !this.form.especie.trim() || !this.form.clienteNombre.trim()) {
      this.error = 'Nombre del paciente, especie y nombre del cliente son obligatorios.';
      return;
    }
    if (this.form.edad != null && this.form.edad < 0) { this.error = 'La edad no puede ser negativa.'; return; }
    const body: Omit<MascotaRequest, 'cliente'> = {
      nombre: this.form.nombre.trim(),
      especie: this.form.especie.trim(),
      raza: this.form.raza.trim(),
      sexo: this.form.sexo.trim(),
      edad: this.form.edad
    };
    this.guardando = true;
    const nombreCliente = this.form.clienteNombre.trim();
    const nombreNormalizado = this.normalizarNombre(nombreCliente);
    const request = this.service.clientes().pipe(
      switchMap(clientes => {
        const coincidencias = clientes.filter(cliente =>
          [cliente.nombre, `${cliente.nombre} ${cliente.apellido ?? ''}`]
            .some(nombre => this.normalizarNombre(nombre) === nombreNormalizado)
        );
        if (coincidencias.length > 1) {
          return throwError(() => new Error(
            'Hay varios clientes con ese nombre. Escribe el nombre completo del cliente.'
          ));
        }
        return coincidencias.length === 1
          ? of(coincidencias[0])
          : this.service.crearCliente(nombreCliente);
      }),
      switchMap(cliente => {
        const mascota: MascotaRequest = { ...body, cliente: { id: cliente.id } };
        return this.editandoId
          ? this.service.actualizar(this.editandoId, mascota)
          : this.service.crear(mascota);
      })
    );
    request.subscribe({ next: () => { this.modal = false; this.guardando = false; this.mensaje = 'Paciente guardado en la base de datos.'; this.cargar(); }, error: e => { this.error = this.mensajeError(e); this.guardando = false; this.cdr.detectChanges(); } });
  }
  desactivar(m: MascotaPaciente): void {
    if (!confirm(`¿Desactivar a ${m.nombre}? Sus citas e historia clínica se conservarán.`)) return;
    this.service.desactivar(m.id).subscribe({ next: () => { this.mensaje = 'Paciente desactivado; el historial se conserva.'; this.cargar(); }, error: e => { this.error = this.mensajeError(e); } });
  }
  private mensajeError(e: any): string {
    if (e?.status === 0) return 'No se pudo conectar con el backend. Verifica que Spring Boot y MySQL estén iniciados.';
    if (e?.status === 401) return e?.error?.mensaje ?? 'Tu sesión venció. Inicia sesión nuevamente.';
    if (e?.status === 403) return e?.error?.mensaje ?? 'No tienes permiso para realizar esta acción.';
    return e?.error?.mensaje ?? e?.error?.message ?? e?.message ?? 'No fue posible guardar el paciente. Intenta de nuevo.';
  }
  private normalizarNombre(nombre: string): string {
    return nombre.normalize('NFD').replace(/[\u0300-\u036f]/g, '').trim().replace(/\s+/g, ' ').toLocaleLowerCase();
  }
}
