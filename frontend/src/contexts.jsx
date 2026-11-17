import { createContext, useCallback, useContext, useEffect, useMemo, useRef, useState } from 'react';
import { api, ApiError } from './api';

const AuthContext = createContext(null);
const CartContext = createContext(null);
const NoticeContext = createContext(null);

export function AuthProvider({ children }) {
  const [user, setUser] = useState(null);
  const [loading, setLoading] = useState(true);

  const refreshUser = useCallback(async () => {
    try {
      const current = await api('/usuarios/me');
      setUser(current);
      return current;
    } catch (error) {
      if (error instanceof ApiError && error.status === 401) {
        setUser(null);
        return null;
      }
      throw error;
    }
  }, []);

  useEffect(() => {
    refreshUser().catch(() => setUser(null)).finally(() => setLoading(false));
  }, [refreshUser]);

  const value = useMemo(() => ({ user, setUser, loading, refreshUser }), [user, loading, refreshUser]);
  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export function CartProvider({ children }) {
  const { user, loading: authLoading } = useAuth();
  const [cart, setCart] = useState({ items: [], total: 0 });
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const requestVersion = useRef(0);
  const updateCart = useCallback((value) => {
    requestVersion.current += 1;
    setCart(value);
    setError('');
  }, []);

  const refresh = useCallback(async () => {
    if (authLoading) {
      setLoading(true);
      return null;
    }
    if (!user || user.rol !== 'CLIENTE') {
      requestVersion.current += 1;
      setCart({ items: [], total: 0 });
      setError('');
      setLoading(false);
      return null;
    }
    const version = ++requestVersion.current;
    setLoading(true);
    setError('');
    try {
      const result = await api('/carrito');
      if (version === requestVersion.current) setCart(result);
      return result;
    } catch (requestError) {
      if (version === requestVersion.current) setError(requestError.message);
      throw requestError;
    } finally {
      if (version === requestVersion.current) setLoading(false);
    }
  }, [user, authLoading]);

  useEffect(() => {
    refresh().catch(() => {});
  }, [refresh]);

  const value = useMemo(() => ({ cart, setCart: updateCart, loading, error, refresh }), [cart, loading, error, refresh, updateCart]);
  return <CartContext.Provider value={value}>{children}</CartContext.Provider>;
}

export function NoticeProvider({ children }) {
  const [notice, setNotice] = useState(null);
  const notify = useCallback((message, tone = 'success') => {
    setNotice({ message, tone, id: Date.now() });
    window.setTimeout(() => setNotice(null), 4200);
  }, []);

  return (
    <NoticeContext.Provider value={notify}>
      {children}
      {notice && (
        <div className={`toast-note toast-${notice.tone}`} role="status">
          <i className={`bi ${notice.tone === 'danger' ? 'bi-exclamation-circle' : 'bi-check-circle'} me-2`} />
          {notice.message}
          <button type="button" className="btn-close ms-3" aria-label="Cerrar" onClick={() => setNotice(null)} />
        </div>
      )}
    </NoticeContext.Provider>
  );
}

export const useAuth = () => useContext(AuthContext);
export const useCart = () => useContext(CartContext);
export const useNotify = () => useContext(NoticeContext);
