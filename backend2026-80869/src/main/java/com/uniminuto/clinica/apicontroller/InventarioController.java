package com.uniminuto.clinica.apicontroller;

import com.uniminuto.clinica.dto.LoteInventarioRequest;
import com.uniminuto.clinica.dto.MedicamentoRequest;
import com.uniminuto.clinica.dto.DispensarFormulaRequest;
import com.uniminuto.clinica.dto.PrevisualizacionImportacion;
import com.uniminuto.clinica.entity.ImportacionInventario;
import com.uniminuto.clinica.entity.LoteInventario;
import com.uniminuto.clinica.entity.MovimientoInventario;
import com.uniminuto.clinica.service.InventarioService;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/inventario")
public class InventarioController {
    private final InventarioService service;
    public InventarioController(InventarioService service) { this.service = service; }

    @GetMapping("/medicamentos")
    public List<?> medicamentos(@RequestParam(defaultValue = "false") boolean disponibles) { return service.listar(disponibles); }

    @PostMapping("/medicamentos")
    public ResponseEntity<?> crear(@RequestBody MedicamentoRequest request) { return ResponseEntity.status(HttpStatus.CREATED).body(service.guardar(request, null)); }

    @PutMapping("/medicamentos/{id}")
    public ResponseEntity<?> actualizar(@PathVariable Long id, @RequestBody MedicamentoRequest request) { return ResponseEntity.ok(service.guardar(request, id)); }

    @PatchMapping("/medicamentos/{id}/iva")
    public ResponseEntity<?> actualizarIva(@PathVariable Long id,@RequestBody Map<String,java.math.BigDecimal> body){try{return ResponseEntity.ok(service.actualizarImpuesto(id,body.get("ivaPorcentaje")));}catch(IllegalArgumentException e){return ResponseEntity.badRequest().body(Map.of("mensaje",e.getMessage()));}}

    @GetMapping("/lotes")
    public List<LoteInventario> lotes(@RequestParam(required = false) Long medicamentoId) { return service.listarLotes(medicamentoId); }

    @PostMapping("/lotes")
    public ResponseEntity<LoteInventario> ingresar(@RequestBody LoteInventarioRequest request) { return ResponseEntity.status(HttpStatus.CREATED).body(service.ingresar(request)); }

    @GetMapping("/movimientos")
    public List<MovimientoInventario> movimientos(@RequestParam(required = false) Long medicamentoId) { return service.listarMovimientos(medicamentoId); }

    @GetMapping("/alertas")
    public List<?> alertas() { return service.alertas(); }

    @PostMapping("/importaciones/preview")
    public PrevisualizacionImportacion previsualizar(@RequestParam("archivo") MultipartFile archivo,
            @RequestParam(defaultValue = "") String usuarioResponsable) {
        return service.previsualizar(archivo, usuarioResponsable);
    }

    @PostMapping("/importaciones/{token}/confirmar")
    public ResponseEntity<ImportacionInventario> confirmar(@PathVariable String token,
            @RequestParam(defaultValue = "") String usuarioResponsable) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.confirmar(token, usuarioResponsable));
    }

    @GetMapping("/importaciones")
    public List<ImportacionInventario> importaciones() { return service.listarImportaciones(); }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> datosInvalidos(IllegalArgumentException e) {
        return ResponseEntity.badRequest().body(Map.of("mensaje", e.getMessage()));
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<Map<String, String>> conflicto(IllegalStateException e) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("mensaje", e.getMessage()));
    }
}
