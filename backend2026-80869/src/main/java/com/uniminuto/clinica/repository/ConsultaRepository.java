
package com.uniminuto.clinica.repository;

import com.uniminuto.clinica.entity.Consulta;
import com.uniminuto.clinica.entity.EstadoConsulta;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.time.LocalDate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface ConsultaRepository extends JpaRepository<Consulta, Long> {

        List<Consulta> findByVeterinarioIdAndFechaHoraBetweenOrderByFechaHoraAsc(Long veterinarioId, LocalDateTime desde, LocalDateTime hasta);
        List<Consulta> findByMascotaIdOrderByFechaHoraDesc(Long mascotaId);

        @Query("""
                        SELECT COUNT(c)
                        FROM Consulta c
                        WHERE c.veterinario.id = :veterinarioId
                          AND c.estado IN :estados
                          AND c.fechaHora BETWEEN :desde AND :hasta
                          AND (:consultaId IS NULL OR c.id <> :consultaId)
                        """)
        long contarConflictosVeterinario(
                        @Param("veterinarioId") Long veterinarioId,
                        @Param("desde") LocalDateTime desde,
                        @Param("hasta") LocalDateTime hasta,
                        @Param("estados") Collection<EstadoConsulta> estados,
                        @Param("consultaId") Long consultaId);

        @Query("""
                        SELECT COUNT(c)
                        FROM Consulta c
                        WHERE c.mascota.id = :mascotaId
                          AND c.fechaHora = :fechaHora
                          AND c.estado IN :estados
                          AND (:consultaId IS NULL OR c.id <> :consultaId)
                        """)
        long contarCitasSimultaneasMascota(
                        @Param("mascotaId") Long mascotaId,
                        @Param("fechaHora") LocalDateTime fechaHora,
                        @Param("estados") Collection<EstadoConsulta> estados,
                        @Param("consultaId") Long consultaId);

        @Query(value = "SELECT COUNT(*) FROM consulta c WHERE c.veterinario_id = :veterinarioId AND c.estado IN (:estados) AND c.fecha_hora < :fin AND DATE_ADD(c.fecha_hora, INTERVAL COALESCE(c.duracion_minutos, 30) MINUTE) > :inicio AND (:consultaId IS NULL OR c.id <> :consultaId)", nativeQuery = true)
        long contarSolapamientosVeterinario(@Param("veterinarioId") Long veterinarioId, @Param("inicio") LocalDateTime inicio, @Param("fin") LocalDateTime fin, @Param("estados") Collection<EstadoConsulta> estados, @Param("consultaId") Long consultaId);

        @Query(value = "SELECT COUNT(*) FROM consulta c WHERE c.mascota_id = :mascotaId AND c.estado IN (:estados) AND c.fecha_hora < :fin AND DATE_ADD(c.fecha_hora, INTERVAL COALESCE(c.duracion_minutos, 30) MINUTE) > :inicio AND (:consultaId IS NULL OR c.id <> :consultaId)", nativeQuery = true)
        long contarSolapamientosMascota(@Param("mascotaId") Long mascotaId, @Param("inicio") LocalDateTime inicio, @Param("fin") LocalDateTime fin, @Param("estados") Collection<EstadoConsulta> estados, @Param("consultaId") Long consultaId);
}
