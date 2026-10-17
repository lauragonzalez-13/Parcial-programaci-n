package com.uniminuto.clinica.service;

import com.uniminuto.clinica.dto.AnotacionClinicaRequest;
import com.uniminuto.clinica.dto.HistoriaClinicaRequest;
import com.uniminuto.clinica.entity.AnotacionClinica;
import com.uniminuto.clinica.entity.HistoriaClinica;
import java.util.List;

public interface HistoriaClinicaService {
    List<AnotacionClinica> listarHistorias();
    HistoriaClinica buscarHistoria(Long id);
    HistoriaClinica crearHistoria(HistoriaClinicaRequest request);
    AnotacionClinica agregarAnotacion(Long historiaId, AnotacionClinicaRequest request);
    List<AnotacionClinica> listarAnotaciones(Long historiaId);
    AnotacionClinica buscarAnotacion(Long historiaId, Long anotacionId);
}
