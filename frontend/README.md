# ReservaYa — Frontend

Interfaz web del MVP de **ReservaYa**, la plataforma de reservas de servicios. Es una SPA en
**React 19 + TypeScript + Vite** que consume la API REST versionada del backend
(`/api/v1`). No usa librerías de UI: todo el estilo vive en una única hoja
`src/estilos.css`, pensada para cumplir **WCAG 2.2 nivel AA** en tema claro y oscuro.

## Requisitos

- Node.js 20 o superior (probado con Node 22).
- Acceso al backend, desplegado en Render o levantado en local.

## Puesta en marcha

```bash
cd frontend
cp .env.example .env     # opcional: sin .env se usa el backend de Render
npm install
npm run dev              # http://localhost:5173
```

| Comando | Qué hace |
|---------|----------|
| `npm run dev` | Servidor de desarrollo con recarga en caliente (puerto 5173). |
| `npm run build` | Verificación de tipos (`tsc -b`) y empaquetado de producción en `dist/`. |
| `npm run preview` | Sirve `dist/` para revisar el build localmente. |
| `npm run typecheck` | Solo la verificación de tipos. |

## Variables de entorno

| Variable | Descripción | Valor por defecto |
|----------|-------------|-------------------|
| `VITE_API_URL` | URL base de la API, **incluido** el prefijo de versión. | `https://reservaya-vvke.onrender.com/api/v1` |

La plantilla está en `.env.example`. Para desarrollo contra el backend local levantado con
`podman-compose`, usa `VITE_API_URL=http://localhost:8081/api/v1`.

> El backend restringe CORS a los orígenes de `APP_CORS_ALLOWED_ORIGINS`. En desarrollo el
> perfil `dev` ya permite `http://localhost:5173`. Para el despliegue hay que **añadir el
> origen del sitio estático** a esa variable en Render; si no, el navegador bloqueará las
> peticiones.

## Páginas y endpoints

| Ruta | Pantalla | Endpoints que consume |
|------|----------|-----------------------|
| `/` | Catálogo público de servicios con filtro por categoría | `GET /servicios`, `GET /servicios?idCategoria=`, `GET /catalogos/categorias` |
| `/servicios/:id` | Ficha del servicio y disponibilidad por rango de fechas | `GET /servicios/{id}`, `GET /servicios/{id}/disponibilidad?desde=&hasta=`, `POST /reservas` |
| `/ingresar` | Inicio de sesión | `POST /auth/login`, `POST /auth/verificacion/reenvio` |
| `/registro` | Alta de cliente o proveedor (con servicios) | `POST /auth/registro`, `GET /catalogos/categorias` |
| `/verificacion` | Verificación por token y reenvío | `GET /auth/verificacion?token=`, `POST /auth/verificacion/reenvio` |
| `/panel/cliente` | Reservas próximas e historial | `GET /reservas/mias` |
| `/panel/proveedor` | Servicios, agenda y recursos | `GET`/`POST /servicios`, `GET`/`POST /servicios/{id}/horarios`, `PUT`/`DELETE /horarios/{id}`, `GET`/`POST /recursos`, `PUT /servicios/{id}/recursos`, `GET /catalogos/tipos-recurso` |

## Decisiones de interfaz

- **Carga, vacío y error en todas las pantallas.** Están centralizados en
  `src/componentes/Estados.tsx` (`Cargando`, `Vacio`, `CajaError`).
- **Errores trazables.** Cada respuesta de error sigue el contrato
  `{errorCode, message, details, traceId}`. Se muestra `message` al usuario y el `traceId`
  queda visible en pequeño bajo el mensaje, para poder cruzarlo con los logs del backend.
- **409 con sugerencias.** Cuando `POST /reservas` responde `TURNO_SIN_CUPO` o
  `RECURSO_NO_DISPONIBLE`, los turnos de `details` se pintan como botones que reservan la
  alternativa con un clic.
- **Conflicto de agenda.** Editar o eliminar un bloque que afecta reservas confirmadas
  devuelve `409 HORARIO_CON_RESERVAS` con la lista en `details`. La interfaz abre un diálogo
  modal con esas reservas y solo al confirmar repite la llamada con `?confirmar=true`.
- **Sesión.** El token se guarda en `localStorage` con `try/catch` (modo privado o
  almacenamiento bloqueado no rompen la app) y caduca solo al vencer `expiresIn`. Un 401 en
  una llamada protegida cierra la sesión y lleva a `/ingresar` con un mensaje explícito y el
  destino al que volver.
- **Volver al turno tras ingresar.** Si se pulsa «Reservar» sin sesión, el turno viaja en el
  estado de navegación y la reserva se confirma automáticamente al volver del login.
- **Id de proveedor.** La API no expone `GET /proveedores/me` ni incluye el id de proveedor
  en el token (solo `sub` y `rol`). El panel lo descubre una vez con una prueba de lectura
  sobre `GET /servicios/{id}/reservas` —que responde 403 a quien no es dueño— y lo memoriza
  en la sesión (`src/api/proveedor.ts`).

## Accesibilidad

- `lang="es"` en el documento y enlace «Saltar al contenido principal».
- Landmarks `header`, `nav`, `main` y `footer`; `main` recibe el foco desde el salto.
- Todas las etiquetas van asociadas por `htmlFor`/`id` (`src/componentes/Campo.tsx`), con
  `aria-describedby` para ayudas y errores y `aria-invalid` en los campos con error.
- Anuncios asíncronos en regiones `aria-live="polite"`; los errores usan `role="alert"`.
- Foco visible de 3 px en todo elemento interactivo y diálogo modal con foco atrapado,
  cierre con `Escape` y devolución del foco al disparador.
- Objetivos táctiles de al menos 44 px, diseño fluido de 360 px a escritorio, navegación
  plegable en móvil y sin desplazamiento horizontal.
- Paleta con contraste AA en claro y oscuro (`prefers-color-scheme`), y respeto a
  `prefers-reduced-motion`.

## Despliegue en Render (sitio estático)

| Ajuste | Valor |
|--------|-------|
| Root Directory | `frontend` |
| Build Command | `npm ci && npm run build` |
| Publish Directory | `dist` |
| Variable de entorno | `VITE_API_URL=https://reservaya-vvke.onrender.com/api/v1` |

Al ser una SPA con rutas del lado del cliente hay que añadir una regla de reescritura
`/*` → `/index.html` (tipo *Rewrite*). El archivo `static.json` de esta carpeta deja esa
regla documentada.

Después del primer despliegue, añade la URL pública del sitio a
`APP_CORS_ALLOWED_ORIGINS` en el servicio del backend.

## Fuera de alcance

Cancelar reservas y los reportes de ocupación no existen todavía en el backend
(`docs/modelo-datos-y-paquetes.md` §5), así que tampoco están en la interfaz. Tampoco hay
MFA de administrador ni refresh de token: el acceso caduca a los 30 minutos y se vuelve a
pedir el inicio de sesión.
