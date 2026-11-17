# Ferretería Digital

Plataforma de comercio electrónico para una ferretería o corralón. Incluye una tienda responsiva en React y una API Spring Boot para catálogo, usuarios, carrito, pedidos, pagos, comprobantes e imágenes.

La aplicación está organizada como un backend Java con Spring Boot y una persistencia relacional en MySQL. La lógica de negocio se concentra en servicios independientes, con repositorios JPA, DTOs de entrada/salida, mappers y excepciones de dominio.

## Estado actual

La aplicación cuenta con una experiencia de compra de punta a punta y una API con reglas de negocio:

- El dominio y el modelo de datos principal están definidos como entidades JPA.
- Las operaciones de usuarios, catálogo, carrito, pedidos, pagos, comprobantes e imágenes están implementadas en la capa de servicios.
- La integración de imágenes utiliza Cloudinary.
- La gestión de imágenes tiene controladores REST disponibles.
- La tienda React permite buscar productos, explorar categorías, registrarse, iniciar sesión, gestionar el carrito y confirmar pedidos.
- La autenticación utiliza JWT firmado con HS256, transportado en una cookie `HttpOnly`; las APIs validan los roles en el servidor.
- Clientes y vendedores tienen formularios de alta separados, pero utilizan el mismo login con email y contraseña.
- Las solicitudes de cuenta vendedora requieren aprobación administrativa; las publicaciones quedan pendientes hasta que un administrador las apruebe.
- El panel de administración permite crear categorías y subcategorías para que los vendedores puedan clasificar sus productos.
- Los controladores y servicios registran inicio, resultado, duración y errores sin escribir argumentos, cuerpos, credenciales ni tokens en los logs.
- Cada solicitud HTTP recibe un `X-Request-ID`, incluido en los logs para correlacionar el recorrido por filtros, controladores y servicios.
- Las rutas de carrito y compra requieren un cliente autenticado y carrito no vacío; los recursos administrativos requieren el rol `ADMIN`.
- Docker Compose crea de manera idempotente dos administradores y dos clientes iniciales en MySQL con contraseñas BCrypt.
- La lógica de negocio cuenta con pruebas unitarias de servicios usando JUnit 5, Mockito y AssertJ.
- Docker Compose levanta MySQL, Adminer, backend y frontend juntos de forma predeterminada.

## Objetivo del proyecto

Construir una base sólida para un e-commerce de productos de ferretería que permita:

1. Administrar categorías, subcategorías y productos.
2. Registrar y administrar usuarios clientes y administradores.
3. Mantener un carrito por cliente y controlar cantidades contra el stock.
4. Confirmar pedidos y administrar sus estados.
5. Registrar y procesar pagos.
6. Emitir comprobantes internos asociados a pagos aprobados.
7. Gestionar imágenes de productos y fotografías de usuarios.
8. Responder errores de forma uniforme mediante una API de excepciones centralizada.

## Fases realizadas

### 1. Definición del dominio y modelo de datos

Se definieron las entidades principales del negocio y sus relaciones:

- `Usuario`, `UsuarioCliente`, `UsuarioVendedor` y `UsuarioAdmin`.
- `Categoria` y `Subcategoria`.
- `Producto`.
- `Carrito` y `CarritoItem`.
- `Pedido` y `DetallePedido`.
- `Pago`.
- `Comprobante`.

También se definieron enumeraciones para los roles y estados del sistema:

- Roles: `ADMIN`, `CLIENTE` y `VENDEDOR`.
- Estado de usuario.
- Estado de pedido.
- Estado de pago.

El detalle conceptual del modelo se encuentra en [Documentación del modelo de datos - Ferretería (Parte 1).docx](./Documentación%20del%20modelo%20de%20datos%20-%20Ferretería%20(Parte%201).docx).

### 2. Organización de la arquitectura backend

El backend se separó en capas:

- **Modelos**: entidades JPA persistentes.
- **Repositorios**: acceso a datos mediante Spring Data JPA.
- **Servicios**: reglas y casos de uso del negocio.
- **DTOs**: contratos de entrada y salida.
- **Mappers**: conversión entre entidades y DTOs.
- **Controladores**: entrada HTTP para los casos de uso expuestos.
- **Excepciones**: errores de negocio y respuestas HTTP uniformes.
- **Configuración**: seguridad de contraseñas, Cloudinary y aplicación.

### 3. Implementación de los casos de uso

Se implementaron servicios para:

