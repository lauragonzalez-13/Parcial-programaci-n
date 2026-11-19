import { CommonModule } from '@angular/common';
import { Component, OnInit, inject } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ReportesService, ReporteClinica, TipoReporte } from '../../services/reportes.service';
import { ConsultasService, VeterinarioCita } from '../../services/consultas.service';
import { TarifasService, Especialidad } from '../../services/tarifas.service';
import { InventarioService, ProductoInventario } from '../../services/inventario.service';

@Component({ selector: 'app-reportes-administrativos', standalone: true, imports: [CommonModule, FormsModule], templateUrl: './reportes-administrativos.html' })
export class ReportesAdministrativosComponent implements OnInit {
  private readonly api = inject(ReportesService);
  private readonly consultas = inject(ConsultasService);
  private readonly tarifas = inject(TarifasService);
  private readonly inventario = inject(InventarioService);
  tipo: TipoReporte = 'FINANCIERO';
  desde = ''; hasta = '';
  veterinarioId = 0; especialidadId = 0; medicamentoId = 0;
  veterinarios: VeterinarioCita[] = []; especialidades: Especialidad[] = []; productos: ProductoInventario[] = [];
  reporte?: ReporteClinica;
  cargando = false; error = '';
  ngOnInit(): void {
    const now = new Date(); this.hasta = this.fechaLocal(now); this.desde = this.fechaLocal(new Date(now.getFullYear(), now.getMonth(), 1));
    this.consultas.veterinarios().subscribe({ next: x => this.veterinarios = x.filter(v => v.activo), error: () => {} });
    this.tarifas.especialidades().subscribe({ next: x => this.especialidades = x.filter(e => e.activa), error: () => {} });
    this.inventario.productos().subscribe({ next: x => this.productos = x, error: () => {} });
    this.cargar();
  }
  cambiarTipo(): void { this.veterinarioId = 0; this.especialidadId = 0; this.medicamentoId = 0; this.cargar(); }
  cargar(): void {
    if (!this.desde || !this.hasta || this.hasta < this.desde) { this.error = 'Selecciona un rango de fechas válido.'; return; }
    this.error = ''; this.cargando = true;
    this.api.consultar(this.tipo, this.desde, this.hasta, this.filtros()).subscribe({ next: r => { this.reporte = r; this.cargando = false; }, error: e => { this.error = e?.error?.mensaje || 'No fue posible consultar el reporte.'; this.cargando = false; } });
  }
  exportar(formato: 'pdf' | 'excel'): void {
    if (!this.reporte) return;
    this.api.descargar(this.tipo, formato, this.desde, this.hasta, this.filtros()).subscribe({ next: blob => {
      const url = URL.createObjectURL(blob); const a = document.createElement('a'); a.href = url; a.download = `reporte-${this.tipo.toLowerCase()}-${this.desde}-${this.hasta}.${formato === 'excel' ? 'xlsx' : 'pdf'}`; a.click(); URL.revokeObjectURL(url);
    }, error: e => this.error = e?.error?.mensaje || 'No se pudo descargar el reporte.' });
  }
  get columnas(): string[] { return [...new Set((this.reporte?.filas || []).flatMap(f => Object.keys(f)))]; }
  get metricas(): [string, string | number][] { return Object.entries(this.reporte?.totales || {}); }
  etiqueta(k: string): string { return k.replace(/([A-Z])/g, ' $1').replace(/^./, x => x.toUpperCase()); }
  valor(v: unknown): string { return v === null || v === undefined ? '—' : typeof v === 'object' ? JSON.stringify(v) : String(v); }
  private filtros(): { veterinarioId?: number; especialidadId?: number; medicamentoId?: number } { return { veterinarioId: this.veterinarioId || undefined, especialidadId: this.especialidadId || undefined, medicamentoId: this.medicamentoId || undefined }; }
  private fechaLocal(d: Date): string { return `${d.getFullYear()}-${String(d.getMonth()+1).padStart(2,'0')}-${String(d.getDate()).padStart(2,'0')}`; }
}
