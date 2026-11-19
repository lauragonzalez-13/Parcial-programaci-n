package com.uniminuto.clinica.serviceimpl;

import com.uniminuto.clinica.dto.*;
import com.uniminuto.clinica.entity.*;
import com.uniminuto.clinica.repository.*;
import java.math.*;
import java.time.LocalDateTime;
import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service @Transactional
public class FacturacionService {
    private static final BigDecimal CERO=BigDecimal.ZERO.setScale(2);
    private final FacturaRepository facturas;private final ConsultaRepository consultas;private final TarifaServicioRepository tarifas;private final MovimientoInventarioRepository movimientos;private final FormulaMedicaRepository formulas;private final DatosClinicaRepository datosClinica;
    public FacturacionService(FacturaRepository f,ConsultaRepository c,TarifaServicioRepository t,MovimientoInventarioRepository m,FormulaMedicaRepository formulas,DatosClinicaRepository datosClinica){facturas=f;consultas=c;tarifas=t;movimientos=m;this.formulas=formulas;this.datosClinica=datosClinica;}

    public Factura generar(Long consultaId,GenerarFacturaRequest request){
        Consulta c=consultas.findById(consultaId).orElseThrow(()->new IllegalArgumentException("La cita no existe."));
        if(c.getEstado()!=EstadoConsulta.ATENDIDA)throw new IllegalStateException("Solo una cita atendida puede generar factura.");
        if(facturas.findByConsultaId(consultaId).isPresent())throw new IllegalStateException("Esta atención ya tiene factura; no se puede cobrar dos veces.");
        boolean pendientes=formulas.findByConsultaIdOrderByFechaEmisionDesc(consultaId).stream().anyMatch(x->x.getMedicamentoInventario()!=null&&x.getEstadoDispensacion()!=EstadoDispensacion.DISPENSADA);
        if(pendientes)throw new IllegalStateException("Dispensa los medicamentos formulados desde inventario antes de generar la factura final.");
        BigDecimal descuento=request==null||request.descuento()==null?CERO:request.descuento();String autorizador=request==null?null:request.autorizadoPor();
        if(descuento.signum()<0)throw new IllegalArgumentException("El descuento no puede ser negativo.");
        if(descuento.signum()>0&&(autorizador==null||autorizador.isBlank()))throw new IllegalArgumentException("El descuento requiere registrar quién lo autorizó.");
        TarifaServicio tarifa=tarifaConsulta(c);
        Factura f=new Factura();f.setConsulta(c);f.setCliente(c.getMascota().getCliente());f.setMascota(c.getMascota());f.setVeterinario(c.getVeterinario());f.setDescuentoAutorizadoPor(descuento.signum()>0?autorizador.trim():null);
        BigDecimal subtotal=CERO,impuestos=CERO;
        LineaFactura servicio=linea("SERVICIO",tarifa.getNombre()+" - "+c.getMotivo(),BigDecimal.ONE,tarifa.getValor(),tarifa.getIvaPorcentaje(),null);f.agregarLinea(servicio);subtotal=subtotal.add(servicio.getSubtotal());impuestos=impuestos.add(servicio.getImpuesto());
        if(request!=null&&request.tarifasAdicionales()!=null){Set<Long> unicos=new HashSet<>();for(Long tarifaId:request.tarifasAdicionales()){
            if(tarifaId==null||!unicos.add(tarifaId))continue;
            TarifaServicio extra=tarifas.findById(tarifaId).orElseThrow(()->new IllegalArgumentException("Una tarifa adicional seleccionada no existe."));
            if(!extra.isActiva()||"CONSULTA".equalsIgnoreCase(extra.getTipoServicio()))throw new IllegalArgumentException("Las tarifas adicionales deben estar activas y ser de procedimiento o servicio.");
            Long especialidadId=c.getVeterinario().getEspecialidad()==null?null:c.getVeterinario().getEspecialidad().getId();
            if(extra.getEspecialidad()!=null&&!extra.getEspecialidad().getId().equals(especialidadId))throw new IllegalArgumentException("La tarifa adicional no corresponde a la especialidad del veterinario.");
            LineaFactura extraLinea=linea(extra.getTipoServicio(),extra.getNombre(),BigDecimal.ONE,extra.getValor(),extra.getIvaPorcentaje(),null);f.agregarLinea(extraLinea);subtotal=subtotal.add(extraLinea.getSubtotal());impuestos=impuestos.add(extraLinea.getImpuesto());
        }}
        for(MovimientoInventario m:movimientos.findByFormulaMedicaConsultaIdAndTipoOrderByFechaAsc(consultaId,TipoMovimiento.SALIDA)){
            BigDecimal iva=m.getMedicamento().getIvaPorcentaje()==null?BigDecimal.ZERO:m.getMedicamento().getIvaPorcentaje();
            LineaFactura product=linea("PRODUCTO",m.getMedicamento().getNombre(),m.getCantidad(),m.getLote().getPrecioVenta(),iva,m.getLote().getCodigoLote());f.agregarLinea(product);subtotal=subtotal.add(product.getSubtotal());impuestos=impuestos.add(product.getImpuesto());
        }
        if(descuento.compareTo(subtotal)>0)throw new IllegalArgumentException("El descuento no puede superar el subtotal facturado.");
        f.setSubtotal(subtotal);f.setDescuento(descuento);f.setImpuestos(impuestos);f.setTotal(subtotal.subtract(descuento).add(impuestos));f.setPagado(CERO);f.setEstado(EstadoFactura.PENDIENTE_PAGO);
        f=facturas.save(f);f.setNumero(String.format(Locale.ROOT,"FAC-%d-%06d",LocalDateTime.now().getYear(),f.getId()));evento(f,"GENERAR","Factura generada para la cita #"+consultaId,"sistema-facturacion");return facturas.save(f);
    }

