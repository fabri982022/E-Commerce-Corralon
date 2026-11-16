package e.commerce.digital.controller;

import e.commerce.digital.dto.pago.PagoRequest;
import e.commerce.digital.dto.pago.PagoResponse;
import e.commerce.digital.service.PagoService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Pagos del cliente. Registrar un pago lo deja PENDIENTE: solo un administrador puede aprobarlo.
 */
@RestController
@RequestMapping("/api")
@PreAuthorize("hasRole('CLIENTE')")
@RequiredArgsConstructor
public class PagoController {

    private final PagoService pagoService;

    @PostMapping("/pedidos/{idPedido}/pagos")
    public ResponseEntity<PagoResponse> registrar(@PathVariable Long idPedido,
                                                  @Valid @RequestBody PagoRequest req,
                                                  Authentication auth) {
        PagoResponse creado = pagoService.registrarComoCliente(auth.getName(), idPedido, req);
        return Respuestas.creado(creado, "/api/pagos/{id}", creado.idPago());
    }

    @GetMapping("/pedidos/{idPedido}/pagos")
    public List<PagoResponse> deUnPedido(@PathVariable Long idPedido, Authentication auth) {
        return pagoService.listarMiosPorPedido(auth.getName(), idPedido);
    }

    @GetMapping("/pagos/{idPago}")
    public PagoResponse obtener(@PathVariable Long idPago, Authentication auth) {
        return pagoService.obtenerMio(auth.getName(), idPago);
    }
}
