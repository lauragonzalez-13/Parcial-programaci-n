package com.uniminuto.clinica.repository;

import com.uniminuto.clinica.entity.Mascota;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import org.springframework.stereotype.Repository;

@Repository
public interface MascotaRepository
        extends JpaRepository<Mascota, Long> {
    List<Mascota> findByClienteId(Long clienteId);
}
