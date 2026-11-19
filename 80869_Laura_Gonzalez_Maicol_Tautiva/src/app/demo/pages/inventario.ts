import { CommonModule } from '@angular/common';
import { ChangeDetectorRef, Component, OnInit } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { forkJoin, of } from 'rxjs';
import { SharedModule } from 'src/app/theme/shared/shared.module';
import { Importacion, InventarioService, LoteInventario, MovimientoInventario, PreviewImportacion, ProductoInventario, ProductoRequest } from 'src/app/services/inventario.service';
import { PerfilDemoService } from 'src/app/services/perfil-demo.service';

@Component({ selector: 'app-inventario', standalone: true, imports: [CommonModule, FormsModule, SharedModule], templateUrl: './inventario.html', styleUrl: './inventario.scss' })
export class InventarioComponent implements OnInit {
  productos: ProductoInventario[] = []; lotes: LoteInventario[] = []; movimientos: MovimientoInventario[] = []; importaciones: Importacion[] = [];
  cargando = true; guardando = false; error = ''; mensaje = ''; filtro = '';
  seccion: 'productos' | 'lotes' | 'movimientos' | 'importar' = 'productos';
  modalProducto = false; modalLote = false; productoEditando: number | null = null;
  usuario = 'veterinario'; archivo: File | null = null; vistaPrevia: PreviewImportacion | null = null;
  productoForm = this.formularioVacio();
  loteForm = { medicamentoId: 0, codigoLote: '', cantidad: 1, precioCompra: 0, precioVenta: 0, fechaIngreso: new Date().toLocaleDateString('en-CA'), fechaVencimiento: '', observacion: '' };

  constructor(private service: InventarioService, private cdr: ChangeDetectorRef, private perfil: PerfilDemoService) {}
  ngOnInit(): void { this.cargar(); }
  get soloLectura(): boolean { return this.perfil.perfil === 'DESPACHO_MEDICO'; }
  get productosFiltrados(): ProductoInventario[] { const q = this.filtro.trim().toLocaleLowerCase(); return this.productos.filter(p => !q || `${p.codigo} ${p.nombre} ${p.tipo} ${p.registroInvima}`.toLocaleLowerCase().includes(q)); }
  cargar(): void {
    this.cargando = true;
    const peticiones = this.soloLectura
      ? forkJoin({ productos: this.service.productos(), lotes: this.service.lotes(), movimientos: this.service.movimientos(), importaciones: of([] as Importacion[]) })
      : forkJoin({ productos: this.service.productos(), lotes: this.service.lotes(), movimientos: this.service.movimientos(), importaciones: this.service.importaciones() });
    peticiones.subscribe({
      next: x => { this.productos = x.productos; this.lotes = x.lotes; this.movimientos = x.movimientos; this.importaciones = x.importaciones; this.cargando = false; this.cdr.detectChanges(); },
      error: e => { this.error = this.errorMensaje(e); this.cargando = false; this.cdr.detectChanges(); }
    });
  }
  formularioVacio(): ProductoRequest { return { codigo: '', nombre: '', descripcion: '', presentacion: '', tipo: 'MEDICAMENTO', registroInvima: '', stockMinimo: 0, fechaProximoPedido: null }; }
  nuevoProducto(): void { if(this.soloLectura)return; this.productoEditando = null; this.productoForm = this.formularioVacio(); this.error = ''; this.modalProducto = true; }
  editarProducto(p: ProductoInventario): void { if(this.soloLectura)return; this.productoEditando = p.id; this.productoForm = { codigo: p.codigo, nombre: p.nombre, descripcion: p.descripcion ?? '', presentacion: p.presentacion ?? '', tipo: p.tipo, registroInvima: p.registroInvima ?? '', stockMinimo: p.stockMinimo, fechaProximoPedido: p.fechaProximoPedido ?? null }; this.error = ''; this.modalProducto = true; }
  guardarProducto(): void {
    if(this.soloLectura)return;
    if (!/^[a-z\d]{8}$/i.test(this.productoForm.codigo) || !this.productoForm.nombre.trim() || this.productoForm.stockMinimo < 0 || (this.productoForm.tipo === 'MEDICAMENTO' && !this.productoForm.registroInvima.trim())) { this.error = 'Revisa el código de 8 caracteres, el nombre, el stock mínimo y el Invima requerido.'; return; }
    this.guardando = true; const request = this.productoEditando ? this.service.actualizarProducto(this.productoEditando, this.productoForm) : this.service.crearProducto(this.productoForm);
    request.subscribe({ next: () => { this.guardando = false; this.modalProducto = false; this.mensaje = 'Producto guardado.'; this.cargar(); }, error: e => { this.error = this.errorMensaje(e); this.guardando = false; this.cdr.detectChanges(); } });
  }
  nuevoLote(): void { if(this.soloLectura)return; this.loteForm = { medicamentoId: 0, codigoLote: '', cantidad: 1, precioCompra: 0, precioVenta: 0, fechaIngreso: new Date().toLocaleDateString('en-CA'), fechaVencimiento: '', observacion: '' }; this.error = ''; this.modalLote = true; }
  guardarLote(): void {
    if(this.soloLectura)return;
    if (!this.loteForm.medicamentoId || !this.loteForm.codigoLote.trim() || this.loteForm.cantidad <= 0 || this.loteForm.precioCompra < 0 || this.loteForm.precioVenta < 0 || !this.loteForm.fechaIngreso) { this.error = 'Completa el producto, lote, cantidad, precios y fecha de ingreso.'; return; }
    this.guardando = true; this.service.ingresarLote({ ...this.loteForm, fechaVencimiento: this.loteForm.fechaVencimiento || null, usuarioResponsable: this.usuario }).subscribe({ next: () => { this.guardando = false; this.modalLote = false; this.mensaje = 'Ingreso registrado y movimiento de entrada creado.'; this.cargar(); }, error: e => { this.error = this.errorMensaje(e); this.guardando = false; this.cdr.detectChanges(); } });
  }
  seleccionarArchivo(event: Event): void { this.archivo = (event.target as HTMLInputElement).files?.[0] ?? null; this.vistaPrevia = null; }
  previsualizar(): void { if(this.soloLectura)return; if (!this.archivo) { this.error = 'Selecciona un archivo CSV o XLSX.'; return; } this.guardando = true; this.error = ''; this.service.previsualizar(this.archivo, this.usuario).subscribe({ next: p => { this.vistaPrevia = p; this.guardando = false; this.cdr.detectChanges(); }, error: e => { this.error = this.errorMensaje(e); this.guardando = false; this.cdr.detectChanges(); } }); }
  confirmarImportacion(): void { if(this.soloLectura)return; if (!this.vistaPrevia || this.vistaPrevia.registrosValidos < 1) return; this.guardando = true; this.service.confirmar(this.vistaPrevia.token, this.usuario).subscribe({ next: log => { this.mensaje = `Importación completada: ${log.registrosImportados} importados, ${log.registrosRechazados} rechazados.`; this.vistaPrevia = null; this.archivo = null; this.guardando = false; this.cargar(); }, error: e => { this.error = this.errorMensaje(e); this.guardando = false; this.cdr.detectChanges(); } }); }
  private errorMensaje(e: any): string { return e?.error?.mensaje ?? e?.error?.message ?? 'No fue posible comunicarse con el backend.'; }
}
