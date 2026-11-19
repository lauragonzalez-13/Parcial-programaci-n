package com.uniminuto.clinica.repository;

import com.uniminuto.clinica.entity.DetalleDespacho;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DetalleDespachoRepository extends JpaRepository<DetalleDespacho,Long> {
    List<DetalleDespacho> findByDespachoIdOrderByFechaAsc(Long despachoId);
}
