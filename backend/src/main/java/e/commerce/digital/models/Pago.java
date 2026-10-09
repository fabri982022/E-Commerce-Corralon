package e.commerce.digital.models;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "pagos")
public class Pago {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long idPago;

    @Column(nullable = false, length = 30)
    private String metodo;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal monto;

    @Column(name = "fecha_pago")
    private LocalDateTime fechaPago;

    @Column(nullable = false, length = 20)
    private String estado;

    // Pedido 1 --- 1 Pago (lado dueño)
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_pedido", nullable = false, unique = true)
    private Pedido pedido;

    // Pago 1 --- 1 Comprobante (lado inverso)
    @OneToOne(mappedBy = "pago", cascade = CascadeType.ALL)
    private Comprobante comprobante;

    public void setComprobante(Comprobante comprobante) {
        this.comprobante = comprobante;
        if (comprobante != null) {
            comprobante.setPago(this);
        }
    }
}
