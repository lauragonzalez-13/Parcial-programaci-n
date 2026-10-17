package com.uniminuto.clinica.repository;

import com.uniminuto.clinica.entity.HistoriaClinica;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface HistoriaClinicaRepository extends JpaRepository<HistoriaClinica, Long> {
    Optional<HistoriaClinica> findByMascotaId(Long mascotaId);
}
