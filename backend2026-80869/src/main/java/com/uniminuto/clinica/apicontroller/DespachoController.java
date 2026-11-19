package com.uniminuto.clinica.apicontroller;

import com.uniminuto.clinica.dto.*;
import com.uniminuto.clinica.serviceimpl.DespachoService;
import java.util.List;
import java.util.Map;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

@RestController @RequestMapping("/despachos")
public class DespachoController {
    private final DespachoService service;
    public DespachoController(DespachoService service){this.service=service;}

    @GetMapping public List<Map<String,Object>> pendientes(){return service.pendientes();}
    @GetMapping("/historial") public List<Map<String,Object>> historial(){return service.historial();}
    @GetMapping("/formula/{formulaId}/lotes") public List<Map<String,Object>> lotes(@PathVariable Long formulaId){return service.lotes(formulaId);}
    @GetMapping("/formula/{formulaId}/eventos") public List<Map<String,Object>> eventos(@PathVariable Long formulaId){return service.eventos(formulaId);}
    @PostMapping("/{formulaId}/preparar") public Map<String,Object> preparar(@PathVariable Long formulaId,@RequestBody EstadoDespachoRequest request){return service.preparar(formulaId,request);}
    @PostMapping("/{formulaId}/confirmar") public Map<String,Object> confirmar(@PathVariable Long formulaId,@RequestBody ConfirmarDespachoRequest request){return service.confirmar(formulaId,request);}
    @PostMapping("/{formulaId}/pendiente-disponibilidad") public Map<String,Object> pendienteDisponibilidad(@PathVariable Long formulaId,@RequestBody EstadoDespachoRequest request){return service.pendienteDisponibilidad(formulaId,request);}
    @PostMapping("/{formulaId}/cancelar") public Map<String,Object> cancelar(@PathVariable Long formulaId,@RequestBody EstadoDespachoRequest request){return service.cancelar(formulaId,request);}
    @GetMapping(value="/{despachoId}/comprobante.pdf",produces=MediaType.APPLICATION_PDF_VALUE)
    public ResponseEntity<byte[]> comprobante(@PathVariable Long despachoId){return ResponseEntity.ok().header(HttpHeaders.CONTENT_DISPOSITION,"attachment; filename=comprobante-despacho-"+despachoId+".pdf").contentType(MediaType.APPLICATION_PDF).body(service.comprobante(despachoId));}
    @ExceptionHandler(IllegalArgumentException.class) public ResponseEntity<Map<String,String>> invalid(IllegalArgumentException e){return ResponseEntity.badRequest().body(Map.of("mensaje",e.getMessage()));}
    @ExceptionHandler(IllegalStateException.class) public ResponseEntity<Map<String,String>> conflict(IllegalStateException e){return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("mensaje",e.getMessage()));}
}
