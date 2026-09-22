import { useEffect, useState } from 'react';
import { Link, NavLink, Outlet, useLocation, useNavigate } from 'react-router-dom';
import { useSesion } from '../sesion/useSesion';

interface Enlace {
  a: string;
  texto: string;
}

export function Disposicion() {
  const { sesion, cerrarSesion } = useSesion();
  const navegar = useNavigate();
  const ubicacion = useLocation();
  const [menuAbierto, setMenuAbierto] = useState(false);

  // La navegación móvil se pliega al cambiar de página.
  useEffect(() => {
    setMenuAbierto(false);
  }, [ubicacion.pathname]);

  const enlaces: Enlace[] = [{ a: '/', texto: 'Catálogo' }];
  if (sesion?.rol === 'CLIENTE') enlaces.push({ a: '/panel/cliente', texto: 'Mis reservas' });
  if (sesion?.rol === 'PROVEEDOR') enlaces.push({ a: '/panel/proveedor', texto: 'Mi negocio' });
  if (!sesion) {
    enlaces.push({ a: '/ingresar', texto: 'Ingresar' }, { a: '/registro', texto: 'Crear cuenta' });
  }

  return (
    <>
      <a className="salto-contenido" href="#contenido">
        Saltar al contenido principal
      </a>
      <header className="cabecera">
        <div className="cabecera__interior">
          <Link className="marca" to="/">
            <span className="marca__logo" aria-hidden="true">
              RY
            </span>
            <span className="marca__nombre">ReservaYa</span>
          </Link>

          <button
            type="button"
            className="boton-menu"
            aria-expanded={menuAbierto}
            aria-controls="navegacion-principal"
            onClick={() => setMenuAbierto((abierto) => !abierto)}
          >
            {menuAbierto ? 'Cerrar menú de navegación' : 'Abrir menú de navegación'}
          </button>

          <nav
            id="navegacion-principal"
            className={`navegacion${menuAbierto ? ' navegacion--abierta' : ''}`}
            aria-label="Navegación principal"
          >
            <ul className="navegacion__lista">
              {enlaces.map((enlace) => (
                <li key={enlace.a}>
                  <NavLink
                    to={enlace.a}
                    end={enlace.a === '/'}
                    className={({ isActive }) =>
                      `navegacion__enlace${isActive ? ' navegacion__enlace--activo' : ''}`
                    }
                  >
                    {enlace.texto}
                  </NavLink>
                </li>
              ))}
            </ul>

            {sesion ? (
              <div className="sesion-activa">
                <p className="sesion-activa__usuario">
                  <span className="sesion-activa__etiqueta">Sesión iniciada como</span>
                  <strong>{sesion.email}</strong>
                  <span className="etiqueta-rol">{sesion.rol}</span>
                </p>
                <button
                  type="button"
                  className="boton boton--secundario"
                  onClick={() => {
                    cerrarSesion();
                    navegar('/', { replace: true });
                  }}
                >
                  Cerrar sesión
                </button>
              </div>
            ) : null}
          </nav>
        </div>
      </header>

      <main id="contenido" className="contenido" tabIndex={-1}>
        <Outlet />
      </main>

      <footer className="pie">
        <p>
          ReservaYa — proyecto académico CodeF@ctory UdeA 2026-2. Plataforma de reservas de
          servicios.
        </p>
      </footer>
    </>
  );
}
