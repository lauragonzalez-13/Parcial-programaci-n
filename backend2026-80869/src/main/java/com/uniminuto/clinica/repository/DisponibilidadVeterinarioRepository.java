package com.uniminuto.clinica.repository;

import com.uniminuto.clinica.entity.DisponibilidadVeterinario;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DisponibilidadVeterinarioRepository extends JpaRepository<DisponibilidadVeterinario, Long> {
    List<DisponibilidadVeterinario> findByVeterinarioIdAndActivaTrueOrderByDiaSemanaAscHoraInicioAsc(Long veterinarioId);
    List<DisponibilidadVeterinario> findByVeterinarioIdAndDiaSemanaAndActivaTrue(Long veterinarioId, int diaSemana);
}
