# Inventario de equipos informáticos

Aplicación para el inventario de equipos de COMPUTO TOTAL GMD S. A. Usa Java 17, servicios de negocio y almacenamiento en memoria. Los datos se reinician al cerrar la aplicación.

## Requisitos

- JDK 17 o superior.
- Maven 3.9 o superior.
- PowerShell abierto en la raiz del proyecto.

## Iniciar

```powershell
mvn clean compile
java -cp target/classes com.computototal.inventario.Main
```

Inicia sesión con una de las cuentas de demostración:

| Rol | Usuario | Contraseña |
|---|---|---|
| Administrador | `admin` | `admin-demo-2026` |
| Almacenero | `almacen` | `almacen-demo-2026` |
| Auditor | `auditor` | `auditor-demo-2026` |

Escribe el numero de la opción y pulsa Enter. Puedes seleccionar categorias, proveedores y ubicaciones desde las listas que muestra la consola. Para reportes, ingresa fechas con formato `AAAA-MM-DD`. `0` cierra sesión y, desde el menú de acceso, `0` termina la aplicación. La aplicación maneja el fin de entrada cerrando la sesión.

El Administrador y el Almacenero pueden registrar y operar inventario y configurar minimos. El Auditor puede consultar existencias, alertas, historial e informes. Las operaciones vuelven a comprobar permisos en los servicios.

## Demostración automática

Ejecuta los mismos servicios con reloj y datos reproducibles, sin entrada interactiva:

```powershell
java -cp target/classes com.computototal.inventario.Main --demo
```

Las verificaciones aisladas se pueden repetir con:

```powershell
java -cp target/classes com.computototal.inventario.demo.VerificacionDao
java -cp target/classes com.computototal.inventario.demo.VerificacionSeguridad
java -cp target/classes com.computototal.inventario.demo.VerificacionInventario
java -cp target/classes com.computototal.inventario.demo.VerificacionStockReportes
```

## Datos y persistencia

Los usuarios y catalogos de demostración se cargan al iniciar; los hashes y sales se generan aleatoriamente en el momento. No se guardan datos entre ejecuciones. [sql/schema.sql](sql/schema.sql) es una referencia de diseño MySQL y no se ejecuta ni se usa para conectarse a una base de datos.

No incluye GUI, JDBC, JUnit ni dependencias de ejecución externas.