    public PagoFactura pagar(Long id,PagoFacturaRequest r){
        Factura f=buscar(id);if(f.getEstado()==EstadoFactura.ANULADA)throw new IllegalStateException("No se puede pagar una factura anulada.");
        if(r==null||r.valor()==null||r.valor().signum()<=0||r.medio()==null||r.medio().isBlank())throw new IllegalArgumentException("Indica valor positivo y medio de pago.");
        BigDecimal saldo=f.getTotal().subtract(f.getPagado());if(r.valor().compareTo(saldo)>0)throw new IllegalArgumentException("El pago supera el saldo pendiente de la factura.");
        PagoFactura p=new PagoFactura();p.setValor(r.valor().setScale(2,RoundingMode.HALF_UP));p.setMedio(r.medio().trim());p.setReferencia(limpiar(r.referencia()));p.setRegistradoPor(r.registradoPor()==null||r.registradoPor().isBlank()?"usuario-no-autenticado":r.registradoPor().trim());f.agregarPago(p);
        f.setPagado(f.getPagado().add(p.getValor()));f.setEstado(f.getPagado().compareTo(f.getTotal())>=0?EstadoFactura.PAGADA:EstadoFactura.PAGO_PARCIAL);evento(f,"PAGO","Pago por $"+p.getValor()+" mediante "+p.getMedio(),p.getRegistradoPor());facturas.save(f);return p;
    }

    public Factura anular(Long id,AnularFacturaRequest r){Factura f=buscar(id);if(f.getEstado()==EstadoFactura.ANULADA)throw new IllegalStateException("La factura ya está anulada.");if(f.getPagado().signum()>0)throw new IllegalStateException("No se puede anular una factura con pagos registrados; requiere proceso de reversión.");if(r==null||r.motivo()==null||r.motivo().isBlank()||r.usuario()==null||r.usuario().isBlank())throw new IllegalArgumentException("La anulación requiere motivo y responsable.");f.setEstado(EstadoFactura.ANULADA);f.setMotivoAnulacion(r.motivo().trim());f.setAnuladaPor(r.usuario().trim());f.setFechaAnulacion(LocalDateTime.now());evento(f,"ANULAR",f.getMotivoAnulacion(),f.getAnuladaPor());return facturas.save(f);}

    @Transactional(readOnly=true) public Factura buscar(Long id){return facturas.findById(id).orElseThrow(()->new IllegalArgumentException("La factura no existe."));}
    @Transactional(readOnly=true) public List<Factura> listar(){return facturas.findAllByOrderByFechaGeneracionDesc();}
    @Transactional(readOnly=true) public List<Factura> cliente(Long id){return facturas.findByClienteIdOrderByFechaGeneracionDesc(id);}
    @Transactional(readOnly=true) public List<Factura> mascota(Long id){return facturas.findByMascotaIdOrderByFechaGeneracionDesc(id);}
    @Transactional(readOnly=true) public Optional<Factura> porConsulta(Long id){return facturas.findByConsultaId(id);}

