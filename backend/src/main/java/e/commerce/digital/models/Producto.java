package e.commerce.digital.models;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "productos")
public class Producto {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long idProducto;

    @Column(nullable = false, length = 100)
    private String nombre;

    @Column(length = 500)
    private String descripcion;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal precio;

    @Column(name = "stock_disponible", nullable = false)
    private Integer stockDisponible;

    @Column(name = "imagen_url", length = 255)
    private String imagenUrl;

    @Column(name = "estado", nullable = false)
    private String estado;

    // Producto *** 1 Subcategorias (sin cascade: la subcategoría existe por sí sola)
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_subcategoria", nullable = false)
    private Subcategorias subcategoria;
}
