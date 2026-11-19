package com.uniminuto.clinica.config;

import com.uniminuto.clinica.entity.DisponibilidadVeterinario;
import com.uniminuto.clinica.entity.EspecialidadMedica;
import com.uniminuto.clinica.repository.DisponibilidadVeterinarioRepository;
import com.uniminuto.clinica.repository.EspecialidadMedicaRepository;
import com.uniminuto.clinica.repository.VeterinarioRepository;
import com.uniminuto.clinica.repository.TarifaServicioRepository;
import com.uniminuto.clinica.entity.TarifaServicio;
import java.math.BigDecimal;
import java.time.LocalTime;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MigracionAgendaConfig {
    @Bean
    CommandLineRunner prepararAgenda(EspecialidadMedicaRepository especialidades, VeterinarioRepository veterinarios,
            DisponibilidadVeterinarioRepository horarios, TarifaServicioRepository tarifas) {
        return args -> {
            EspecialidadMedica general = especialidades.findByNombreIgnoreCase("General").orElseGet(() -> {
                EspecialidadMedica e = new EspecialidadMedica(); e.setNombre("General"); return especialidades.save(e);
            });
            if (!tarifas.existsByCodigoIgnoreCase("CONSULTA-GENERAL")) {
                TarifaServicio tarifa = new TarifaServicio(); tarifa.setCodigo("CONSULTA-GENERAL"); tarifa.setNombre("Consulta veterinaria general");
                tarifa.setTipoServicio("CONSULTA"); tarifa.setEspecialidad(general); tarifa.setValor(new BigDecimal("50000")); tarifa.setIvaPorcentaje(BigDecimal.ZERO); tarifas.save(tarifa);
            }
            veterinarios.findAll().stream().filter(v -> v.isActivo()).forEach(v -> {
                if (v.getEspecialidad() == null) { v.setEspecialidad(general); veterinarios.save(v); }
                if (horarios.findByVeterinarioIdAndActivaTrueOrderByDiaSemanaAscHoraInicioAsc(v.getId()).isEmpty()) {
                    for (int dia = 1; dia <= 5; dia++) {
                        DisponibilidadVeterinario h = new DisponibilidadVeterinario(); h.setVeterinario(v); h.setDiaSemana(dia);
                        h.setHoraInicio(LocalTime.of(8, 0)); h.setHoraFin(LocalTime.of(17, 0)); horarios.save(h);
                    }
                }
            });
        };
    }
}
