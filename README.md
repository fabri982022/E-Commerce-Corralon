# Ferretería Digital

Backend de una plataforma de comercio electrónico para una ferretería o corralón. El proyecto modela el catálogo de productos, la gestión de usuarios, el carrito de compras, los pedidos, los pagos, los comprobantes y la carga de imágenes.

La aplicación está organizada como un backend Java con Spring Boot y una persistencia relacional en MySQL. La lógica de negocio se concentra en servicios independientes, con repositorios JPA, DTOs de entrada/salida, mappers y excepciones de dominio.

## Estado actual

El proyecto se encuentra en una etapa funcional de backend y validación de la lógica de negocio:

- El dominio y el modelo de datos principal están definidos como entidades JPA.
- Las operaciones de usuarios, catálogo, carrito, pedidos, pagos, comprobantes e imágenes están implementadas en la capa de servicios.
- La integración de imágenes utiliza Cloudinary.
- La gestión de imágenes tiene controladores REST disponibles.
- La lógica de negocio cuenta con pruebas unitarias de servicios usando JUnit 5, Mockito y AssertJ.
- La configuración de Docker Compose contempla MySQL, Adminer, backend y un frontend opcional.

> Importante: actualmente los controladores REST visibles en el backend están concentrados en la gestión de imágenes. Las operaciones de usuarios, catálogo, carrito, pedidos, pagos y comprobantes están implementadas y probadas principalmente en la capa de servicios; la exposición completa de esos casos de uso mediante controladores REST es una etapa posterior de integración.

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

- `Usuario`, `UsuarioCliente` y `UsuarioAdmin`.
- `Categoria` y `Subcategoria`.
- `Producto`.
- `Carrito` y `CarritoItem`.
- `Pedido` y `DetallePedido`.
- `Pago`.
- `Comprobante`.

También se definieron enumeraciones para los roles y estados del sistema:

- Roles: `ADMIN` y `CLIENTE`.
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

La carga y eliminación de imágenes se delega a Cloudinary. El flujo contempla:

- Validación del tamaño máximo de archivos.
- Generación y almacenamiento de URL y `public_id`.
- Actualización de la imagen de perfil del usuario autenticado.
- Actualización o eliminación de la imagen de un producto por parte de un administrador.
- Eliminación diferida de imágenes anteriores al finalizar correctamente una transacción.
- Traducción de fallas de Cloudinary a respuestas HTTP controladas.

### 5. Validación mediante pruebas

Se agregaron pruebas unitarias para la lógica de servicios. Las pruebas utilizan mocks para repositorios y servicios externos, por lo que no dependen de una base de datos real ni de Cloudinary para validar las reglas principales.

En los resultados Maven disponibles en `backend/target/surefire-reports` se registran **160 pruebas ejecutadas, sin fallos ni errores**.

### 6. Preparación para ejecución local y contenedores

Se configuraron:

- Maven Wrapper para compilar sin instalar Maven manualmente.
- Java 25 como versión de compilación.
- MySQL 8.4 para persistencia.
- Adminer para inspección de la base de datos.
- Dockerfile multi-etapa para el backend.
- Docker Compose para levantar el entorno completo.
- Nginx como proxy del frontend opcional.

## Funcionalidades por área

### Usuarios

- Registro de clientes.
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
│   └── nginx.conf
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
- Nginx para el frontend opcional.

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
| `MYSQL_HOST_PORT` | Puerto local publicado por MySQL | `3307` |
| `ADMINER_PORT` | Puerto local de Adminer | `8081` |
| `BACKEND_PORT` | Puerto local del backend | `8080` |
| `FRONTEND_PORT` | Puerto local del frontend | `3000` |
| `JPA_DDL_AUTO` | Estrategia de generación/actualización de tablas | `update` |
| `CLOUDINARY_CLOUD_NAME` | Cloud de Cloudinary | Vacío si no se usa |
| `CLOUDINARY_API_KEY` | API key de Cloudinary | Vacío si no se usa |
| `CLOUDINARY_API_SECRET` | API secret de Cloudinary | Vacío si no se usa |
| `TZ` | Zona horaria de la aplicación | `America/Argentina/Jujuy` |

No subir `.env` al repositorio ni colocar credenciales reales en el README.

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
| MySQL | `localhost:3307` |
| Adminer | `http://localhost:8081` |
| Backend | `http://localhost:8080` |

Para incluir también el frontend opcional:

```powershell
docker compose --profile frontend up -d --build
```

El frontend, si está preparado, queda disponible en:

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