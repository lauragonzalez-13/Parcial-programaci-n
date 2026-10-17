package com.uniminuto.clinica.repository;

import com.uniminuto.clinica.entity.AnotacionClinica;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AnotacionClinicaRepository extends JpaRepository<AnotacionClinica, Long> {
    List<AnotacionClinica> findAllByOrderByFechaHoraDesc();
    List<AnotacionClinica> findByHistoriaClinicaIdOrderByFechaHoraDesc(Long historiaId);
    Optional<AnotacionClinica> findByIdAndHistoriaClinicaId(Long id, Long historiaId);
    boolean existsByConsultaId(Long consultaId);
}
