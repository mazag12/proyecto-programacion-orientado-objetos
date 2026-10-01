package com.computototal.inventario.demo;

import com.computototal.inventario.dao.CategoriaDAO;
import com.computototal.inventario.dao.ConfiguracionStockMinimoDAO;
import com.computototal.inventario.dao.EquipoDAO;
import com.computototal.inventario.dao.MovimientoDAO;
import com.computototal.inventario.dao.ProveedorDAO;
import com.computototal.inventario.dao.UbicacionDAO;
import com.computototal.inventario.dao.UsuarioDAO;
import com.computototal.inventario.dao.memoria.CategoriaDAOMemoria;
import com.computototal.inventario.dao.memoria.ConfiguracionStockMinimoDAOMemoria;
import com.computototal.inventario.dao.memoria.EquipoDAOMemoria;
import com.computototal.inventario.dao.memoria.MovimientoDAOMemoria;
import com.computototal.inventario.dao.memoria.ProveedorDAOMemoria;
import com.computototal.inventario.dao.memoria.UbicacionDAOMemoria;
import com.computototal.inventario.dao.memoria.UsuarioDAOMemoria;
import com.computototal.inventario.modelo.Categoria;
import com.computototal.inventario.modelo.ConfiguracionStockMinimo;
import com.computototal.inventario.modelo.Equipo;
import com.computototal.inventario.modelo.EstadoEquipo;
import com.computototal.inventario.modelo.Movimiento;
import com.computototal.inventario.modelo.Proveedor;
import com.computototal.inventario.modelo.Rol;
import com.computototal.inventario.modelo.TipoMovimiento;
import com.computototal.inventario.modelo.Ubicacion;
import com.computototal.inventario.modelo.Usuario;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

public final class VerificacionDao {
    private static int verificaciones;

    private VerificacionDao() {
    }

    public static void main(String[] args) {
        System.out.println("Verificacion local de DAOs en memoria");
        System.out.println("Usuario usa hash y sal ficticios; esta demo solo verifica almacenamiento, no autenticacion.");
        verificarEquipo();
        verificarCategoria();
        verificarProveedor();
        verificarUbicacion();
        verificarUsuario();
        verificarConfiguracionStockMinimo();
        verificarMovimiento();
        System.out.println("RESULTADO: " + verificaciones + " comprobaciones OK.");
    }

