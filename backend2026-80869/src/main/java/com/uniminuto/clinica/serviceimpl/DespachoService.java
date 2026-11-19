package com.uniminuto.clinica.serviceimpl;

import com.uniminuto.clinica.dto.*;
import com.uniminuto.clinica.entity.*;
import com.uniminuto.clinica.repository.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DespachoService {
    private final FormulaMedicaRepository formulas;
    private final DespachoRepository despachos;
    private final DetalleDespachoRepository detalles;
    private final EventoDespachoRepository eventos;
    private final LoteInventarioRepository lotes;
    private final MovimientoInventarioRepository movimientos;

    public DespachoService(FormulaMedicaRepository formulas,DespachoRepository despachos,DetalleDespachoRepository detalles,
            EventoDespachoRepository eventos,LoteInventarioRepository lotes,MovimientoInventarioRepository movimientos){
        this.formulas=formulas;this.despachos=despachos;this.detalles=detalles;this.eventos=eventos;this.lotes=lotes;this.movimientos=movimientos;
    }

    @Transactional(readOnly=true)
    public List<Map<String,Object>> pendientes(){
        return formulas.findByActivaTrue().stream().filter(f->f.getMedicamentoInventario()!=null&&f.getCantidadRequerida()!=null)
                .filter(f->f.getEstadoDispensacion()!=EstadoDispensacion.DISPENSADA)
                .filter(f->despachos.findByFormulaId(f.getId()).map(d->d.getEstado()!=EstadoDespacho.CANCELADO).orElse(true))
                .sorted(Comparator.comparing(FormulaMedica::getFechaEmision).reversed()).map(this::vista).toList();
    }

    @Transactional(readOnly=true)
    public List<Map<String,Object>> historial(){
        return despachos.findAllByOrderByCreadoEnDesc().stream().map(d->{Map<String,Object> row=vista(d.getFormula());row.put("detalleEntregas",detalles.findByDespachoIdOrderByFechaAsc(d.getId()).stream().map(x->map("fecha",x.getFecha(),"lote",x.getLote().getCodigoLote(),"cantidad",x.getCantidad(),"movimientoId",x.getMovimiento().getId())).toList());row.put("eventos",eventos.findByDespachoIdOrderByFechaDesc(d.getId()).stream().map(e->map("estado",e.getEstado(),"usuarioResponsable",e.getUsuarioResponsable(),"fecha",e.getFecha(),"observacion",e.getObservacion())).toList());return row;}).toList();
    }

    @Transactional
    public Map<String,Object> preparar(Long formulaId,EstadoDespachoRequest request){
        FormulaMedica formula=buscarBloqueada(formulaId);validarPendiente(formula);
        String usuario=usuario(request==null?null:request.usuarioResponsable());
        Despacho d=despachos.findByFormulaId(formulaId).orElseGet(()->crear(formula,usuario));
        if(d.getEstado()==EstadoDespacho.CANCELADO||d.getEstado()==EstadoDespacho.DESPACHADO)
            throw new IllegalStateException("El despacho ya está cerrado.");
        actualizarEstado(d,EstadoDespacho.EN_PREPARACION,usuario,request==null?null:request.observacion());
        return vista(formula);
    }

    @Transactional
    public Map<String,Object> confirmar(Long formulaId,ConfirmarDespachoRequest request){
        if(request==null||request.detalles()==null||request.detalles().isEmpty())throw new IllegalArgumentException("Selecciona los lotes y las cantidades que se entregarán.");
        FormulaMedica formula=buscarBloqueada(formulaId);validarPendiente(formula);
        if(!formula.isActiva())throw new IllegalStateException("La fórmula está inactiva y no se puede despachar.");
        BigDecimal restante=restante(formula);String usuario=usuario(request.usuarioResponsable());
        Despacho d=despachos.findByFormulaId(formulaId).orElseGet(()->crear(formula,usuario));
        if(d.getEstado()==EstadoDespacho.CANCELADO||d.getEstado()==EstadoDespacho.DESPACHADO)throw new IllegalStateException("El despacho ya está cerrado.");

        List<LoteInventario> bloqueados=lotes.bloquearLotesDisponibles(formula.getMedicamentoInventario().getId());
        Map<Long,LoteInventario> porId=bloqueados.stream().collect(Collectors.toMap(LoteInventario::getId,Function.identity()));
        Set<Long> unicos=new HashSet<>();BigDecimal total=BigDecimal.ZERO;LocalDate hoy=LocalDate.now();
        for(DetalleEntregaDespachoRequest linea:request.detalles()){
            if(linea==null||linea.loteId()==null||linea.cantidad()==null||linea.cantidad().signum()<=0)throw new IllegalArgumentException("Cada lote debe tener una cantidad mayor que cero.");
            if(!unicos.add(linea.loteId()))throw new IllegalArgumentException("No repitas un lote en el mismo despacho.");
            LoteInventario lote=porId.get(linea.loteId());
            if(lote==null||!lote.getMedicamento().getId().equals(formula.getMedicamentoInventario().getId()))throw new IllegalArgumentException("El lote no corresponde al medicamento formulado o está agotado.");
            if(lote.getFechaVencimiento()!=null&&lote.getFechaVencimiento().isBefore(hoy))throw new IllegalStateException("No se pueden entregar medicamentos vencidos.");
            if(lote.getCantidadDisponible().compareTo(linea.cantidad())<0)throw new IllegalStateException("La cantidad seleccionada supera la existencia actual del lote "+lote.getCodigoLote()+".");
            total=total.add(linea.cantidad());
        }
        if(total.compareTo(restante)>0)throw new IllegalArgumentException("La entrega supera la cantidad pendiente de la fórmula.");
        Map<Long,BigDecimal> solicitadas=request.detalles().stream().collect(Collectors.toMap(DetalleEntregaDespachoRequest::loteId,DetalleEntregaDespachoRequest::cantidad));
        BigDecimal porAsignar=total;
        for(LoteInventario lote:bloqueados.stream().filter(l->l.getFechaVencimiento()==null||!l.getFechaVencimiento().isBefore(hoy))
                .sorted(Comparator.comparing((LoteInventario l)->l.getFechaVencimiento()==null).thenComparing(LoteInventario::getFechaVencimiento,Comparator.nullsLast(Comparator.naturalOrder()))).toList()) {
            if(porAsignar.signum()==0)break;
            BigDecimal esperado=lote.getCantidadDisponible().min(porAsignar);
            BigDecimal declarado=solicitadas.getOrDefault(lote.getId(),BigDecimal.ZERO);
            if(declarado.compareTo(esperado)!=0)throw new IllegalArgumentException("Prioriza los lotes por fecha de vencimiento; selecciona primero el lote vigente que vence antes.");
            porAsignar=porAsignar.subtract(esperado);
        }
        if(porAsignar.signum()>0)throw new IllegalStateException("No hay suficiente existencia vigente para la cantidad solicitada.");

        for(DetalleEntregaDespachoRequest linea:request.detalles()){
            LoteInventario lote=porId.get(linea.loteId());lote.setCantidadDisponible(lote.getCantidadDisponible().subtract(linea.cantidad()));lotes.save(lote);
            MovimientoInventario movimiento=new MovimientoInventario();movimiento.setTipo(TipoMovimiento.SALIDA);movimiento.setMedicamento(formula.getMedicamentoInventario());
            movimiento.setLote(lote);movimiento.setFormulaMedica(formula);movimiento.setDespacho(d);movimiento.setCantidad(linea.cantidad());
            movimiento.setFecha(LocalDateTime.now());movimiento.setUsuarioResponsable(usuario);movimiento.setObservacion("Despacho #"+d.getId()+" de fórmula médica #"+formulaId);
            movimiento=movimientos.save(movimiento);
            DetalleDespacho detalle=new DetalleDespacho();detalle.setDespacho(d);detalle.setLote(lote);detalle.setMovimiento(movimiento);detalle.setCantidad(linea.cantidad());detalle.setFecha(movimiento.getFecha());detalles.save(detalle);
        }
        BigDecimal entregado=valor(formula.getCantidadDispensada()).add(total);formula.setCantidadDispensada(entregado);
        boolean completo=entregado.compareTo(formula.getCantidadRequerida())>=0;
        formula.setEstadoDispensacion(completo?EstadoDispensacion.DISPENSADA:EstadoDispensacion.PARCIAL);formulas.save(formula);
        d.setUsuarioResponsable(usuario);d.setObservacion(limpiar(request.observacion()));d.setEntregadoEn(LocalDateTime.now());
        actualizarEstado(d,completo?EstadoDespacho.DESPACHADO:EstadoDespacho.DESPACHO_PARCIAL,usuario,request.observacion());
        return vista(formula);
    }

    @Transactional
    public Map<String,Object> pendienteDisponibilidad(Long formulaId,EstadoDespachoRequest request){
        FormulaMedica formula=buscarBloqueada(formulaId);validarPendiente(formula);String usuario=usuario(request==null?null:request.usuarioResponsable());
        Despacho d=despachos.findByFormulaId(formulaId).orElseGet(()->crear(formula,usuario));
        if(d.getEstado()==EstadoDespacho.CANCELADO||d.getEstado()==EstadoDespacho.DESPACHADO)throw new IllegalStateException("El despacho ya está cerrado.");
        actualizarEstado(d,EstadoDespacho.PENDIENTE_POR_DISPONIBILIDAD,usuario,request==null?"Pendiente por falta de existencias":request.observacion());
        return vista(formula);
    }

    @Transactional
    public Map<String,Object> cancelar(Long formulaId,EstadoDespachoRequest request){
        FormulaMedica formula=buscarBloqueada(formulaId);String usuario=usuario(request==null?null:request.usuarioResponsable());
        Despacho d=despachos.findByFormulaId(formulaId).orElseThrow(()->new IllegalStateException("La fórmula todavía no tiene un despacho que cancelar."));
        if(!detalles.findByDespachoIdOrderByFechaAsc(d.getId()).isEmpty())throw new IllegalStateException("Un despacho con medicamentos entregados no se puede cancelar ni borrar; requiere una reversión autorizada.");
        if(d.getEstado()==EstadoDespacho.CANCELADO)throw new IllegalStateException("El despacho ya está cancelado.");
        actualizarEstado(d,EstadoDespacho.CANCELADO,usuario,request==null?null:request.observacion());return vista(formula);
    }

    @Transactional(readOnly=true)
    public List<Map<String,Object>> lotes(Long formulaId){
        FormulaMedica f=formulas.findById(formulaId).orElseThrow(()->new IllegalArgumentException("La fórmula no existe."));
        if(f.getMedicamentoInventario()==null)throw new IllegalStateException("La fórmula no está asociada a un producto del inventario.");
        return lotes.findByMedicamentoIdOrderByFechaVencimientoAsc(f.getMedicamentoInventario().getId()).stream().sorted(Comparator
                .comparing((LoteInventario l)->l.getFechaVencimiento()==null).thenComparing(LoteInventario::getFechaVencimiento,Comparator.nullsLast(Comparator.naturalOrder())))
                .map(this::loteVista).toList();
    }

    @Transactional(readOnly=true)
    public List<Map<String,Object>> eventos(Long formulaId){
        Despacho d=despachos.findByFormulaId(formulaId).orElseThrow(()->new IllegalArgumentException("La fórmula todavía no tiene historial de despacho."));
        return eventos.findByDespachoIdOrderByFechaDesc(d.getId()).stream().map(e->map("estado",e.getEstado(),"usuarioResponsable",e.getUsuarioResponsable(),"fecha",e.getFecha(),"observacion",e.getObservacion())).toList();
    }

    @Transactional(readOnly=true)
    public byte[] comprobante(Long despachoId){
        Despacho d=despachos.findById(despachoId).orElseThrow(()->new IllegalArgumentException("El despacho no existe."));FormulaMedica f=d.getFormula();
        List<String> lineas=new ArrayList<>(List.of("Despacho #"+d.getId(),"Fórmula médica #"+f.getId(),"Fecha de entrega: "+d.getEntregadoEn(),"Responsable: "+d.getUsuarioResponsable(),"Propietario: "+f.getCliente().getNombre()+" "+f.getCliente().getApellido(),"Mascota: "+f.getMascota().getNombre(),"Veterinario: "+f.getVeterinario().getNombre()+" "+f.getVeterinario().getApellido(),"Medicamento: "+f.getMedicamento(),"Dosis: "+f.getDosis()+" · "+f.getFrecuencia(),"Estado: "+d.getEstado(),"Entregas:"));
        for(DetalleDespacho x:detalles.findByDespachoIdOrderByFechaAsc(despachoId))lineas.add(x.getFecha()+" | Lote "+x.getLote().getCodigoLote()+" | Cantidad "+x.getCantidad());
        return PdfSimple.crear("Comprobante de despacho",lineas);
    }

    private FormulaMedica buscarBloqueada(Long id){return formulas.bloquearPorId(id).orElseThrow(()->new IllegalArgumentException("La fórmula médica no existe."));}
    private void validarPendiente(FormulaMedica f){if(f.getMedicamentoInventario()==null||f.getCantidadRequerida()==null||f.getCantidadRequerida().signum()<=0)throw new IllegalStateException("La fórmula no está asociada a un medicamento dispensable de inventario.");if(restante(f).signum()<=0)throw new IllegalStateException("La fórmula ya fue dispensada por completo.");}
    private Despacho crear(FormulaMedica formula,String user){Despacho d=new Despacho();d.setFormula(formula);d.setUsuarioResponsable(user);d=despachos.save(d);evento(d,EstadoDespacho.PENDIENTE_DESPACHO,user,"Fórmula recibida en la cola de despacho.");return d;}
    private void actualizarEstado(Despacho d,EstadoDespacho estado,String user,String obs){if(d.getEstado()==estado&&Objects.equals(d.getUsuarioResponsable(),user)&&Objects.equals(d.getObservacion(),limpiar(obs)))return;d.setEstado(estado);d.setUsuarioResponsable(user);d.setObservacion(limpiar(obs));d.setActualizadoEn(LocalDateTime.now());despachos.save(d);evento(d,estado,user,obs);}
    private void evento(Despacho d,EstadoDespacho estado,String user,String obs){EventoDespacho e=new EventoDespacho();e.setDespacho(d);e.setEstado(estado);e.setUsuarioResponsable(user);e.setFecha(LocalDateTime.now());e.setObservacion(limpiar(obs));eventos.save(e);}
    private String usuario(String user){if(user==null||user.isBlank())throw new IllegalArgumentException("Indica el usuario responsable del despacho.");return user.trim();}
    private String limpiar(String text){return text==null||text.isBlank()?null:text.trim();}
    private BigDecimal restante(FormulaMedica f){return f.getCantidadRequerida().subtract(valor(f.getCantidadDispensada())).max(BigDecimal.ZERO);}
    private static BigDecimal valor(BigDecimal n){return n==null?BigDecimal.ZERO:n;}
    private Map<String,Object> vista(FormulaMedica f){Despacho d=despachos.findByFormulaId(f.getId()).orElse(null);BigDecimal pendiente=restante(f);Map<String,Object> result=map(
            "formulaId",f.getId(),"fechaGeneracion",f.getFechaEmision(),"veterinario",f.getVeterinario().getNombre()+" "+f.getVeterinario().getApellido(),
            "propietario",f.getCliente().getNombre()+" "+f.getCliente().getApellido(),"mascota",f.getMascota().getNombre(),"medicamento",f.getMedicamento(),
            "medicamentoId",f.getMedicamentoInventario()==null?null:f.getMedicamentoInventario().getId(),"cantidadPrescrita",f.getCantidadRequerida(),"cantidadEntregada",valor(f.getCantidadDispensada()),
            "cantidadPendiente",pendiente,"dosis",f.getDosis(),"via",f.getViaAdministracion(),"frecuencia",f.getFrecuencia(),"duracionDias",f.getDuracionDias(),
            "indicaciones",f.getIndicaciones(),"estado",d==null?(pendiente.signum()>0?EstadoDespacho.PENDIENTE_DESPACHO:EstadoDespacho.DESPACHADO):d.getEstado(),
            "despachoId",d==null?null:d.getId(),"usuarioResponsable",d==null?null:d.getUsuarioResponsable(),"entregadoEn",d==null?null:d.getEntregadoEn());
        if(f.getMedicamentoInventario()!=null){List<LoteInventario> ls=lotes.findByMedicamentoIdOrderByFechaVencimientoAsc(f.getMedicamentoInventario().getId());BigDecimal exist=ls.stream().filter(l->l.getFechaVencimiento()==null||!l.getFechaVencimiento().isBefore(LocalDate.now())).map(LoteInventario::getCantidadDisponible).map(DespachoService::valor).reduce(BigDecimal.ZERO,BigDecimal::add);result.put("existenciaActual",exist);result.put("lotes",ls.stream().sorted(Comparator.comparing((LoteInventario l)->l.getFechaVencimiento()==null).thenComparing(LoteInventario::getFechaVencimiento,Comparator.nullsLast(Comparator.naturalOrder()))).map(this::loteVista).toList());}
        return result;
    }
    private Map<String,Object> loteVista(LoteInventario l){LocalDate hoy=LocalDate.now();boolean vencido=l.getFechaVencimiento()!=null&&l.getFechaVencimiento().isBefore(hoy);boolean proximo=!vencido&&l.getFechaVencimiento()!=null&&!l.getFechaVencimiento().isAfter(hoy.plusDays(30));return map("id",l.getId(),"codigo",l.getCodigoLote(),"cantidadDisponible",l.getCantidadDisponible(),"fechaVencimiento",l.getFechaVencimiento(),"vencido",vencido,"proximoVencer",proximo,"vigente",!vencido&&l.getCantidadDisponible().signum()>0);}
    private static Map<String,Object> map(Object... values){Map<String,Object> m=new LinkedHashMap<>();for(int i=0;i<values.length;i+=2)m.put(String.valueOf(values[i]),values[i+1]);return m;}
}
