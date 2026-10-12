package e.commerce.digital.models;

import jakarta.persistence.*;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Entity
@Getter
@Setter
@NoArgsConstructor
@Table(name = "productos")
public class Producto {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long idProducto;

    @Column(nullable = false, length = 100)
    private String nombre;

    @Column(length = 500)
    private String descripcion;

    @PositiveOrZero
    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal precio;

    @PositiveOrZero
    @Column(nullable = false)
    private Integer stock;

    @Column(name = "imagen_url", length = 500)
    private String imagenUrl;

    // Identificador en Cloudinary, necesario para reemplazar o borrar la imagen
    @Column(name = "imagen_public_id", length = 255)
    private String imagenPublicId;

    @Column(nullable = false)
    private Boolean activo = true;

    // Producto *--- 1 Subcategoria (sin cascade)
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_subcategoria", nullable = false)
    private Subcategoria subcategoria;
}
