import { CommonModule } from '@angular/common';
import { ChangeDetectorRef, Component, OnInit } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { forkJoin } from 'rxjs';
import { SharedModule } from 'src/app/theme/shared/shared.module';
import { ClienteMascota, MascotaPaciente, MascotaRequest, MascotasService } from 'src/app/services/mascotas.service';

@Component({
  selector: 'app-mascotas', standalone: true, imports: [CommonModule, FormsModule, SharedModule],
  templateUrl: './mascotas.html', styleUrl: './mascotas.scss'
})
export class MascotasComponent implements OnInit {
  mascotas: MascotaPaciente[] = [];
  clientes: ClienteMascota[] = [];
  cargando = true; guardando = false; modal = false; error = ''; mensaje = '';
  editandoId: number | null = null;
  filtro = '';
  form = { nombre: '', especie: '', raza: '', sexo: '', edad: null as number | null, clienteId: 0 };

  constructor(private service: MascotasService, private cdr: ChangeDetectorRef) {}
  ngOnInit(): void { this.cargar(); }
  get filtradas(): MascotaPaciente[] {
    const q = this.filtro.trim().toLocaleLowerCase();
    return this.mascotas.filter(m => !q || `${m.nombre} ${m.especie} ${m.cliente?.nombre} ${m.cliente?.apellido}`.toLocaleLowerCase().includes(q));
  }
  cargar(): void {
    this.cargando = true;
    forkJoin({ mascotas: this.service.listar(), clientes: this.service.clientes() }).subscribe({
      next: data => { this.mascotas = data.mascotas; this.clientes = data.clientes.filter(c => c.activo); this.cargando = false; this.cdr.detectChanges(); },
      error: e => { this.error = this.mensajeError(e); this.cargando = false; this.cdr.detectChanges(); }
    });
  }
  nueva(): void { this.editandoId = null; this.form = { nombre: '', especie: '', raza: '', sexo: '', edad: null, clienteId: 0 }; this.error = ''; this.modal = true; }
  editar(m: MascotaPaciente): void { this.editandoId = m.id; this.form = { nombre: m.nombre, especie: m.especie, raza: m.raza ?? '', sexo: m.sexo ?? '', edad: m.edad ?? null, clienteId: m.cliente.id }; this.error = ''; this.modal = true; }
  guardar(): void {
    if (!this.form.nombre.trim() || !this.form.especie.trim() || !this.form.clienteId) { this.error = 'Nombre, especie y cliente son obligatorios.'; return; }
    if (this.form.edad != null && this.form.edad < 0) { this.error = 'La edad no puede ser negativa.'; return; }
    const body: MascotaRequest = { nombre: this.form.nombre.trim(), especie: this.form.especie.trim(), raza: this.form.raza.trim(), sexo: this.form.sexo.trim(), edad: this.form.edad, cliente: { id: Number(this.form.clienteId) } };
    this.guardando = true;
    const request = this.editandoId ? this.service.actualizar(this.editandoId, body) : this.service.crear(body);
    request.subscribe({ next: () => { this.modal = false; this.guardando = false; this.mensaje = 'Paciente guardado en la base de datos.'; this.cargar(); }, error: e => { this.error = this.mensajeError(e); this.guardando = false; this.cdr.detectChanges(); } });
  }
  desactivar(m: MascotaPaciente): void {
    if (!confirm(`¿Desactivar a ${m.nombre}? Sus citas e historia clínica se conservarán.`)) return;
    this.service.desactivar(m.id).subscribe({ next: () => { this.mensaje = 'Paciente desactivado; el historial se conserva.'; this.cargar(); }, error: e => { this.error = this.mensajeError(e); } });
  }
  private mensajeError(e: any): string { return e?.error?.mensaje ?? e?.error?.message ?? 'No se pudo conectar con el backend.'; }
}
