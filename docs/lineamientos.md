# CODEF@CTORY | Lineamientos Integrados para Proyectos de Software

**Documento consolidado · julio de 2026**

**Propósito:** Establecer un marco único, coherente y verificable para la formulación, construcción, evaluación y entrega de proyectos de CodeF@ctory UdeA, diferenciando los niveles básico y avanzado y evitando duplicidades entre criterios y lineamientos.

**Versión consolidada:** 2.0

**Fecha de consolidación:** 30 de julio de 2026

---

## Control del Documento

### Información del Documento

* **Solicitado por:** Dirección de Fábrica-escuela
* **Preparado por:** Catalina Céspedes
* **Revisado por:** Diego Botia, Wilmer Gil, Edison Montoya, Gina Maestre, Luz Viviana Cobaleda, Freddy Gutiérrez
* **Versión del documento:** V2
* **Fecha del documento:** 30/07/2026

### Historial de Versiones

| Versión | Objetivo de la actualización | Fecha | Autor |
| --- | --- | --- | --- |
| **V2** | Consolidar las siguientes fuentes y lineamientos con los documentos de criterios de Proyectos:<br>

<br>• `Criterios_Proyecto_Basico`<br>

<br>• `criterios_GeneracionProyecto_Avanzado`<br>

<br>• `LineamientosBD - V 3.0`<br>

<br>• `Lineamientos de Desarrollo Seguro`<br>

<br>• `Lineamientos DevOps_V4.0`<br>

<br>• `Lineamientos UI/UX-v4.0`<br>

<br>• `Documento Arquitectura de la Solución - V 4.0` | 30/07/2026 | Catalina Céspedes |

### Principios Aplicados para la Consolidación

* Separar claramente las exigencias del proyecto básico y del avanzado.
* Conservar los requisitos evaluables de los criterios y retirar su repetición de los lineamientos técnicos.
* Resolver contradicciones aplicando el requisito más específico, más reciente o exigente, sin aumentar de forma arbitraria el alcance académico.
* Expresar las obligaciones con lenguaje uniforme: **debe** (obligatorio), **debería** (recomendado) y **puede** (opcional).
* Favorecer decisiones tecnológicas justificadas, trazabilidad, seguridad desde el diseño, automatización y mejora continua.

---

## Tabla de Contenido

