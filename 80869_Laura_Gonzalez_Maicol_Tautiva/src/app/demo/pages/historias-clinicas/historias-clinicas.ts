import { CommonModule } from '@angular/common';
import { ChangeDetectorRef, Component, OnInit } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { forkJoin } from 'rxjs';
import { ClienteCita, ConsultaCita, MascotaCita } from 'src/app/services/consultas.service';
import { AnotacionClinica, AnotacionRequest, HistoriaClinica, HistoriaRequest, HistoriasClinicasService } from 'src/app/services/historias-clinicas.service';
import { SharedModule } from 'src/app/theme/shared/shared.module';

@Component({
  selector: 'app-historias-clinicas', standalone: true, imports: [CommonModule, FormsModule, SharedModule],
  templateUrl: './historias-clinicas.html', styleUrl: './historias-clinicas.scss'
})
export class HistoriasClinicasComponent implements OnInit {
  anotaciones: AnotacionClinica[] = [];
  clientes: ClienteCita[] = [];
  mascotas: MascotaCita[] = [];
  citas: ConsultaCita[] = [];
  cargando = true; error = ''; mensaje = ''; guardando = false;
  filtroTexto = ''; paginaActual = 1; tamanoPagina = 10;
  columnaOrden: 'fechaHora' | 'mascota' | 'cliente' | 'veterinario' | 'estado' = 'fechaHora'; ordenAscendente = false;
  mostrarFormulario = false; mostrarDetalle = false; mostrarDetalleAnotacion = false; agregandoAnotacion = false;
  historiaSeleccionada: HistoriaClinica | null = null;
  anotacionesHistoria: AnotacionClinica[] = [];
  anotacionSeleccionada: AnotacionClinica | null = null;
  filtroAnotaciones = ''; paginaAnotaciones = 1; tamanoPaginaAnotaciones = 5;
  form = { clienteId: 0, mascotaId: 0, consultaId: 0, diagnostico: '', observacion: '' };
  formAnotacion = { consultaId: 0, descripcion: '', diagnostico: '', observacion: '' };
  constructor(private service: HistoriasClinicasService, private cdr: ChangeDetectorRef) {}

  ngOnInit(): void {
    forkJoin({ historias: this.service.listar(), clientes: this.service.clientes(), citas: this.service.citas() }).subscribe({
      next: d => { this.anotaciones = d.historias; this.clientes = d.clientes.filter(c => c.activo); this.citas = d.citas; this.cargando = false; this.cdr.detectChanges(); },
      error: e => { this.error = this.errorMensaje(e); this.cargando = false; this.cdr.detectChanges(); }
    });
  }

  get filtradas(): AnotacionClinica[] {
    const q = this.filtroTexto.trim().toLocaleLowerCase();
    return this.anotaciones.filter(a => !q || [a.historiaClinica?.mascota?.nombre, a.historiaClinica?.mascota?.cliente?.nombre,
      a.historiaClinica?.mascota?.cliente?.apellido, a.veterinario?.nombre, a.veterinario?.apellido,
      a.consulta?.motivo, a.consulta?.estado].join(' ').toLocaleLowerCase().includes(q))
      .sort((a, b) => {
        let value = 0;
        if (this.columnaOrden === 'fechaHora') value = a.fechaHora.localeCompare(b.fechaHora);
        if (this.columnaOrden === 'mascota') value = (a.historiaClinica?.mascota?.nombre ?? '').localeCompare(b.historiaClinica?.mascota?.nombre ?? '', 'es');
        if (this.columnaOrden === 'cliente') value = this.clienteNombre(a).localeCompare(this.clienteNombre(b), 'es');
        if (this.columnaOrden === 'veterinario') value = this.veterinarioNombre(a).localeCompare(this.veterinarioNombre(b), 'es');
        if (this.columnaOrden === 'estado') value = (a.consulta?.estado ?? '').localeCompare(b.consulta?.estado ?? '');
        return this.ordenAscendente ? value : -value;
      });
  }
  get pagina(): AnotacionClinica[] { const start = (this.paginaActual - 1) * this.tamanoPagina; return this.filtradas.slice(start, start + this.tamanoPagina); }
  get totalPaginas(): number { return Math.max(1, Math.ceil(this.filtradas.length / this.tamanoPagina)); }
  get mascotasNuevas(): MascotaCita[] { const ids = new Set(this.anotaciones.map(a => a.historiaClinica.mascota.id)); return this.mascotas.filter(m => !ids.has(m.id)); }
  get citasParaHistoria(): ConsultaCita[] {
    return this.citas.filter(c => c.estado === 'ATENDIDA' && c.mascota?.id === Number(this.form.mascotaId)
      && !this.anotaciones.some(a => a.consulta?.id === c.id));
  }
  get citasParaAnotacion(): ConsultaCita[] {
    const mascotaId = this.historiaSeleccionada?.mascota.id;
    return this.citas.filter(c => c.estado === 'ATENDIDA' && c.mascota?.id === mascotaId
      && !this.anotaciones.some(a => a.consulta?.id === c.id));
  }
  get anotacionesFiltradas(): AnotacionClinica[] {
    const q = this.filtroAnotaciones.trim().toLocaleLowerCase();
    return this.anotacionesHistoria.filter(a => !q || [a.descripcion, a.diagnostico, a.observacion, a.veterinario?.nombre, a.veterinario?.apellido, a.consulta?.motivo].join(' ').toLocaleLowerCase().includes(q))
      .sort((a, b) => b.fechaHora.localeCompare(a.fechaHora));
  }
  get paginaDetalle(): AnotacionClinica[] { const start = (this.paginaAnotaciones - 1) * this.tamanoPaginaAnotaciones; return this.anotacionesFiltradas.slice(start, start + this.tamanoPaginaAnotaciones); }
  get totalPaginasDetalle(): number { return Math.max(1, Math.ceil(this.anotacionesFiltradas.length / this.tamanoPaginaAnotaciones)); }

