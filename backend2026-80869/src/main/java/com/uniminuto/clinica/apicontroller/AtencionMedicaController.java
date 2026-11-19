package com.uniminuto.clinica.apicontroller;

import com.uniminuto.clinica.dto.FinalizarAtencionRequest;
import com.uniminuto.clinica.serviceimpl.AtencionMedicaService;
import com.uniminuto.clinica.service.FormulaMedicaService;
import com.uniminuto.clinica.serviceimpl.PdfSimple;
import java.util.List;
import java.util.Map;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

@RestController @RequestMapping("/atencion-medica")
public class AtencionMedicaController {
    private final AtencionMedicaService atencion; private final FormulaMedicaService formulas;
    public AtencionMedicaController(AtencionMedicaService a,FormulaMedicaService f){atencion=a;formulas=f;}
    @PostMapping("/{consultaId}/finalizar") public ResponseEntity<?> finalizar(@PathVariable Long consultaId,@RequestBody FinalizarAtencionRequest request){return ResponseEntity.status(HttpStatus.CREATED).body(atencion.finalizar(consultaId,request));}
    @GetMapping("/{consultaId}/prefactura.pdf") public ResponseEntity<byte[]> prefactura(@PathVariable Long consultaId){return pdf("prefactura-"+consultaId+".pdf",atencion.prefactura(consultaId));}
    @GetMapping("/formulas/{formulaId}.pdf") public ResponseEntity<byte[]> formula(@PathVariable Long formulaId){
        var f=formulas.buscarPorId(formulaId);var lines=List.of("Formula medica #"+f.getId(),"Fecha: "+f.getFechaEmision(),"Paciente: "+f.getMascota().getNombre(),"Propietario: "+f.getCliente().getNombre()+" "+f.getCliente().getApellido(),"Veterinario: "+f.getVeterinario().getNombre()+" "+f.getVeterinario().getApellido(),"Medicamento: "+f.getMedicamento(),"Dosis: "+f.getDosis(),"Via: "+(f.getViaAdministracion()==null?"No especificada":f.getViaAdministracion()),"Frecuencia: "+f.getFrecuencia(),"Duracion: "+f.getDuracionDias()+" dias","Cantidad: "+f.getCantidadRequerida(),"Indicaciones: "+f.getIndicaciones());return pdf("formula-"+formulaId+".pdf",PdfSimple.crear("Formula veterinaria",lines));
    }
    private ResponseEntity<byte[]> pdf(String name,byte[] b){return ResponseEntity.ok().contentType(MediaType.APPLICATION_PDF).header(HttpHeaders.CONTENT_DISPOSITION,"attachment; filename="+name).body(b);}
    @ExceptionHandler(IllegalArgumentException.class) public ResponseEntity<Map<String,String>> invalido(IllegalArgumentException e){return ResponseEntity.badRequest().body(Map.of("mensaje",e.getMessage()));}
    @ExceptionHandler(IllegalStateException.class) public ResponseEntity<Map<String,String>> conflicto(IllegalStateException e){return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("mensaje",e.getMessage()));}
}
