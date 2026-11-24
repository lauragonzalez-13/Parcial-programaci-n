
package com.uniminuto.clinica.serviceimpl;

import com.uniminuto.clinica.entity.Consulta;
import com.uniminuto.clinica.entity.FormulaMedica;
import com.uniminuto.clinica.entity.Mascota;
import com.uniminuto.clinica.entity.Medicamento;
import com.uniminuto.clinica.entity.LoteInventario;
import com.uniminuto.clinica.entity.MovimientoInventario;
import com.uniminuto.clinica.entity.TipoMovimiento;
import com.uniminuto.clinica.entity.EstadoDispensacion;
import com.uniminuto.clinica.repository.MedicamentoRepository;
import com.uniminuto.clinica.repository.LoteInventarioRepository;
import com.uniminuto.clinica.repository.MovimientoInventarioRepository;
import com.uniminuto.clinica.repository.ConsultaRepository;
import com.uniminuto.clinica.repository.FormulaMedicaRepository;
import com.uniminuto.clinica.repository.MascotaRepository;
import com.uniminuto.clinica.service.FormulaMedicaService;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.math.BigDecimal;
import java.util.List;

@Service
@Transactional
public class FormulaMedicaServiceImpl implements FormulaMedicaService {

        private final FormulaMedicaRepository formulaRepository;
        private final ConsultaRepository consultaRepository;
        private final MascotaRepository mascotaRepository;
        private final MedicamentoRepository medicamentoRepository;
        private final LoteInventarioRepository loteRepository;
        private final MovimientoInventarioRepository movimientoRepository;

        public FormulaMedicaServiceImpl(
                        FormulaMedicaRepository formulaRepository,
                        ConsultaRepository consultaRepository,
                        MascotaRepository mascotaRepository,
                        MedicamentoRepository medicamentoRepository,
                        LoteInventarioRepository loteRepository,
                        MovimientoInventarioRepository movimientoRepository) {
                this.formulaRepository = formulaRepository;
                this.consultaRepository = consultaRepository;
                this.mascotaRepository = mascotaRepository;
                this.medicamentoRepository = medicamentoRepository;
                this.loteRepository = loteRepository;
                this.movimientoRepository = movimientoRepository;
        }

        // Registrar una fórmula médica
        @Override
        public FormulaMedica guardar(FormulaMedica formula) {

                validarDatos(formula);

                Mascota mascota = mascotaRepository
                                .findById(formula.getMascota().getId())
                                .orElseThrow(() -> new IllegalArgumentException(
                                                "La mascota indicada no existe."));

                Consulta consulta = consultaRepository
                                .findById(formula.getConsulta().getId())
                                .orElseThrow(() -> new IllegalArgumentException(
                                                "La consulta indicada no existe."));

                validarRelaciones(formula, mascota, consulta);

                formula.setMascota(mascota);
                formula.setConsulta(consulta);
                formula.setCliente(mascota.getCliente());
                formula.setVeterinario(consulta.getVeterinario());
                if (formula.getMedicamentoInventario() != null && formula.getMedicamentoInventario().getId() != null) {
                        Medicamento producto = medicamentoRepository.findById(formula.getMedicamentoInventario().getId())
                                        .orElseThrow(() -> new IllegalArgumentException("El medicamento de inventario no existe."));
                        if (formula.getUnidadesPorToma() == null || formula.getUnidadesPorToma().signum() <= 0
                                        || formula.getTomasPorDia() == null || formula.getTomasPorDia() <= 0) {
                                throw new IllegalArgumentException("Las unidades por toma y tomas al día deben ser mayores que cero.");
                        }
                        BigDecimal cantidadCalculada = formula.getUnidadesPorToma()
                                        .multiply(BigDecimal.valueOf(formula.getTomasPorDia()))
                                        .multiply(BigDecimal.valueOf(formula.getDuracionDias()));
                        formula.setCantidadRequerida(cantidadCalculada);
                        // La prescripción médica no modifica ni reserva existencias; despacho valida el stock real al entregar.
                        formula.setMedicamentoInventario(producto);
                        formula.setMedicamento(producto.getNombre());
                        if (formula.getCantidadDispensada() == null) formula.setCantidadDispensada(BigDecimal.ZERO);
                        if (formula.getEstadoDispensacion() == null) formula.setEstadoDispensacion(EstadoDispensacion.PENDIENTE);
                }
                formula.setActiva(true);

                return formulaRepository.save(formula);
        }

        // Listar todas las fórmulas médicas, activas e inactivas
        @Override
        @Transactional(readOnly = true)
        public List<FormulaMedica> listar() {
                return formulaRepository.findAll();
        }

        @Override
        @Transactional(readOnly = true)
        public List<FormulaMedica> listarPorConsulta(Long consultaId) {
                return formulaRepository.findByConsultaIdOrderByFechaEmisionDesc(consultaId);
        }

