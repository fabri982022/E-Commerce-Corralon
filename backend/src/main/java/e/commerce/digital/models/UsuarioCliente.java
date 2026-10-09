package e.commerce.digital.models;

import e.commerce.digital.util.enumerated.Rol;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@Setter
@DiscriminatorValue("CLIENTE")
public class UsuarioCliente extends Usuario {

    public UsuarioCliente() {
        setRol(Rol.CLIENTE);
    }

    // UsuarioCliente 1 --- 1 Carrito (lado inverso; el dueño es Carrito)
    @OneToOne(mappedBy = "usuario", cascade = CascadeType.ALL, orphanRemoval = true)
    private Carrito carrito;

    // UsuarioCliente 1 --- 0..* Pedido (lado inverso; sin cascade)
    @OneToMany(mappedBy = "usuario")
    private List<Pedido> pedidos = new ArrayList<>();
}