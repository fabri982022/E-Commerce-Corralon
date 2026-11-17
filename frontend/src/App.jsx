import { useCallback, useEffect, useMemo, useState } from 'react';
import { Link, Navigate, Route, Routes, useLocation, useNavigate, useParams, useSearchParams } from 'react-router-dom';
import { api, formatPrice } from './api';
import { useAuth, useCart, useNotify } from './contexts';
import {
  CartRoute, CartSummary, EmptyState, ErrorBanner, Loading, ProductCard, ProtectedRoute, SectionTitle,
} from './components';

const placeholder = 'https://placehold.co/800x600/f2f4f8/778196?text=Producto';

function Header() {
  const { user, setUser } = useAuth();
  const { cart } = useCart();
  const notify = useNotify();
  const navigate = useNavigate();
  const [search, setSearch] = useState('');
  const [menuOpen, setMenuOpen] = useState(false);

  const submitSearch = (event) => {
    event.preventDefault();
    navigate(`/?q=${encodeURIComponent(search.trim())}`);
    setMenuOpen(false);
  };

  const logout = async () => {
    try {
      await api('/auth/logout', { method: 'POST' });
      setUser(null);
      notify('Cerraste sesión correctamente.');
      navigate('/');
    } catch (error) {
      notify(error.message, 'danger');
    }
  };

  return (
    <>
      <div className="utility-bar"><div className="container d-flex justify-content-between">
        <span><i className="bi bi-geo-alt me-1" />Envíos a todo el país</span>
        <span>Tu próximo proyecto empieza acá <i className="bi bi-tools ms-1" /></span>
      </div></div>
      <header className="site-header">
        <div className="container">
          <div className="header-main">
            <Link to="/" className="brand" aria-label="Corralón Central, inicio">
              <span className="brand-mark"><i className="bi bi-bricks" /></span>
              <span><strong>corralón</strong><small>central</small></span>
            </Link>
            <form className="search-form" role="search" onSubmit={submitSearch}>
              <input value={search} onChange={(event) => setSearch(event.target.value)} placeholder="Buscar productos, marcas y más..." aria-label="Buscar productos" />
              <button type="submit" aria-label="Buscar"><i className="bi bi-search" /></button>
            </form>
            <div className="header-actions">
              {user?.rol === 'CLIENTE' && (
                <Link to="/carrito" className="header-action cart-action" aria-label="Ver carrito">
                  <i className="bi bi-cart3" /><span>Carrito</span>
                  {cart.items?.length > 0 && <b>{cart.items.reduce((sum, item) => sum + item.cantidad, 0)}</b>}
                </Link>
              )}
              {user ? (
                <div className="dropdown">
                  <button className="header-action account-action dropdown-toggle" data-bs-toggle="dropdown" aria-expanded="false">
                    <i className="bi bi-person-circle" /><span>{user.nombre}</span>
                  </button>
                  <ul className="dropdown-menu dropdown-menu-end shadow-sm">
                    <li><span className="dropdown-item-text small text-muted">{user.email}</span></li>
                    <li><hr className="dropdown-divider" /></li>
                    {user.rol === 'CLIENTE' && <li><Link className="dropdown-item" to="/mis-compras"><i className="bi bi-box-seam me-2" />Mis compras</Link></li>}
                    {user.rol === 'ADMIN' && <li><Link className="dropdown-item" to="/admin"><i className="bi bi-speedometer2 me-2" />Panel administrador</Link></li>}
                    <li><button className="dropdown-item" onClick={logout}><i className="bi bi-box-arrow-right me-2" />Cerrar sesión</button></li>
                  </ul>
                </div>
              ) : (
                <Link to="/ingresar" className="header-action"><i className="bi bi-person" /><span>Ingresar</span></Link>
              )}
            </div>
            <button className="mobile-menu-toggle" onClick={() => setMenuOpen(!menuOpen)} aria-label="Abrir menú"><i className={`bi ${menuOpen ? 'bi-x-lg' : 'bi-list'}`} /></button>
          </div>
          <nav className={`main-nav ${menuOpen ? 'is-open' : ''}`}>
            <div className="nav-links">
              <Link to="/" onClick={() => setMenuOpen(false)}><i className="bi bi-house-door me-1" />Inicio</Link>
              <Link to="/?categoria=todos" onClick={() => setMenuOpen(false)}><i className="bi bi-grid me-1" />Categorías</Link>
              {user?.rol === 'CLIENTE' && <Link to="/mis-compras" onClick={() => setMenuOpen(false)}>Mis compras</Link>}
              {user?.rol === 'ADMIN' && <Link to="/admin" onClick={() => setMenuOpen(false)}>Administración</Link>}
            </div>
            <div className="nav-benefits"><span><i className="bi bi-patch-check me-1" />Productos seleccionados</span><span><i className="bi bi-credit-card me-1" />Pagá seguro</span></div>
          </nav>
        </div>
      </header>
    </>
  );
}

