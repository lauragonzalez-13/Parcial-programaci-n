package com.uniminuto.clinica.repository;

import com.uniminuto.clinica.entity.EspecialidadMedica;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EspecialidadMedicaRepository extends JpaRepository<EspecialidadMedica, Long> {
    Optional<EspecialidadMedica> findByNombreIgnoreCase(String nombre);
}
