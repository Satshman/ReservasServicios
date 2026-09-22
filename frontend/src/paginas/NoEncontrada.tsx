import { useEffect } from 'react';
import { Link } from 'react-router-dom';

export function NoEncontrada() {
  useEffect(() => {
    document.title = 'Página no encontrada — ReservaYa';
  }, []);

  return (
    <div className="pagina pagina--estrecha">
      <h1>No encontramos esa página</h1>
      <p className="texto-apoyo">
        La dirección no existe o cambió. Vuelve al catálogo para seguir explorando servicios.
      </p>
      <Link className="boton boton--principal" to="/">
        Ir al catálogo
      </Link>
    </div>
  );
}