- Usuarios y perfiles.
- Categorías y subcategorías.
- Productos y búsquedas paginadas.
- Carritos y sus ítems.
- Pedidos y cambios de estado.
- Pagos y comprobantes.
- Imágenes de usuarios y productos.

Las reglas importantes, como validación de duplicados, disponibilidad de productos, stock, propiedad de los recursos, estados válidos y transiciones de pedidos/pagos, se aplican en la capa de servicios.

### 4. Integración de imágenes

La carga y eliminación de imágenes se delega a Cloudinary. El backend configura explícitamente el soporte multipart del servlet, con un máximo de 5 MB por archivo y 6 MB por solicitud. El flujo contempla:

- Validación del tamaño máximo de archivos.
- Generación y almacenamiento de URL y `public_id`.
- Actualización de la imagen de perfil del usuario autenticado.
- Actualización o eliminación de la imagen de un producto por parte de un administrador.
- Eliminación diferida de imágenes anteriores al finalizar correctamente una transacción.
- Traducción de fallas de Cloudinary a respuestas HTTP controladas.

### 5. Validación mediante pruebas

Se agregaron pruebas unitarias para la lógica de servicios. Las pruebas utilizan mocks para repositorios y servicios externos, por lo que no dependen de una base de datos real ni de Cloudinary para validar las reglas principales.

En los resultados Maven disponibles en `backend/target/surefire-reports` se registran **175 pruebas ejecutadas, sin fallos ni errores**.

### 6. Preparación para ejecución local y contenedores

Se configuraron:

- Maven Wrapper para compilar sin instalar Maven manualmente.
- Java 25 como versión de compilación.
- MySQL 8.4 para persistencia.
- Adminer para inspección de la base de datos.
- Dockerfile multi-etapa para el backend.
- Docker Compose para levantar el entorno completo.
- Nginx como proxy del frontend.

### Ejecutar el frontend en desarrollo

Con el backend disponible en `http://localhost:8080`, ejecutá:

```powershell
cd frontend
npm install
npm run dev
```

Vite queda disponible en `http://localhost:5173` y reenvía `/api` al backend. Para otro host, definí `VITE_BACKEND_URL` antes de iniciar Vite. La compilación de producción usa `VITE_API_BASE_URL` (por defecto `/api`); el contenedor Nginx ya reenvía ese prefijo al servicio `backend`.

Docker Compose también levanta el frontend junto con la base de datos y el backend; no hace falta activar perfiles. Antes del primer inicio, completá en `.env` la clave JWT y las cuatro contraseñas iniciales. Las cuentas son `admin1@corralon.local`, `admin2@corralon.local`, `cliente1@corralon.local` y `cliente2@corralon.local`; consultá sus contraseñas en las variables `SEED_*_PASSWORD` de tu `.env` local. Las contraseñas deben ser distintas, tener entre 8 y 72 bytes y no se guardan en el repositorio:

```powershell
docker compose up -d --build
```

La tienda queda disponible en `http://localhost:3000` (o en el puerto configurado por `FRONTEND_PORT`). El inicio de sesión usa un JWT en una cookie `HttpOnly`; el navegador y la API se sirven desde el mismo origen mediante Nginx.

La referencia de endpoints, parámetros y cuerpos JSON está en [API & Service Communication Contracts](./.github/modernize/assessment/engines/facts/api-service-contracts.md), incluyendo los roles requeridos y los usuarios de prueba.

## Funcionalidades por área

### Usuarios

- Registro de clientes.
- Solicitud pública de cuenta vendedora, inicialmente inactiva y pendiente de aprobación.
- Aprobación administrativa de vendedores desde el panel de administración.
- Creación de usuarios por parte de un administrador.
- Normalización y validación de emails.
- Encriptación de contraseñas mediante BCrypt.
- Consulta y actualización del perfil.
- Listado paginado y filtrado por rol.
- Cambio de estado de usuarios.
- Actualización y eliminación de la foto del usuario autenticado.

### Catálogo

- Alta, modificación y cambio de estado de categorías.
- Listado de categorías activas y listado completo.
- Alta, modificación y cambio de estado de subcategorías.
- Listado de subcategorías por categoría.
- Alta y modificación de productos.
- Consulta pública de productos activos.
- Consulta administrativa incluyendo productos inactivos.
- Búsqueda paginada por filtros.
- Actualización y eliminación de imágenes de productos.
- Publicación de productos por vendedores con asignación de categoría y subcategoría.
- Revisión administrativa antes de mostrar publicaciones nuevas en el catálogo.

### Carrito

