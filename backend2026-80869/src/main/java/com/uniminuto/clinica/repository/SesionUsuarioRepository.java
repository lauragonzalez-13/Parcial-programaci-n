package com.uniminuto.clinica.repository;

import com.uniminuto.clinica.entity.SesionUsuario;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SesionUsuarioRepository extends JpaRepository<SesionUsuario, Long> {
    Optional<SesionUsuario> findByTokenHashAndRevocadaFalse(String tokenHash);
    @Modifying
    @Query("delete from SesionUsuario s where s.usuario.id = :usuarioId")
    int borrarPorUsuarioId(@Param("usuarioId") Long usuarioId);
}
