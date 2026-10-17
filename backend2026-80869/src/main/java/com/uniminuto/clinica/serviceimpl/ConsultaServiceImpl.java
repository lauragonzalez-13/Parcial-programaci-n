
package com.uniminuto.clinica.serviceimpl;

import com.uniminuto.clinica.dto.ConsultaRequest;
import com.uniminuto.clinica.entity.Cliente;
import com.uniminuto.clinica.service.ConsultaService;
import com.uniminuto.clinica.entity.Consulta;
import com.uniminuto.clinica.entity.EstadoConsulta;
import com.uniminuto.clinica.entity.Mascota;
import com.uniminuto.clinica.entity.Veterinario;
import com.uniminuto.clinica.repository.ClienteRepository;
import com.uniminuto.clinica.repository.ConsultaRepository;
import com.uniminuto.clinica.repository.MascotaRepository;
import com.uniminuto.clinica.repository.VeterinarioRepository;
import jakarta.transaction.Transactional;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

@Service
@Transactional
public class ConsultaServiceImpl implements ConsultaService {

    private final ConsultaRepository consultaRepository;
    private final ClienteRepository clienteRepository;
    private final MascotaRepository mascotaRepository;
    private final VeterinarioRepository veterinarioRepository;

    private static final List<EstadoConsulta> ESTADOS_QUE_OCUPAN_HORARIO =
        List.of(
            EstadoConsulta.PENDIENTE,
            EstadoConsulta.CONFIRMADA,
            EstadoConsulta.PROGRAMADA
        );

    public ConsultaServiceImpl(
            ConsultaRepository consultaRepository,
            ClienteRepository clienteRepository,
            MascotaRepository mascotaRepository,
            VeterinarioRepository veterinarioRepository) {
        this.consultaRepository = consultaRepository;
        this.clienteRepository = clienteRepository;
        this.mascotaRepository = mascotaRepository;
        this.veterinarioRepository = veterinarioRepository;
    }

    @Override
    public List<Consulta> listar() {
        return consultaRepository.findAll(
            Sort.by(Sort.Direction.DESC, "fechaHora")
        );
    }

    @Override
    public Consulta buscarPorId(Long id) {
        return consultaRepository.findById(id)
            .orElseThrow(() ->
                new IllegalArgumentException("La consulta no existe.")
            );
    }

    @Override
    public Consulta crear(ConsultaRequest request) {
        validarDatos(request);

        if (request.getEstado() == EstadoConsulta.CANCELADA
                || request.getEstado() == EstadoConsulta.ATENDIDA) {
            throw new IllegalArgumentException(
                "Una cita nueva debe iniciar pendiente, confirmada o programada."
            );
        }

        Consulta consulta = new Consulta();
        aplicarDatos(consulta, request, null);

        return consultaRepository.save(consulta);
    }

    @Override
    public Consulta actualizar(Long id, ConsultaRequest request) {
        Consulta consulta = buscarPorId(id);

        if (consulta.getEstado() == EstadoConsulta.CANCELADA) {
            throw new IllegalStateException(
                "Una consulta cancelada no se puede editar ni reactivar."
            );
        }

        if (consulta.getEstado() == EstadoConsulta.ATENDIDA) {
            throw new IllegalStateException(
                "Una consulta atendida no se puede editar desde la gestión de citas."
            );
        }

        if (request != null && request.getEstado() == EstadoConsulta.CANCELADA) {
            throw new IllegalStateException(
                "Use la operación explícita de cancelación para cancelar una cita."
            );
        }

        if (request != null && request.getEstado() == EstadoConsulta.ATENDIDA
                && consulta.getEstado() != EstadoConsulta.ATENDIDA) {
            throw new IllegalStateException(
                "Una cita solo puede marcarse atendida al registrar su historia clínica."
            );
        }

        validarDatos(request);
        aplicarDatos(consulta, request, id);

        return consultaRepository.save(consulta);
    }

    @Override
    public Consulta cancelar(Long id) {
        Consulta consulta = buscarPorId(id);

        if (consulta.getEstado() == EstadoConsulta.CANCELADA) {
            throw new IllegalStateException(
                "La consulta ya está cancelada."
            );
        }

        if (consulta.getEstado() == EstadoConsulta.ATENDIDA) {
            throw new IllegalStateException(
                "No se puede cancelar una consulta que ya fue atendida."
            );
        }

        consulta.setEstado(EstadoConsulta.CANCELADA);
        consulta.setActiva(false);

        return consultaRepository.save(consulta);
    }

    @Override
    public Consulta atender(Long id) {
        Consulta consulta = buscarPorId(id);
        if (consulta.getEstado() == EstadoConsulta.CANCELADA) {
            throw new IllegalStateException("Una cita cancelada no se puede atender.");
        }
        if (consulta.getEstado() == EstadoConsulta.ATENDIDA) {
            throw new IllegalStateException("La cita ya fue atendida.");
        }
        if (consulta.getFechaHora().isAfter(LocalDateTime.now())) {
            throw new IllegalStateException("La cita solo se puede marcar atendida después de su fecha y hora.");
        }
        consulta.setEstado(EstadoConsulta.ATENDIDA);
        consulta.setActiva(true);
        return consultaRepository.save(consulta);
    }

