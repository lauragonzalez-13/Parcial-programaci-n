package com.uniminuto.clinica.repository;

import com.uniminuto.clinica.entity.FormulaMedica;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.math.BigDecimal;
import java.util.Optional;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

@Repository
public interface FormulaMedicaRepository
        extends JpaRepository<FormulaMedica, Long> {

    List<FormulaMedica> findByActivaTrue();

    List<FormulaMedica> findByMascota_Id(Long mascotaId);

    List<FormulaMedica> findByCliente_Id(Long clienteId);

    List<FormulaMedica> findByVeterinario_Id(Long veterinarioId);

    List<FormulaMedica> findByMascota_IdOrderByFechaEmisionDesc(Long mascotaId);
    List<FormulaMedica> findByConsultaIdOrderByFechaEmisionDesc(Long consultaId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select f from FormulaMedica f where f.id = :id")
    Optional<FormulaMedica> bloquearPorId(@Param("id") Long id);

    @org.springframework.data.jpa.repository.Query("select coalesce(sum(f.cantidadRequerida - f.cantidadDispensada), 0) from FormulaMedica f where f.medicamentoInventario.id = :medicamentoId and f.activa = true and f.estadoDispensacion <> com.uniminuto.clinica.entity.EstadoDispensacion.DISPENSADA and (:formulaId is null or f.id <> :formulaId)")
    BigDecimal cantidadPendientePorDispensar(@org.springframework.data.repository.query.Param("medicamentoId") Long medicamentoId,
            @org.springframework.data.repository.query.Param("formulaId") Long formulaId);
}