    private static void verificarEquipo() {
        EquipoDAO dao = new EquipoDAOMemoria();
        Ubicacion almacen = new Ubicacion(id(401), "Lima", "Almacen central", "Inventario", "PB", true);
        Equipo original = crearEquipo(id(101), "EQ-001", "SER-001");
        original.registrarIngreso(almacen);
        dao.insertar(original);

        comparar("Equipo insertado y encontrado por id", true, dao.buscarPorId(id(101)).isPresent());
        comparar("Busqueda de codigo normalizada", id(101), dao.buscarPorCodigo("  eq-001 ")
                .map(Equipo::getId).orElse(null));
        comparar("Busqueda de serie normalizada", id(101), dao.buscarPorNumeroSerie(" ser-001 ")
                .map(Equipo::getId).orElse(null));
        comparar("Listar equipos", 1, dao.listar().size());

        original.setMarca("Marca modificada fuera del DAO");
        original.registrarSalida("Sede externa");
        Equipo guardado = obtener("Equipo almacenado despues de mutar el original", dao.buscarPorId(id(101)));
        comparar("Insertar almacena copia independiente", "Lenovo", guardado.getMarca());
        comparar("Copia preserva presencia y ubicacion", true, guardado.isPresenteEnAlmacen()
                && guardado.getUbicacionActualId().orElseThrow().equals(almacen.getId()));
        comparar("Copia preserva destino aun ausente en original almacenado", false,
                guardado.getDestinoSalida().isPresent());

        Equipo obtenido = obtener("Buscar equipo para mutacion externa", dao.buscarPorId(id(101)));
        obtenido.setMarca("Mutacion de resultado de busqueda");
        comparar("Buscar devuelve copia independiente", "Lenovo",
                dao.buscarPorId(id(101)).map(Equipo::getMarca).orElse(null));

        Equipo listado = dao.listar().get(0);
        listado.setMarca("Mutacion de resultado de lista");
        comparar("Listar devuelve entidades independientes", "Lenovo",
                dao.buscarPorId(id(101)).map(Equipo::getMarca).orElse(null));
        esperarExcepcion("La lista no permite modificar la coleccion interna", UnsupportedOperationException.class,
                () -> dao.listar().clear());

        esperarExcepcion("Rechaza identificador duplicado", IllegalArgumentException.class,
                () -> dao.insertar(crearEquipo(id(101), "EQ-OTRO", "SER-OTRO")));
        esperarExcepcion("Rechaza codigo duplicado ignorando mayusculas y espacios", IllegalArgumentException.class,
                () -> dao.insertar(crearEquipo(id(102), " eq-001 ", "SER-002")));
        esperarExcepcion("Rechaza numero de serie duplicado", IllegalArgumentException.class,
                () -> dao.insertar(crearEquipo(id(102), "EQ-002", "ser-001")));
        esperarExcepcion("Rechaza actualizar equipo inexistente", NoSuchElementException.class,
                () -> dao.actualizar(crearEquipo(id(999), "EQ-999", "SER-999")));

        Equipo segundo = crearEquipo(id(102), "EQ-002", "SER-002");
        dao.insertar(segundo);
        esperarExcepcion("Actualizacion con codigo duplicado es rechazada", IllegalArgumentException.class,
                () -> dao.actualizar(crearEquipo(id(102), "EQ-001", "SER-002")));
        comparar("Actualizacion rechazada conserva el registro anterior", "EQ-002",
                dao.buscarPorId(id(102)).map(Equipo::getCodigo).orElse(null));

        Equipo actualizacion = obtener("Equipo para actualizacion", dao.buscarPorId(id(102)));
        actualizacion.setMarca("Dell");
        dao.actualizar(actualizacion);
        comparar("Actualizar excluye el propio registro de unicidad", "Dell",
                dao.buscarPorId(id(102)).map(Equipo::getMarca).orElse(null));
        dao.eliminar(id(102));
        comparar("Eliminar equipo", false, dao.buscarPorId(id(102)).isPresent());
    }

    private static void verificarCategoria() {
        CategoriaDAO dao = new CategoriaDAOMemoria();
        Categoria original = new Categoria(id(201), "Computadoras", "Equipos de computo");
        dao.insertar(original);
        original.setNombre("Cambio externo");
        comparar("Categoria insertada y buscada", "Computadoras",
                dao.buscarPorId(id(201)).map(Categoria::getNombre).orElse(null));

        Categoria encontrada = obtener("Categoria para mutacion externa", dao.buscarPorId(id(201)));
        encontrada.setDescripcion("Cambio local");
        comparar("Buscar categoria devuelve copia", "Equipos de computo",
                dao.buscarPorId(id(201)).map(Categoria::getDescripcion).orElse(null));
        comparar("Listar categorias", 1, dao.listar().size());

        Categoria actualizada = new Categoria(id(201), "Computadoras portatiles", "Descripcion actualizada");
        dao.actualizar(actualizada);
        comparar("Actualizar categoria", "Computadoras portatiles",
                dao.buscarPorId(id(201)).map(Categoria::getNombre).orElse(null));
        actualizada.setNombre("Cambio posterior a actualizar");
        comparar("Actualizar almacena copia independiente", "Computadoras portatiles",
                dao.buscarPorId(id(201)).map(Categoria::getNombre).orElse(null));
        dao.eliminar(id(201));
        comparar("Eliminar categoria", false, dao.buscarPorId(id(201)).isPresent());
    }