- Creación automática del carrito de un cliente cuando corresponde.
- Consulta del carrito y cálculo del total.
- Agregado de productos.
- Suma de cantidades de un producto ya existente.
- Actualización de cantidad de un ítem.
- Eliminación de ítems.
- Vaciado del carrito.
- Validación de producto activo y stock disponible.

### Pedidos

- Confirmación de pedidos desde el carrito.
- Validación de cliente y productos.
- Validación de productos activos.
- Control y descuento de stock.
- Consulta paginada de pedidos propios.
- Consulta administrativa y filtrado de pedidos.
- Cambios de estado con validación de transiciones permitidas.

### Pagos y comprobantes

- Registro de pagos por cliente o administrador.
- Consulta de pagos por pedido.
- Cambio del estado de un pago.
- Validación de pagos ya procesados.
- Aprobación y rechazo de pagos.
- Emisión de comprobantes internos para pagos aprobados.
- Consulta de comprobantes propios y administrativa.

### Manejo de errores

`GlobalExceptionHandler` transforma las excepciones del dominio en respuestas consistentes:

- `404 Not Found`: recurso inexistente.
- `400 Bad Request`: datos inválidos o operación inválida.
- `409 Conflict`: duplicados, stock insuficiente, producto no disponible o transición no permitida.
- `502 Bad Gateway`: indisponibilidad del servicio de imágenes.

## Endpoints REST disponibles actualmente

### Registro y publicaciones de vendedor

```text
POST /api/auth/registro-vendedor
GET  /api/vendedor/productos
POST /api/vendedor/productos
PUT  /api/vendedor/productos/{idProducto}
PUT  /api/vendedor/productos/{idProducto}/imagen
```

La solicitud de vendedor se aprueba con `PATCH /api/admin/usuarios/{idUsuario}/estado` y el body `{"activo":true}`. Los productos enviados por vendedores quedan inactivos hasta que un administrador los apruebe con `PATCH /api/admin/productos/{idProducto}/activar`. Los vendedores solo pueden modificar sus propias publicaciones; al editarlas vuelven a quedar pendientes de revisión.

### Foto del usuario autenticado

```text
PUT    /api/usuarios/me/foto
DELETE /api/usuarios/me/foto
```

El `PUT` recibe un formulario `multipart/form-data` con el campo `archivo`.

### Imagen de producto para administrador

```text
PUT    /api/admin/productos/{idProducto}/imagen
DELETE /api/admin/productos/{idProducto}/imagen
```

El `PUT` recibe un formulario `multipart/form-data` con el campo `archivo` y requiere el rol `ADMIN`.

## Arquitectura del proyecto

```text
.
├── backend
│   ├── pom.xml
│   ├── mvnw
│   ├── Dockerfile
│   └── src
│       ├── main
│       │   ├── java/e/commerce/digital
│       │   │   ├── config
│       │   │   ├── controller
│       │   │   ├── dto
│       │   │   ├── exception
│       │   │   ├── mapper
│       │   │   ├── models
│       │   │   ├── repository
│       │   │   ├── service
│       │   │   └── util
│       │   └── resources/application.properties
│       └── test/java/e/commerce/digital
│           └── service
├── frontend
│   ├── Dockerfile
│   ├── nginx.conf
│   └── package.json
├── docker-compose.yml
├── .env.example
└── README.md
```

## Tecnologías utilizadas

### Backend

- Java 25.
- Spring Boot 4.1.1.
- Spring Web MVC.
- Spring Data JPA.
- Spring Security.
- Spring Validation.
- Spring Cloud Gateway Server MVC.
- MySQL Connector/J.
- Lombok.
- Cloudinary HTTP5.

### Pruebas

- JUnit 5.
- Mockito.
- AssertJ.
- Spring Boot Test.

### Infraestructura

- Maven Wrapper.
- Docker y Docker Compose.
- MySQL 8.4.
- Adminer.
- Eclipse Temurin JDK/JRE 25.
- Nginx para servir el frontend y reenviar las solicitudes a la API.

## Requisitos previos

Para ejecutar el backend de forma local:

- JDK 25.
- Una instancia de MySQL accesible.
- PowerShell o una terminal compatible con el Maven Wrapper.

Para ejecutar el entorno completo:

- Docker Desktop con Docker Compose habilitado.

## Configuración de variables de entorno

El archivo [.env.example](./.env.example) contiene la plantilla de configuración. Para crear la configuración local:

```powershell
Copy-Item .env.example .env
```

Completar, como mínimo, las credenciales de MySQL:

