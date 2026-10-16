package e.commerce.digital.testutil;

import e.commerce.digital.models.Carrito;
import e.commerce.digital.models.CarritoItem;
import e.commerce.digital.models.Categoria;
import e.commerce.digital.models.Pago;
import e.commerce.digital.models.Pedido;
import e.commerce.digital.models.Producto;
import e.commerce.digital.models.Subcategoria;
import e.commerce.digital.models.UsuarioAdmin;
import e.commerce.digital.models.UsuarioCliente;
import e.commerce.digital.util.enumerated.EstadoPago;
import e.commerce.digital.util.enumerated.EstadoPedido;

import java.math.BigDecimal;

/** Fábricas de entidades para las pruebas: objetos válidos y completos, sin base de datos. */
public final class DatosDePrueba {

    private DatosDePrueba() {
    }

    public static Categoria categoria(Long id, String nombre, boolean activo) {
        Categoria c = new Categoria();
        c.setIdCategoria(id);
        c.setNombre(nombre);
        c.setDescripcion("Descripción de " + nombre);
        c.setActivo(activo);
        return c;
    }

    public static Subcategoria subcategoria(Long id, String nombre, Categoria categoria, boolean activo) {
        Subcategoria s = new Subcategoria();
        s.setIdSubcategoria(id);
        s.setNombre(nombre);
        s.setDescripcion("Descripción de " + nombre);
        s.setActivo(activo);
        s.setCategoria(categoria);
        return s;
    }

    /** Producto disponible: él, su subcategoría y su categoría están activos. */
    public static Producto producto(Long id, String nombre, String precio, int stock) {
        Producto p = new Producto();
        p.setIdProducto(id);
        p.setNombre(nombre);
        p.setDescripcion("Descripción de " + nombre);
        p.setPrecio(new BigDecimal(precio));
        p.setStock(stock);
        p.setActivo(true);
        p.setSubcategoria(subcategoria(1L, "Látex", categoria(1L, "Pinturas", true), true));
        return p;
    }

    public static UsuarioCliente cliente(Long id, String email) {
        UsuarioCliente u = new UsuarioCliente();
        u.setIdUsuario(id);
        u.setNombre("Ana");
        u.setApellido("Pérez");
        u.setEmail(email);
        u.setPasswordHash("hash");
        u.setActivo(true);
        return u;
    }

    public static UsuarioAdmin admin(Long id, String email) {
        UsuarioAdmin u = new UsuarioAdmin();
        u.setIdUsuario(id);
        u.setNombre("Luis");
        u.setApellido("Gómez");
        u.setEmail(email);
        u.setPasswordHash("hash");
        u.setActivo(true);
        return u;
    }

    /** Carrito vacío vinculado a su cliente en ambos sentidos. */
    public static Carrito carrito(Long id, UsuarioCliente cliente) {
        Carrito c = new Carrito();
        c.setIdCarrito(id);
        c.setUsuario(cliente);
        cliente.setCarrito(c);
        return c;
    }

    public static CarritoItem item(Long id, Producto producto, int cantidad) {
        CarritoItem i = new CarritoItem();
        i.setIdCarritoItem(id);
        i.setProducto(producto);
        i.setCantidad(cantidad);
        i.setPrecioUnitario(producto.getPrecio());
        return i;
    }

    public static Pedido pedido(Long id, UsuarioCliente cliente, EstadoPedido estado, String total) {
        Pedido p = new Pedido();
        p.setIdPedido(id);
        p.setUsuario(cliente);
        p.setEstado(estado);
        p.setTotal(new BigDecimal(total));
        p.setDireccionEntrega("Calle 123");
        return p;
    }

    public static Pago pago(Long id, Pedido pedido, String importe, EstadoPago estado) {
        Pago p = new Pago();
        p.setIdPago(id);
        p.setPedido(pedido);
        p.setMetodo("TRANSFERENCIA");
        p.setImporte(new BigDecimal(importe));
        p.setEstado(estado);
        return p;
    }
}