    private static void verificarProveedor() {
        ProveedorDAO dao = new ProveedorDAOMemoria();
        Proveedor original = new Proveedor(id(301), "Proveedor Uno", "999111222", "contacto@uno.pe");
        dao.insertar(original);
        original.setTelefono("000000000");
        comparar("Proveedor insertado y buscado", "999111222",
                dao.buscarPorId(id(301)).map(Proveedor::getTelefono).orElse(null));
        comparar("Listar proveedores", 1, dao.listar().size());

        Proveedor actualizada = new Proveedor(id(301), "Proveedor Uno", "999333444", "ventas@uno.pe");
        dao.actualizar(actualizada);
        comparar("Actualizar proveedor", "ventas@uno.pe",
                dao.buscarPorId(id(301)).map(Proveedor::getCorreoElectronico).orElse(null));
        dao.eliminar(id(301));
        comparar("Eliminar proveedor", false, dao.buscarPorId(id(301)).isPresent());
    }

    private static void verificarUbicacion() {
        UbicacionDAO dao = new UbicacionDAOMemoria();
        Ubicacion original = new Ubicacion(id(401), "Lima", "Almacen central", "Inventario", "PB", true);
        dao.insertar(original);
        original.setArea("Cambio externo");
        comparar("Ubicacion insertada y buscada", "Inventario",
                dao.buscarPorId(id(401)).map(Ubicacion::getArea).orElse(null));
        Ubicacion encontrada = obtener("Ubicacion para mutacion externa", dao.buscarPorId(id(401)));
        encontrada.setPiso("Sotano");
        comparar("Buscar ubicacion devuelve copia", "PB",
                dao.buscarPorId(id(401)).map(Ubicacion::getPiso).orElse(null));
        comparar("Listar ubicaciones", 1, dao.listar().size());

        Ubicacion actualizada = new Ubicacion(id(401), "Lima", "Almacen central", "Activos", "PB", true);
        dao.actualizar(actualizada);
        comparar("Actualizar ubicacion", "Activos", dao.buscarPorId(id(401)).map(Ubicacion::getArea).orElse(null));
        dao.eliminar(id(401));
        comparar("Eliminar ubicacion", false, dao.buscarPorId(id(401)).isPresent());
    }

    private static void verificarUsuario() {
        UsuarioDAO dao = new UsuarioDAOMemoria();
        byte[] hashEntrada = {1, 2, 3, 4};
        byte[] salEntrada = {5, 6, 7, 8};
        Usuario original = new Usuario(id(501), "almacen", hashEntrada, salEntrada, Rol.ALMACENERO);
        hashEntrada[0] = 99;
        salEntrada[0] = 99;
        comparar("Usuario copia hash y sal recibidos", true,
                Arrays.equals(new byte[]{1, 2, 3, 4}, original.getHashContrasena())
                        && Arrays.equals(new byte[]{5, 6, 7, 8}, original.getSalContrasena()));
        dao.insertar(original);
        original.setRol(Rol.AUDITOR);

        comparar("Buscar usuario ignora mayusculas y espacios", Rol.ALMACENERO,
                dao.buscarPorNombreUsuario(" ALMACEN ").map(Usuario::getRol).orElse(null));
        Usuario encontrado = obtener("Usuario para mutacion externa", dao.buscarPorId(id(501)));
        encontrado.setRol(Rol.ADMINISTRADOR);
        encontrado.getHashContrasena()[0] = 88;
        comparar("Getter de hash devuelve copia defensiva", true,
                Arrays.equals(new byte[]{1, 2, 3, 4}, dao.buscarPorId(id(501))
                        .orElseThrow().getHashContrasena()));
        comparar("Buscar usuario devuelve copia y bytes defensivos", true,
                dao.buscarPorId(id(501)).map(usuario -> usuario.getRol() == Rol.ALMACENERO
                        && Arrays.equals(new byte[]{1, 2, 3, 4}, usuario.getHashContrasena())).orElse(false));
        Usuario listado = dao.listar().get(0);
        listado.setNombreUsuario("cambio-local");
        comparar("Listar usuarios devuelve copias", "almacen",
                dao.buscarPorId(id(501)).map(Usuario::getNombreUsuario).orElse(null));

        esperarExcepcion("Rechaza nombre de usuario duplicado normalizado", IllegalArgumentException.class,
                () -> dao.insertar(new Usuario(id(502), " ALMACEN ", new byte[]{1}, new byte[]{2}, Rol.AUDITOR)));
        comparar("Listar usuarios", 1, dao.listar().size());

        Usuario actualizacion = obtener("Usuario para actualizacion", dao.buscarPorId(id(501)));
        actualizacion.setRol(Rol.ADMINISTRADOR);
        dao.actualizar(actualizacion);
        comparar("Actualizar usuario excluye el propio nombre", Rol.ADMINISTRADOR,
                dao.buscarPorId(id(501)).map(Usuario::getRol).orElse(null));
        dao.eliminar(id(501));
        comparar("Eliminar usuario", false, dao.buscarPorId(id(501)).isPresent());
    }