function Footer() {
  return (
    <footer className="site-footer">
      <div className="container footer-main">
        <div className="footer-brand"><span className="brand-mark"><i className="bi bi-bricks" /></span><strong>Corralón Central</strong><p>Todo para construir, renovar y hacer realidad tus ideas.</p></div>
        <div><h6>Comprá tranquilo</h6><p><i className="bi bi-shield-check me-2" />Compra protegida</p><p><i className="bi bi-truck me-2" />Envíos a todo el país</p></div>
        <div><h6>¿Necesitás ayuda?</h6><p>Estamos para acompañarte en cada proyecto.</p><a href="mailto:hola@corraloncentral.com">hola@corraloncentral.com</a></div>
      </div>
      <div className="footer-bottom"><div className="container">© {new Date().getFullYear()} Corralón Central · Construimos juntos.</div></div>
    </footer>
  );
}

function App() {
  return (
    <div className="app-shell">
      <Header />
      <main className="main-content">
        <Routes>
          <Route path="/" element={<HomePage />} />
          <Route path="/producto/:id" element={<ProductPage />} />
          <Route path="/ingresar" element={<AuthPage />} />
          <Route path="/registro" element={<RegisterPage />} />
          <Route path="/carrito" element={<ProtectedRoute role="CLIENTE"><CartPage /></ProtectedRoute>} />
          <Route path="/checkout" element={<ProtectedRoute role="CLIENTE"><CartRoute><CheckoutPage /></CartRoute></ProtectedRoute>} />
          <Route path="/mis-compras" element={<ProtectedRoute role="CLIENTE"><OrdersPage /></ProtectedRoute>} />
          <Route path="/admin" element={<ProtectedRoute role="ADMIN"><AdminPage /></ProtectedRoute>} />
          <Route path="*" element={<NotFoundPage />} />
        </Routes>
      </main>
      <Footer />
    </div>
  );
}

