# Inventario de equipos informáticos

Aplicación para el inventario de equipos de COMPUTO TOTAL GMD S. A. Usa Java 17, servicios de negocio y almacenamiento en memoria. Los datos se reinician al cerrar la aplicación.

## Requisitos

- JDK 17 o superior.
- Maven 3.9 o superior.
- PowerShell abierto en la raiz del proyecto.

## Iniciar

La aplicación abre una ventana gráfica para iniciar sesión y luego muestra el panel de inventario. Las opciones disponibles dependen del rol. Al seleccionar un módulo, la ventana anterior se cierra y se abre la del módulo elegido; el panel principal permite volver a navegar.

```powershell
mvn clean compile
java -cp target/classes com.computototal.inventario.Main
```

Usa una de estas cuentas de demostración en la ventana de acceso:

| Rol | Usuario | Contraseña |
|---|---|---|
| Administrador | `admin` | `admin-demo-2026` |
| Almacenero | `almacen` | `almacen-demo-2026` |
| Auditor | `auditor` | `auditor-demo-2026` |

Los formularios permiten registrar y consultar equipos, gestionar movimientos, revisar stock y alertas, consultar historiales y generar reportes. El menú ofrece anular altas con un único ingreso inicial; la anulación conserva el historial y solicita un motivo. Los equipos anulados no aparecen en las listas operativas, pero su historial sigue disponible. El número de serie debe tener entre 3 y 40 caracteres, usar letras, números, guion, punto, guion bajo o barra, y empezar y terminar con letra o número; se normaliza a mayúsculas. Para reportes, ingresa fechas con formato `AAAA-MM-DD`. El boton para cerrar sesión finaliza la aplicación.

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