    private static void verificarConfiguracionStockMinimo() {
        ConfiguracionStockMinimoDAO dao = new ConfiguracionStockMinimoDAOMemoria();
        ConfiguracionStockMinimo original = new ConfiguracionStockMinimo(id(601), id(201), "Lima", 2);
        dao.guardar(original);
        original.setMinimo(9);
        comparar("Guardar configuracion almacena copia", 2,
                dao.buscarPorId(id(601)).map(ConfiguracionStockMinimo::getMinimo).orElse(null));
        comparar("Buscar minimo normaliza sede", id(601), dao.buscarPorCategoriaYSede(id(201), " lima ")
                .map(ConfiguracionStockMinimo::getId).orElse(null));
        esperarExcepcion("Rechaza segunda configuracion categoria-sede", IllegalArgumentException.class,
                () -> dao.guardar(new ConfiguracionStockMinimo(id(602), id(201), " LIMA ", 4)));
        comparar("Listar configuraciones", 1, dao.listar().size());

        ConfiguracionStockMinimo actualizacion = obtener("Minimo para actualizacion", dao.buscarPorId(id(601)));
        actualizacion.setMinimo(5);
        dao.actualizar(actualizacion);
        comparar("Actualizar minimo", 5,
                dao.buscarPorCategoriaYSede(id(201), "Lima").map(ConfiguracionStockMinimo::getMinimo).orElse(null));
        esperarExcepcion("Rechaza actualizar minimo inexistente", NoSuchElementException.class,
                () -> dao.actualizar(new ConfiguracionStockMinimo(id(699), id(201), "Lima", 1)));
        dao.eliminar(id(601));
        comparar("Eliminar configuracion", false, dao.buscarPorId(id(601)).isPresent());
    }

