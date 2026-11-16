package e.commerce.digital.controller;

import e.commerce.digital.dto.common.PageResponse;
import e.commerce.digital.dto.pedido.PedidoRequest;
import e.commerce.digital.dto.pedido.PedidoResponse;
import e.commerce.digital.dto.pedido.PedidoResumenResponse;
import e.commerce.digital.service.PedidoService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Pedidos del cliente autenticado. Un pedido ajeno responde 404, igual que uno inexistente. */
@RestController
@RequestMapping("/api/pedidos")
@PreAuthorize("hasRole('CLIENTE')")
@RequiredArgsConstructor
public class PedidoController {

    private final PedidoService pedidoService;

    /** Confirma el carrito: genera el pedido, descuenta stock y vacía el carrito. */
    @PostMapping
    public ResponseEntity<PedidoResponse> confirmar(@Valid @RequestBody PedidoRequest req, Authentication auth) {
        PedidoResponse creado = pedidoService.confirmar(auth.getName(), req);
        return Respuestas.creado(creado, "/api/pedidos/{id}", creado.idPedido());
    }

    /** Historial de compras, del más reciente al más antiguo. */
    @GetMapping
    public PageResponse<PedidoResumenResponse> historial(
            Authentication auth,
            @PageableDefault(size = 10, sort = "fechaPedido", direction = Sort.Direction.DESC) Pageable pageable) {
        return pedidoService.listarMios(auth.getName(), pageable);
    }

    @GetMapping("/{idPedido}")
    public PedidoResponse obtener(@PathVariable Long idPedido, Authentication auth) {
        return pedidoService.obtenerMio(auth.getName(), idPedido);
    }
}
