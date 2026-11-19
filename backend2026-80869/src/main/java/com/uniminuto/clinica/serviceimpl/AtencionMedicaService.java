package com.uniminuto.clinica.serviceimpl;

import com.uniminuto.clinica.dto.FinalizarAtencionRequest;
import com.uniminuto.clinica.entity.*;
import com.uniminuto.clinica.repository.*;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AtencionMedicaService {
    private final ConsultaRepository consultas; private final HistoriaClinicaRepository historias; private final AnotacionClinicaRepository anotaciones;
    private final FormulaMedicaRepository formulas; private final TarifaServicioRepository tarifas;
    public AtencionMedicaService(ConsultaRepository c,HistoriaClinicaRepository h,AnotacionClinicaRepository a,FormulaMedicaRepository f,TarifaServicioRepository t){consultas=c;historias=h;anotaciones=a;formulas=f;tarifas=t;}

    @Transactional
    public AnotacionClinica finalizar(Long consultaId,FinalizarAtencionRequest r){
        if(r==null||blank(r.diagnostico()))throw new IllegalArgumentException("El diagnóstico es obligatorio para finalizar la atención.");
        Consulta c=consultas.findById(consultaId).orElseThrow(()->new IllegalArgumentException("La cita no existe."));
        if(c.getEstado()==EstadoConsulta.CANCELADA)throw new IllegalStateException("No se puede atender una cita cancelada.");
        if(c.getEstado()==EstadoConsulta.ATENDIDA||anotaciones.existsByConsultaId(c.getId()))throw new IllegalStateException("La cita ya tiene una atención registrada.");
        if(c.getFechaHora().isAfter(LocalDateTime.now()))throw new IllegalStateException("La cita todavía no ha llegado a su fecha y hora.");
        HistoriaClinica h=historias.findByMascotaId(c.getMascota().getId()).orElseGet(()->{HistoriaClinica nueva=new HistoriaClinica();nueva.setMascota(c.getMascota());nueva.setFechaCreacion(LocalDateTime.now());return historias.save(nueva);});
        AnotacionClinica a=new AnotacionClinica();a.setHistoriaClinica(h);a.setConsulta(c);a.setVeterinario(c.getVeterinario());a.setFechaHora(LocalDateTime.now());
        a.setDescripcion(blank(r.motivo())?c.getMotivo():r.motivo().trim());a.setSintomas(limpiar(r.sintomas()));a.setDiagnostico(r.diagnostico().trim());a.setObservacion(limpiar(r.observaciones()));a.setRecomendaciones(limpiar(r.recomendaciones()));
        c.setDiagnostico(r.diagnostico().trim());c.setEstado(EstadoConsulta.ATENDIDA);
        AnotacionClinica guardada=anotaciones.save(a);consultas.save(c);return guardada;
    }

    @Transactional(readOnly=true)
    public byte[] prefactura(Long id){
        Consulta c=consultas.findById(id).orElseThrow(()->new IllegalArgumentException("La cita no existe."));
        if(c.getEstado()!=EstadoConsulta.ATENDIDA)throw new IllegalStateException("Finaliza la atención antes de emitir la prefactura.");
        List<String> lines=new ArrayList<>();lines.add("Prefactura de atencion #"+c.getId());lines.add("Fecha: "+LocalDateTime.now());lines.add("Propietario: "+c.getMascota().getCliente().getNombre()+" "+c.getMascota().getCliente().getApellido());lines.add("Paciente: "+c.getMascota().getNombre()+" ("+c.getMascota().getEspecie()+")");lines.add("Veterinario: "+c.getVeterinario().getNombre()+" "+c.getVeterinario().getApellido());lines.add("Especialidad: "+(c.getVeterinario().getEspecialidad()==null?"General":c.getVeterinario().getEspecialidad().getNombre()));lines.add("");lines.add("Servicio: "+c.getMotivo());
        TarifaServicio t=tarifaConsulta(c);BigDecimal total=t.getValor();BigDecimal imp=impuesto(total,t.getIvaPorcentaje());lines.add("Consulta: $"+total+" | IVA "+t.getIvaPorcentaje()+"%: $"+imp);
        lines.add("");lines.add("Medicamentos formulados:");for(FormulaMedica f:formulas.findByMascota_IdOrderByFechaEmisionDesc(c.getMascota().getId()))if(f.getConsulta().getId().equals(id)){lines.add("- "+f.getMedicamento()+" | dosis "+f.getDosis()+" | "+f.getFrecuencia()+" por "+f.getDuracionDias()+" dias | cantidad "+f.getCantidadRequerida());}
        lines.add("");lines.add("Total consulta estimado: $"+total.add(imp));lines.add("Prefactura informativa; los productos se cobran al dispensarse y la tarifa queda fijada en la factura final.");return PdfSimple.crear("Prefactura veterinaria",lines);
    }
    private TarifaServicio tarifaConsulta(Consulta c){Long e=c.getVeterinario().getEspecialidad()==null?null:c.getVeterinario().getEspecialidad().getId();return (e==null?java.util.Optional.<TarifaServicio>empty():tarifas.findFirstByTipoServicioAndEspecialidadIdAndActivaTrueOrderByIdDesc("CONSULTA",e)).or(()->tarifas.findFirstByTipoServicioAndEspecialidadIsNullAndActivaTrueOrderByIdDesc("CONSULTA")).orElseThrow(()->new IllegalStateException("No hay tarifa activa de consulta para esta especialidad."));}
    private BigDecimal impuesto(BigDecimal base,BigDecimal rate){return base.multiply(rate==null?BigDecimal.ZERO:rate).divide(new BigDecimal("100"),2,RoundingMode.HALF_UP);}
    private boolean blank(String s){return s==null||s.isBlank();}private String limpiar(String s){return blank(s)?null:s.trim();}
}