        // Buscar una fórmula por ID
        @Override
        @Transactional(readOnly = true)
        public FormulaMedica buscarPorId(Long id) {

                if (id == null) {
                        throw new IllegalArgumentException(
                                        "El identificador es obligatorio.");
                }

                return formulaRepository.findById(id)
                                .orElseThrow(() -> new IllegalArgumentException(
                                                "La fórmula médica no existe."));
        }

        // Actualizar una fórmula existente
        @Override
        public FormulaMedica actualizar(Long id, FormulaMedica formula) {

                if (id == null) {
                        throw new IllegalArgumentException(
                                        "El identificador es obligatorio.");
                }

                if (formula == null) {
                        throw new IllegalArgumentException(
                                        "Los datos de la fórmula son obligatorios.");
                }

                FormulaMedica existente = buscarPorId(id);

                if (existente.getCantidadDispensada() != null && existente.getCantidadDispensada().signum() > 0) {
                        throw new IllegalStateException("No se puede editar una fórmula que ya tuvo dispensación.");
                }

                // Conservar el estado actual de la fórmula
                boolean estabaActiva = existente.isActiva();

                // Actualizar los campos recibidos
                existente.setFechaEmision(formula.getFechaEmision());
                existente.setMedicamento(formula.getMedicamento());
                existente.setDosis(formula.getDosis());
                existente.setFrecuencia(formula.getFrecuencia());
                existente.setDuracionDias(formula.getDuracionDias());
                existente.setIndicaciones(formula.getIndicaciones());
                existente.setObservaciones(formula.getObservaciones());
                existente.setViaAdministracion(formula.getViaAdministracion());
                existente.setMascota(formula.getMascota());
                existente.setConsulta(formula.getConsulta());
                existente.setCliente(formula.getCliente());
                existente.setVeterinario(formula.getVeterinario());
                existente.setMedicamentoInventario(formula.getMedicamentoInventario());
                existente.setCantidadRequerida(formula.getCantidadRequerida());
                existente.setUnidadesPorToma(formula.getUnidadesPorToma());
                existente.setTomasPorDia(formula.getTomasPorDia());

                // Reutilizar las validaciones del registro
                FormulaMedica actualizada = guardar(existente);

                // Evitar reactivar automáticamente una fórmula inactiva
                actualizada.setActiva(estabaActiva);

                return formulaRepository.save(actualizada);
        }

        // Listar las fórmulas activas de una mascota
        @Override
        @Transactional(readOnly = true)
        public List<FormulaMedica> listarPorMascota(Long mascotaId) {

                if (mascotaId == null) {
                        throw new IllegalArgumentException(
                                        "El identificador de la mascota es obligatorio.");
                }

                return formulaRepository.findByMascota_Id(mascotaId)
                                .stream()
                                .filter(FormulaMedica::isActiva)
                                .toList();
        }

        // Desactivar una fórmula sin eliminarla
        @Override
        public FormulaMedica desactivar(Long id) {
                FormulaMedica formula = buscarPorId(id);
                formula.setActiva(false);
                return formulaRepository.save(formula);
        }

        @Override
        public FormulaMedica dispensar(Long id, String usuarioResponsable) {
                FormulaMedica formula = buscarPorId(id);
                if (!formula.isActiva()) throw new IllegalStateException("No se puede dispensar una fórmula inactiva.");
                Medicamento producto = formula.getMedicamentoInventario();
                if (producto == null || formula.getCantidadRequerida() == null) {
                        throw new IllegalArgumentException("Esta fórmula no está vinculada a un producto del inventario.");
                }
                if (formula.getEstadoDispensacion() == EstadoDispensacion.DISPENSADA) {
                        throw new IllegalStateException("La fórmula ya fue dispensada completamente.");
                }
                BigDecimal entregado = formula.getCantidadDispensada() == null ? BigDecimal.ZERO : formula.getCantidadDispensada();
                BigDecimal restante = formula.getCantidadRequerida().subtract(entregado);
                if (restante.signum() <= 0) throw new IllegalStateException("La fórmula no tiene cantidades pendientes.");
                List<LoteInventario> disponibles = loteRepository.bloquearLotesDisponibles(producto.getId()).stream()
                        .filter(l -> l.getFechaVencimiento() == null || !l.getFechaVencimiento().isBefore(LocalDate.now()))
                        .toList();
                BigDecimal existencias = disponibles.stream().map(LoteInventario::getCantidadDisponible)
                        .reduce(BigDecimal.ZERO, BigDecimal::add);
                if (existencias.compareTo(restante) < 0) throw new IllegalStateException("No hay existencias suficientes para dispensar la fórmula.");
                BigDecimal porDescontar = restante;
                for (LoteInventario lote : disponibles) {
                        if (porDescontar.signum() == 0) break;
                        BigDecimal salida = lote.getCantidadDisponible().min(porDescontar);
                        lote.setCantidadDisponible(lote.getCantidadDisponible().subtract(salida));
                        loteRepository.save(lote);
                        MovimientoInventario movimiento = new MovimientoInventario();
                        movimiento.setTipo(TipoMovimiento.SALIDA);
                        movimiento.setMedicamento(producto);
                        movimiento.setLote(lote);
                        movimiento.setFormulaMedica(formula);
                        movimiento.setCantidad(salida);
                        movimiento.setFecha(LocalDateTime.now());
                        movimiento.setUsuarioResponsable(usuarioResponsable == null || usuarioResponsable.isBlank() ? "usuario-no-autenticado" : usuarioResponsable.trim());
                        movimiento.setObservacion("Dispensación de fórmula médica #" + formula.getId());
                        movimientoRepository.save(movimiento);
                        porDescontar = porDescontar.subtract(salida);
                }
                formula.setCantidadDispensada(entregado.add(restante));
                formula.setEstadoDispensacion(EstadoDispensacion.DISPENSADA);
                return formulaRepository.save(formula);
        }

