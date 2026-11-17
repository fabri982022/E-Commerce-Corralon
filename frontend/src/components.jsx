import { Link, Navigate, useLocation } from 'react-router-dom';
import { useAuth, useCart } from './contexts';
import { formatPrice } from './api';

export function Loading({ label = 'Cargando...' }) {
  return <div className="loading-state"><span className="spinner-border spinner-border-sm text-primary" /> {label}</div>;
}

export function ErrorBanner({ message, onRetry }) {
  if (!message) return null;
  return (
    <div className="alert alert-warning d-flex align-items-center justify-content-between gap-3" role="alert">
      <span><i className="bi bi-wifi-off me-2" />{message}</span>
      {onRetry && <button className="btn btn-sm btn-outline-dark flex-shrink-0" onClick={onRetry}>Reintentar</button>}
    </div>
  );
}

export function ProtectedRoute({ children, role }) {
  const { user, loading } = useAuth();
  const location = useLocation();

  if (loading) return <Loading />;
  if (!user) return <Navigate to="/ingresar" replace state={{ returnTo: location }} />;
  if (role && user.rol !== role) return <Navigate to={user.rol === 'ADMIN' ? '/admin' : user.rol === 'VENDEDOR' ? '/vendedor' : '/'} replace />;
  return children;
}

export function CartRoute({ children }) {
  const { cart, loading, error } = useCart();
  if (loading) return <Loading label="Estamos preparando tu carrito..." />;
  if (error) return <Navigate to="/carrito" replace />;
  if (!cart.items?.length) return <Navigate to="/carrito" replace />;
  return children;
}

export function ProductCard({ product, onAdd }) {
  return (
    <article className="product-card h-100">
      <Link to={`/producto/${product.idProducto}`} className="product-image-link">
        <img
          src={product.imagenUrl || 'https://placehold.co/600x440/f2f4f8/778196?text=Producto'}
          alt={product.nombre}
          loading="lazy"
          onError={(event) => { event.currentTarget.src = 'https://placehold.co/600x440/f2f4f8/778196?text=Producto'; }}
        />
      </Link>
      <div className="product-card-body">
        <span className="product-category">{product.nombreCategoria || 'Construcción'}</span>
        <Link to={`/producto/${product.idProducto}`} className="product-title">{product.nombre}</Link>
        <div className="product-price">{formatPrice(product.precio)}</div>
        <div className="product-delivery"><i className="bi bi-truck me-1" />Envío a todo el país</div>
        <button className="btn btn-add-cart" disabled={!product.stock} onClick={() => onAdd(product)}>
          <i className={`bi ${product.stock ? 'bi-cart-plus' : 'bi-dash-circle'} me-1`} />{product.stock ? 'Agregar al carrito' : 'Sin stock'}
        </button>
      </div>
    </article>
  );
}

export function SectionTitle({ eyebrow, title, action }) {
  return (
    <div className="section-title">
      <div><div className="eyebrow">{eyebrow}</div><h2>{title}</h2></div>
      {action}
    </div>
  );
}

export function EmptyState({ icon = 'bi-inbox', title, description, action }) {
  return (
    <div className="empty-state">
      <i className={`bi ${icon}`} />
      <h3>{title}</h3>
      <p>{description}</p>
      {action && <Link className="btn btn-primary" to="/">{action}</Link>}
    </div>
  );
}

export function CartSummary({ cart, buttonText = 'Continuar la compra', to = '/checkout' }) {
  return (
    <aside className="cart-summary">
      <h3>Resumen de compra</h3>
      <div className="summary-row"><span>Productos</span><strong>{formatPrice(cart.total)}</strong></div>
      <div className="summary-row"><span>Envío</span><strong className="text-success">Gratis</strong></div>
      <hr />
      <div className="summary-total"><span>Total</span><strong>{formatPrice(cart.total)}</strong></div>
      <Link to={to} className="btn btn-primary btn-lg w-100 mt-4">{buttonText}<i className="bi bi-arrow-right ms-2" /></Link>
      <p className="summary-safe"><i className="bi bi-shield-check me-1" />Compra protegida</p>
    </aside>
  );
}
