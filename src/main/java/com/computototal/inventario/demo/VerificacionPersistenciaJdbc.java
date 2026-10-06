package com.computototal.inventario.demo;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;

import com.computototal.inventario.dao.jdbc.CategoriaDAOJdbc;
import com.computototal.inventario.dao.jdbc.ConfiguracionStockMinimoDAOJdbc;
import com.computototal.inventario.dao.jdbc.EquipoDAOJdbc;
import com.computototal.inventario.dao.jdbc.HsqlDatabase;
import com.computototal.inventario.dao.jdbc.MovimientoDAOJdbc;
import com.computototal.inventario.dao.jdbc.ProveedorDAOJdbc;
import com.computototal.inventario.dao.jdbc.UbicacionDAOJdbc;
import com.computototal.inventario.dao.jdbc.UsuarioDAOJdbc;
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

public final class VerificacionPersistenciaJdbc {
    private VerificacionPersistenciaJdbc() {
    }

    public static void main(String[] args) throws Exception {
        Path directorio = Files.createTempDirectory("inventario-hsqldb-");
        String url = "jdbc:hsqldb:file:" + directorio.resolve("persistencia").toAbsolutePath()
                .toString().replace('\\', '/') + ";shutdown=true";
        UUID categoriaId = UUID.randomUUID();
        UUID proveedorId = UUID.randomUUID();
        UUID ubicacionId = UUID.randomUUID();
        UUID usuarioId = UUID.randomUUID();
        UUID equipoId = UUID.randomUUID();
        UUID movimientoId = UUID.randomUUID();
        String codigo = "EQ-" + equipoId.toString().substring(0, 8);
        String serie = "SN-" + equipoId.toString().substring(0, 8);

        try {
            guardarDatos(url, categoriaId, proveedorId, ubicacionId, usuarioId, equipoId, movimientoId, codigo, serie);
            verificarDatosReabiertos(url, categoriaId, proveedorId, ubicacionId, usuarioId,
                    equipoId, movimientoId, codigo);
            System.out.println("OK: los registros JDBC sobreviven al cierre y reapertura de HSQLDB.");
        } finally {
            eliminarDirectorio(directorio);
        }
    }

    private static void guardarDatos(String url, UUID categoriaId, UUID proveedorId, UUID ubicacionId,
                                    UUID usuarioId, UUID equipoId, UUID movimientoId, String codigo, String serie)
            throws SQLException {
        try (HsqlDatabase base = HsqlDatabase.abrir(url, "SA", "")) {
            Categoria categoria = new Categoria(categoriaId, "Laptop", "Equipo portátil");
            Proveedor proveedor = new Proveedor(proveedorId, "Proveedor Uno", "999111222", "uno@example.com");
            Ubicacion ubicacion = new Ubicacion(ubicacionId, "Lima", "Almacén", "Inventario", "PB", true);
            Usuario usuario = new Usuario(usuarioId, "admin-jdbc", new byte[]{1, 2, 3},
                    new byte[]{4, 5, 6}, Rol.ADMINISTRADOR);
            Equipo equipo = new Equipo(equipoId, codigo, serie, "Lenovo", "ThinkPad", categoriaId,
                    proveedorId, EstadoEquipo.DISPONIBLE);
            equipo.registrarIngreso(ubicacion);

            new CategoriaDAOJdbc(base).insertar(categoria);
            new ProveedorDAOJdbc(base).insertar(proveedor);
            new UbicacionDAOJdbc(base).insertar(ubicacion);
            new UsuarioDAOJdbc(base).insertar(usuario);
            new EquipoDAOJdbc(base).insertar(equipo);
            new ConfiguracionStockMinimoDAOJdbc(base).guardar(
                    new ConfiguracionStockMinimo(UUID.randomUUID(), categoriaId, "Lima", 4));
            new MovimientoDAOJdbc(base).insertar(crearMovimiento(movimientoId, usuarioId, equipoId,
                    categoriaId, ubicacionId, codigo, serie));
        }
    }

    private static void verificarDatosReabiertos(String url, UUID categoriaId, UUID proveedorId, UUID ubicacionId,
                                                  UUID usuarioId, UUID equipoId, UUID movimientoId, String codigo)
            throws SQLException {
        try (HsqlDatabase base = HsqlDatabase.abrir(url, "SA", "")) {
            exigir(new CategoriaDAOJdbc(base).buscarPorId(categoriaId).isPresent(), "categoría");
            exigir(new ProveedorDAOJdbc(base).buscarPorId(proveedorId).isPresent(), "proveedor");
            exigir(new UbicacionDAOJdbc(base).buscarPorId(ubicacionId).isPresent(), "ubicación");
            exigir(new UsuarioDAOJdbc(base).buscarPorId(usuarioId).isPresent(), "usuario");
            exigir(new ConfiguracionStockMinimoDAOJdbc(base).listar().get(0).getMinimo() == 4, "mínimo de stock");
            Equipo equipo = new EquipoDAOJdbc(base).buscarPorCodigo(" " + codigo.toLowerCase() + " ").orElseThrow();
            exigir(equipo.getEstado() == EstadoEquipo.DISPONIBLE
                    && equipo.getUbicacionActualId().filter(ubicacionId::equals).isPresent(), "estado de equipo");
            Movimiento movimiento = new MovimientoDAOJdbc(base).buscarPorId(movimientoId).orElseThrow();
            exigir(movimiento.getResponsableId().equals(usuarioId)
                    && movimiento.getEquipo().id().equals(equipoId)
                    && movimiento.getUbicacionDestino().map(Movimiento.InstantaneaUbicacion::id)
                            .filter(ubicacionId::equals).isPresent(), "historial e instantáneas");
        }
    }

    private static Movimiento crearMovimiento(UUID movimientoId, UUID usuarioId, UUID equipoId, UUID categoriaId,
                                               UUID ubicacionId, String codigo, String serie) {
        Movimiento.InstantaneaEquipo equipo = new Movimiento.InstantaneaEquipo(equipoId, categoriaId, codigo, serie,
                "Lenovo", "ThinkPad", "Laptop", "Proveedor Uno");
        Movimiento.InstantaneaUbicacion ubicacion = new Movimiento.InstantaneaUbicacion(ubicacionId, "Lima",
                "Almacén", "Inventario", "PB", true);
        return new Movimiento(movimientoId, LocalDateTime.of(2026, 10, 5, 10, 0), TipoMovimiento.INGRESO,
                "Ingreso de prueba", equipo, usuarioId, "admin-jdbc", Optional.empty(), Optional.of(ubicacion),
                Optional.empty(), Optional.of(EstadoEquipo.EN_USO), Optional.of(EstadoEquipo.DISPONIBLE), false, true);
    }

    private static void exigir(boolean condicion, String dato) {
        if (!condicion) {
            throw new AssertionError("No persistió " + dato);
        }
    }

    private static void eliminarDirectorio(Path directorio) throws IOException {
        try (Stream<Path> rutas = Files.walk(directorio)) {
            rutas.sorted(Comparator.reverseOrder()).forEach(ruta -> {
                try {
                    Files.deleteIfExists(ruta);
                } catch (IOException e) {
                    throw new UncheckedIOException(e);
                }
            });
        } catch (UncheckedIOException e) {
            throw e.getCause();
        }
    }
}