    private static void verificarMovimiento() {
        MovimientoDAO dao = new MovimientoDAOMemoria();
        UUID equipoUno = id(101);
        UUID equipoDos = id(102);
        UUID responsable = id(701);
        Movimiento.InstantaneaEquipo instantaneaUno = new Movimiento.InstantaneaEquipo(
                equipoUno, id(201), "EQ-001", "SER-001", "Lenovo", "T14", "Computadoras", "Proveedor Uno");
        Movimiento.InstantaneaEquipo instantaneaDos = new Movimiento.InstantaneaEquipo(
                equipoDos, id(201), "EQ-002", "SER-002", "Dell", "P1", "Computadoras", "Proveedor Uno");
        Movimiento.InstantaneaUbicacion almacenLima = new Movimiento.InstantaneaUbicacion(
                id(801), "Lima", "Almacen central", "Inventario", "PB", true);
        Movimiento.InstantaneaUbicacion almacenArequipa = new Movimiento.InstantaneaUbicacion(
                id(802), "Arequipa", "Almacen regional", "Inventario", "Sotano", true);
        LocalDateTime inicio = LocalDateTime.of(2026, 6, 1, 8, 0);
        LocalDateTime medio = LocalDateTime.of(2026, 6, 15, 12, 0);
        LocalDateTime fin = LocalDateTime.of(2026, 6, 30, 18, 0);

        Movimiento ingresoUno = new Movimiento(id(900), inicio, TipoMovimiento.INGRESO, "Ingreso inicial", instantaneaUno,
                responsable, "Operador", Optional.empty(), Optional.of(almacenLima), Optional.empty(),
                Optional.of(EstadoEquipo.DISPONIBLE), Optional.of(EstadoEquipo.DISPONIBLE), false, true);
        Movimiento salidaDos = new Movimiento(id(100), inicio, TipoMovimiento.SALIDA, "Entrega", instantaneaDos,
                responsable, "Operador", Optional.of(almacenLima), Optional.empty(), Optional.of("Sede externa"),
                Optional.of(EstadoEquipo.DISPONIBLE), Optional.of(EstadoEquipo.EN_USO), true, false);
        Movimiento cambioEstado = new Movimiento(id(500), medio, TipoMovimiento.CAMBIO_ESTADO, "Revision tecnica", instantaneaUno,
                responsable, "Operador", Optional.empty(), Optional.empty(), Optional.empty(),
                Optional.of(EstadoEquipo.DISPONIBLE), Optional.of(EstadoEquipo.EN_MANTENIMIENTO), true, true);
        Movimiento traslado = new Movimiento(id(800), fin, TipoMovimiento.TRASLADO, "Cambio de sede", instantaneaUno,
                responsable, "Operador", Optional.of(almacenLima), Optional.of(almacenArequipa), Optional.empty(),
                Optional.of(EstadoEquipo.DISPONIBLE), Optional.of(EstadoEquipo.DISPONIBLE), true, true);
        Movimiento fueraDeRango = new Movimiento(id(810), fin.plusSeconds(1), TipoMovimiento.CAMBIO_ESTADO, "Asignacion", 
                instantaneaUno, responsable, "Operador", Optional.empty(), Optional.empty(), Optional.empty(),
                Optional.of(EstadoEquipo.EN_MANTENIMIENTO), Optional.of(EstadoEquipo.EN_USO), true, true);

        dao.insertar(traslado);
        dao.insertar(ingresoUno);
        dao.insertar(cambioEstado);
        dao.insertar(salidaDos);
        dao.insertar(fueraDeRango);

        comparar("Busqueda de movimiento por id", true, dao.buscarPorId(id(900)).isPresent());
        comparar("Listar movimientos conserva el total", 5, dao.listar().size());
        comparar("Historial filtra por equipo y ordena por fecha", List.of(id(900), id(500), id(800), id(810)),
                idsDe(dao.listarPorEquipo(equipoUno)));
        comparar("Rango incluye ambos limites y conserva insercion en fechas iguales",
                List.of(id(900), id(100), id(500), id(800)), idsDe(dao.listarPorRangoFechas(inicio, fin)));
        comparar("Movimiento inexistente retorna Optional vacio", false, dao.buscarPorId(id(999)).isPresent());
        esperarExcepcion("Rechaza rango de fechas invertido", IllegalArgumentException.class,
                () -> dao.listarPorRangoFechas(fin, inicio));
        esperarExcepcion("Rechaza identificador de movimiento duplicado", IllegalArgumentException.class,
                () -> dao.insertar(ingresoUno));
    }

    private static Equipo crearEquipo(UUID id, String codigo, String serie) {
        return new Equipo(id, codigo, serie, "Lenovo", "T14", id(201), id(301), EstadoEquipo.DISPONIBLE);
    }

    private static UUID id(long numero) {
        return new UUID(0L, numero);
    }

    private static List<UUID> idsDe(List<Movimiento> movimientos) {
        return movimientos.stream().map(Movimiento::getId).toList();
    }

    private static <T> T obtener(String descripcion, Optional<T> valor) {
        comparar(descripcion, true, valor.isPresent());
        return valor.orElseThrow();
    }

    private static void esperarExcepcion(String descripcion, Class<? extends RuntimeException> esperada,
                                         Runnable operacion) {
        String obtenida = "ninguna";
        try {
            operacion.run();
        } catch (RuntimeException excepcion) {
            obtenida = excepcion.getClass().getSimpleName();
        }
        comparar(descripcion, esperada.getSimpleName(), obtenida);
    }

    private static void comparar(String descripcion, Object esperado, Object obtenido) {
        boolean correcto = Objects.equals(esperado, obtenido);
        System.out.println((correcto ? "OK" : "FALLO") + " | " + descripcion
                + " | esperado=" + esperado + " | obtenido=" + obtenido);
        if (!correcto) {
            throw new IllegalStateException("Verificacion fallida: " + descripcion);
        }
        verificaciones++;
    }
}