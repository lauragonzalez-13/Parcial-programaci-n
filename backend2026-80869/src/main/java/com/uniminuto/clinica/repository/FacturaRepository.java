package com.uniminuto.clinica.repository;
import com.uniminuto.clinica.entity.Factura;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
public interface FacturaRepository extends JpaRepository<Factura,Long> {
    Optional<Factura> findByConsultaId(Long consultaId);
    List<Factura> findByClienteIdOrderByFechaGeneracionDesc(Long clienteId);
    List<Factura> findByMascotaIdOrderByFechaGeneracionDesc(Long mascotaId);
    List<Factura> findAllByOrderByFechaGeneracionDesc();
}
