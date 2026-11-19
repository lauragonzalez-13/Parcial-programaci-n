
package com.uniminuto.clinica.service;
import com.uniminuto.clinica.dto.ConsultaRequest;
import com.uniminuto.clinica.entity.Consulta;
import java.util.List;
import java.time.LocalDateTime;

public interface ConsultaService {

    List<Consulta> listar();
    List<Consulta> listarPorVeterinario(Long id, LocalDateTime desde, LocalDateTime hasta);
    List<Consulta> historialMascota(Long mascotaId);

    Consulta buscarPorId(Long id);

    Consulta crear(ConsultaRequest request);

    Consulta actualizar(Long id, ConsultaRequest request);

    Consulta cancelar(Long id);

    Consulta atender(Long id);
}