function HomePage() {
  const [searchParams] = useSearchParams();
  const query = searchParams.get('q') || '';
  const [categories, setCategories] = useState([]);
  const [subcategories, setSubcategories] = useState([]);
  const [categoryId, setCategoryId] = useState('');
  const [subcategoryId, setSubcategoryId] = useState('');
  const [pageData, setPageData] = useState({ contenido: [], pagina: 0, totalPaginas: 0, totalElementos: 0 });
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const { user } = useAuth();
  const { setCart } = useCart();
  const navigate = useNavigate();
  const location = useLocation();
  const notify = useNotify();

  const loadCatalog = useCallback(async (page = 0) => {
    setLoading(true);
    setError('');
    try {
      const params = new URLSearchParams({ page, size: 12, sort: 'nombre,asc' });
      if (query.trim()) params.set('texto', query.trim());
      if (categoryId) params.set('idCategoria', categoryId);
      if (subcategoryId) params.set('idSubcategoria', subcategoryId);
      const result = await api(`/productos?${params}`);
      setPageData(result);
    } catch (requestError) {
      setError(requestError.message);
    } finally {
      setLoading(false);
    }
  }, [query, categoryId, subcategoryId]);

  useEffect(() => { loadCatalog(); }, [loadCatalog]);
  useEffect(() => {
    api('/categorias').then(setCategories).catch((requestError) => setError(requestError.message));
  }, []);
  useEffect(() => {
    setSubcategoryId('');
    if (!categoryId) {
      setSubcategories([]);
      return;
    }
    api(`/categorias/${categoryId}/subcategorias`)
      .then(setSubcategories)
      .catch((requestError) => setError(requestError.message));
  }, [categoryId]);

  const addToCart = async (product) => {
    if (user?.rol !== 'CLIENTE') {
      if (user?.rol === 'ADMIN') {
        notify('La compra está disponible para cuentas de cliente.', 'danger');
        return;
      }
      notify('Ingresá para agregar productos a tu carrito.', 'danger');
      navigate('/ingresar', { state: { pendingProduct: product, returnTo: location } });
      return;
    }
    try {
      setCart(await api('/carrito/items', { method: 'POST', body: JSON.stringify({ idProducto: product.idProducto, cantidad: 1 }) }));
      notify(`${product.nombre} se agregó al carrito.`);
    } catch (requestError) {
      notify(requestError.message, 'danger');
    }
  };

  return (
    <>
      <section className="hero-section">
        <div className="container hero-content">
          <div className="hero-copy">
            <span className="hero-kicker"><i className="bi bi-stars me-2" />Todo para tu próximo proyecto</span>
            <h1>Construí tus ideas.<br /><span>Nosotros te acompañamos.</span></h1>
            <p>Encontrá materiales, herramientas y todo lo que necesitás para transformar tus espacios.</p>
            <a href="#productos" className="btn btn-hero">Explorar productos <i className="bi bi-arrow-down ms-2" /></a>
            <div className="hero-trust"><span><i className="bi bi-check-circle-fill" /> Calidad garantizada</span><span><i className="bi bi-check-circle-fill" /> Atención personalizada</span></div>
          </div>
          <div className="hero-illustration" aria-hidden="true">
            <div className="hero-sun" />
            <div className="hero-card"><i className="bi bi-house-gear-fill" /><strong>Tu proyecto</strong><span>Empieza hoy</span></div>
            <div className="hero-orbit orbit-one" /><div className="hero-orbit orbit-two" />
            <i className="bi bi-hammer floating-tool tool-one" /><i className="bi bi-rulers floating-tool tool-two" />
            <span className="hero-dot dot-one" /><span className="hero-dot dot-two" />
          </div>
        </div>
        <div className="hero-bottom container">
          <div><i className="bi bi-truck" /><span><strong>Envíos a todo el país</strong><small>Recibí tu compra donde estés</small></span></div>
          <div><i className="bi bi-credit-card-2-front" /><span><strong>Pagá como quieras</strong><small>Simple y seguro</small></span></div>
          <div><i className="bi bi-headset" /><span><strong>Estamos para ayudarte</strong><small>Te acompañamos en tu proyecto</small></span></div>
        </div>
      </section>

      <section className="container category-section">
        <SectionTitle eyebrow="ENCONTRÁ LO QUE BUSCÁS" title="Explorá por categoría" />
        {categories.length > 0 ? (
          <div className="category-pills">
            <button className={`category-pill ${!categoryId ? 'active' : ''}`} onClick={() => setCategoryId('')}><span className="category-icon"><i className="bi bi-grid-3x3-gap" /></span>Todo</button>
            {categories.map((category, index) => (
              <button key={category.idCategoria} className={`category-pill ${categoryId === String(category.idCategoria) ? 'active' : ''}`} onClick={() => setCategoryId(String(category.idCategoria))}>
                <span className={`category-icon category-color-${index % 5}`}><i className={['bi bi-bricks', 'bi bi-tools', 'bi bi-lightning-charge', 'bi bi-paint-bucket', 'bi bi-droplet'][index % 5]} /></span>
                {category.nombre}
              </button>
            ))}
          </div>
        ) : <p className="text-muted small">Cargando categorías del catálogo…</p>}
      </section>

      <section className="container products-section" id="productos">
        <SectionTitle
          eyebrow={query ? 'RESULTADOS DE BÚSQUEDA' : 'NUESTRO CATÁLOGO'}
          title={query ? `Resultados para “${query}”` : 'Productos para tu proyecto'}
          action={<span className="catalog-count">{pageData.totalElementos || 0} productos</span>}
        />
        <ErrorBanner message={error} onRetry={() => loadCatalog()} />
        {categoryId && subcategories.length > 0 && (
          <div className="subcategory-filter">
            <span>Subcategoría:</span>
            <select className="form-select" value={subcategoryId} onChange={(event) => setSubcategoryId(event.target.value)}>
              <option value="">Todas</option>
              {subcategories.map((sub) => <option key={sub.idSubcategoria} value={sub.idSubcategoria}>{sub.nombre}</option>)}
            </select>
          </div>
        )}
        {loading ? <Loading label="Buscando productos..." /> : pageData.contenido?.length ? (
          <>
            <div className="product-grid">
              {pageData.contenido.map((product) => <ProductCard key={product.idProducto} product={product} onAdd={addToCart} />)}
            </div>
            {pageData.totalPaginas > 1 && <Pagination page={pageData.pagina} totalPages={pageData.totalPaginas} onChange={loadCatalog} />}
          </>
        ) : (
          <EmptyState icon="bi-search" title="No encontramos productos" description="Probá con otra búsqueda o explorá todas las categorías." action="Ver todo el catálogo" />
        )}
      </section>

      <section className="container help-banner">
        <div className="help-icon"><i className="bi bi-chat-dots" /></div>
        <div><strong>¿Tenés un proyecto en mente?</strong><p>Encontrá todo lo que necesitás para hacerlo realidad.</p></div>
        <Link to="/?q=" className="btn btn-outline-primary">Explorar el catálogo <i className="bi bi-arrow-right ms-1" /></Link>
      </section>
    </>
  );
}

