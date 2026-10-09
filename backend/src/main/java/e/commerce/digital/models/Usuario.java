package e.commerce.digital.models;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import e.commerce.digital.util.enumerated.EstadoUsuario;
import e.commerce.digital.util.enumerated.Rol;

import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "usuarios")
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(name = "tipo_usuario", discriminatorType = DiscriminatorType.STRING)
public abstract class Usuario {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long idUsuario;
    @Column(length = 25)
    private String nombre;

    @Column(length= 25)
    private String apellido;

    @Column(unique=true,  length= 50)
    private String email;

    @Column(unique=true,  length= 15)
    private String celular;

    @Column
    private String foto;

    @Column
    @Enumerated(EnumType.STRING)
    private EstadoUsuario estado;

    @Column
    @Enumerated(EnumType.STRING)
    private Rol rol;

    @Column
    private String passwordHash;

    @CreationTimestamp
    private LocalDateTime created;
    @UpdateTimestamp
    private LocalDateTime updated;

}
