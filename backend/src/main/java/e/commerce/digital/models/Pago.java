package e.commerce.digital.models;

import e.commerce.digital.util.enumerated.EstadoPago;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@NoArgsConstructor
@Table(name = "pagos")
public class Pago {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long idPago;

    @Column(nullable = false, length = 30)
    private String metodo;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal importe;

    @Column(name = "fecha_pago")
    private LocalDateTime fechaPago;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EstadoPago estado = EstadoPago.PENDIENTE;

    @Column(name = "referencia_operacion", length = 100)
    private String referenciaOperacion;

    // Pedido 1 --- 1..* Pago (lado dueño; FK sin restricción única)
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_pedido", nullable = false)
    private Pedido pedido;

    // Pago 1 --- 0..1 Comprobante (lado inverso)
    @OneToOne(mappedBy = "pago", cascade = CascadeType.ALL)
    private Comprobante comprobante;

    public void setComprobante(Comprobante comprobante) {
        this.comprobante = comprobante;
        if (comprobante != null) {
            comprobante.setPago(this);
        }
    }
}
