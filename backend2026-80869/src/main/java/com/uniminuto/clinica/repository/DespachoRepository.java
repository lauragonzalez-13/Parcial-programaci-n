package com.uniminuto.clinica.repository;

import com.uniminuto.clinica.entity.Despacho;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DespachoRepository extends JpaRepository<Despacho,Long> {
    Optional<Despacho> findByFormulaId(Long formulaId);
    List<Despacho> findAllByOrderByCreadoEnDesc();
}