        // Validar los campos obligatorios
        private void validarDatos(FormulaMedica formula) {

                if (formula == null) {
                        throw new IllegalArgumentException(
                                        "La fórmula médica es obligatoria.");
                }

                if (formula.getMascota() == null
                                || formula.getMascota().getId() == null) {
                        throw new IllegalArgumentException(
                                        "Debe indicar la mascota.");
                }

                if (formula.getConsulta() == null
                                || formula.getConsulta().getId() == null) {
                        throw new IllegalArgumentException(
                                        "Debe indicar la consulta.");
                }

                if (formula.getFechaEmision() == null) {
                        throw new IllegalArgumentException(
                                        "La fecha de emisión es obligatoria.");
                }

                if (formula.getMedicamento() == null
                                || formula.getMedicamento().isBlank()) {
                        throw new IllegalArgumentException(
                                        "El medicamento es obligatorio.");
                }

                if (formula.getDosis() == null
                                || formula.getDosis().isBlank()) {
                        throw new IllegalArgumentException(
                                        "La dosis es obligatoria.");
                }

                if (formula.getFrecuencia() == null
                                || formula.getFrecuencia().isBlank()) {
                        throw new IllegalArgumentException(
                                        "La frecuencia es obligatoria.");
                }

                if (formula.getDuracionDias() == null
                                || formula.getDuracionDias() <= 0) {
                        throw new IllegalArgumentException(
                                        "La duración debe ser mayor que cero.");
                }

                if (formula.getIndicaciones() == null
                                || formula.getIndicaciones().isBlank()) {
                        throw new IllegalArgumentException(
                                        "Las indicaciones son obligatorias.");
                }

                if (formula.getFechaEmision().isAfter(LocalDate.now())) {
                        throw new IllegalArgumentException(
                                        "La fecha de emisión no puede estar en el futuro.");
                }
        }

        // Verificar que las relaciones sean correctas
        private void validarRelaciones(
                        FormulaMedica formula,
                        Mascota mascota,
                        Consulta consulta) {

                if (mascota.getCliente() == null
                                || mascota.getCliente().getId() == null) {
                        throw new IllegalArgumentException(
                                        "La mascota no tiene un cliente propietario válido.");
                }

                if (consulta.getMascota() == null
                                || consulta.getMascota().getId() == null
                                || !consulta.getMascota().getId().equals(mascota.getId())) {
                        throw new IllegalArgumentException(
                                        "La consulta no corresponde a la mascota indicada.");
                }

                if (consulta.getVeterinario() == null
                                || consulta.getVeterinario().getId() == null) {
                        throw new IllegalArgumentException(
                                        "La consulta no tiene un veterinario válido.");
                }

                if (consulta.getFechaHora() == null) {
                        throw new IllegalArgumentException(
                                        "La consulta debe tener fecha y hora.");
                }

                if (formula.getFechaEmision()
                                .isBefore(consulta.getFechaHora().toLocalDate())) {
                        throw new IllegalArgumentException(
                                        "La fecha de emisión no puede ser anterior a la consulta.");
                }

                if (!mascota.isActiva()) {
                        throw new IllegalArgumentException(
                                        "No se pueden emitir fórmulas para una mascota inactiva.");
                }

                if (!consulta.isActiva()) {
                        throw new IllegalArgumentException(
                                        "No se pueden emitir fórmulas para una consulta inactiva.");
                }

                if (consulta.getEstado() != com.uniminuto.clinica.entity.EstadoConsulta.ATENDIDA
                                && consulta.getEstado() != com.uniminuto.clinica.entity.EstadoConsulta.EN_ATENCION) {
                        throw new IllegalArgumentException(
                                        "La fórmula solo se puede asociar a una consulta en atención o atendida.");
                }
        }
}