    private void validarDatos(ConsultaRequest request) {
        if (request == null) {
            throw new IllegalArgumentException(
                "Los datos de la consulta son obligatorios."
            );
        }

        if (request.getClienteId() == null
                || request.getMascotaId() == null
                || request.getVeterinarioId() == null) {
            throw new IllegalArgumentException(
                "Debe seleccionar cliente, mascota y veterinario."
            );
        }

        if (request.getFechaHora() == null) {
            throw new IllegalArgumentException(
                "La fecha y hora son obligatorias."
            );
        }

        if (!request.getFechaHora().isAfter(LocalDateTime.now())) {
            throw new IllegalArgumentException(
                "La fecha y hora de la consulta deben estar en el futuro."
            );
        }

        if (request.getMotivo() == null
                || request.getMotivo().isBlank()) {
            throw new IllegalArgumentException(
                "El motivo de la consulta es obligatorio."
            );
        }

        Cliente cliente = clienteRepository.findById(request.getClienteId())
            .orElseThrow(() ->
                new IllegalArgumentException("El cliente no existe.")
            );

        if (!cliente.isActivo()) {
            throw new IllegalArgumentException(
                "No se pueden agendar consultas para un cliente inactivo."
            );
        }

        Mascota mascota = mascotaRepository.findById(request.getMascotaId())
            .orElseThrow(() ->
                new IllegalArgumentException("La mascota no existe.")
            );

        if (!mascota.isActiva()) {
            throw new IllegalArgumentException(
                "No se pueden agendar consultas para una mascota inactiva."
            );
        }

        if (mascota.getCliente() == null
                || !mascota.getCliente().getId().equals(cliente.getId())) {
            throw new IllegalArgumentException(
                "La mascota seleccionada no pertenece al cliente elegido."
            );
        }

        Veterinario veterinario =
            veterinarioRepository.findById(request.getVeterinarioId())
                .orElseThrow(() ->
                    new IllegalArgumentException(
                        "El veterinario no existe."
                    )
                );

        if (!veterinario.isActivo()) {
            throw new IllegalArgumentException(
                "No se pueden agendar consultas con un veterinario inactivo."
            );
        }

        EstadoConsulta estado = request.getEstado() == null
            ? EstadoConsulta.PENDIENTE
            : request.getEstado();

        if (estado != EstadoConsulta.PENDIENTE
                && estado != EstadoConsulta.CONFIRMADA
                && estado != EstadoConsulta.PROGRAMADA
                && estado != EstadoConsulta.ATENDIDA
                && estado != EstadoConsulta.CANCELADA) {
            throw new IllegalArgumentException(
                "El estado de la consulta no es válido."
            );
        }
    }

    private void aplicarDatos(
            Consulta consulta,
            ConsultaRequest request,
            Long consultaId) {

        Mascota mascota = mascotaRepository.findById(request.getMascotaId())
            .orElseThrow(() ->
                new IllegalArgumentException("La mascota no existe.")
            );

        Veterinario veterinario =
            veterinarioRepository.findById(request.getVeterinarioId())
                .orElseThrow(() ->
                    new IllegalArgumentException(
                        "El veterinario no existe."
                    )
                );

        EstadoConsulta estado = request.getEstado() == null
            ? (consultaId == null
                ? EstadoConsulta.PENDIENTE
                : consulta.getEstado())
            : request.getEstado();

        if (estado == EstadoConsulta.CANCELADA) {
            consulta.setEstado(EstadoConsulta.CANCELADA);
            consulta.setActiva(false);
            return;
        }

        if (ESTADOS_QUE_OCUPAN_HORARIO.contains(estado)) {
            LocalDateTime desde = request.getFechaHora().minusMinutes(15);
            LocalDateTime hasta = request.getFechaHora().plusMinutes(15);

            long conflictosVeterinario =
                consultaRepository.contarConflictosVeterinario(
                    veterinario.getId(),
                    desde,
                    hasta,
                    ESTADOS_QUE_OCUPAN_HORARIO,
                    consultaId
                );

            if (conflictosVeterinario > 0) {
                throw new IllegalStateException(
                    "El veterinario tiene otra consulta dentro del margen de 15 minutos."
                );
            }

            long conflictosMascota =
                consultaRepository.contarCitasSimultaneasMascota(
                    mascota.getId(),
                    request.getFechaHora(),
                    ESTADOS_QUE_OCUPAN_HORARIO,
                    consultaId
                );

            if (conflictosMascota > 0) {
                throw new IllegalStateException(
                    "La mascota ya tiene una consulta programada a esa hora."
                );
            }
        }

        consulta.setFechaHora(request.getFechaHora());
        consulta.setMotivo(request.getMotivo().trim());
        consulta.setMascota(mascota);
        consulta.setVeterinario(veterinario);
        consulta.setEstado(estado);
        consulta.setActiva(true);
    }
}
