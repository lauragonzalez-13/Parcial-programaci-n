package com.uniminuto.clinica.repository;

import com.uniminuto.clinica.entity.ImportacionInventario;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ImportacionInventarioRepository extends JpaRepository<ImportacionInventario, Long> {
    List<ImportacionInventario> findAllByOrderByFechaDesc();
}
