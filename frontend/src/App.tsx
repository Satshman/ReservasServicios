import { BrowserRouter, Route, Routes } from 'react-router-dom';
import { Disposicion } from './componentes/Disposicion';
import { RutaProtegida } from './componentes/RutaProtegida';
import { Catalogo } from './paginas/Catalogo';
import { DetalleServicio } from './paginas/DetalleServicio';
import { Ingresar } from './paginas/Ingresar';
import { NoEncontrada } from './paginas/NoEncontrada';
import { PanelCliente } from './paginas/PanelCliente';
import { PanelProveedor } from './paginas/PanelProveedor';
import { Registro } from './paginas/Registro';
import { Verificacion } from './paginas/Verificacion';
import { ProveedorSesion } from './sesion/SesionContext';

export function App() {
  return (
    <BrowserRouter>
      <ProveedorSesion>
        <Routes>
          <Route element={<Disposicion />}>
            <Route path="/" element={<Catalogo />} />
            <Route path="/servicios/:id" element={<DetalleServicio />} />
            <Route path="/ingresar" element={<Ingresar />} />
            <Route path="/registro" element={<Registro />} />
            <Route path="/verificacion" element={<Verificacion />} />
            <Route
              path="/panel/cliente"
              element={
                <RutaProtegida rol="CLIENTE">
                  <PanelCliente />
                </RutaProtegida>
              }
            />
            <Route
              path="/panel/proveedor"
              element={
                <RutaProtegida rol="PROVEEDOR">
                  <PanelProveedor />
                </RutaProtegida>
              }
            />
            <Route path="*" element={<NoEncontrada />} />
          </Route>
        </Routes>
      </ProveedorSesion>
    </BrowserRouter>
  );
}
