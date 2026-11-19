import { CommonModule } from '@angular/common';
import { ChangeDetectorRef, Component, OnInit } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { finalize } from 'rxjs';
import { ConsultasService, Disponibilidad, VeterinarioCita } from 'src/app/services/consultas.service';

@Component({
  selector: 'app-disponibilidad-veterinarios', standalone: true, imports: [CommonModule, FormsModule],
  templateUrl: './disponibilidad-veterinarios.html', styleUrl: './disponibilidad-veterinarios.scss'
})
export class DisponibilidadVeterinariosComponent implements OnInit {
  veterinarios: VeterinarioCita[] = [];
  horarios: Disponibilidad[] = [];
  dias = ['Lunes', 'Martes', 'Miércoles', 'Jueves', 'Viernes', 'Sábado', 'Domingo'];
  veterinarioId = 0;
  cargando = true;
  guardando = false;
  error = '';
  mensaje = '';
  form = { diaSemana: 1, horaInicio: '08:00', horaFin: '17:00' };

  constructor(private api: ConsultasService, private cdr:ChangeDetectorRef) {}

  ngOnInit(): void {
    this.api.veterinarios().pipe(finalize(() => {this.cargando = false;this.cdr.detectChanges();})).subscribe({
      next: vets => {
        this.veterinarios = vets.filter(v => v.activo);
        this.veterinarioId = this.veterinarios[0]?.id || 0;
        this.cargarHorarios();
        this.cdr.detectChanges();
      },
      error: e => {this.error = e?.error?.mensaje || 'No se pudieron cargar los veterinarios.';this.cdr.detectChanges();}
    });
  }

  cargarHorarios(): void {
    if (!this.veterinarioId) { this.horarios = []; return; }
    this.error = '';
    this.api.disponibilidad(this.veterinarioId).subscribe({
      next: rows => {this.horarios = rows;this.cdr.detectChanges();},
      error: e => {this.error = e?.error?.mensaje || 'No se pudo consultar la disponibilidad.';this.cdr.detectChanges();}
    });
  }

  agregar(): void {
    this.error = ''; this.mensaje = '';
    if (!this.veterinarioId || !this.form.horaInicio || !this.form.horaFin || this.form.horaInicio >= this.form.horaFin) {
      this.error = 'Selecciona un veterinario y un rango horario válido.'; return;
    }
    this.guardando = true;this.cdr.detectChanges();
    this.api.guardarDisponibilidad(this.veterinarioId, this.form).pipe(finalize(() => {this.guardando = false;this.cdr.detectChanges();})).subscribe({
      next: () => { this.mensaje = 'La disponibilidad se guardó.'; this.cargarHorarios();this.cdr.detectChanges(); },
      error: e => {this.error = e?.error?.mensaje || 'No se pudo guardar la disponibilidad.';this.cdr.detectChanges();}
    });
  }

  quitar(horarioId: number): void {
    this.error = ''; this.mensaje = '';
    if(this.guardando)return;this.guardando=true;this.cdr.detectChanges();
    this.api.borrarDisponibilidad(this.veterinarioId, horarioId).pipe(finalize(()=>{this.guardando=false;this.cdr.detectChanges();})).subscribe({
      next: () => { this.mensaje = 'El horario se desactivó.'; this.cargarHorarios();this.cdr.detectChanges(); },
      error: e => {this.error = e?.error?.mensaje || 'No se pudo quitar el horario.';this.cdr.detectChanges();}
    });
  }
}