    @Transactional(readOnly=true) public byte[] pdf(Long id){Factura f=buscar(id);DatosClinica d=datosClinica.findById(1L).orElseGet(DatosClinica::new);List<String> l=new ArrayList<>();l.add("Factura: "+f.getNumero());l.add("Fecha: "+f.getFechaGeneracion());l.add("Estado de pago: "+f.getEstado());l.add(d.getNombre()+" | Nit: "+(d.getNit()==null?"No registrado":d.getNit()));l.add("Direccion: "+(d.getDireccion()==null?"No registrada":d.getDireccion())+" | Telefono: "+(d.getTelefono()==null?"No registrado":d.getTelefono()));l.add("Propietario: "+f.getCliente().getNombre()+" "+f.getCliente().getApellido()+" | documento "+f.getCliente().getDocumento());l.add("Mascota: "+f.getMascota().getNombre()+" ("+f.getMascota().getEspecie()+")");l.add("Veterinario: "+f.getVeterinario().getNombre()+" "+f.getVeterinario().getApellido()+" | Especialidad: "+(f.getVeterinario().getEspecialidad()==null?"General":f.getVeterinario().getEspecialidad().getNombre()));l.add("");l.add("Detalle:");for(LineaFactura x:f.getLineas())l.add(x.getDescripcion()+" | Cantidad "+x.getCantidad()+" | Unitario $"+x.getValorUnitario()+" | IVA "+x.getIvaPorcentaje()+"% | Total $"+x.getTotal());l.add("");l.add("Subtotal: $"+f.getSubtotal());l.add("Descuento: $"+f.getDescuento()+(f.getDescuentoAutorizadoPor()==null?"":" (Autorizado por "+f.getDescuentoAutorizadoPor()+")"));l.add("Impuestos: $"+f.getImpuestos());l.add("Total: $"+f.getTotal());l.add("Pagado: $"+f.getPagado());l.add("Saldo: $"+f.getTotal().subtract(f.getPagado()));l.add("Pagos:");for(PagoFactura p:f.getPagos())l.add(p.getFecha()+" | "+p.getMedio()+" | $"+p.getValor()+" | "+p.getRegistradoPor());if(f.getEstado()==EstadoFactura.ANULADA)l.add("ANULADA: "+f.getMotivoAnulacion()+" | "+f.getAnuladaPor());return PdfSimple.crear("Factura veterinaria",l);}

    private TarifaServicio tarifaConsulta(Consulta c){Long e=c.getVeterinario().getEspecialidad()==null?null:c.getVeterinario().getEspecialidad().getId();return (e==null?Optional.<TarifaServicio>empty():tarifas.findFirstByTipoServicioAndEspecialidadIdAndActivaTrueOrderByIdDesc("CONSULTA",e)).or(()->tarifas.findFirstByTipoServicioAndEspecialidadIsNullAndActivaTrueOrderByIdDesc("CONSULTA")).orElseThrow(()->new IllegalStateException("No hay tarifa activa de consulta para la especialidad del veterinario."));}
    private LineaFactura linea(String tipo,String descripcion,BigDecimal cant,BigDecimal unit,BigDecimal iva,String lote){LineaFactura l=new LineaFactura();l.setTipo(tipo);l.setDescripcion(descripcion);l.setCantidad(cant);l.setValorUnitario(unit);l.setIvaPorcentaje(iva);l.setCodigoLote(lote);BigDecimal sub=cant.multiply(unit).setScale(2,RoundingMode.HALF_UP);BigDecimal imp=sub.multiply(iva).divide(new BigDecimal("100"),2,RoundingMode.HALF_UP);l.setSubtotal(sub);l.setImpuesto(imp);l.setTotal(sub.add(imp));return l;}
    private void evento(Factura f,String action,String detail,String user){EventoFactura e=new EventoFactura();e.setAccion(action);e.setDetalle(detail);e.setUsuario(user);f.agregarEvento(e);}
    private String limpiar(String s){return s==null||s.isBlank()?null:s.trim();}
}
