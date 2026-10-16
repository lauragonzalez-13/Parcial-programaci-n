
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
  editandoId: number | null = null;

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
  ) { }

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

  get mascotaSeleccionada(): Mascota | undefined {
    return this.mascotas.find(
      mascota => mascota.id === Number(this.formulario.mascotaId)
    );
  }

  get consultaSeleccionada(): Consulta | undefined {
    return this.consultasDisponibles.find(
      consulta => consulta.id === Number(this.formulario.consultaId)
    );
  }

  abrirFormulario(): void {
    this.editandoId = null;
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

  editar(formula: FormulaMedica): void {
    this.error = '';
    this.mensaje = '';

    this.editandoId = formula.id;

    this.formulario = {
      fechaEmision: formula.fechaEmision,
      medicamento: formula.medicamento,
      dosis: formula.dosis,
      frecuencia: formula.frecuencia,
      duracionDias: formula.duracionDias,
      indicaciones: formula.indicaciones,
      mascotaId: formula.mascota.id,
      consultaId: formula.consulta.id
    };

    this.mostrarFormulario = true;
  }


  desactivar(formula: FormulaMedica): void {
    if (!formula.activa || !formula.id) {
      return;
    }

    const confirmar = window.confirm(
      `¿Deseas desactivar la fórmula médica "${formula.medicamento}"?`
    );

    if (!confirmar) {
      return;
    }

    this.error = '';
    this.mensaje = '';

    this.formulasService.desactivar(formula.id).subscribe({
      next: formulaActualizada => {
        this.formulas = this.formulas.map(f =>
          f.id === formulaActualizada.id
            ? formulaActualizada
            : f
        );

        this.mensaje = 'La fórmula médica se desactivó correctamente.';
        this.cdr.detectChanges();
      },
      error: err => {
        console.error('Error al desactivar la fórmula:', err);

        this.error =
          err?.error?.mensaje ||
          err?.error?.message ||
          'No fue posible desactivar la fórmula médica.';

        this.cdr.detectChanges();
      }
    });
  }

  alCambiarMascota(): void {
    this.formulario.consultaId = 0;
  }

  cancelar(): void {
    this.mostrarFormulario = false;
    this.editandoId = null;
    this.error = '';
  }

  guardar(): void {
    this.error = '';
    this.mensaje = '';

    const mascota = this.mascotaSeleccionada;
    const consulta = this.consultaSeleccionada;

    if (!mascota || !consulta) {
      this.error = 'Selecciona una mascota y una consulta válida.';
      return;
    }

    if (!mascota.cliente?.id) {
      this.error = 'No se encontró el cliente propietario de la mascota.';
      return;
    }

    if (!consulta.veterinario?.id) {
      this.error = 'No se encontró el veterinario asociado a la consulta.';
      return;
    }

    if (
      !this.formulario.fechaEmision ||
      !this.formulario.medicamento.trim() ||
      !this.formulario.dosis.trim() ||
      !this.formulario.frecuencia.trim() ||
      !this.formulario.indicaciones.trim() ||
      !Number.isInteger(Number(this.formulario.duracionDias)) ||
      Number(this.formulario.duracionDias) <= 0
    ) {
      this.error = 'Completa todos los campos obligatorios con valores válidos.';
      return;
    }

    const nuevaFormula: NuevaFormulaMedica = {
      fechaEmision: this.formulario.fechaEmision,
      medicamento: this.formulario.medicamento.trim(),
      dosis: this.formulario.dosis.trim(),
      frecuencia: this.formulario.frecuencia.trim(),
      duracionDias: Number(this.formulario.duracionDias),
      indicaciones: this.formulario.indicaciones.trim(),
      mascota: { id: mascota.id },
      consulta: { id: consulta.id }
    };

    const idEdicion = this.editandoId;
    this.guardando = true;

    const solicitud = idEdicion !== null
      ? this.formulasService.actualizar(idEdicion, nuevaFormula)
      : this.formulasService.guardar(nuevaFormula);

    solicitud.subscribe({
      next: formula => {
        if (idEdicion !== null) {
          this.formulas = this.formulas.map(f =>
            f.id === idEdicion ? formula : f
          );
          this.mensaje = 'La fórmula médica se actualizó correctamente.';
        } else {
          this.formulas = [...this.formulas, formula];
          this.mensaje = 'La fórmula médica se registró correctamente.';
        }

        this.mostrarFormulario = false;
        this.editandoId = null;
        this.guardando = false;
        this.cdr.detectChanges();
      },
      error: err => {
        console.error('Error al guardar o actualizar la fórmula:', err);

        this.error =
          err?.error?.mensaje ||
          err?.error?.message ||
          'No fue posible guardar los cambios. Revisa los datos y el backend.';

        this.guardando = false;
        this.cdr.detectChanges();
      }
    });
  }

  frecuenciaInvalida(valor: string | null | undefined): boolean {
    const texto = String(valor ?? '').trim();

    // El campo vacío lo valida "required".
    if (!texto) {
      return false;
    }

    // Si escriben solamente un número, debe ser entero y positivo.
    if (/^-?\d+(\.\d+)?$/.test(texto)) {
      const numero = Number(texto);
      return !Number.isInteger(numero) || numero <= 0;
    }

    // También rechaza valores que sean claramente negativos.
    if (/^-\d+(\.\d+)?$/.test(texto)) {
      return true;
    }

    return false;
  }
}
