package com.uniminuto.clinica.repository;

import com.uniminuto.clinica.entity.MovimientoInventario;
import com.uniminuto.clinica.entity.TipoMovimiento;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MovimientoInventarioRepository extends JpaRepository<MovimientoInventario, Long> {
    List<MovimientoInventario> findAllByOrderByFechaDesc();
    List<MovimientoInventario> findByMedicamentoIdOrderByFechaDesc(Long medicamentoId);
    List<MovimientoInventario> findByFormulaMedicaIdOrderByFechaDesc(Long formulaMedicaId);
    List<MovimientoInventario> findByFormulaMedicaConsultaIdAndTipoOrderByFechaAsc(Long consultaId, TipoMovimiento tipo);
    List<MovimientoInventario> findByDespachoIdOrderByFechaAsc(Long despachoId);
}
