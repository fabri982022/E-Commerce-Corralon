package e.commerce.digital.models;

import e.commerce.digital.util.enumerated.Rol;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import lombok.Getter;
import lombok.Setter;

@Entity
@Getter
@Setter
@DiscriminatorValue("VENDEDOR")
public class UsuarioVendedor extends Usuario {

    public UsuarioVendedor() {
        setRol(Rol.VENDEDOR);
    }
}
