
package com.uniminuto.clinica.serviceimpl;

import com.uniminuto.clinica.entity.Consulta;
import com.uniminuto.clinica.entity.FormulaMedica;
import com.uniminuto.clinica.entity.Mascota;
import com.uniminuto.clinica.repository.ConsultaRepository;
import com.uniminuto.clinica.repository.FormulaMedicaRepository;
import com.uniminuto.clinica.repository.MascotaRepository;
import com.uniminuto.clinica.service.FormulaMedicaService;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
@Transactional
public class FormulaMedicaServiceImpl implements FormulaMedicaService {

    private final FormulaMedicaRepository formulaRepository;
    private final ConsultaRepository consultaRepository;
    private final MascotaRepository mascotaRepository;

    public FormulaMedicaServiceImpl(
            FormulaMedicaRepository formulaRepository,
            ConsultaRepository consultaRepository,
            MascotaRepository mascotaRepository) {
        this.formulaRepository = formulaRepository;
        this.consultaRepository = consultaRepository;
        this.mascotaRepository = mascotaRepository;
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
        formula.setActiva(true);

        return formulaRepository.save(formula);
    }

    // Listar las fórmulas activas
    @Override
    @Transactional(readOnly = true)
    public List<FormulaMedica> listar() {
        return formulaRepository.findByActivaTrue();
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

        // Conservar el estado actual de la fórmula
        boolean estabaActiva = existente.isActiva();

        // Actualizar los campos recibidos
        existente.setFechaEmision(formula.getFechaEmision());
        existente.setMedicamento(formula.getMedicamento());
        existente.setDosis(formula.getDosis());
        existente.setFrecuencia(formula.getFrecuencia());
        existente.setDuracionDias(formula.getDuracionDias());
        existente.setIndicaciones(formula.getIndicaciones());
        existente.setMascota(formula.getMascota());
        existente.setConsulta(formula.getConsulta());
        existente.setCliente(formula.getCliente());
        existente.setVeterinario(formula.getVeterinario());

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

        if (formula.getCliente() == null
                || formula.getCliente().getId() == null) {
            throw new IllegalArgumentException(
                    "Debe indicar el cliente.");
        }

        if (formula.getVeterinario() == null
                || formula.getVeterinario().getId() == null) {
            throw new IllegalArgumentException(
                    "Debe indicar el veterinario.");
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

        if (consulta.getMascota() == null
                || !consulta.getMascota().getId().equals(mascota.getId())) {
            throw new IllegalArgumentException(
                    "La consulta no corresponde a la mascota indicada.");
        }

        if (consulta.getVeterinario() == null
                || !consulta.getVeterinario().getId()
                        .equals(formula.getVeterinario().getId())) {
            throw new IllegalArgumentException(
                    "El veterinario no corresponde a la consulta.");
        }

        if (mascota.getCliente() == null
                || !mascota.getCliente().getId()
                        .equals(formula.getCliente().getId())) {
            throw new IllegalArgumentException(
                    "El cliente no es el dueño de la mascota.");
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
    }
}
