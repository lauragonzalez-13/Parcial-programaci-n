package com.uniminuto.clinica.repository;

import com.uniminuto.clinica.entity.EventoDespacho;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EventoDespachoRepository extends JpaRepository<EventoDespacho,Long> {
    List<EventoDespacho> findByDespachoIdOrderByFechaDesc(Long despachoId);
}
