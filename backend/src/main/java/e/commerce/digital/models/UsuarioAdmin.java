package e.commerce.digital.models;
import jakarta.persistence.*;
import lombok.*;


@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@DiscriminatorValue("ADMIN")

public class UsuarioAdmin extends Usuario {
    // Additional fields and methods specific to UsuarioAdmin can be added here
}
