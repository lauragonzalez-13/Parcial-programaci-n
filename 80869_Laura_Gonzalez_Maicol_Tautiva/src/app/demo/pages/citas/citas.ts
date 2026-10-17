import { CommonModule } from '@angular/common';
import { ChangeDetectorRef, Component, OnInit } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { finalize, forkJoin, timeout } from 'rxjs';
import { SharedModule } from 'src/app/theme/shared/shared.module';
import { ClienteCita, ConsultaCita, ConsultaRequest, ConsultasService, EstadoConsulta, MascotaCita, VeterinarioCita } from 'src/app/services/consultas.service';

@Component({
  selector: 'app-citas', standalone: true, imports: [CommonModule, FormsModule, SharedModule],
  templateUrl: './citas.html', styleUrl: './citas.scss'
})
export class CitasComponent implements OnInit {
  citas: ConsultaCita[] = []; clientes: ClienteCita[] = []; mascotas: MascotaCita[] = []; veterinarios: VeterinarioCita[] = [];
  cargando = true; guardando = false; mostrarFormulario = false; editandoId: number | null = null;
  error = ''; mensaje = ''; filtroTexto = ''; estadoFiltro = 'todos'; paginaActual = 1; tamanoPagina = 10;
  columnaOrden: 'fechaHora' | 'cliente' | 'mascota' | 'veterinario' | 'estado' = 'fechaHora'; ordenAscendente = false;
  estados: EstadoConsulta[] = ['PENDIENTE', 'CONFIRMADA', 'ATENDIDA', 'CANCELADA', 'PROGRAMADA'];
  hoy = this.fechaLocal(new Date());
  form = { clienteId: 0, mascotaId: 0, veterinarioId: 0, fecha: this.hoy, hora: '', motivo: '', estado: 'PENDIENTE' as EstadoConsulta };
  constructor(private service: ConsultasService, private cdr: ChangeDetectorRef) {}
  ngOnInit(): void {
    forkJoin({ citas: this.service.listar(), clientes: this.service.clientes(), veterinarios: this.service.veterinarios() }).subscribe({
      next: data => { this.citas = data.citas; this.clientes = data.clientes.filter(c => c.activo); this.veterinarios = data.veterinarios.filter(v => v.activo); this.cargando = false; this.cdr.detectChanges(); },
      error: e => { this.error = this.mensajeError(e); this.cargando = false; this.cdr.detectChanges(); }
    });
  }
  get filtradas(): ConsultaCita[] {
    const q = this.filtroTexto.trim().toLocaleLowerCase();
    return this.citas.filter(c => {
      const persona = `${c.mascota?.cliente?.nombre ?? ''} ${c.mascota?.cliente?.apellido ?? ''}`;
      const texto = [persona, c.mascota?.nombre, c.veterinario?.nombre, c.veterinario?.apellido, c.motivo, c.estado].join(' ').toLocaleLowerCase();
      return (!q || texto.includes(q)) && (this.estadoFiltro === 'todos' || c.estado === this.estadoFiltro);
    }).sort((a, b) => {
      let n = 0;
      if (this.columnaOrden === 'fechaHora') n = a.fechaHora.localeCompare(b.fechaHora);
      else if (this.columnaOrden === 'cliente') n = this.nombreCliente(a).localeCompare(this.nombreCliente(b), 'es');
      else if (this.columnaOrden === 'mascota') n = (a.mascota?.nombre ?? '').localeCompare(b.mascota?.nombre ?? '', 'es');
      else if (this.columnaOrden === 'veterinario') n = this.nombreVeterinario(a).localeCompare(this.nombreVeterinario(b), 'es');
      else n = a.estado.localeCompare(b.estado);
      return this.ordenAscendente ? n : -n;
    });
  }
  get pagina(): ConsultaCita[] { const i = (this.paginaActual - 1) * this.tamanoPagina; return this.filtradas.slice(i, i + this.tamanoPagina); }
  get totalPaginas(): number { return Math.max(1, Math.ceil(this.filtradas.length / this.tamanoPagina)); }
  nombreCliente(c: ConsultaCita): string { return `${c.mascota?.cliente?.nombre ?? ''} ${c.mascota?.cliente?.apellido ?? ''}`.trim(); }
  nombreVeterinario(c: ConsultaCita): string { return `${c.veterinario?.nombre ?? ''} ${c.veterinario?.apellido ?? ''}`.trim(); }
  etiqueta(estado: string): string { return estado.charAt(0) + estado.slice(1).toLocaleLowerCase(); }
  cambiarOrden(col: typeof this.columnaOrden): void { if (this.columnaOrden === col) this.ordenAscendente = !this.ordenAscendente; else { this.columnaOrden = col; this.ordenAscendente = col !== 'fechaHora'; } this.paginaActual = 1; }
  cambiarCliente(): void { this.form.mascotaId = 0; this.mascotas = []; if (this.form.clienteId) this.service.mascotas(this.form.clienteId).subscribe({ next: ms => this.mascotas = ms.filter(m => m.activa), error: e => this.error = this.mensajeError(e) }); }
  nuevo(): void { this.editandoId = null; this.error = ''; this.form = { clienteId: 0, mascotaId: 0, veterinarioId: 0, fecha: this.hoy, hora: '', motivo: '', estado: 'PENDIENTE' }; this.mascotas = []; this.mostrarFormulario = true; }
  editar(c: ConsultaCita): void {
    if (c.estado === 'CANCELADA' || c.estado === 'ATENDIDA') return;
    this.error = ''; this.editandoId = c.id;
    this.form = { clienteId: c.mascota.cliente.id, mascotaId: c.mascota.id, veterinarioId: c.veterinario.id, fecha: c.fechaHora.slice(0, 10), hora: c.fechaHora.slice(11, 16), motivo: c.motivo, estado: c.estado };
    this.service.mascotas(this.form.clienteId).subscribe({ next: ms => this.mascotas = ms.filter(m => m.activa), error: e => this.error = this.mensajeError(e) }); this.mostrarFormulario = true;
  }
  guardar(): void {
    this.error = ''; this.mensaje = '';
    if (!this.form.clienteId || !this.form.mascotaId || !this.form.veterinarioId || !this.form.fecha || !this.form.hora || !this.form.motivo.trim()) { this.error = 'Completa todos los campos obligatorios.'; return; }
    if (this.form.fecha < this.hoy) { this.error = 'No se pueden registrar citas en fechas anteriores a hoy.'; return; }
    const request: ConsultaRequest = { clienteId: Number(this.form.clienteId), mascotaId: Number(this.form.mascotaId), veterinarioId: Number(this.form.veterinarioId), fechaHora: `${this.form.fecha}T${this.form.hora}:00`, motivo: this.form.motivo.trim(), estado: this.form.estado };
    this.guardando = true; const id = this.editandoId;
    (id ? this.service.actualizar(id, request) : this.service.crear(request)).pipe(
      timeout({ first: 20000 }),
      finalize(() => { this.guardando = false; this.cdr.detectChanges(); })
    ).subscribe({
      next: cita => { this.citas = id ? this.citas.map(c => c.id === id ? cita : c) : [cita, ...this.citas]; this.mostrarFormulario = false; this.editandoId = null; this.guardando = false; this.mensaje = id ? 'La cita se actualizó correctamente.' : 'La cita se registró correctamente.'; },
      error: e => { this.error = this.mensajeError(e); this.guardando = false; }
    });
  }
  cancelarCita(c: ConsultaCita): void { if (!confirm(`¿Cancelar la cita de ${c.mascota.nombre}?`)) return; this.service.cancelar(c.id).subscribe({ next: updated => { this.citas = this.citas.map(x => x.id === c.id ? updated : x); this.mensaje = 'La cita fue cancelada.'; }, error: e => this.error = this.mensajeError(e) }); }
  atenderCita(c: ConsultaCita): void { this.service.atender(c.id).subscribe({ next: updated => { this.citas = this.citas.map(x => x.id === c.id ? updated : x); this.mensaje = 'La cita quedó atendida y disponible para asociar su historia clínica.'; }, error: e => this.error = this.mensajeError(e) }); }
  limpiar(): void { this.filtroTexto = ''; this.estadoFiltro = 'todos'; this.paginaActual = 1; }
  fechaLocal(d: Date): string { const offset = d.getTimezoneOffset(); return new Date(d.getTime() - offset * 60000).toISOString().slice(0, 10); }
  private mensajeError(e: any): string {
    if (e?.name === 'TimeoutError') return 'El backend tardó demasiado en responder. Comprueba si la cita se guardó y vuelve a cargar el listado.';
    if (e?.status === 0) return 'No se pudo conectar con el backend (localhost:8080). Verifica que Spring Boot y MySQL estén iniciados.';
    return e?.error?.mensaje ?? e?.error?.message ?? 'No fue posible completar la operación.';
  }
  Math = Math;
}
