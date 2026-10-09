package e.commerce.digital.models;
import jakarta.persistence.*;
import lombok.*;

import e.commerce.digital.models.Pedido;
import e.commerce.digital.models.Carrito;
import java.util.List;
import java.util.ArrayList;

@Entity
@DiscriminatorValue("CLIENTE")
@Getter
@Setter
@NoArgsConstructor
public class UsuarioCliente extends Usuario {
    // Cliente 1 --- 1 Carrito (lado inverso; el dueño es Carrito)
    @OneToOne(mappedBy = "cliente", cascade = CascadeType.ALL, orphanRemoval = true)
    private Carrito carrito;

    // Cliente 1 --- 0..* Pedido
    @OneToMany(mappedBy = "cliente")
    private List<Pedido> pedidos = new ArrayList<>();
}
