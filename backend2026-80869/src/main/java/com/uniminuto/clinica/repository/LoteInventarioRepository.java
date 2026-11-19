package com.uniminuto.clinica.repository;

import com.uniminuto.clinica.entity.LoteInventario;
import jakarta.persistence.LockModeType;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface LoteInventarioRepository extends JpaRepository<LoteInventario, Long> {
    List<LoteInventario> findByMedicamentoIdOrderByFechaVencimientoAsc(Long medicamentoId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select l from LoteInventario l where l.medicamento.id = :medicamentoId and l.cantidadDisponible > 0 order by case when l.fechaVencimiento is null then 1 else 0 end, l.fechaVencimiento asc, l.id asc")
    List<LoteInventario> bloquearLotesDisponibles(@Param("medicamentoId") Long medicamentoId);

    boolean existsByMedicamentoIdAndCodigoLoteIgnoreCase(Long medicamentoId, String codigoLote);
}
