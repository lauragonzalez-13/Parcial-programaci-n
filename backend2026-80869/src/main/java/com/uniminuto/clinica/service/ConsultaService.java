
package com.uniminuto.clinica.service;
import com.uniminuto.clinica.dto.ConsultaRequest;
import com.uniminuto.clinica.entity.Consulta;
import java.util.List;

public interface ConsultaService {

    List<Consulta> listar();

    Consulta buscarPorId(Long id);

    Consulta crear(ConsultaRequest request);

    Consulta actualizar(Long id, ConsultaRequest request);

    Consulta cancelar(Long id);

    Consulta atender(Long id);
}