1. [Disposiciones generales](https://www.google.com/search?q=%231-disposiciones-generales)
* [1.1 Objeto y alcance](https://www.google.com/search?q=%2311-objeto-y-alcance)
* [1.2 Orden de aplicación](https://www.google.com/search?q=%2312-orden-de-aplicaci%C3%B3n)
* [1.3 Perfiles tecnológicos armonizados](https://www.google.com/search?q=%2313-perfiles-tecnol%C3%B3gicos-armonizados)


2. [Definiciones para el proyecto de EQUIPOS BÁSICOS](https://www.google.com/search?q=%232-definiciones-para-el-proyecto-de-equipos-b%C3%A1sicos)
* [2.1 Propósito y alcance](https://www.google.com/search?q=%2321-prop%C3%B3sito-y-alcance)
* [2.2 Alcance mínimo del proyecto](https://www.google.com/search?q=%2322-alcance-m%C3%ADnimo-del-proyecto)
* [2.3 Arquitectura, datos e integración](https://www.google.com/search?q=%2323-arquitectura-datos-e-integraci%C3%B3n)
* [2.4 Gestión del proyecto y del equipo](https://www.google.com/search?q=%2324-gesti%C3%B3n-del-proyecto-y-del-equipo)
* [2.5 Evidencias por hito](https://www.google.com/search?q=%2325-evidencias-por-hito)


3. [Definiciones para el EQUIPO AVANZADO](https://www.google.com/search?q=%233-definiciones-para-el-equipo-avanzado)
* [3.1 Alcance funcional y arquitectura](https://www.google.com/search?q=%2331-alcance-funcional-y-arquitectura)
* [3.2 Persistencia y modelado](https://www.google.com/search?q=%2332-persistencia-y-modelado)
* [3.3 API backend robusta](https://www.google.com/search?q=%2333-api-backend-robusta)
* [3.4 Seguridad exigida](https://www.google.com/search?q=%2334-seguridad-exigida)
* [3.5 Calidad y pruebas](https://www.google.com/search?q=%2335-calidad-y-pruebas)
* [3.6 Gestión del proyecto y del equipo](https://www.google.com/search?q=%2336-gesti%C3%B3n-del-proyecto-y-del-equipo)
* [3.7 Entregables por curso y sprint](https://www.google.com/search?q=%2337-entregables-por-curso-y-sprint)


4. [Lineamientos de arquitectura de la solución](https://www.google.com/search?q=%234-lineamientos-de-arquitectura-de-la-soluci%C3%B3n)
* [4.1 Arquitectura y vistas](https://www.google.com/search?q=%2341-arquitectura-y-vistas)
* [4.2 Modelo por capas](https://www.google.com/search?q=%2342-modelo-por-capas)
* [4.3 Requisitos no funcionales](https://www.google.com/search?q=%2343-requisitos-no-funcionales)
* [4.4 Decisiones y documentación](https://www.google.com/search?q=%2344-decisiones-y-documentaci%C3%B3n)


5. [Lineamientos de bases de datos](https://www.google.com/search?q=%235-lineamientos-de-bases-de-datos)
* [5.1 Análisis y modelado](https://www.google.com/search?q=%2351-an%C3%A1lisis-y-modelado)
* [5.2 Selección y uso de tecnologías](https://www.google.com/search?q=%2352-selecci%C3%B3n-y-uso-de-tecnolog%C3%ADas)
* [5.3 Rendimiento, seguridad y operación](https://www.google.com/search?q=%2353-rendimiento-seguridad-y-operaci%C3%B3n)
* [5.4 Lógica en la base de datos](https://www.google.com/search?q=%2354-l%C3%B3gica-en-la-base-de-datos)


6. [Lineamientos de desarrollo seguro](https://www.google.com/search?q=%236-lineamientos-de-desarrollo-seguro)
* [6.1 Seguridad durante el ciclo de vida](https://www.google.com/search?q=%2361-seguridad-durante-el-ciclo-de-vida)



---

## 1. Disposiciones Generales

### 1.1 Objeto y Alcance

Este documento define los requisitos mínimos, lineamientos técnicos y evidencias esperadas para los proyectos de software desarrollados por los equipos básico y avanzado. Se aplica desde la selección del reto y la definición del alcance hasta el despliegue, la demostración y el cierre del proyecto.

### 1.2 Orden de Aplicación

| Prioridad | Regla |
| --- | --- |
| **1** | Aplicar primero los criterios del nivel del equipo: básico o avanzado. |
| **2** | Aplicar después los lineamientos de arquitectura, datos, seguridad, DevOps y UI/UX que correspondan al alcance. |
| **3** | Cuando exista una diferencia tecnológica, prevalece el perfil definido para el nivel. Cualquier excepción debe justificarse y aprobarse. |
| **4** | Cuando dos requisitos cuantitativos difieran, se adopta el umbral más exigente, salvo decisión formal del equipo docente. |

### 1.3 Perfiles Tecnológicos Armonizados

| Aspecto | Proyecto Básico | Proyecto Avanzado |
| --- | --- | --- |
| **Propósito** | Aplicación web completa, con mínimo el 60% de desarrollo propio. | Backend robusto integrado a base de datos, con arquitectura, seguridad, calidad y operación verificables. |
| **Frontend** | Código del frontend en React (Ideal usar TypeScript). Puede complementarlo con el framework NEXTJS. Prototipos en Figma. | N/A |
| **Backend** | Spring Boot 3.X o superior (JDK 11 o superior). | Spring Boot 3.X o superior (JDK 11 o superior). |
| **Datos** | PostgreSQL. | PostgreSQL; Supabase o Neon como opción de servicio administrado. |
| **API** | Integración básica entre capas según el diseño. | REST o GraphQL, con contratos bien definidos, versionado (solo en el caso de REST), validación y documentación. |
| **Despliegue** | Render para el despliegue funcional acordado por los cursos. | Render mínimo en nube con contenedores (Ejemplo Docker); evolución opcional a orquestación con Kubernetes. |
| **Gestión del proyecto** | Azure DevOps. | Azure DevOps. |

---

## 2. Definiciones para el Proyecto de EQUIPOS BÁSICOS

### 2.1 Propósito y Alcance

El equipo debe seleccionar una idea del banco de proyectos y definir un producto de software que sea acorde al contexto de negocio, el problema y el valor esperado. El resultado será una aplicación web, con complejidad superior a un CRUD aislado.

### 2.2 Alcance Mínimo del Proyecto

1. Documentar el proceso principal mediante diagramas UML o BPMN. El proceso debe incluir al menos tres estados e incluir validaciones de reglas de negocio.
2. Definir al menos dos roles de usuario con responsabilidades diferenciadas.
3. Construir el User Story Mapping (USM) de la primera visión del producto, agrupando las historias por épicas (*epics*), características (*features*) e historias de usuario (*user story*).
4. Escribir e implementar progresivamente las historias de usuario (HU) comprometidas en cada sprint, detallando los criterios de aceptación.
5. Integrar las características de seguridad y accesibilidad en el producto final.

### 2.3 Arquitectura, Datos e Integración

El equipo debe diseñar la arquitectura y sus diagramas, modelar los datos para garantizar persistencia e implementar la integración entre las capas. El perfil de referencia utiliza React en el frontend y Spring Boot en el backend. Las decisiones deben ser coherentes con el proceso principal y comprobables en el producto.

### 2.4 Gestión del Proyecto y del Equipo

* Product Backlog priorizado con épicas, características e historias de usuario.
* Plan de releases; acuerdos de equipo; Definition of Ready (DoR) y Definition of Done (DoD).
* Sprint Planning con desglose de tareas y trazabilidad en el tablero.
* Retrospectivas con acciones de mejora documentadas y verificadas.
* Evaluación SAMM al inicio y al cierre.
* Dashboard con burndown del proyecto y de cada sprint, y al menos dos métricas de flujo, calidad o predictibilidad diferentes del simple conteo comprometido/terminado.
* Lecciones aprendidas y cierre formal del proyecto.

### 2.5 Evidencias por Hito

| Sprint | Evidencia Funcional | Evidencia Transversal |
| --- | --- | --- |
| **1** | Demostración de, al menos, tres historias del proceso principal. | **AYD1:** User Story Mapping, historias de usuario, prototipo, código frontend.<br>

<br>**AYD2:** Modelo de procesos (BPMN), modelo entidad-relación (MER), UML de despliegue y código backend.<br>

<br>**Gestión de Proyectos:** Gestión del proyecto y del producto en Azure DevOps. |
| **2** | Demostración con un incremento de, al menos, cuatro historias de usuario, incluyendo las HU de seguridad. | **AYD1:** User Story Mapping, historias de usuario, prototipo, código frontend.<br>

<br>**AYD2:** Modelo de procesos (BPMN), modelo entidad-relación (MER), UML de despliegue y código backend.<br>

<br>**Gestión de Proyectos:** Gestión del proyecto y del producto en Azure DevOps. |
| **3** | Demostración con un incremento de, al menos, cuatro historias de usuario, incluyendo las HU de accesibilidad. | **AYD1:** User Story Mapping, historias de usuario, prototipo, código frontend.<br>

<br>**AYD2:** Modelo de procesos (BPMN), modelo entidad-relación (MER), UML de despliegue y código backend.<br>

<br>**Gestión de Proyectos:** Gestión del proyecto y del producto en Azure DevOps. |

> **Criterio de aceptación:** En cada hito se evalúa el avance integrado del proyecto, no únicamente documentos o componentes aislados. La demostración del producto software debe evidenciar valor funcional y trazabilidad con las historias comprometidas.

---

## 3. Definiciones para el EQUIPO AVANZADO

### 3.1 Alcance Funcional y Arquitectura

* Desarrollar un backend con integración a base de datos, organizado en dominios o módulos de negocio y, al menos, un módulo transversal: autenticación y autorización, auditoría, logging u observabilidad.
* Implementar reglas de negocio que no se reduzcan a operaciones CRUD, con autorización RBAC y control de permisos en los endpoints.
* Documentar la arquitectura con C4 (contexto y contenedores; componentes cuando aplique) o UML equivalente (paquetes, componentes y despliegue).
* Justificar el estilo arquitectónico seleccionado, la persistencia, la seguridad, el manejo de errores, el versionado de API (si usa REST APIs) y la estrategia de pruebas.
* Mantener una separación explícita mediante arquitectura limpia, hexagonal, por capas, monolito modular o un microservicio claramente delimitado.
* Definir y publicar el contrato: OpenAPI/Swagger versionado para REST o esquema (SDL) para GraphQL.

### 3.2 Persistencia y Modelado

* Elaborar modelos lógico y físico normalizados.
* Implementar auditoría y trazabilidad de cambios relevantes.
* Definir índices para consultas clave y justificar su aporte.
* Implementar consultas no triviales: *joins*, agregaciones o filtros compuestos.
* Incluir al menos un procedimiento almacenado cuando sea coherente con la distribución de responsabilidades definida en la arquitectura.
* Puede incorporarse caché (por ejemplo, Redis o Memcached) cuando exista una necesidad demostrada.

### 3.3 API Backend Robusta

* Exponer endpoints de diferentes dominios o módulos.
* Para REST, aplicar principios de diseño orientado a recursos; cuando el proyecto adopte HATEOAS, incluir enlaces de navegación pertinentes y no decorativos.
* Validar payloads y devolver errores uniformes con códigos HTTP correctos y campos como `errorCode`, `message`, `details` y `traceId`.
* Para GraphQL, implementar queries y mutaciones; las subscriptions son opcionales y deben responder a una necesidad de tiempo real.
* Versionar el contrato y aplicar una política de compatibilidad y retiro.
* Generar logs estructurados en JSON o en otro formato consistente.
* Desplegar en nube, como mínimo con backend y base de datos contenerizados mediante Docker Compose en Render o plataforma equivalente. Kubernetes constituye la evolución opcional esperada acorde al desarrollo del alcance académico del curso.

### 3.4 Seguridad Exigida

* Exigir MFA para accesos administrativos o sensibles; extenderlo a otros usuarios según el análisis de riesgo.
* Aplicar políticas de contraseña seguras, bloqueo ante intentos fallidos y controles contra credenciales comprometidas. No exigir cambios periódicos sin indicio de compromiso.
* Implementar revocación, expiración y rotación segura de tokens; usar cookies `SameSite`, `HttpOnly` y `Secure` cuando corresponda.
* Usar OIDC u OAuth 2.0 cuando se integre un proveedor de identidad autorizado.
* Proteger APIs contra replay, validar esquemas y restringir CORS.
* Aplicar mínimo privilegio, RBAC por endpoint y reglas ABAC simples cuando la propiedad o el contexto determinen el acceso.
* Validar entradas, prevenir inyecciones, proteger secretos y registrar eventos de seguridad.
* Realizar SCA, revisión de código seguro y gestión de vulnerabilidades con tiempos de remediación definidos.

### 3.5 Calidad y Pruebas

| Dimensión | Criterio Mínimo |
| --- | --- |
| **Análisis estático** | Informe de SonarCloud o SonarQube y Quality Gate activo. |
| **Cobertura unitaria** | Mayor o igual al 65 %. |
| **Deuda técnica** | Tiempo de remediación máximo de dos días o valor inferior equivalente. |
| **Complejidad Ciclomática** | Menor que 50; además, revisar la complejidad cognitiva. |
| **Severidad** | Minor o mejor, conforme a la escala configurada. |
| **Vulnerabilidades** | Cero vulnerabilidades críticas. |
| **Historias** | Criterios de aceptación refinados en Gherkin. |
| **Automatización** | Pruebas unitarias, integración con base de datos real o contenedor y aceptación del backend. |

### 3.6 Gestión del Proyecto y del Equipo

* Product Backlog priorizado con épicas, características (*features*) e historias (HU).
* Plan de releases; acuerdos de equipo; Definition of Ready y Definition of Done.
* Sprint Planning con desglose de tareas y trazabilidad en el tablero.
* Retrospectivas con acciones de mejora documentadas y verificadas.
* Evaluación SAMM al inicio y al cierre.
* Dashboard con burndown del proyecto y de cada sprint, y al menos dos métricas de flujo, calidad o predictibilidad diferentes del simple conteo comprometido/terminado.
* Lecciones aprendidas y cierre formal del proyecto.

### 3.7 Entregables por Curso y Sprint

#### Arquitectura de Software

* **Sprint 1:** Diagrama de Paquetes y componentes con interfaces; estilo preliminar; proyecto base Spring Boot en GitHub; al menos una HU implementada; despliegue inicial. Mínimo 3 ADR priorizados.
* **Sprint 2:** Diagrama de despliegue; APIs priorizadas; al menos tres APIs REST o GraphQL; CI/CD inicial con GitHub Actions; identificación de vulnerabilidades con OWASP Top Ten; documentación de las APIs.
* **Sprint 3:** Refinamiento de APIs; contenedores y orquestación cuando aplique; observabilidad con Grafana/Prometheus; pruebas de integración; aseguramiento mediante tokens.

#### Bases de Datos

* **Sprint 1:** Entidades y relaciones; preguntas/consultas clave; modelo lógico; modelo físico inicial.
* **Sprint 2:** MER refinado; modelo físico completo con claves y restricciones; script de estructuras; consultas de los dos primeros sprints; estimación de volumen; roles y esquema de seguridad.
* **Sprint 3:** MER y script refinados; triggers y procedimientos coherentes con las HU; actualización del análisis de volumen.

#### Calidad de Software

* **Sprint 1:** Plan de aseguramiento de calidad; plan de pruebas; casos de prueba y Gherkin para las HU priorizadas.
* **Sprint 2:** Pruebas unitarias con AAA; análisis de cobertura; Gherkin y casos positivos/excepcionales; ejecución y gestión de defectos; Sonar y Quality Gate; análisis de cobertura, complejidad y deuda.
* **Sprint 3:** Automatización de criterios de aceptación y ejecución E2E sobre el objeto de prueba.

#### Gestión de Proyectos

* **Sprint 1:** Proyecto y backlog en Azure; acuerdos de equipo, DoR y DoD; sprint backlog con tareas (cada tarea incluye responsable y estimación); diagnóstico SAMM inicial.
* **Sprint 2:** Plan de entregas; sprint backlog actualizado; métricas ágiles; evidencia de realización de retrospectiva con el equipo y resultados y acciones definidas.
* **Sprint 3:** Cierre adecuado del proyecto en Azure; análisis de métricas y mejoras; SAMM final; lecciones aprendidas.

---

## 4. Lineamientos de Arquitectura de la Solución

### 4.1 Arquitectura y Vistas

La arquitectura debe describir la solución actual y las decisiones necesarias para cumplir los atributos de calidad. Debe incluir, según el nivel, contexto, contenedores, componentes, despliegue, datos y seguridad. Los diagramas deben coincidir con el código y la infraestructura desplegada.

| Componente | Responsabilidad Consolidada |
| --- | --- |
| **Cliente web** | Presentar las funciones, validar la interacción y ofrecer una experiencia accesible y responsiva. |
| **Gestión de identidad y acceso** | Centralizar autenticación, autorización, roles y permisos cuando el alcance lo justifique. |
| **API Gateway** *(cuando aplique)* | Punto de entrada, validación, políticas de seguridad, enrutamiento, balanceo de carga, telemetría y desacoplamiento. |
| **Frontend** | Componentes web reutilizables, gestión de estado, navegación, manejo de errores y comunicación con backend. |
| **Backend** | Reglas de negocio, Gestión de entidades, servicios de aplicación, controladores/resolvers, validación, autorización y persistencia. |
| **Base de datos transaccional** | Persistencia confiable, integridad, rendimiento, auditoría y acceso restringido. |
| **Repositorio de recursos** | Almacenamiento controlado de imágenes, documentos y otros recursos, evitando incluir binarios innecesarios en el código. |
| **Observabilidad** | Logs, métricas, trazas, dashboards y alertas accionables. |
| **Plataforma DevOps** | Construcción, pruebas, análisis, empaquetado, despliegue y promoción automatizados. |

### 4.2 Modelo por Capas

* **Presentación:** Componentes accesibles y responsivos, gestión de estado controlada y manejo visible de estados de carga, vacío y error.
* **Aplicación:** Casos de uso y coordinación de operaciones, sin acoplar la lógica de negocio al transporte.
* **Dominio:** Reglas, entidades y políticas independientes de frameworks cuando el estilo adoptado lo permita.
* **Infraestructura:** Persistencia, mensajería, proveedores técnicos y adaptadores.
* **Integración:** Contratos versionados, validación, idempotencia cuando aplique y manejo uniforme de errores.

### 4.3 Requisitos No Funcionales

| Atributo | Lineamiento |
| --- | --- |
| **Escalabilidad** | Diseñar componentes sin estado cuando sea viable, identificar cuellos de botella y permitir escalamiento horizontal o vertical según evidencia. Kubernetes se usa solo cuando su complejidad es justificable. |
| **Rendimiento** | Como línea base académica, soportar 200 solicitudes por minuto y respuesta menor o igual a 30 segundos. El equipo debe definir objetivos más exigentes por operación y validarlos con pruebas representativas. |
| **Disponibilidad** | Definir un objetivo de disponibilidad; cuando se adopte 99,9 %, acompañarlo de monitoreo, redundancia y procedimientos de recuperación. |
| **Mantenibilidad** | Modularidad, responsabilidades claras, reglas parametrizables, pruebas automatizadas y documentación viva. |
| **Interoperabilidad** | Contratos estables, formatos estándar, versionado (cuando aplique) y bajo acoplamiento. |
| **Usabilidad y accesibilidad** | Interacción comprensible, consistente, eficiente y conforme con el objetivo WCAG 2.2 nivel AA. |
| **Trazabilidad** | Correlacionar requisitos, historias, cambios, pruebas, despliegues y eventos operativos. |

### 4.4 Decisiones y Documentación

Las decisiones relevantes deben registrarse mediante ADR u otro mecanismo equivalente. Cada decisión incluirá contexto, alternativas, elección, consecuencias, responsable y fecha. La documentación de arquitectura se actualizará en el mismo sprint en que cambie la solución.

---

## 5. Lineamientos de Bases de Datos

### 5.1 Análisis y Modelado

* Identificar datos, actores, entidades, relaciones, reglas, restricciones y requisitos no funcionales del caso de negocio.
* Construir modelos conceptual, lógico y físico y mantener trazabilidad con las historias de usuario.
* Integrar los modelos de los módulos para evitar duplicidad semántica y garantizar una fuente de verdad coherente.
* Normalizar hasta el nivel apropiado; toda desnormalización debe justificarse mediante necesidades de lectura o rendimiento.
* Definir claves, restricciones, dominios, nulabilidad, integridad referencial y convenciones de nombres.

### 5.2 Selección y Uso de Tecnologías

PostgreSQL es la base relacional de referencia para operaciones transaccionales. Una bodega de datos, caché, Kafka u otra tecnología especializada solo se incorpora cuando el caso de uso y la capacidad operativa lo justifican. Las integraciones externas —incluidas APIs geográficas— en principio no forman parte del alcance básico ni avanzado actual, salvo que el reto seleccionado para el proyecto lo requiera.

### 5.3 Rendimiento, Seguridad y Operación

* Identificar tablas y consultas críticas, estimar volúmenes y medir planes de ejecución.
* Crear índices a partir de patrones reales de consulta; evitar índices redundantes y validar su costo de escritura.
* Evaluar particionamiento, almacenamiento y capacidad únicamente cuando el volumen lo requiera.
* Aplicar paginación determinista y limitar consultas costosas.
* Usar cuentas de servicio con mínimo privilegio, separar funciones administrativas y rotar credenciales gestionadas como secretos.
* Usar consultas parametrizadas u ORM seguro; nunca concatenar entrada del usuario en SQL.
* Cifrar comunicaciones con TLS y proteger respaldos y datos sensibles en reposo.
* Definir respaldo, restauración, migraciones versionadas y pruebas de recuperación.
* Registrar auditoría para acciones críticas sin almacenar secretos ni datos personales innecesarios en logs.

### 5.4 Lógica en la Base de Datos

Triggers y procedimientos almacenados deben responder a una decisión arquitectónica. Son apropiados para integridad, auditoría o procesamiento cercano a los datos; no deben duplicar reglas sin una fuente de verdad definida ni crear acoplamiento oculto entre módulos.

---

## 6. Lineamientos de Desarrollo Seguro

### 6.1 Seguridad Durante el Ciclo de Vida

| Fase | Prácticas Mínimas |
| --- | --- |
| **Definición** | Clasificar datos, identificar requisitos de seguridad, privacidad, roles y controles de acceso. |
| **Diseño** | Modelar amenazas; aplicar mínimo privilegio, segregación de funciones, defensa en profundidad y fallos seguros. |
| **Construcción** | Guías OWASP, validación de entradas, codificación segura, revisión por pares, SAST y detección de secretos. |
| **Dependencias** | Inventario y SCA; versiones soportadas; parches priorizados según criticidad y exposición. |
| **Pruebas** | Pruebas negativas, DAST, configuración, autorización y penetración proporcional al riesgo. |
| **Despliegue** | Entornos separados, configuración endurecida, secretos externos al código, TLS y artefactos trazables. |
| **Operación** | Monitoreo, respuesta a incidentes y actualización continua. |
