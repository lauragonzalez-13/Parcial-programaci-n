
package com.uniminuto.clinica.apicontroller;

import com.uniminuto.clinica.entity.FormulaMedica;
import com.uniminuto.clinica.service.FormulaMedicaService;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/formulas-medicas")
public class FormulaMedicaController {

    private final FormulaMedicaService formulaService;

    public FormulaMedicaController(
            FormulaMedicaService formulaService) {
        this.formulaService = formulaService;
    }

    // Registrar una fórmula médica
    @PostMapping
    public ResponseEntity<?> guardar(
            @RequestBody FormulaMedica formula) {
        try {
            FormulaMedica guardada =
                    formulaService.guardar(formula);

            return ResponseEntity
                    .status(HttpStatus.CREATED)
                    .body(guardada);

        } catch (IllegalArgumentException e) {
            return ResponseEntity
                    .badRequest()
                    .body(Map.of("mensaje", e.getMessage()));
        }
    }

    // Listar las fórmulas médicas
    @GetMapping
    public ResponseEntity<List<FormulaMedica>> listar() {
        return ResponseEntity.ok(
                formulaService.listar());
    }

    // Consultar una fórmula por su ID
    @GetMapping("/{id}")
    public ResponseEntity<?> buscarPorId(
            @PathVariable Long id) {
        try {
            return ResponseEntity.ok(
                    formulaService.buscarPorId(id));

        } catch (IllegalArgumentException e) {
            return ResponseEntity
                    .status(HttpStatus.NOT_FOUND)
                    .body(Map.of("mensaje", e.getMessage()));
        }
    }

    // Actualizar una fórmula médica
    @PutMapping("/{id}")
    public ResponseEntity<?> actualizar(
            @PathVariable Long id,
            @RequestBody FormulaMedica formula) {
        try {
            FormulaMedica actualizada =
                    formulaService.actualizar(id, formula);

            return ResponseEntity.ok(actualizada);

        } catch (IllegalArgumentException e) {
            return ResponseEntity
                    .status(HttpStatus.NOT_FOUND)
                    .body(Map.of("mensaje", e.getMessage()));
        }
    }

    // Consultar las fórmulas de una mascota
    @GetMapping("/mascota/{mascotaId}")
    public ResponseEntity<List<FormulaMedica>> listarPorMascota(
            @PathVariable Long mascotaId) {
        return ResponseEntity.ok(
                formulaService.listarPorMascota(mascotaId));
    }

    // Desactivar una fórmula sin eliminarla
    @PatchMapping("/{id}/desactivar")
    public ResponseEntity<?> desactivar(
            @PathVariable Long id) {
        try {
            return ResponseEntity.ok(
                    formulaService.desactivar(id));

        } catch (IllegalArgumentException e) {
            return ResponseEntity
                    .status(HttpStatus.NOT_FOUND)
                    .body(Map.of("mensaje", e.getMessage()));
        }
    }
}
