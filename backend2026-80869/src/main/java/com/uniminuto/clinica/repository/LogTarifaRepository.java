package com.uniminuto.clinica.repository;
import com.uniminuto.clinica.entity.LogTarifa;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
public interface LogTarifaRepository extends JpaRepository<LogTarifa,Long> { List<LogTarifa> findByTarifaIdOrderByFechaDesc(Long tarifaId); }