  nombreCliente(a: AnotacionClinica): string { return `${a.historiaClinica?.mascota?.cliente?.nombre ?? ''} ${a.historiaClinica?.mascota?.cliente?.apellido ?? ''}`.trim(); }
  clienteNombre(a: AnotacionClinica): string { return this.nombreCliente(a); }
  veterinarioNombre(a: AnotacionClinica): string { return `${a.veterinario?.nombre ?? ''} ${a.veterinario?.apellido ?? ''}`.trim(); }
  cambiarOrden(col: typeof this.columnaOrden): void { if (this.columnaOrden === col) this.ordenAscendente = !this.ordenAscendente; else { this.columnaOrden = col; this.ordenAscendente = col !== 'fechaHora'; } this.paginaActual = 1; }
  cambiarCliente(): void { this.form.mascotaId = 0; this.form.consultaId = 0; this.mascotas = []; if (this.form.clienteId) this.service.mascotas(this.form.clienteId).subscribe({ next: ms => { this.mascotas = ms.filter(m => m.activa); this.cdr.detectChanges(); }, error: e => { this.error = this.errorMensaje(e); this.cdr.detectChanges(); } }); }
  abrirFormulario(): void { this.error = ''; this.form = { clienteId: 0, mascotaId: 0, consultaId: 0, diagnostico: '', observacion: '' }; this.mostrarFormulario = true; }
  nuevaMascota(): void { this.form.consultaId = 0; }
  crear(): void {
    const cita = this.citasParaHistoria.find(c => c.id === Number(this.form.consultaId));
    if (!cita || !this.form.diagnostico.trim()) { this.error = 'Selecciona una cita atendida e ingresa el diagnóstico.'; return; }
    const payload: HistoriaRequest = { clienteId: Number(this.form.clienteId), mascotaId: Number(this.form.mascotaId), consultaId: cita.id, descripcion: cita.motivo, observacion: this.form.observacion.trim(), diagnostico: this.form.diagnostico.trim() };
    this.guardando = true; this.service.crear(payload).subscribe({
      next: () => { this.mostrarFormulario = false; this.mensaje = 'Historia clínica creada con su primera anotación.'; this.recargarListado(); },
      error: e => { this.error = this.errorMensaje(e); this.guardando = false; this.cdr.detectChanges(); }
    });
  }
  verHistoria(row: AnotacionClinica): void {
    this.error = ''; this.historiaSeleccionada = row.historiaClinica; this.mostrarDetalle = true;
    this.service.anotaciones(row.historiaClinica.id).subscribe({ next: items => { this.anotacionesHistoria = items; this.paginaAnotaciones = 1; this.cdr.detectChanges(); }, error: e => { this.error = this.errorMensaje(e); this.cdr.detectChanges(); } });
  }
  abrirNuevaAnotacion(): void { this.formAnotacion = { consultaId: 0, descripcion: '', diagnostico: '', observacion: '' }; this.error = ''; this.agregandoAnotacion = true; }
  agregarAnotacion(): void {
    if (!this.historiaSeleccionada || !this.formAnotacion.consultaId || !this.formAnotacion.descripcion.trim()) { this.error = 'Completa la cita y la descripción de la anotación.'; return; }
    const payload: AnotacionRequest = { consultaId: Number(this.formAnotacion.consultaId), descripcion: this.formAnotacion.descripcion.trim(), diagnostico: this.formAnotacion.diagnostico.trim(), observacion: this.formAnotacion.observacion.trim() };
    this.guardando = true; this.service.agregarAnotacion(this.historiaSeleccionada.id, payload).subscribe({
      next: annotation => { this.anotacionesHistoria = [annotation, ...this.anotacionesHistoria]; this.agregandoAnotacion = false; this.mostrarDetalle = true; this.mensaje = 'Anotación agregada al historial.'; this.recargarListado(); },
      error: e => { this.error = this.errorMensaje(e); this.guardando = false; this.cdr.detectChanges(); }
    });
  }
  verAnotacion(a: AnotacionClinica): void { this.anotacionSeleccionada = a; this.mostrarDetalleAnotacion = true; }
  paginaSiguiente(): void { if (this.paginaActual < this.totalPaginas) this.paginaActual++; }
  recargarListado(): void { this.service.listar().subscribe({ next: a => { this.anotaciones = a; this.guardando = false; this.cdr.detectChanges(); }, error: e => { this.error = this.errorMensaje(e); this.guardando = false; this.cdr.detectChanges(); } }); }
  cerrarFormulario(): void { this.mostrarFormulario = false; this.error = ''; }
  errorMensaje(e: any): string { if (e?.status === 0) return 'No se pudo conectar con el backend. Verifica que Spring Boot y MySQL estén iniciados.'; return e?.error?.mensaje ?? e?.error?.message ?? 'No fue posible completar la operación.'; }
  Math = Math;
}
