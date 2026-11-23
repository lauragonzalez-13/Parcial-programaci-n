package com.uniminuto.clinica.apicontroller;

import com.uniminuto.clinica.entity.Cliente;
import com.uniminuto.clinica.entity.Mascota;
import com.uniminuto.clinica.entity.Veterinario;
import com.uniminuto.clinica.dto.CrearVeterinarioRequest;
import com.uniminuto.clinica.serviceimpl.AuthService.UsuarioAutenticado;
import com.uniminuto.clinica.repository.ClienteRepository;
import com.uniminuto.clinica.repository.MascotaRepository;
import com.uniminuto.clinica.repository.VeterinarioRepository;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.http.ResponseEntity;
import java.util.Map;
import java.text.Normalizer;
import java.util.UUID;

@RestController
public class CatalogoController {
    private final ClienteRepository clienteRepository;
    private final MascotaRepository mascotaRepository;
    private final VeterinarioRepository veterinarioRepository;

    public CatalogoController(ClienteRepository clienteRepository,
            MascotaRepository mascotaRepository,
            VeterinarioRepository veterinarioRepository) {
        this.clienteRepository = clienteRepository;
        this.mascotaRepository = mascotaRepository;
        this.veterinarioRepository = veterinarioRepository;
    }

    @GetMapping("/clientes")
    public List<Cliente> clientes() {
        return clienteRepository.findAll();
    }

    @PostMapping("/clientes")
    public ResponseEntity<?> crearCliente(@RequestBody java.util.Map<String, String> datos) {
        String nombre = datos.get("nombre");
        if (nombre == null || nombre.trim().isEmpty()) {
            throw new IllegalArgumentException("El nombre del cliente es obligatorio.");
        }
        String nombreNormalizado = normalizar(nombre);
        List<Cliente> coincidencias = clienteRepository.findAll().stream()
                .filter(cliente -> nombreNormalizado.equals(normalizar(cliente.getNombre()))
                        || nombreNormalizado.equals(normalizar(
                                cliente.getNombre() + " " + (cliente.getApellido() == null
                                        ? "" : cliente.getApellido()))))
                .toList();
        if (coincidencias.size() == 1) {
            return ResponseEntity.ok(coincidencias.get(0));
        }
        if (coincidencias.size() > 1) {
            return ResponseEntity.status(409).body(Map.of(
                    "mensaje", "Hay varios clientes con ese nombre. Selecciona el correcto de la lista."));
        }
        Cliente cliente = new Cliente();
        cliente.setNombre(nombre.trim());
        cliente.setApellido("");
        cliente.setDocumento("CLI-" + UUID.randomUUID().toString().replace("-", "").substring(0, 16));
        cliente.setActivo(true);
        return ResponseEntity.status(201).body(clienteRepository.save(cliente));
    }

    @GetMapping("/clientes/{clienteId}")
    public Cliente cliente(@PathVariable Long clienteId) {
        return clienteRepository.findById(clienteId)
                .orElseThrow(() -> new IllegalArgumentException("El cliente no existe."));
    }

    @GetMapping("/veterinarios")
    public List<Veterinario> veterinarios() {
        return veterinarioRepository.findAll();
    }

    @PostMapping("/veterinarios")
    public ResponseEntity<?> crearVeterinario(@RequestBody CrearVeterinarioRequest request,
            @RequestAttribute("usuarioAutenticado") UsuarioAutenticado actor) {
        if (!"ADMINISTRADOR".equals(actor.rol()))
            return ResponseEntity.status(403).body(Map.of("mensaje", "Solo un administrador puede crear veterinarios."));
        if (request == null || request.nombre() == null || request.nombre().isBlank()
                || request.apellido() == null || request.apellido().isBlank()
                || request.documento() == null || request.documento().isBlank())
            return ResponseEntity.badRequest().body(Map.of("mensaje", "Nombre, apellido y documento son obligatorios."));
        String documento = request.documento().trim();
        if (request.nombre().trim().length() > 100 || request.apellido().trim().length() > 100 || documento.length() > 20
                || (request.tarjetaProfesional() != null && request.tarjetaProfesional().length() > 30)
                || (request.correo() != null && request.correo().length() > 100))
            return ResponseEntity.badRequest().body(Map.of("mensaje", "Revisa la longitud de los datos: nombres/apellidos 100, documento 20, tarjeta 30 y correo 100 caracteres máximo."));
        if (veterinarioRepository.existsByDocumentoIgnoreCase(documento))
            return ResponseEntity.badRequest().body(Map.of("mensaje", "Ya existe un veterinario con ese documento."));
        Veterinario veterinario = new Veterinario();
        veterinario.setNombre(request.nombre().trim()); veterinario.setApellido(request.apellido().trim());
        veterinario.setDocumento(documento); veterinario.setTarjetaProfesional(limpiar(request.tarjetaProfesional()));
        veterinario.setCorreo(limpiar(request.correo())); veterinario.setActivo(true);
        return ResponseEntity.status(201).body(veterinarioRepository.save(veterinario));
    }

    @GetMapping("/clientes/{clienteId}/mascotas")
    public List<Mascota> mascotasDelCliente(@PathVariable Long clienteId) {
        return mascotaRepository.findByClienteId(clienteId);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> datosInvalidos(IllegalArgumentException e) {
        return ResponseEntity.badRequest().body(Map.of("mensaje", e.getMessage()));
    }

    private String limpiar(String value) { return value == null || value.isBlank() ? null : value.trim(); }

    private String normalizar(String value) {
        return Normalizer.normalize(value == null ? "" : value, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .trim()
                .replaceAll("\\s+", " ")
                .toLowerCase(java.util.Locale.ROOT);
    }
}
