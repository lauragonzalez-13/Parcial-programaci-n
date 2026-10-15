
import { Component, OnInit, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { SharedModule } from 'src/app/theme/shared/shared.module';

import {
  FormulasMedicasService,
  FormulaMedica,
  Mascota,
  Consulta,
  NuevaFormulaMedica
} from 'src/app/services/formulas-medicas.service';

@Component({
  selector: 'app-pacientes',
  standalone: true,
  imports: [CommonModule, FormsModule, SharedModule],
  templateUrl: './pacientes.html',
  styleUrl: './pacientes.scss'
})
export class PacientesComponent implements OnInit {
  formulas: FormulaMedica[] = [];
  mascotas: Mascota[] = [];
  consultas: Consulta[] = [];

  cargando = true;
  error = '';
  mensaje = '';
  guardando = false;
  mostrarFormulario = false;

  hoy = new Date().toLocaleDateString('en-CA');

  formulario = {
    fechaEmision: this.hoy,
    medicamento: '',
    dosis: '',
    frecuencia: '',
    duracionDias: 1,
    indicaciones: '',
    mascotaId: 0,
    consultaId: 0
  };

  constructor(
    private formulasService: FormulasMedicasService,
    private cdr: ChangeDetectorRef
  ) {}

  ngOnInit(): void {
    this.cargarDatos();
  }

  cargarDatos(): void {
    this.cargando = true;
    this.error = '';

    this.formulasService.listar().subscribe({
      next: datos => {
        this.formulas = datos;

        this.formulasService.listarMascotas().subscribe({
          next: mascotas => {
            this.mascotas = mascotas.filter(m => m.activa);

            this.formulasService.listarConsultas().subscribe({
              next: consultas => {
                this.consultas = consultas.filter(c => c.activa);
                this.cargando = false;
                this.cdr.detectChanges();
              },
              error: err => this.manejarErrorCarga(err)
            });
          },
          error: err => this.manejarErrorCarga(err)
        });
      },
      error: err => this.manejarErrorCarga(err)
    });
  }

  private manejarErrorCarga(err: unknown): void {
    console.error('Error al cargar los datos:', err);
    this.error = 'No se pudieron cargar los datos. Verifica que el backend esté funcionando.';
    this.cargando = false;
    this.cdr.detectChanges();
  }

  get consultasDisponibles(): Consulta[] {
    return this.consultas.filter(
      consulta => consulta.mascota.id === Number(this.formulario.mascotaId)
    );
  }

  abrirFormulario(): void {
    this.mensaje = '';
    this.error = '';

    this.formulario = {
      fechaEmision: this.hoy,
      medicamento: '',
      dosis: '',
      frecuencia: '',
      duracionDias: 1,
      indicaciones: '',
      mascotaId: 0,
      consultaId: 0
    };

    this.mostrarFormulario = true;
  }

  alCambiarMascota(): void {
    this.formulario.consultaId = 0;
  }

  cancelar(): void {
    this.mostrarFormulario = false;
    this.error = '';
  }

  guardar(): void {
    this.error = '';
    this.mensaje = '';

    const mascota = this.mascotas.find(
      m => m.id === Number(this.formulario.mascotaId)
    );

    const consulta = this.consultasDisponibles.find(
      c => c.id === Number(this.formulario.consultaId)
    );

    if (!mascota || !consulta) {
      this.error = 'Selecciona una mascota y una consulta válida.';
      return;
    }

    if (!consulta.veterinario?.id || !mascota.cliente?.id) {
      this.error = 'No se encontraron el cliente o el veterinario asociados.';
      return;
    }

    const nuevaFormula: NuevaFormulaMedica = {
      fechaEmision: this.formulario.fechaEmision,
      medicamento: this.formulario.medicamento.trim(),
      dosis: this.formulario.dosis.trim(),
      frecuencia: this.formulario.frecuencia.trim(),
      duracionDias: Number(this.formulario.duracionDias),
      indicaciones: this.formulario.indicaciones.trim(),
      activa: true,
      mascota: { id: mascota.id },
      cliente: { id: mascota.cliente.id },
      veterinario: { id: consulta.veterinario.id },
      consulta: { id: consulta.id }
    };

    this.guardando = true;

    this.formulasService.guardar(nuevaFormula).subscribe({
      next: formula => {
        this.formulas = [...this.formulas, formula];
        this.mostrarFormulario = false;
        this.guardando = false;
        this.mensaje = 'La fórmula médica se registró correctamente.';
        this.cdr.detectChanges();
      },
      error: err => {
        console.error('Error al guardar la fórmula:', err);

        this.error =
          err?.error?.mensaje ||
          'No fue posible guardar la fórmula. Revisa los datos y la fecha de emisión.';

        this.guardando = false;
        this.cdr.detectChanges();
      }
    });
  }
}
