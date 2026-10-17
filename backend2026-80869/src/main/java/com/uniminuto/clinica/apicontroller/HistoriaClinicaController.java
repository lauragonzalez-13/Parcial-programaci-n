package com.uniminuto.clinica.apicontroller;

import com.uniminuto.clinica.dto.AnotacionClinicaRequest;
import com.uniminuto.clinica.dto.HistoriaClinicaRequest;
import com.uniminuto.clinica.entity.AnotacionClinica;
import com.uniminuto.clinica.entity.HistoriaClinica;
import com.uniminuto.clinica.service.HistoriaClinicaService;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/historias-clinicas")
public class HistoriaClinicaController {
    private final HistoriaClinicaService service;

    public HistoriaClinicaController(HistoriaClinicaService service) { this.service = service; }

    @GetMapping
    public List<AnotacionClinica> listar() { return service.listarHistorias(); }

    @GetMapping("/{id}")
    public HistoriaClinica buscar(@PathVariable Long id) { return service.buscarHistoria(id); }

    @PostMapping
    public ResponseEntity<HistoriaClinica> crear(@RequestBody HistoriaClinicaRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.crearHistoria(request));
    }

    @GetMapping("/{id}/anotaciones")
    public List<AnotacionClinica> listarAnotaciones(@PathVariable Long id) {
        return service.listarAnotaciones(id);
    }

    @GetMapping("/{id}/anotaciones/{anotacionId}")
    public AnotacionClinica buscarAnotacion(@PathVariable Long id, @PathVariable Long anotacionId) {
        return service.buscarAnotacion(id, anotacionId);
    }

    @PostMapping("/{id}/anotaciones")
    public ResponseEntity<AnotacionClinica> agregarAnotacion(@PathVariable Long id,
            @RequestBody AnotacionClinicaRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.agregarAnotacion(id, request));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> datosInvalidos(IllegalArgumentException e) {
        return ResponseEntity.badRequest().body(Map.of("mensaje", e.getMessage()));
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<Map<String, String>> conflicto(IllegalStateException e) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("mensaje", e.getMessage()));
    }
}
