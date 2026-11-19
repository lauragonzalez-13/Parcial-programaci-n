package com.uniminuto.clinica.apicontroller;

import com.uniminuto.clinica.dto.TarifaRequest;
import com.uniminuto.clinica.entity.*;
import com.uniminuto.clinica.repository.*;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.transaction.annotation.Transactional;

@Transactional
@RestController
@RequestMapping("/administracion")
public class AdministracionTarifasController {
    private final EspecialidadMedicaRepository especialidades;
    private final TarifaServicioRepository tarifas;
    private final LogTarifaRepository logs;
    private final DatosClinicaRepository datosClinica;
    public AdministracionTarifasController(EspecialidadMedicaRepository e,TarifaServicioRepository t,LogTarifaRepository l,DatosClinicaRepository d){especialidades=e;tarifas=t;logs=l;datosClinica=d;}

    @GetMapping("/clinica") public DatosClinica datosClinica(){return datosClinica.findById(1L).orElseGet(()->datosClinica.save(new DatosClinica()));}
    @PutMapping("/clinica") public ResponseEntity<?> actualizarDatosClinica(@RequestBody DatosClinica datos){if(datos.getNombre()==null||datos.getNombre().isBlank())return ResponseEntity.badRequest().body(Map.of("mensaje","El nombre de la clínica es obligatorio."));DatosClinica actual=datosClinica.findById(1L).orElseGet(DatosClinica::new);actual.setNombre(datos.getNombre().trim());actual.setNit(datos.getNit());actual.setTelefono(datos.getTelefono());actual.setDireccion(datos.getDireccion());actual.setCorreo(datos.getCorreo());return ResponseEntity.ok(datosClinica.save(actual));}

    @GetMapping("/especialidades") public List<EspecialidadMedica> especialidades(){return especialidades.findAll();}
    @PostMapping("/especialidades") public ResponseEntity<?> crearEspecialidad(@RequestBody Map<String,String> body){
        String nombre=body.getOrDefault("nombre","").trim();if(nombre.isBlank())return ResponseEntity.badRequest().body(Map.of("mensaje","El nombre de especialidad es obligatorio."));
        if(especialidades.findByNombreIgnoreCase(nombre).isPresent())return ResponseEntity.badRequest().body(Map.of("mensaje","Ya existe esa especialidad."));
        EspecialidadMedica e=new EspecialidadMedica();e.setNombre(nombre);e.setDescripcion(body.get("descripcion"));e.setActiva(true);return ResponseEntity.status(HttpStatus.CREATED).body(especialidades.save(e));
    }
    @PutMapping("/especialidades/{id}") public ResponseEntity<?> editarEspecialidad(@PathVariable Long id,@RequestBody Map<String,String> body){
        EspecialidadMedica e=especialidades.findById(id).orElseThrow(()->new IllegalArgumentException("La especialidad no existe."));
        String nombre=body.getOrDefault("nombre","").trim();if(nombre.isBlank())return ResponseEntity.badRequest().body(Map.of("mensaje","El nombre de especialidad es obligatorio."));
        e.setNombre(nombre);e.setDescripcion(body.get("descripcion"));return ResponseEntity.ok(especialidades.save(e));
    }
    @PatchMapping("/especialidades/{id}/estado") public EspecialidadMedica estadoEspecialidad(@PathVariable Long id,@RequestBody Map<String,Boolean> body){EspecialidadMedica e=especialidades.findById(id).orElseThrow(()->new IllegalArgumentException("La especialidad no existe."));e.setActiva(Boolean.TRUE.equals(body.get("activa")));return especialidades.save(e);}

