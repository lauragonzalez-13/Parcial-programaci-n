
package com.uniminuto.clinica.apicontroller;

import com.uniminuto.clinica.dto.ConsultaRequest;
import com.uniminuto.clinica.entity.Consulta;
import com.uniminuto.clinica.service.ConsultaService;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/consultas")
public class ConsultaController {

    private final ConsultaService consultaService;

    public ConsultaController(ConsultaService consultaService) {
        this.consultaService = consultaService;
    }

    // Listar consultas: más recientes primero
    @GetMapping
    public ResponseEntity<List<Consulta>> listar() {
        return ResponseEntity.ok(consultaService.listar());
    }

    // Buscar una consulta por ID
    @GetMapping("/{id}")
    public ResponseEntity<Consulta> buscarPorId(@PathVariable Long id) {
        return ResponseEntity.ok(consultaService.buscarPorId(id));
    }

    // Crear una consulta
    @PostMapping
    public ResponseEntity<Consulta> crear(
            @RequestBody ConsultaRequest request) {

        Consulta consulta = consultaService.crear(request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(consulta);
    }

    // Actualizar una consulta
    @PutMapping("/{id}")
    public ResponseEntity<Consulta> actualizar(
            @PathVariable Long id,
            @RequestBody ConsultaRequest request) {
        return ResponseEntity.ok(
                consultaService.actualizar(id, request));
    }

    // Cancelar una consulta explícitamente
    @PatchMapping("/{id}/cancelar")
    public ResponseEntity<Consulta> cancelar(@PathVariable Long id) {
        return ResponseEntity.ok(consultaService.cancelar(id));
    }

    // Registrar que la cita ocurrió; su información clínica queda asociada a la consulta.
    @PatchMapping("/{id}/atender")
    public ResponseEntity<Consulta> atender(@PathVariable Long id) {
        return ResponseEntity.ok(consultaService.atender(id));
    }

    // Errores de validación de datos
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> manejarDatosInvalidos(
            IllegalArgumentException ex) {
        return ResponseEntity.badRequest().body(
                Map.of("mensaje", ex.getMessage()));
    }

    // Conflictos de estado o de horario
    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<Map<String, String>> manejarConflictos(
            IllegalStateException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(
                Map.of("mensaje", ex.getMessage()));
    }
}
