package com.uniminuto.clinica.apicontroller;

import com.uniminuto.clinica.dto.*;
import com.uniminuto.clinica.serviceimpl.FacturacionService;
import com.uniminuto.clinica.serviceimpl.AuthService.UsuarioAutenticado;
import java.util.List;
import java.util.Map;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

@RestController @RequestMapping("/facturas")
public class FacturacionController {
    private final FacturacionService service;
    public FacturacionController(FacturacionService s){service=s;}
    @GetMapping public List<?> listar(@RequestParam(required=false) Long clienteId,@RequestParam(required=false) Long mascotaId){if(mascotaId!=null)return service.mascota(mascotaId);if(clienteId!=null)return service.cliente(clienteId);return service.listar();}
    @GetMapping("/{id}") public ResponseEntity<?> buscar(@PathVariable Long id){try{return ResponseEntity.ok(service.buscar(id));}catch(IllegalArgumentException e){return ResponseEntity.notFound().build();}}
    @GetMapping("/consulta/{consultaId}") public ResponseEntity<?> porConsulta(@PathVariable Long consultaId){return service.porConsulta(consultaId).<ResponseEntity<?>>map(ResponseEntity::ok).orElseGet(()->ResponseEntity.notFound().build());}
    @PostMapping("/consulta/{consultaId}") public ResponseEntity<?> generar(@PathVariable Long consultaId,@RequestBody(required=false) GenerarFacturaRequest request){try{return ResponseEntity.status(HttpStatus.CREATED).body(service.generar(consultaId,request));}catch(IllegalArgumentException e){return ResponseEntity.badRequest().body(Map.of("mensaje",e.getMessage()));}catch(IllegalStateException e){return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("mensaje",e.getMessage()));}}
    @PostMapping("/{id}/pagos") public ResponseEntity<?> pagar(@PathVariable Long id,@RequestBody PagoFacturaRequest r,@RequestAttribute("usuarioAutenticado") UsuarioAutenticado actor){try{PagoFacturaRequest auditado=r==null?null:new PagoFacturaRequest(r.valor(),r.medio(),r.referencia(),actor.username(),r.montoRecibido());return ResponseEntity.status(HttpStatus.CREATED).body(service.pagar(id,auditado));}catch(IllegalArgumentException e){return ResponseEntity.badRequest().body(Map.of("mensaje",e.getMessage()));}catch(IllegalStateException e){return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("mensaje",e.getMessage()));}}
    @GetMapping(value="/{id}/pagos/{pagoId}/ticket.pdf",produces=MediaType.APPLICATION_PDF_VALUE) public ResponseEntity<byte[]> ticket(@PathVariable Long id,@PathVariable Long pagoId){return ResponseEntity.ok().header(HttpHeaders.CONTENT_DISPOSITION,"attachment; filename=ticket-pago-"+pagoId+".pdf").contentType(MediaType.APPLICATION_PDF).body(service.ticketPdf(id,pagoId));}
    @PatchMapping("/{id}/anular") public ResponseEntity<?> anular(@PathVariable Long id,@RequestBody AnularFacturaRequest r){try{return ResponseEntity.ok(service.anular(id,r));}catch(IllegalArgumentException e){return ResponseEntity.badRequest().body(Map.of("mensaje",e.getMessage()));}catch(IllegalStateException e){return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("mensaje",e.getMessage()));}}
    @GetMapping(value="/{id}/pdf",produces=MediaType.APPLICATION_PDF_VALUE) public ResponseEntity<byte[]> pdf(@PathVariable Long id){return ResponseEntity.ok().header(HttpHeaders.CONTENT_DISPOSITION,"attachment; filename=factura-"+id+".pdf").contentType(MediaType.APPLICATION_PDF).body(service.pdf(id));}
}
