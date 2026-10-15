package com.uniminuto.clinica.repository;

import com.uniminuto.clinica.entity.FormulaMedica;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface FormulaMedicaRepository
        extends JpaRepository<FormulaMedica, Long> {

    List<FormulaMedica> findByActivaTrue();

    List<FormulaMedica> findByMascota_Id(Long mascotaId);

    List<FormulaMedica> findByCliente_Id(Long clienteId);

    List<FormulaMedica> findByVeterinario_Id(Long veterinarioId);
}