```dotenv
DB_NAME=ferreteria
DB_USER=ferreteria_user
DB_PASSWORD=una-contraseña-segura
DB_ROOT_PASSWORD=otra-contraseña-segura
```

Variables disponibles:

| Variable | Uso | Valor habitual |
|---|---|---|
| `DB_NAME` | Nombre de la base de datos | `ferreteria` |
| `DB_USER` | Usuario de MySQL | `ferreteria_user` |
| `DB_PASSWORD` | Contraseña del usuario de MySQL | Obligatoria con Docker Compose |
| `DB_ROOT_PASSWORD` | Contraseña root de MySQL | Obligatoria con Docker Compose |
| `MYSQL_HOST_PORT` | Puerto local publicado por MySQL | `3308` |
| `ADMINER_PORT` | Puerto local de Adminer | `8081` |
| `BACKEND_PORT` | Puerto local del backend | `8080` |
| `FRONTEND_PORT` | Puerto local del frontend | `3000` |
| `JPA_DDL_AUTO` | Estrategia de generación/actualización de tablas | `update` |
| `CLOUDINARY_CLOUD_NAME` | Cloud de Cloudinary | Vacío si no se usa |
| `CLOUDINARY_API_KEY` | API key de Cloudinary | Vacío si no se usa |
| `CLOUDINARY_API_SECRET` | API secret de Cloudinary | Vacío si no se usa |
| `TZ` | Zona horaria de la aplicación | `America/Argentina/Jujuy` |
| `JWT_SECRET` | Clave de firma JWT en Base64 (mínimo 32 bytes) | Obligatoria con Docker Compose |
| `JWT_EXPIRATION_MINUTES` | Duración de cada JWT | `60` |
| `AUTH_COOKIE_SECURE` | Restringe la cookie a HTTPS | `false` en localhost; `true` en producción |
| `SEED_ADMIN1_EMAIL/PASSWORD` | Credenciales de la primera cuenta ADMIN | Definir en `.env` |
| `SEED_ADMIN2_EMAIL/PASSWORD` | Credenciales de la segunda cuenta ADMIN | Definir en `.env` |
| `SEED_RESET_ADMIN_PASSWORDS` | Restablece las contraseñas de las cuentas ADMIN iniciales usando el `.env` | `false`; activar solo para un reinicio de recuperación |
| `SEED_CLIENT1_EMAIL/PASSWORD` | Credenciales de la primera cuenta CLIENTE | Definir en `.env` |
| `SEED_CLIENT2_EMAIL/PASSWORD` | Credenciales de la segunda cuenta CLIENTE | Definir en `.env` |

No subir `.env` al repositorio ni colocar credenciales reales en el README.

Para recuperar el acceso de `admin1@corralon.local` o `admin2@corralon.local`, definí una contraseña nueva en el `.env`, activá temporalmente `SEED_RESET_ADMIN_PASSWORDS=true` y reiniciá el backend una vez. Luego volvé a dejarlo en `false`; iniciá sesión en `/ingresar` con el email y la contraseña definidos. Esta opción no cambia contraseñas de clientes.

## Ejecución local del backend

Desde la terminal integrada de Visual Studio Code:

```powershell
cd F:\proyecto-e-commerce\backend
.\mvnw.cmd spring-boot:run
```

El backend queda disponible en:

```text
http://localhost:8080
```

La configuración usa por defecto:

- Host de MySQL: `localhost`.
- Puerto de MySQL: `3306`.
- Base de datos: `ferreteria`.
- `spring.jpa.hibernate.ddl-auto=update`.

Si la base de datos utiliza otros valores, definir `DB_HOST`, `DB_PORT`, `DB_NAME`, `DB_USER` y `DB_PASSWORD` en el entorno antes de arrancar.

## Ejecución con Docker Compose

Desde la raíz del proyecto:

```powershell
docker compose up -d --build
```

Este comando levanta:

| Servicio | Dirección |
|---|---|
| MySQL | `localhost:3308` |
| Adminer | `http://localhost:8081` |
| Backend | `http://localhost:8080` |
| MySQL (host) | `localhost:3308` |

El frontend queda disponible en:

```text
http://localhost:3000
```

Comandos útiles:

```powershell
# Ver los servicios activos
docker compose ps

# Ver los logs del backend
docker compose logs -f backend

# Detener los contenedores y conservar los datos
docker compose down

# Detener los contenedores y eliminar también el volumen de MySQL
docker compose down -v
```

## Ejecución de pruebas

Los tests deben ejecutarse con Maven para que se incluyan automáticamente:

