package com.uniminuto.clinica.repository;

import com.uniminuto.clinica.entity.Medicamento;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MedicamentoRepository extends JpaRepository<Medicamento, Long> {
    boolean existsByCodigoIgnoreCase(String codigo);
    boolean existsByNombreIgnoreCase(String nombre);
    Optional<Medicamento> findByCodigoIgnoreCase(String codigo);
    Optional<Medicamento> findByNombreIgnoreCase(String nombre);
}
