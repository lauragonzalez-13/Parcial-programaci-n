package com.uniminuto.clinica.repository;
import com.uniminuto.clinica.entity.TarifaServicio;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
public interface TarifaServicioRepository extends JpaRepository<TarifaServicio,Long> {
    Optional<TarifaServicio> findFirstByTipoServicioAndEspecialidadIdAndActivaTrueOrderByIdDesc(String tipo,Long especialidadId);
    Optional<TarifaServicio> findFirstByTipoServicioAndEspecialidadIsNullAndActivaTrueOrderByIdDesc(String tipo);
    boolean existsByCodigoIgnoreCase(String codigo);
}
