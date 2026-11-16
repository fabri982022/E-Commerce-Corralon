package e.commerce.digital.controller;

import e.commerce.digital.dto.common.PageResponse;
import e.commerce.digital.dto.pago.PagoRequest;
import e.commerce.digital.dto.pago.PagoResponse;
import e.commerce.digital.dto.pedido.CambioEstadoPedidoRequest;
import e.commerce.digital.dto.pedido.PedidoFiltroRequest;
import e.commerce.digital.dto.pedido.PedidoResponse;
import e.commerce.digital.dto.pedido.PedidoResumenResponse;
import e.commerce.digital.service.PagoService;
import e.commerce.digital.service.PedidoService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** Pedidos del negocio (RF08, RF13): filtros por fecha, estado y cliente, y cambio de estado. */
@RestController
@RequestMapping("/api/admin/pedidos")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class AdminPedidoController {

    private final PedidoService pedidoService;
    private final PagoService pagoService;

    /** Parámetros opcionales: desde, hasta (AAAA-MM-DD), estado, idUsuario, page, size y sort. */
    @GetMapping
    public PageResponse<PedidoResumenResponse> filtrar(
            PedidoFiltroRequest filtro,
            @PageableDefault(size = 20, sort = "fechaPedido", direction = Sort.Direction.DESC) Pageable pageable) {
        return pedidoService.filtrar(filtro, pageable);
    }

    @GetMapping("/{idPedido}")
    public PedidoResponse obtener(@PathVariable Long idPedido) {
        return pedidoService.obtenerAdmin(idPedido);
    }

    /** Valida la transición: por ejemplo, un pedido ENTREGADO o CANCELADO ya no cambia. */
    @PatchMapping("/{idPedido}/estado")
    public PedidoResponse cambiarEstado(@PathVariable Long idPedido,
                                        @Valid @RequestBody CambioEstadoPedidoRequest req) {
        return pedidoService.cambiarEstado(idPedido, req);
    }

    @GetMapping("/{idPedido}/pagos")
    public List<PagoResponse> pagos(@PathVariable Long idPedido) {
        return pagoService.listarPorPedidoAdmin(idPedido);
    }

    /** Registra un pago manual (queda PENDIENTE hasta que se apruebe). */
    @PostMapping("/{idPedido}/pagos")
    public ResponseEntity<PagoResponse> registrarPago(@PathVariable Long idPedido,
                                                      @Valid @RequestBody PagoRequest req) {
        PagoResponse creado = pagoService.registrarComoAdmin(idPedido, req);
        return Respuestas.creado(creado, "/api/admin/pagos/{id}", creado.idPago());
    }
}
