
package com.uniminuto.clinica.repository;

import com.uniminuto.clinica.entity.Consulta;
import com.uniminuto.clinica.entity.EstadoConsulta;
import java.time.LocalDateTime;
import java.util.Collection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface ConsultaRepository extends JpaRepository<Consulta, Long> {

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
}
