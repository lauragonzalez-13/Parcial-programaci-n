
package com.uniminuto.clinica.service;

import com.uniminuto.clinica.entity.FormulaMedica;
import java.util.List;

public interface FormulaMedicaService {

    // Registrar una fórmula médica
    FormulaMedica guardar(FormulaMedica formula);

    // Listar las fórmulas médicas
    List<FormulaMedica> listar();

    // Consultar una fórmula por su ID
    FormulaMedica buscarPorId(Long id);

    // Actualizar una fórmula médica
    FormulaMedica actualizar(Long id, FormulaMedica formula);

    // Listar las fórmulas de una mascota
    List<FormulaMedica> listarPorMascota(Long mascotaId);

    // Desactivar una fórmula médica
    FormulaMedica desactivar(Long id);
}
