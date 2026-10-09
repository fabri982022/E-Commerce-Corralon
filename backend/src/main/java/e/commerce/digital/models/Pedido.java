package e.commerce.digital.models;

import e.commerce.digital.util.enumerated.EstadoPedido;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "pedidos")
public class Pedido {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long idPedido;

    @CreationTimestamp
    @Column(name = "fecha_pedido", updatable = false)
    private LocalDateTime fechaPedido;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EstadoPedido estado = EstadoPedido.PENDIENTE;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal total = BigDecimal.ZERO;

    @Column(name = "direccion_envio", nullable = false, length = 255)
    private String direccionEnvio;

    // UsuarioCliente 1 --- 0..* Pedido (lado dueño)
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_usuario", nullable = false)
    private UsuarioCliente cliente;

    // Pedido 1 --- 1..* DetallePedido (composición: sin pedido no hay detalle)
    @OneToMany(mappedBy = "pedido", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<DetallePedido> detalles = new ArrayList<>();

    // Pedido 1 --- 1 Pago (lado inverso)
    @OneToOne(mappedBy = "pedido", cascade = CascadeType.ALL)
    private Pago pago;

    // Helpers
    public void agregarDetalle(DetallePedido detalle) {
        detalles.add(detalle);
        detalle.setPedido(this);
        recalcularTotal();
    }

    public void recalcularTotal() {
        this.total = detalles.stream()
                .map(DetallePedido::getSubtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

}