    @GetMapping("/tarifas") public List<TarifaServicio> listarTarifas(){return tarifas.findAll();}
    @GetMapping("/tarifas/{id}/historial") public List<LogTarifa> historial(@PathVariable Long id){return logs.findByTarifaIdOrderByFechaDesc(id);}
    @PostMapping("/tarifas") public ResponseEntity<?> crearTarifa(@RequestBody TarifaRequest r){return guardarTarifa(null,r);}
    @PutMapping("/tarifas/{id}") public ResponseEntity<?> editarTarifa(@PathVariable Long id,@RequestBody TarifaRequest r){return guardarTarifa(id,r);}
    @PatchMapping("/tarifas/{id}/estado") public ResponseEntity<?> estadoTarifa(@PathVariable Long id,@RequestBody Map<String,Object> body){
        TarifaServicio t=tarifas.findById(id).orElseThrow(()->new IllegalArgumentException("La tarifa no existe."));boolean activa=Boolean.TRUE.equals(body.get("activa"));t.setActiva(activa);t=tarifas.save(t);registrar(t,activa?"ACTIVAR":"INACTIVAR",t.getValor(),t.getIvaPorcentaje(),t.getValor(),t.getIvaPorcentaje(),String.valueOf(body.getOrDefault("usuario","administrador")));return ResponseEntity.ok(t);
    }
    private ResponseEntity<?> guardarTarifa(Long id,TarifaRequest r){
        if(r==null||r.codigo()==null||r.codigo().isBlank()||r.nombre()==null||r.nombre().isBlank()||r.tipoServicio()==null||r.tipoServicio().isBlank()||r.valor()==null||r.valor().signum()<0)return ResponseEntity.badRequest().body(Map.of("mensaje","Completa código, nombre, tipo de servicio y un valor válido."));
        BigDecimal iva=r.ivaPorcentaje()==null?BigDecimal.ZERO:r.ivaPorcentaje();if(iva.signum()<0||iva.compareTo(new BigDecimal("100"))>0)return ResponseEntity.badRequest().body(Map.of("mensaje","El IVA debe estar entre 0 y 100 por ciento."));
        TarifaServicio t;BigDecimal anterior=null,ivaAnterior=null;String accion;
        if(id==null){if(tarifas.existsByCodigoIgnoreCase(r.codigo().trim()))return ResponseEntity.badRequest().body(Map.of("mensaje","El código de tarifa ya existe."));t=new TarifaServicio();accion="CREAR";}
        else{t=tarifas.findById(id).orElseThrow(()->new IllegalArgumentException("La tarifa no existe."));anterior=t.getValor();ivaAnterior=t.getIvaPorcentaje();accion="ACTUALIZAR";}
        EspecialidadMedica esp=null;if(r.especialidadId()!=null)esp=especialidades.findById(r.especialidadId()).orElseThrow(()->new IllegalArgumentException("La especialidad no existe."));
        t.setCodigo(r.codigo().trim().toUpperCase());t.setNombre(r.nombre().trim());t.setDescripcion(r.descripcion());t.setTipoServicio(r.tipoServicio().trim().toUpperCase());t.setEspecialidad(esp);t.setValor(r.valor());t.setIvaPorcentaje(iva);t.setActiva(r.activa());t=tarifas.save(t);
        registrar(t,accion,anterior,ivaAnterior,t.getValor(),t.getIvaPorcentaje(),r.usuario()==null||r.usuario().isBlank()?"administrador":r.usuario());
        return ResponseEntity.status(id==null?HttpStatus.CREATED:HttpStatus.OK).body(t);
    }
    private void registrar(TarifaServicio t,String accion,BigDecimal antes,BigDecimal ivaAntes,BigDecimal nuevo,BigDecimal ivaNuevo,String usuario){LogTarifa l=new LogTarifa();l.setTarifa(t);l.setAccion(accion);l.setValorAnterior(antes);l.setIvaAnterior(ivaAntes);l.setValorNuevo(nuevo);l.setIvaNuevo(ivaNuevo);l.setUsuario(usuario);logs.save(l);}
    @ExceptionHandler(IllegalArgumentException.class) public ResponseEntity<Map<String,String>> error(IllegalArgumentException e){return ResponseEntity.badRequest().body(Map.of("mensaje",e.getMessage()));}
}