- Las clases de `src/main/java`.
- Las dependencias de Spring.
- JUnit 5.
- Mockito.
- AssertJ.
- Los procesadores de anotaciones de Lombok.

Desde `F:\proyecto-e-commerce\backend`:

```powershell
# Todas las pruebas
.\mvnw.cmd test
```

Pruebas individuales:

```powershell
.\mvnw.cmd -Dtest=CategoriaServiceTest test
.\mvnw.cmd -Dtest=ComprobanteServiceTest test
.\mvnw.cmd -Dtest=UsuarioServiceTest test
.\mvnw.cmd -Dtest=CarritoServiceTest test
.\mvnw.cmd -Dtest=ImagenServiceTest test
.\mvnw.cmd -Dtest=PagoServiceTest test
.\mvnw.cmd -Dtest=PedidoServiceTest test
.\mvnw.cmd -Dtest=ProductoImagenServiceTest test
.\mvnw.cmd -Dtest=ProductoServiceTest test
.\mvnw.cmd -Dtest=SubcategoriaServiceTest test
.\mvnw.cmd -Dtest=UsuarioImagenServiceTest test
.\mvnw.cmd -Dtest=DigitalApplicationTests test
```

No se deben ejecutar estos tests con:

```powershell
javac NombreDelTest.java
java NombreDelTest
```

Ese flujo compila únicamente el archivo abierto, no agrega el classpath de Maven y produce errores como `package ... does not exist` o `cannot find symbol`. En Visual Studio Code se puede utilizar el botón **Run Test** del explorador de pruebas Java, siempre que el proyecto se haya abierto desde la carpeta `backend` o se ejecute mediante Maven.

## Matriz de pruebas

| Clase | Área | Casos registrados |
|---|---|---:|
| `DigitalApplicationTests` | Contexto de la aplicación | 1 |
| `CategoriaServiceTest` | Categorías | 11 |
| `SubcategoriaServiceTest` | Subcategorías | 11 |
| `ProductoServiceTest` | Productos | 13 |
| `ProductoImagenServiceTest` | Imágenes de productos | 5 |
| `CarritoServiceTest` | Carrito | 16 |
| `PedidoServiceTest` | Pedidos | 32 |
| `PagoServiceTest` | Pagos | 29 |
| `ComprobanteServiceTest` | Comprobantes | 7 |
| `UsuarioServiceTest` | Usuarios | 14 |
| `ImagenServiceTest` | Cloudinary y validación de archivos | 15 |
| `UsuarioImagenServiceTest` | Fotos de usuarios | 6 |
| **Total** |  | **160** |

Las pruebas cubren tanto flujos correctos como errores de negocio: recursos inexistentes, duplicados, stock insuficiente, productos inactivos, estados no permitidos, pagos repetidos, autorización por propiedad y fallos controlados del servicio de imágenes.

## Persistencia y datos

En desarrollo, Hibernate utiliza `ddl-auto=update` para crear o ajustar las tablas a partir de las entidades. Esta configuración es conveniente para desarrollo y pruebas, pero debe revisarse antes de un entorno productivo.

Al iniciar, el backend también actualiza las restricciones MySQL obsoletas de `usuarios.rol` y `usuarios.tipo_usuario` para aceptar los roles actuales, incluido `VENDEDOR`; conserva los demás controles y los datos existentes.

La base de datos MySQL de Docker utiliza un volumen persistente llamado `ferreteria_mysql_data`. Por eso `docker compose down` conserva la información, mientras que `docker compose down -v` la elimina.

Adminer está disponible en `http://localhost:8081`. Cuando se ejecuta con Docker Compose, el servidor de base de datos es `mysql`, no `localhost`.

## Próximas etapas sugeridas

- Completar los controladores REST para autenticación, usuarios, catálogo, carrito, pedidos, pagos y comprobantes.
- Definir y documentar el contrato HTTP completo de la API.
- Integrar y validar el frontend con el backend.
- Incorporar pruebas de integración de endpoints.
- Incorporar migraciones versionadas de base de datos para reemplazar `ddl-auto=update` en producción.
- Completar la configuración de seguridad HTTP y los flujos de autenticación/autorización.
- Añadir documentación OpenAPI/Swagger.
- Definir perfiles separados para desarrollo, pruebas y producción.

## Licencia y credenciales

Este proyecto es de uso académico o interno mientras no se indique lo contrario. Las credenciales, claves de Cloudinary y archivos `.env` son información sensible y deben mantenerse fuera del control de versiones.