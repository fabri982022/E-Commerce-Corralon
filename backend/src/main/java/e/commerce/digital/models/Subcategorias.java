package e.commerce.digital.models;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "subcategorias")
public class Subcategorias {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long idSubcategoria;

    @Column(nullable = false, length = 100)
    private String nombre;

    @Column(length = 500)
    private String descripcion;

    // Subcategoria *** 1 Categoria (lado dueño)
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_categoria", nullable = false)
    private Categoria categoria;

    // Subcategoria 1 --- 0..* Producto (lado inverso, sin cascade)
    @OneToMany(mappedBy = "subcategoria")
    private List<Producto> productos = new ArrayList<>();
}