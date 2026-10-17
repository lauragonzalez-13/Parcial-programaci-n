package com.uniminuto.clinica.serviceimpl;

import com.uniminuto.clinica.dto.AnotacionClinicaRequest;
import com.uniminuto.clinica.dto.HistoriaClinicaRequest;
import com.uniminuto.clinica.entity.AnotacionClinica;
import com.uniminuto.clinica.entity.Cliente;
import com.uniminuto.clinica.entity.Consulta;
import com.uniminuto.clinica.entity.EstadoConsulta;
import com.uniminuto.clinica.entity.HistoriaClinica;
import com.uniminuto.clinica.entity.Mascota;
import com.uniminuto.clinica.repository.AnotacionClinicaRepository;
import com.uniminuto.clinica.repository.ClienteRepository;
import com.uniminuto.clinica.repository.ConsultaRepository;
import com.uniminuto.clinica.repository.HistoriaClinicaRepository;
import com.uniminuto.clinica.repository.MascotaRepository;
import com.uniminuto.clinica.service.HistoriaClinicaService;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class HistoriaClinicaServiceImpl implements HistoriaClinicaService {
    private final HistoriaClinicaRepository historiaRepository;
    private final AnotacionClinicaRepository anotacionRepository;
    private final ClienteRepository clienteRepository;
    private final MascotaRepository mascotaRepository;
    private final ConsultaRepository consultaRepository;

    public HistoriaClinicaServiceImpl(HistoriaClinicaRepository historiaRepository,
            AnotacionClinicaRepository anotacionRepository,
            ClienteRepository clienteRepository,
            MascotaRepository mascotaRepository,
            ConsultaRepository consultaRepository) {
        this.historiaRepository = historiaRepository;
        this.anotacionRepository = anotacionRepository;
        this.clienteRepository = clienteRepository;
        this.mascotaRepository = mascotaRepository;
        this.consultaRepository = consultaRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<AnotacionClinica> listarHistorias() {
        return anotacionRepository.findAllByOrderByFechaHoraDesc();
    }

    @Override
    @Transactional(readOnly = true)
    public HistoriaClinica buscarHistoria(Long id) {
        return historiaRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("La historia clínica no existe."));
    }

    @Override
    public HistoriaClinica crearHistoria(HistoriaClinicaRequest request) {
        if (request == null || request.clienteId() == null || request.mascotaId() == null
                || request.consultaId() == null) {
            throw new IllegalArgumentException("Cliente, mascota y cita son obligatorios.");
        }
        Cliente cliente = clienteRepository.findById(request.clienteId())
                .orElseThrow(() -> new IllegalArgumentException("El cliente no existe."));
        Mascota mascota = mascotaRepository.findById(request.mascotaId())
                .orElseThrow(() -> new IllegalArgumentException("La mascota no existe."));
        if (mascota.getCliente() == null || !mascota.getCliente().getId().equals(cliente.getId())) {
            throw new IllegalArgumentException("La mascota no pertenece al cliente seleccionado.");
        }
        if (historiaRepository.findByMascotaId(mascota.getId()).isPresent()) {
            throw new IllegalStateException("La mascota ya tiene historia clínica. Agrega una anotación a la existente.");
        }
        Consulta consulta = buscarConsultaAtendida(request.consultaId(), mascota.getId());
        validarTexto(request.descripcion(), "La descripción de la anotación es obligatoria.");
        validarTexto(request.observacion(), "La observación es obligatoria.");
        validarTexto(request.diagnostico(), "El diagnóstico o evolución es obligatorio.");

        HistoriaClinica historia = new HistoriaClinica();
        historia.setMascota(mascota);
        historia.setFechaCreacion(LocalDateTime.now());
        historia = historiaRepository.save(historia);
        guardarAnotacion(historia, consulta, request.descripcion(), request.observacion(), request.diagnostico());
        return historia;
    }

    @Override
    public AnotacionClinica agregarAnotacion(Long historiaId, AnotacionClinicaRequest request) {
        if (request == null || request.consultaId() == null) {
            throw new IllegalArgumentException("Debe seleccionar la cita atendida.");
        }
        HistoriaClinica historia = buscarHistoria(historiaId);
        Consulta consulta = buscarConsultaAtendida(request.consultaId(), historia.getMascota().getId());
        validarTexto(request.descripcion(), "La descripción de la anotación es obligatoria.");
        validarTexto(request.observacion(), "La observación es obligatoria.");
        validarTexto(request.diagnostico(), "El diagnóstico o evolución es obligatorio.");
        return guardarAnotacion(historia, consulta, request.descripcion(), request.observacion(), request.diagnostico());
    }

    @Override
    @Transactional(readOnly = true)
    public List<AnotacionClinica> listarAnotaciones(Long historiaId) {
        buscarHistoria(historiaId);
        return anotacionRepository.findByHistoriaClinicaIdOrderByFechaHoraDesc(historiaId);
    }

    @Override
    @Transactional(readOnly = true)
    public AnotacionClinica buscarAnotacion(Long historiaId, Long anotacionId) {
        return anotacionRepository.findByIdAndHistoriaClinicaId(anotacionId, historiaId)
                .orElseThrow(() -> new IllegalArgumentException("La anotación no existe en esta historia clínica."));
    }

    private Consulta buscarConsultaAtendida(Long consultaId, Long mascotaId) {
        Consulta consulta = consultaRepository.findById(consultaId)
                .orElseThrow(() -> new IllegalArgumentException("La cita no existe."));
        if (consulta.getMascota() == null || !consulta.getMascota().getId().equals(mascotaId)) {
            throw new IllegalArgumentException("La cita no corresponde a la mascota de esta historia clínica.");
        }
        if (consulta.getEstado() != EstadoConsulta.ATENDIDA) {
            throw new IllegalArgumentException("Solo se pueden registrar historias para citas atendidas.");
        }
        if (anotacionRepository.existsByConsultaId(consultaId)) {
            throw new IllegalStateException("La cita ya tiene una anotación clínica asociada.");
        }
        return consulta;
    }

    private AnotacionClinica guardarAnotacion(HistoriaClinica historia, Consulta consulta,
            String descripcion, String observacion, String diagnostico) {
        AnotacionClinica anotacion = new AnotacionClinica();
        anotacion.setHistoriaClinica(historia);
        anotacion.setConsulta(consulta);
        anotacion.setVeterinario(consulta.getVeterinario());
        anotacion.setFechaHora(consulta.getFechaHora());
        anotacion.setDescripcion(descripcion.trim());
        anotacion.setObservacion(limpiar(observacion));
        anotacion.setDiagnostico(limpiar(diagnostico));
        if (diagnostico != null && !diagnostico.isBlank()) {
            consulta.setDiagnostico(diagnostico.trim());
            consultaRepository.save(consulta);
        }
        return anotacionRepository.save(anotacion);
    }

    private void validarTexto(String valor, String mensaje) {
        if (valor == null || valor.isBlank()) throw new IllegalArgumentException(mensaje);
    }

    private String limpiar(String valor) {
        return valor == null || valor.isBlank() ? null : valor.trim();
    }
}