function Pagination({ page, totalPages, onChange }) {
  return (
    <nav className="pagination-wrap" aria-label="Paginación del catálogo">
      <button className="btn btn-outline-secondary" disabled={page <= 0} onClick={() => { onChange(page - 1); window.scrollTo({ top: 0, behavior: 'smooth' }); }}>Anterior</button>
      <span>Página {page + 1} de {totalPages}</span>
      <button className="btn btn-outline-secondary" disabled={page + 1 >= totalPages} onClick={() => { onChange(page + 1); window.scrollTo({ top: 0, behavior: 'smooth' }); }}>Siguiente</button>
    </nav>
  );
}

function ProductPage() {
  const { id: productId } = useParams();
  const [product, setProduct] = useState(null);
  const [loading, setLoading] = useState(true);
  const [quantity, setQuantity] = useState(1);
  const { user } = useAuth();
  const { setCart } = useCart();
  const navigate = useNavigate();
  const location = useLocation();
  const notify = useNotify();

  useEffect(() => {
    api(`/productos/${productId}`).then(setProduct).catch((error) => notify(error.message, 'danger')).finally(() => setLoading(false));
  }, [productId]);

  const addToCart = async () => {
    if (user?.rol !== 'CLIENTE') {
      if (user?.rol === 'ADMIN') return notify('La compra está disponible para cuentas de cliente.', 'danger');
      return navigate('/ingresar', { state: { pendingProduct: product, quantity, returnTo: location } });
    }
    try {
      setCart(await api('/carrito/items', { method: 'POST', body: JSON.stringify({ idProducto: product.idProducto, cantidad: quantity }) }));
      notify('Producto agregado al carrito.');
      navigate('/carrito');
    } catch (error) {
      notify(error.message, 'danger');
    }
  };

  if (loading) return <div className="container page-container"><Loading /></div>;
  if (!product) return <div className="container page-container"><EmptyState icon="bi-box" title="No encontramos este producto" description="Puede que ya no esté disponible." action="Volver al catálogo" /></div>;

  return (
    <div className="container page-container">
      <div className="breadcrumbs"><Link to="/">Inicio</Link><i className="bi bi-chevron-right" /><span>{product.nombreCategoria}</span><i className="bi bi-chevron-right" /><span>{product.nombre}</span></div>
      <div className="product-detail">
        <div className="detail-image"><img src={product.imagenUrl || placeholder} alt={product.nombre} onError={(event) => { event.currentTarget.src = placeholder; }} /></div>
        <div className="detail-info">
          <span className="detail-category">{product.nombreCategoria} · {product.nombreSubcategoria}</span>
          <h1>{product.nombre}</h1>
          <div className="detail-price">{formatPrice(product.precio)}</div>
          <div className="detail-stock"><i className="bi bi-check-circle-fill me-2" />{product.stock > 0 ? `Stock disponible: ${product.stock} unidades` : 'Sin stock disponible'}</div>
          <p className="detail-description">{product.descripcion || 'Un producto seleccionado para acompañar tus proyectos de construcción y renovación.'}</p>
          <div className="detail-delivery"><i className="bi bi-truck me-2" /><span><strong>Envío a todo el país</strong><small>Coordinamos la entrega después de tu compra.</small></span></div>
          <div className="detail-buy-row">
            <div className="quantity-control"><button aria-label="Restar" onClick={() => setQuantity(Math.max(1, quantity - 1))}>−</button><span>{quantity}</span><button aria-label="Sumar" disabled={quantity >= product.stock} onClick={() => setQuantity(Math.min(product.stock, quantity + 1))}>+</button></div>
            <button className="btn btn-primary btn-lg" disabled={!product.stock} onClick={addToCart}><i className="bi bi-cart-plus me-2" />Agregar al carrito</button>
          </div>
          <div className="detail-protected"><i className="bi bi-shield-check me-2" />Comprá con la tranquilidad de una compra protegida.</div>
        </div>
      </div>
    </div>
  );
}

