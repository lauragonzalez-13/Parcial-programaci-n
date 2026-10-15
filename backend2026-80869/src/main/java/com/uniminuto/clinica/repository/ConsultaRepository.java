package com.uniminuto.clinica.repository;

import com.uniminuto.clinica.entity.Consulta;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ConsultaRepository
        extends JpaRepository<Consulta, Long> {
}