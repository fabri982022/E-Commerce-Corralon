package e.commerce.digital.models;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "comprobantes")
public class Comprobante {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long idComprobante;

    @Column(nullable = false, length = 30)
    private String tipo;

    @Column(nullable = false, unique = true, length = 30)
    private String numero;

    @Column(name = "fecha_emision", nullable = false)
    private LocalDateTime fechaEmision;

    @Column(length = 500)
    private String url;

    // Pago 1 --- 1 Comprobante (lado dueño)
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_pago", nullable = false, unique = true)
    private Pago pago;
}