function AuthPage() {
  const [form, setForm] = useState({ email: '', password: '' });
  const [error, setError] = useState('');
  const [submitting, setSubmitting] = useState(false);
  const { user, setUser } = useAuth();
  const { setCart } = useCart();
  const navigate = useNavigate();
  const location = useLocation();
  const notify = useNotify();

  if (user) return <Navigate to={user.rol === 'ADMIN' ? '/admin' : '/'} replace />;

  const onChange = (event) => setForm({ ...form, [event.target.name]: event.target.value });
  const submit = async (event) => {
    event.preventDefault();
    setSubmitting(true);
    setError('');
    try {
      const profile = await api('/auth/login', { method: 'POST', body: JSON.stringify(form) });
      const pending = location.state?.pendingProduct;
      if (pending && profile.rol === 'CLIENTE') {
        const updatedCart = await api('/carrito/items', {
          method: 'POST',
          body: JSON.stringify({ idProducto: pending.idProducto, cantidad: location.state?.quantity || 1 }),
        });
        setUser(profile);
        setCart(updatedCart);
        notify('Producto agregado al carrito.');
        return navigate('/carrito', { replace: true });
      }
      setUser(profile);
      const target = location.state?.returnTo;
      navigate(profile.rol === 'ADMIN' ? '/admin' : target?.pathname || '/', { replace: true });
    } catch (requestError) {
      setError(requestError.status === 401 ? 'Correo o contraseña incorrectos.' : requestError.message);
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <AuthLayout>
      <div className="auth-card">
        <div className="auth-icon"><i className="bi bi-person-lock" /></div>
        <h1>¡Qué bueno verte!</h1><p>Ingresá a tu cuenta para continuar con tu proyecto.</p>
        <ErrorBanner message={error} />
        <form onSubmit={submit} className="auth-form">
          <label htmlFor="email">Correo electrónico</label>
          <div className="input-icon"><i className="bi bi-envelope" /><input id="email" name="email" type="email" className="form-control" autoComplete="email" required value={form.email} onChange={onChange} placeholder="vos@ejemplo.com" /></div>
          <label htmlFor="password">Contraseña</label>
          <div className="input-icon"><i className="bi bi-lock" /><input id="password" name="password" type="password" className="form-control" autoComplete="current-password" required value={form.password} onChange={onChange} placeholder="Tu contraseña" /></div>
          <button className="btn btn-primary btn-lg w-100" disabled={submitting}>{submitting ? 'Ingresando...' : 'Ingresar'} {!submitting && <i className="bi bi-arrow-right ms-2" />}</button>
        </form>
        <div className="auth-switch">¿Todavía no tenés cuenta? <Link to="/registro" state={location.state}>Crear cuenta</Link></div>
        <div className="auth-secure"><i className="bi bi-shield-lock me-1" />Tu información está protegida</div>
      </div>
    </AuthLayout>
  );
}

function RegisterPage() {
  const [form, setForm] = useState({ nombre: '', apellido: '', email: '', password: '', telefono: '', direccion: '' });
  const [error, setError] = useState('');
  const [submitting, setSubmitting] = useState(false);
  const navigate = useNavigate();
  const location = useLocation();
  const notify = useNotify();

  const onChange = (event) => setForm({ ...form, [event.target.name]: event.target.value });
  const submit = async (event) => {
    event.preventDefault();
    setSubmitting(true);
    setError('');
    try {
      await api('/auth/registro', { method: 'POST', body: JSON.stringify(form) });
      notify('Tu cuenta ya está lista. Ingresá para empezar a comprar.');
      navigate('/ingresar', { state: location.state, replace: true });
    } catch (requestError) {
      setError(requestError.message);
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <AuthLayout>
      <div className="auth-card register-card">
        <div className="auth-icon"><i className="bi bi-person-plus" /></div>
        <h1>Creá tu cuenta</h1><p>Sumate y empezá a construir tus ideas.</p>
        <ErrorBanner message={error} />
        <form onSubmit={submit} className="auth-form">
          <div className="form-row"><div><label htmlFor="nombre">Nombre</label><input id="nombre" name="nombre" className="form-control" required maxLength={50} value={form.nombre} onChange={onChange} /></div><div><label htmlFor="apellido">Apellido</label><input id="apellido" name="apellido" className="form-control" required maxLength={50} value={form.apellido} onChange={onChange} /></div></div>
          <label htmlFor="email">Correo electrónico</label><input id="email" type="email" name="email" className="form-control" autoComplete="email" required value={form.email} onChange={onChange} />
          <label htmlFor="password">Contraseña</label><input id="password" type="password" name="password" className="form-control" autoComplete="new-password" minLength={8} required value={form.password} onChange={onChange} />
          <label htmlFor="telefono">Teléfono <span className="optional">(opcional)</span></label><input id="telefono" name="telefono" className="form-control" maxLength={15} value={form.telefono} onChange={onChange} />
          <label htmlFor="direccion">Dirección de entrega <span className="optional">(opcional)</span></label><input id="direccion" name="direccion" className="form-control" maxLength={255} value={form.direccion} onChange={onChange} />
          <button className="btn btn-primary btn-lg w-100 mt-2" disabled={submitting}>{submitting ? 'Creando cuenta...' : 'Crear cuenta'}</button>
        </form>
        <div className="auth-switch">¿Ya tenés cuenta? <Link to="/ingresar" state={location.state}>Ingresar</Link></div>
      </div>
    </AuthLayout>
  );
}

function AuthLayout({ children }) {
  return (
    <div className="auth-page">
      <div className="auth-aside"><div className="auth-aside-inner"><span className="auth-aside-logo"><i className="bi bi-bricks" /></span><h2>Un buen proyecto<br />empieza con todo.</h2><p>Encontrá materiales y herramientas para construir a tu manera.</p><div className="auth-aside-art"><i className="bi bi-house-gear-fill" /><span className="auth-art-sun" /></div></div></div>
      <div className="auth-form-side">{children}</div>
    </div>
  );
}

function CartPage() {
  const { cart, setCart, loading, error, refresh } = useCart();
  const notify = useNotify();
  const [busyItem, setBusyItem] = useState(null);

  const updateQuantity = async (item, quantity) => {
    if (quantity < 1) return removeItem(item.idCarritoItem);
    setBusyItem(item.idCarritoItem);
    try {
      const result = await api(`/carrito/items/${item.idCarritoItem}`, { method: 'PUT', body: JSON.stringify({ cantidad: quantity }) });
      setCart(result);
    } catch (error) {
      notify(error.message, 'danger');
    } finally {
      setBusyItem(null);
    }
  };
  const removeItem = async (id) => {
    setBusyItem(id);
    try {
      setCart(await api(`/carrito/items/${id}`, { method: 'DELETE' }));
      notify('Producto quitado del carrito.');
    } catch (error) {
      notify(error.message, 'danger');
    } finally {
      setBusyItem(null);
    }
  };
  const clearCart = async () => {
    try {
      setCart(await api('/carrito', { method: 'DELETE' }));
      notify('Vaciaste tu carrito.');
    } catch (error) {
      notify(error.message, 'danger');
    }
  };

  if (loading) return <div className="container page-container"><Loading /></div>;
  return (
    <div className="container page-container">
      <div className="breadcrumbs"><Link to="/">Inicio</Link><i className="bi bi-chevron-right" /><span>Mi carrito</span></div>
      <SectionTitle eyebrow="TU SELECCIÓN" title="Mi carrito" action={cart.items?.length > 0 && <button className="btn btn-link text-muted" onClick={clearCart}>Vaciar carrito</button>} />
      <ErrorBanner message={error} onRetry={() => refresh().catch(() => {})} />
      {!cart.items?.length ? (
        <EmptyState icon="bi-cart-x" title="Tu carrito está esperando" description="Cuando encuentres algo que te guste, agregalo acá." action="Explorar productos" />
      ) : (
        <div className="cart-layout">
          <div className="cart-items">
            {cart.items.map((item) => (
              <article className="cart-item" key={item.idCarritoItem}>
                <Link to={`/producto/${item.idProducto}`} className="cart-item-image"><img src={item.imagenUrl || placeholder} alt={item.nombreProducto} onError={(event) => { event.currentTarget.src = placeholder; }} /></Link>
                <div className="cart-item-info"><Link to={`/producto/${item.idProducto}`} className="cart-item-name">{item.nombreProducto}</Link><span className="cart-item-unit">{formatPrice(item.precioUnitario)} c/u</span><button className="remove-item" onClick={() => removeItem(item.idCarritoItem)} disabled={busyItem === item.idCarritoItem}><i className="bi bi-trash3 me-1" />Quitar</button></div>
                <div className="cart-item-controls"><div className="quantity-control"><button disabled={busyItem === item.idCarritoItem} onClick={() => updateQuantity(item, item.cantidad - 1)}>−</button><span>{item.cantidad}</span><button disabled={busyItem === item.idCarritoItem} onClick={() => updateQuantity(item, item.cantidad + 1)}>+</button></div><strong>{formatPrice(item.subtotal)}</strong></div>
              </article>
            ))}
          </div>
          <CartSummary cart={cart} />
        </div>
      )}
    </div>
  );
}

function CheckoutPage() {
  const { cart, refresh } = useCart();
  const { user } = useAuth();
  const [address, setAddress] = useState(user?.direccion || '');
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState('');
  const navigate = useNavigate();
  const notify = useNotify();

  const submit = async (event) => {
    event.preventDefault();
    if (!cart.items?.length) return navigate('/carrito', { replace: true });
    setSubmitting(true);
    setError('');
    try {
      const order = await api('/pedidos', { method: 'POST', body: JSON.stringify({ direccionEntrega: address.trim() }) });
      await refresh();
      notify('¡Pedido confirmado! Ya podés seguir su estado en Mis compras.');
      navigate(`/mis-compras?pedido=${order.idPedido}`, { replace: true });
    } catch (requestError) {
      setError(requestError.message);
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <div className="container page-container">
      <div className="breadcrumbs"><Link to="/">Inicio</Link><i className="bi bi-chevron-right" /><Link to="/carrito">Mi carrito</Link><i className="bi bi-chevron-right" /><span>Finalizar compra</span></div>
      <SectionTitle eyebrow="UN PASO MÁS" title="Finalizar compra" />
      <div className="checkout-layout">
        <form className="checkout-form" onSubmit={submit}>
          <h3><span>1</span> Entrega</h3><p>¿A dónde enviamos tu compra?</p>
          <label htmlFor="address">Dirección de entrega</label>
          <textarea id="address" className="form-control" rows="3" maxLength={255} required value={address} onChange={(event) => setAddress(event.target.value)} placeholder="Calle, número, localidad y provincia" />
          <div className="checkout-note"><i className="bi bi-info-circle me-2" />Confirmá que la dirección esté completa para coordinar la entrega.</div>
          {error && <ErrorBanner message={error} />}
          <button className="btn btn-primary btn-lg mt-4" disabled={submitting || !cart.items?.length}>{submitting ? 'Confirmando pedido...' : 'Confirmar pedido'} <i className="bi bi-arrow-right ms-2" /></button>
          <p className="checkout-terms"><i className="bi bi-shield-check me-1" />Al confirmar, el pedido queda registrado en tu cuenta.</p>
        </form>
        <div className="checkout-summary">
          <h3>Tu compra</h3>
          {cart.items?.map((item) => <div className="checkout-product" key={item.idCarritoItem}><span>{item.nombreProducto} <small>× {item.cantidad}</small></span><strong>{formatPrice(item.subtotal)}</strong></div>)}
          <div className="summary-row mt-3"><span>Envío</span><strong className="text-success">Gratis</strong></div><hr /><div className="summary-total"><span>Total</span><strong>{formatPrice(cart.total)}</strong></div>
        </div>
      </div>
    </div>
  );
}

function OrdersPage() {
  const [orders, setOrders] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [searchParams] = useSearchParams();
  useEffect(() => {
    api('/pedidos?page=0&size=20&sort=fechaPedido,desc')
      .then((result) => setOrders(result.contenido || []))
      .catch((requestError) => setError(requestError.message))
      .finally(() => setLoading(false));
  }, []);

  return (
    <div className="container page-container">
      <div className="breadcrumbs"><Link to="/">Inicio</Link><i className="bi bi-chevron-right" /><span>Mis compras</span></div>
      <SectionTitle eyebrow="TU ACTIVIDAD" title="Mis compras" />
      <ErrorBanner message={error} />
      {searchParams.get('pedido') && <div className="alert alert-success"><i className="bi bi-check-circle me-2" />Pedido #{searchParams.get('pedido')} confirmado. Gracias por tu compra.</div>}
      {loading ? <Loading /> : orders.length ? (
        <div className="orders-list">{orders.map((order) => (
          <article className="order-card" key={order.idPedido}>
            <div className="order-head"><span>Pedido <strong>#{order.idPedido}</strong></span><span className={`order-status status-${String(order.estado).toLowerCase()}`}>{String(order.estado).replaceAll('_', ' ')}</span></div>
            <div className="order-meta"><span><i className="bi bi-calendar3 me-2" />{new Date(order.fechaPedido).toLocaleDateString('es-AR')}</span><span><i className="bi bi-person me-2" />{order.nombreCliente}</span></div>
            <div className="order-total"><span>Total del pedido</span><strong>{formatPrice(order.total)}</strong></div>
          </article>
        ))}</div>
      ) : <EmptyState icon="bi-box-seam" title="Todavía no hiciste compras" description="Cuando confirmes tu primer pedido, vas a poder seguirlo desde acá." action="Explorar productos" />}
    </div>
  );
}

function AdminPage() {
  const [products, setProducts] = useState([]);
  const [categories, setCategories] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [filter, setFilter] = useState('');
  const notify = useNotify();

  const load = useCallback(async () => {
    setLoading(true);
    setError('');
    try {
      const [productPage, categoryList] = await Promise.all([
        api('/admin/productos?page=0&size=100&sort=nombre,asc'),
        api('/admin/categorias'),
      ]);
      setProducts(productPage.contenido || []);
      setCategories(categoryList);
    } catch (requestError) {
      setError(requestError.message);
    } finally {
      setLoading(false);
    }
  }, []);
  useEffect(() => { load(); }, [load]);

  const toggleProduct = async (product) => {
    try {
      await api(`/admin/productos/${product.idProducto}/${product.activo ? 'desactivar' : 'activar'}`, { method: 'PATCH' });
      notify(product.activo ? 'Producto desactivado.' : 'Producto activado.');
      load();
    } catch (requestError) {
      notify(requestError.message, 'danger');
    }
  };

  const filtered = useMemo(() => products.filter((product) => product.nombre.toLowerCase().includes(filter.toLowerCase())), [products, filter]);
  return (
    <div className="container page-container">
      <div className="breadcrumbs"><Link to="/">Inicio</Link><i className="bi bi-chevron-right" /><span>Administración</span></div>
      <SectionTitle eyebrow="GESTIÓN DE LA TIENDA" title="Panel administrador" />
      <div className="admin-stats"><div><span className="admin-stat-icon"><i className="bi bi-box-seam" /></span><span><small>Productos</small><strong>{products.length}</strong></span></div><div><span className="admin-stat-icon"><i className="bi bi-grid" /></span><span><small>Categorías</small><strong>{categories.length}</strong></span></div><div><span className="admin-stat-icon"><i className="bi bi-check2-circle" /></span><span><small>Activos</small><strong>{products.filter((item) => item.activo).length}</strong></span></div></div>
      <div className="admin-table-head"><div><h3>Productos</h3><p>Administrá la visibilidad de los productos del catálogo.</p></div><input className="form-control" placeholder="Filtrar productos..." value={filter} onChange={(event) => setFilter(event.target.value)} /></div>
      <ErrorBanner message={error} onRetry={load} />
      {loading ? <Loading /> : (
        <div className="table-responsive admin-table-wrap">
          <table className="table align-middle admin-table"><thead><tr><th>Producto</th><th>Categoría</th><th>Precio</th><th>Stock</th><th>Estado</th><th className="text-end">Acción</th></tr></thead><tbody>
            {filtered.map((product) => <tr key={product.idProducto}><td><div className="admin-product"><img src={product.imagenUrl || placeholder} alt="" /><strong>{product.nombre}</strong></div></td><td>{product.nombreCategoria}</td><td>{formatPrice(product.precio)}</td><td>{product.stock}</td><td><span className={`badge ${product.activo ? 'text-bg-success' : 'text-bg-secondary'}`}>{product.activo ? 'Activo' : 'Inactivo'}</span></td><td className="text-end"><button className={`btn btn-sm ${product.activo ? 'btn-outline-danger' : 'btn-outline-success'}`} onClick={() => toggleProduct(product)}>{product.activo ? 'Desactivar' : 'Activar'}</button></td></tr>)}
            {!filtered.length && <tr><td colSpan="6" className="text-center text-muted py-5">No hay productos para mostrar.</td></tr>}
          </tbody></table>
        </div>
      )}
    </div>
  );
}

function NotFoundPage() {
  return <div className="container page-container"><EmptyState icon="bi-compass" title="Esta página no está disponible" description="Volvé al inicio y seguí explorando el catálogo." action="Ir al inicio" /></div>;
}

export default App;
